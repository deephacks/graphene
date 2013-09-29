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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DeleteConstraintException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryMultiKeyCreator;
import com.sleepycat.je.Transaction;
import org.deephacks.graphene.Query.DefaultQuery;
import org.deephacks.graphene.internal.EntityClassWrapper;
import org.deephacks.graphene.internal.EntityClassWrapper.EntityFieldWrapper;
import org.deephacks.graphene.internal.FastKeyComparator;
import org.deephacks.graphene.internal.RowKey;
import org.deephacks.graphene.internal.Serializer;
import org.deephacks.graphene.internal.UniqueIds;
import org.deephacks.graphene.internal.ValueSerialization.ValueReader;

import java.util.Collection;
import java.util.Set;

/**
 * EntityRepository will never commit or rollback a transactions.
 */
public class EntityRepository {
    private static final Object WRITE_LOCK = new Object();
    private final Handle<Graphene> graphene = Graphene.get();
    private final TransactionManager tm = graphene.get().getTransactionManager();
    private final Handle<Database> db;
    private final Handle<SecondaryDatabase> secondary;

    public EntityRepository() {
        this.db = graphene.get().getPrimary();
        this.secondary = graphene.get().getSecondary();
    }

    /**
     * Get an instance from storage and acquire a shared read lock on this instance.
     * The lock will be held until the transaction commit or rollback.
     *
     * @param key primary key of instance
     * @param entityClass instance type
     * @param <E> instance type
     * @return the instance if it exist, otherwise absent
     */
    public <E> Optional<E> get(Object key, Class<E> entityClass) {
        Optional<byte[][]> optional = getKv(key, entityClass, LockMode.READ_COMMITTED);
        if (optional.isPresent()) {
            return Optional.fromNullable((E) getSerializer(entityClass).deserializeEntity(optional.get()));
        }
        return Optional.absent();
    }

