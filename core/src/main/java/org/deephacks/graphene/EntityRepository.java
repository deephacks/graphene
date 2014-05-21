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

import deephacks.streamql.Query;
import org.deephacks.graphene.internal.EntityClassWrapper;
import org.deephacks.graphene.internal.EntityValidator;
import org.deephacks.graphene.internal.FastKeyComparator;
import org.deephacks.graphene.internal.RowKey;
import org.deephacks.graphene.internal.Serializer;
import org.fusesource.lmdbjni.Constants;
import org.fusesource.lmdbjni.Cursor;
import org.fusesource.lmdbjni.Database;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.GetOp;
import org.fusesource.lmdbjni.SeekOp;
import org.fusesource.lmdbjni.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.deephacks.graphene.TransactionManager.*;

/**
 * EntityRepository will never commit or rollback a transactions.
 */
public class EntityRepository {
  private static final Handle<Graphene> graphene = Graphene.get();
  private static final Object WRITE_LOCK = new Object();
  private final Handle<Database> db;
  private final Handle<Database> secondary;
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
    Optional<byte[][]> optional = getKv(key, entityClass);
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
    Optional<byte[][]> optional = getKv(key, entityClass);
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
    Guavas.checkNotNull(entity);
    if (validator.isPresent()) {
      validator.get().validate(entity);
    }
    Class<?> entityClass = entity.getClass();
    byte[][] data = getSerializer(entityClass).serializeEntity(entity);
    if (data == null || data.length != 2) {
      throw new IllegalArgumentException("Could not serialize entity");
    }
    if (data[0].length == 0 && EntityClassWrapper.get(entityClass).isEmbedded()) {
      String msg = "Cannot store @Embedded classes " + entityClass.getName();
      throw new IllegalArgumentException(msg);
    }
    Transaction tx = getInternalTx();
    if (tx == null) {
      db.get().put(data[0], data[1]);
    } else {
      db.get().put(tx, data[0], data[1]);
    }
    return true;
  }

  /**
   * Put an instance if it does not exist. If the instance exist, nothing will be written.
   *
   * @param entity instance to be written
   * @param <E>    instance type
   * @return true if the instance did not exist, false otherwise.
   */
  public <E> boolean putNoOverwrite(E entity) {
    Guavas.checkNotNull(entity);
    if (validator.isPresent()) {
      validator.get().validate(entity);
    }
    Class<?> entityClass = entity.getClass();
    byte[][] data = getSerializer(entityClass).serializeEntity(entity);
    if (data == null || data.length != 2) {
      throw new IllegalArgumentException("Could not serialize entity");
    }
    if (data[0].length == 0 && EntityClassWrapper.get(entityClass).isEmbedded()) {
      String msg = "Cannot store @Embedded classes " + entityClass.getName();
      throw new IllegalArgumentException(msg);
    }
    return db.get().put(getInternalTx(), data[0], data[1], Constants.NOOVERWRITE) == null;
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
      final Optional<byte[][]> optional = getKv(key, entityClass);
      if (!optional.isPresent()) {
        return Optional.empty();
      }
      try {
        Transaction tx = getInternalTx();
        if (tx == null) {
          if (!db.get().delete(optional.get()[0])) {
            return Optional.empty();
          }
        } else {
          if (!db.get().delete(tx, optional.get()[0])) {
            return Optional.empty();
          }
        }
        return Optional.ofNullable((E) getSerializer(entityClass).deserializeEntity(optional.get()));
      } catch (DeleteConstraintException e) {
        /* FIXME
        throw new org.deephacks.graphene.DeleteConstraintException(
                new RowKey(e.getPrimaryKey().getData()) + " have a reference to " + new RowKey(e.getSecondaryKey().getData()), e);
        */
        throw new RuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> query(String query, Class<T> cls) {
    final Cursor cursor = openPrimaryCursor();
    Query<T> q = Query.parse(query, cls);
    StreamResultSet objects = new StreamResultSet<>(q.getType(), cursor);
    Spliterator spliterator = objects.spliterator();
    try (Stream stream = StreamSupport.stream(spliterator, false)) {
      stream.onClose(cursor::close);
      if (peek() != null) {
        push(cursor);
      }
      return q.collect(stream);
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
    if (peek() != null) {
      push(cursor);
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
    return withTxReturn(tx -> {
      try (Stream<E> stream = stream(entityClass)) {
        return stream.collect(Collectors.toList());
      }
    });
  }

  public void deleteAll(Class<?> entityClass) {
    synchronized (WRITE_LOCK) {
      try {
        try (Cursor cursor = openPrimaryCursor()) {
          byte[] firstKey = RowKey.getMinId(entityClass).getKey();
          byte[] lastKey = RowKey.getMaxId(entityClass).getKey();
          Entry entry = cursor.seek(SeekOp.KEY, firstKey);
          if (entry != null) {
            // important; we must respect the class prefix boundaries of the key
            if (!FastKeyComparator.withinKeyRange(entry.getKey(), firstKey, lastKey)) {
              return;
            }
            cursor.delete();
          } else {
            return;
          }
          while ((entry = cursor.get(GetOp.NEXT)) != null) {
            // important; we must respect the class prefix boundaries of the key
            if (!FastKeyComparator.withinKeyRange(entry.getKey(), firstKey, lastKey)) {
              return;
            }
            cursor.delete();
          }
        }
      } catch (DeleteConstraintException e) {
        /* FIXME
        throw new org.deephacks.graphene.DeleteConstraintException(
                new RowKey(e.getPrimaryKey().getData()) + " have a reference to " + new RowKey(e.getSecondaryKey().getData()), e);
                */
      }
    }
  }

  public <E> Optional<byte[][]> getKv(Object key, Class<E> entityClass) {
    return getKv(new RowKey(entityClass, key));
  }

  public Optional<byte[][]> getKv(RowKey key) {
    byte[] dataKey = getSerializer(key.getClass()).serializeRowKey(key);
    Transaction tx = getInternalTx();
    byte[] value;
    if (tx == null) {
      if ((value = db.get().get(dataKey)) != null) {
        byte[][] kv = new byte[][]{dataKey, value};
        return Optional.ofNullable(kv);
      }
      return Optional.empty();
    } else {
      if ((value = db.get().get(tx, dataKey)) != null) {
        byte[][] kv = new byte[][]{dataKey, value};
        return Optional.ofNullable(kv);
      }
      return Optional.empty();
    }
  }

  Cursor openPrimaryCursor() {
    return db.get().openCursor(getInternalTx());
  }

  private Serializer getSerializer(Class<?> entityClass) {
    return graphene.get().getSerializer(entityClass);
  }

  /*
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
        Object value;
        if (!method.isCollection()) {
          value = reader.getValue(fieldId, header, DataType.STRING);
        } else {
          value = reader.getValues(fieldId, header, DataType.STRING);
        }
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
  */
}
