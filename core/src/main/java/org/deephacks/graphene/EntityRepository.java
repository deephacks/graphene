/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.graphene;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DeleteConstraintException;
import com.sleepycat.je.ForeignConstraintException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryMultiKeyCreator;
import com.sleepycat.je.Transaction;
import org.deephacks.graphene.internal.EntityClassWrapper;
import org.deephacks.graphene.internal.EntityClassWrapper.EntityMethodWrapper;
import org.deephacks.graphene.internal.EntityValidator;
import org.deephacks.graphene.internal.FastKeyComparator;
import org.deephacks.graphene.internal.RowKey;
import org.deephacks.graphene.internal.Serializer;
import org.deephacks.graphene.internal.UniqueIds;
import org.deephacks.graphene.internal.ValueSerialization.ValueReader;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * EntityRepository will never commit or rollback a transactions.
 */
public class EntityRepository {
  private static final Handle<Graphene> graphene = Graphene.get();
  private static final TransactionManager tm = graphene.get().getTransactionManager();
  private static final Object WRITE_LOCK = new Object();
  private final Handle<Database> db;
  private final Handle<SecondaryDatabase> secondary;
  private final Optional<EntityValidator> validator;

  public EntityRepository() {
    this.db = graphene.get().getPrimary();
    this.secondary = graphene.get().getSecondary();
    this.validator = graphene.get().getValidator();
  }

  /**
   * Get an instance from storage and acquire a shared read lock on this instance.
   * The lock will be held until the transaction commit or rollback.
   *
   * @param key         primary key of instance
   * @param entityClass instance type
   * @param <E>         instance type
   * @return the instance if it exist, otherwise absent
   */
  public <E> Optional<E> get(Object key, Class<E> entityClass) {
    Optional<byte[][]> optional = getKv(key, entityClass, LockMode.READ_COMMITTED);
    if (optional.isPresent()) {
      return Optional.ofNullable((E) getSerializer(entityClass).deserializeEntity(optional.get()));
    }
    return Optional.empty();
  }

  /**
   * Get instance from storage according and acquire an exclusive write lock on this instance.
   * The lock will be held until the transaction commit or rollback.
   *
   * @param key         primary key of instance
   * @param entityClass instance type
   * @param <E>         instance type
   * @return the instance if it exist, otherwise absent
   */
  public <E> Optional<E> getForUpdate(Object key, Class<E> entityClass) {
    Optional<byte[][]> optional = getKv(key, entityClass, LockMode.RMW);
    if (optional.isPresent()) {
      return Optional.ofNullable((E) getSerializer(entityClass).deserializeEntity(optional.get()));
    }
    return Optional.empty();
  }

  /**
   * Put provided instance in storage, overwrite if instance already exist.
   *
   * @param entity instance to be written
   * @param <E>    instance type
   * @return true if writing the instance was successful
   */
  public <E> boolean put(final E entity) {
    synchronized (WRITE_LOCK) {
      try {
        Guavas.checkNotNull(entity);
        if (validator.isPresent()) {
          validator.get().validate(entity);
        }
        Class<?> entityClass = entity.getClass();
        byte[][] data = getSerializer(entityClass).serializeEntity(entity);
        if (data == null || data.length != 2) {
          throw new IllegalArgumentException("Could not serialize entity");
        }
        DatabaseEntry key = new DatabaseEntry(data[0]);
        DatabaseEntry value = new DatabaseEntry(data[1]);
        if (OperationStatus.SUCCESS != db.get().put(tm.getInternalTx(), key, value)) {
          return false;
        }
        return true;
      } catch (ForeignConstraintException e) {
        throw new ForeignKeyConstraintException(e);
      }
    }
  }

  /**
   * Put an instance if it does not exist. If the instance exist, nothing will be written.
   *
   * @param entity instance to be written
   * @param <E>    instance type
   * @return true if the instance did not exist, false otherwise.
   */
  public <E> boolean putNoOverwrite(E entity) {
    synchronized (WRITE_LOCK) {
      try {
        Guavas.checkNotNull(entity);
        if (validator.isPresent()) {
          validator.get().validate(entity);
        }
        Class<?> entityClass = entity.getClass();
        byte[][] data = getSerializer(entityClass).serializeEntity(entity);
        if (data == null || data.length != 2) {
          throw new IllegalArgumentException("Could not serialize entity");
        }
        DatabaseEntry key = new DatabaseEntry(data[0]);
        DatabaseEntry value = new DatabaseEntry(data[1]);
        return OperationStatus.SUCCESS == db.get().putNoOverwrite(tm.getInternalTx(), key, value);
      } catch (ForeignConstraintException e) {
        throw new ForeignKeyConstraintException(e);
      }
    }
  }

  /**
   * Delete an instance with a specific primary key.
   *
   * @param key         primary key to delete
   * @param entityClass instance type to delete
   * @param <E>         instance type
   * @return the instance if it was deleted, otherwise absent
   * @throws DeleteConstraintException if another instance have a reference on the deleted instance
   */
  public <E> Optional<E> delete(Object key, Class<E> entityClass) throws DeleteConstraintException {
    synchronized (WRITE_LOCK) {
      final Optional<byte[][]> optional = getKv(key, entityClass, LockMode.RMW);
      if (!optional.isPresent()) {
        return Optional.empty();
      }
      try {
        OperationStatus status = db.get().delete(tm.getInternalTx(), new DatabaseEntry(optional.get()[0]));
        if (status == OperationStatus.NOTFOUND) {
          return Optional.empty();
        }
        return Optional.ofNullable((E) getSerializer(entityClass).deserializeEntity(optional.get()));
      } catch (DeleteConstraintException e) {
        throw new org.deephacks.graphene.DeleteConstraintException(
                new RowKey(e.getPrimaryKey().getData()) + " have a reference to " + new RowKey(e.getSecondaryKey().getData()), e);
      }
    }
  }