    /**
     * Get instance from storage according and acquire an exclusive write lock on this instance.
     * The lock will be held until the transaction commit or rollback.
     *
     * @param key primary key of instance
     * @param entityClass instance type
     * @param <E> instance type
     * @return the instance if it exist, otherwise absent
     *
     */
    public <E> Optional<E> getForUpdate(Object key, Class<E> entityClass) {
        Optional<byte[][]> optional = getKv(key, entityClass, LockMode.RMW);
        if (optional.isPresent()) {
            return Optional.fromNullable((E) getSerializer(entityClass).deserializeEntity(optional.get()));
        }
        return Optional.absent();
    }
    /**
     * Put provided instance in storage, overwrite if instance already exist.
     *
     * @param entity instance to be written
     * @param <E> instance type
     * @return true if writing the instance was successful
     */
    public <E> boolean put(E entity) {
        synchronized (WRITE_LOCK) {
            Preconditions.checkNotNull(entity);
            Class<?> entityClass = entity.getClass();
            byte[][] data = getSerializer(entityClass).serializeEntity(entity);
            if (data == null || data.length != 2) {
                throw new IllegalArgumentException("Could not serialize entity");
            }
            DatabaseEntry key = new DatabaseEntry(data[0]);
            DatabaseEntry value = new DatabaseEntry(data[1]);
            if (OperationStatus.SUCCESS != db.get().put(getTx(), key, value)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Put an instance if it does not exist. If the instance exist, nothing will be written.
     *
     * @param entity instance to be written
     * @param <E> instance type
     * @return true if the instance did not exist, false otherwise.
     */
    public <E> boolean putNoOverwrite(E entity) {
        synchronized (WRITE_LOCK) {
            Preconditions.checkNotNull(entity);
            Class<?> entityClass = entity.getClass();
            byte[][] data = getSerializer(entityClass).serializeEntity(entity);
            if (data == null || data.length != 2) {
                throw new IllegalArgumentException("Could not serialize entity");
            }
            DatabaseEntry key = new DatabaseEntry(data[0]);
            DatabaseEntry value = new DatabaseEntry(data[1]);
            return OperationStatus.SUCCESS == db.get().putNoOverwrite(getTx(), key, value);
        }
    }

    /**
     * Delete an instance with a specific primary key.
     *
     * @param key primary key to delete
     * @param entityClass instance type to delete
     * @param <E> instance type
     * @return the instance if it was deleted, otherwise absent
     * @throws DeleteConstraintException if another instance have a reference on the deleted instance
     */
    public <E> Optional<E> delete(Object key, Class<E> entityClass) throws DeleteConstraintException {
        synchronized (WRITE_LOCK) {
            final Optional<byte[][]> optional = getKv(key, entityClass, LockMode.RMW);
            if(!optional.isPresent()) {
                return Optional.absent();
            }
            try {
                OperationStatus status = db.get().delete(getTx(), new DatabaseEntry(optional.get()[0]));
                if (status == OperationStatus.NOTFOUND) {
                    return Optional.absent();
                }
                return  Optional.fromNullable((E) getSerializer(entityClass).deserializeEntity(optional.get()));
            } catch (DeleteConstraintException e) {
                throw new org.deephacks.graphene.DeleteConstraintException(
                        new RowKey(e.getPrimaryKey().getData()) + " have a reference to " + new RowKey(e.getSecondaryKey().getData()), e);
            }
        }
    }

    /**
     * Select instances based on the provided Criteria.
     *
     * @param entityClass the type which is target for selection
     * @param criteria criteria used for selecting instances
     * @param <E> instance type
     * @return instances match criteria
     */
    public <E> Query<E> select(final Class<E> entityClass, final Criteria criteria) {
        return new DefaultQuery<>(entityClass, this, criteria);
    }

    public <E> Query<E> select(Class<E> entityClass) {
        return new DefaultQuery<>(entityClass, this);
    }

    public void deleteAll(Class<?> entityClass) {
        synchronized (WRITE_LOCK) {
            try {
                try(Cursor cursor = openPrimaryCursor()) {
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

    /**
     * @return the current transaction manager
     */
    public TransactionManager getTransactionManager() {
        return tm;
    }

    /**
     * Start a transaction.
     */
    public void beginTransaction() {
        tm.beginTransaction();
    }

    /**
     * Get the current transaction associated with this thread or create a new one.
     *
     * @return the transaction.
     */
    public Transaction getTx() {
        return tm.peek();
    }

    /**
     * Commit the transaction associated with the current thread.
     */
    public void commit() {
        tm.commit();
    }

    /**
     * Rollback the transaction associated with the current thread.
     */
    public void rollback() {
        tm.rollback();
    }

    private <E> Optional<byte[][]> getKv(Object key, Class<E> entityClass, LockMode mode) {
        byte[] dataKey = getSerializer(entityClass).serializeRowKey(new RowKey(entityClass, key));
        DatabaseEntry entryKey = new DatabaseEntry(dataKey);
        DatabaseEntry entryValue = new DatabaseEntry();
        if (OperationStatus.SUCCESS == db.get().get(getTx(), entryKey, entryValue, mode)) {
            byte[][] kv = new byte[][]{ entryKey.getData(), entryValue.getData()};
            return Optional.fromNullable(kv);
        }
        return Optional.absent();
    }

    Cursor openPrimaryCursor() {
        CursorConfig config = new CursorConfig();
        config.setReadCommitted(true);
        return db.get().openCursor(getTx(), config);
    }

    Cursor openForeignCursor() {
        CursorConfig config = new CursorConfig();
        config.setReadCommitted(true);
        return secondary.get().openCursor(getTx(), config);
    }

    private Serializer getSerializer(Class<?> entityClass){
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
            for (EntityFieldWrapper field : cls.getReferences().values()) {
                int fieldId = uniqueIds.getSchemaId(field.getName());
                Object value = reader.getValue(fieldId, header);
                if (value != null) {
                    if (value instanceof Collection) {
                        // TODO: make string an LONG instance id instead
                        for (String instanceId : (Collection<String>) value) {
                            int schemaId = uniqueIds.getSchemaId(field.getType().getName());
                            long iid = uniqueIds.getInstanceId(instanceId);
                            results.add(new DatabaseEntry(RowKey.toKey(schemaId, iid)));
                        }
                    } else {
                        long iid = uniqueIds.getInstanceId(value.toString());
                        int schemaId = uniqueIds.getSchemaId(field.getType().getName());
                        results.add(new DatabaseEntry(RowKey.toKey(schemaId, iid)));
                    }
                }
            }
        }
    }
}
