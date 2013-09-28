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
import org.deephacks.graphene.internal.BytesUtils;
import org.deephacks.graphene.internal.EntityClassWrapper;
import org.deephacks.graphene.internal.EntityClassWrapper.EntityFieldWrapper;
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
    private final Handle<Graphene> graphene = Graphene.get();
    private final Handle<Database> db;
    private final Handle<SecondaryDatabase> secondary;

    public EntityRepository() {
        this.db = graphene.get().getPrimary();
        this.secondary = graphene.get().getSecondary();

    }

    public <E> Optional<E> get(Object key, Class<E> entityClass) {
            Optional<byte[][]> optional = getKv(key, entityClass);
            if (optional.isPresent()) {
                return Optional.fromNullable((E) getSerializer(entityClass).deserializeEntity(optional.get()));
            }
            return Optional.absent();
    }

    public <E> Optional<byte[][]> getKv(Object key, Class<E> entityClass) {
        byte[] dataKey = getSerializer(entityClass).serializeRowKey(new RowKey(entityClass, key));
        DatabaseEntry entryKey = new DatabaseEntry(dataKey);
        DatabaseEntry entryValue = new DatabaseEntry();
        if (OperationStatus.SUCCESS == db.get().get(getTx(), entryKey, entryValue, LockMode.READ_COMMITTED)) {
            byte[][] kv = new byte[][]{ entryKey.getData(), entryValue.getData()};
            return Optional.fromNullable(kv);
        }
        return Optional.absent();
    }

    public <E> boolean put(E entity) {
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

    private boolean exists(RowKey refKey) {
        try(Cursor cursor = db.get().openCursor(getTx(), null)) {
            DatabaseEntry key = new DatabaseEntry(refKey.getKey());
            OperationStatus status = cursor.getSearchKey(key, new DatabaseEntry(), LockMode.DEFAULT);
            if (OperationStatus.SUCCESS == status) {
                return true;
            }
            return false;
        }
    }

    public <E> boolean putNoOverwrite(E entity) {
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

    public <E> Optional<E> delete(Object key, Class<E> entityClass) throws DeleteConstraintException {
        final Optional<byte[][]> optional = getKv(key, entityClass);
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

    public <E> Query<E> select(final Class<E> entityClass, final Criteria criteria) {
        return new DefaultQuery<>(entityClass, this, criteria);
    }

    public <E> Query<E> select(Class<E> entityClass) {
        return new DefaultQuery<>(entityClass, this);
    }

    public void deleteAll(Class<?> entityClass) {
        try {
            try(Cursor cursor = openPrimaryCursor()) {
                byte[] firstKey = RowKey.getMinId(entityClass).getKey();
                byte[] lastKey = RowKey.getMaxId(entityClass).getKey();
                DatabaseEntry keyEntry = new DatabaseEntry(firstKey);
                if (cursor.getSearchKeyRange(keyEntry, new DatabaseEntry(), LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    // important; we must respect the class prefix boundaries of the key
                    if (!withinKeyRange(keyEntry.getData(), firstKey, lastKey)) {
                        return;
                    }
                    cursor.delete();
                }
                while (cursor.getNextNoDup(keyEntry, new DatabaseEntry(), LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    // important; we must respect the class prefix boundaries of the key
                    if (!withinKeyRange(keyEntry.getData(), firstKey, lastKey)) {
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

    private boolean withinKeyRange(byte[] key, byte[] firstKey, byte[] lastKey) {
        if (BytesUtils.compareTo(firstKey, 0, firstKey.length, key, 0, key.length) > 0) {
            return false;
        }
        if (BytesUtils.compareTo(lastKey, 0, lastKey.length, key, 0, key.length) < 0) {
            return false;
        }
        return true;
    }

    public long countAll() {
        return db.get().count();
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

    public Transaction getTx() {
        return graphene.get().getTx();
    }

    public void commit() {
        graphene.get().commit();
    }

    public void rollback() {
        graphene.get().abort();
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