  /**
   * Stream instances.
   *
   * @param entityClass the type which is target for selection
   * @param <E>         instance type
   * @return stream of instances.
   */
  public <E> Stream<E> stream(Class<E> entityClass) {
    final Cursor cursor = openPrimaryCursor();
    StreamResultSet<E> objects = new StreamResultSet<>(entityClass, cursor);
    Spliterator<E> spliterator = Spliterators.spliterator(objects.iterator(), Long.MAX_VALUE, Spliterator.SIZED);
    Stream<E> stream = StreamSupport.stream(spliterator, false);
    stream.onClose(cursor::close);
    if (tm.peek() != null) {
      tm.push(cursor);
    }
    return stream;
  }

  /**
   * Select and return all instances.
   *
   * @param entityClass the type which is target for selection
   * @param <E>         instance type
   * @return instances match criteria
   */
  public <E> List<E> selectAll(Class<E> entityClass) {
    try (Stream<E> stream = stream(entityClass)) {
      return stream.collect(Collectors.toList());
    }
  }

  public void deleteAll(Class<?> entityClass) {
    synchronized (WRITE_LOCK) {
      try {
        try (Cursor cursor = openPrimaryCursor()) {
          byte[] firstKey = RowKey.getMinId(entityClass).getKey();
          byte[] lastKey = RowKey.getMaxId(entityClass).getKey();
          DatabaseEntry keyEntry = new DatabaseEntry(firstKey);
          if (cursor.getSearchKeyRange(keyEntry, new DatabaseEntry(), LockMode.RMW) == OperationStatus.SUCCESS) {
            // important; we must respect the class prefix boundaries of the key
            if (!FastKeyComparator.withinKeyRange(keyEntry.getData(), firstKey, lastKey)) {
              return;
            }
            cursor.delete();
          }
          while (cursor.getNextNoDup(keyEntry, new DatabaseEntry(), LockMode.RMW) == OperationStatus.SUCCESS) {
            // important; we must respect the class prefix boundaries of the key
            if (!FastKeyComparator.withinKeyRange(keyEntry.getData(), firstKey, lastKey)) {
              return;
            }
            cursor.delete();
          }
        }
      } catch (DeleteConstraintException e) {
        throw new org.deephacks.graphene.DeleteConstraintException(
                new RowKey(e.getPrimaryKey().getData()) + " have a reference to " + new RowKey(e.getSecondaryKey().getData()), e);
      }
    }
  }

  /**
   * Count all instances that exist in storage. The count may not be
   * accurate in the face of concurrent update operations in the database.
   *
   * @return total number of instance (all types included)
   */
  public long countAll() {
    return db.get().count();
  }

  public <E> Optional<byte[][]> getKv(Object key, Class<E> entityClass, LockMode mode) {
    return getKv(new RowKey(entityClass, key), mode);
  }

  public Optional<byte[][]> getKv(RowKey key, LockMode mode) {
    byte[] dataKey = getSerializer(key.getClass()).serializeRowKey(key);
    DatabaseEntry entryKey = new DatabaseEntry(dataKey);
    DatabaseEntry entryValue = new DatabaseEntry();
    Transaction tx = tm.getInternalTx();
    LockMode lockMode = tx == null ? LockMode.DEFAULT : mode;
    if (OperationStatus.SUCCESS == db.get().get(tx, entryKey, entryValue, lockMode)) {
      byte[][] kv = new byte[][]{entryKey.getData(), entryValue.getData()};
      return Optional.ofNullable(kv);
    }
    return Optional.empty();
  }

  Cursor openPrimaryCursor() {
    CursorConfig config = new CursorConfig();
    config.setReadCommitted(true);
    return db.get().openCursor(tm.getInternalTx(), config);
  }

  Cursor openForeignCursor() {
    CursorConfig config = new CursorConfig();
    config.setReadCommitted(true);
    return secondary.get().openCursor(tm.getInternalTx(), config);
  }

  private Serializer getSerializer(Class<?> entityClass) {
    return graphene.get().getSerializer(entityClass);
  }

  public static class KeyCreator implements SecondaryMultiKeyCreator {
    // private static final SchemaManager schemaManager = SchemaManager.lookup();
    private static final UniqueIds uniqueIds = new UniqueIds();

    public KeyCreator() {
    }

    @Override
    public void createSecondaryKeys(SecondaryDatabase secondary, DatabaseEntry key, DatabaseEntry data, Set<DatabaseEntry> results) {
      RowKey rowKey = new RowKey(key.getData());
      EntityClassWrapper cls = EntityClassWrapper.get(rowKey.getCls());

      ValueReader reader = new ValueReader(data.getData());
      int[][] header = reader.getHeader();
      for (EntityMethodWrapper method : cls.getReferences().values()) {
        int fieldId = uniqueIds.getSchemaId(method.getName());
        Object value = reader.getValue(fieldId, header);
        if (value != null) {
          if (value instanceof Collection) {
            // TODO: make string an LONG instance id instead
            for (String instanceId : (Collection<String>) value) {
              int schemaId = uniqueIds.getSchemaId(method.getType().getName());
              long iid = uniqueIds.getInstanceId(instanceId);
              results.add(new DatabaseEntry(RowKey.toKey(schemaId, iid)));
            }
          } else {
            long iid = uniqueIds.getInstanceId(value.toString());
            int schemaId = uniqueIds.getSchemaId(method.getType().getName());
            results.add(new DatabaseEntry(RowKey.toKey(schemaId, iid)));
          }
        }
      }
    }
  }
}
