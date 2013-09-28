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
import org.deephacks.graphene.internal.RowKey;
import org.deephacks.graphene.internal.Serializer;
import org.deephacks.graphene.internal.UniqueIds;
import org.deephacks.graphene.internal.ValueSerialization.ValueReader;

import java.util.Collection;
import java.util.Set;

public class EntityRepository {
    private final Handle<Graphene> graphene = Graphene.get();
    private final Handle<Database> db;
    private final Handle<SecondaryDatabase> secondary;
    private final UniqueIds ids = new UniqueIds();

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
        OperationStatus status = null;
        try {
            status = db.get().delete(getTx(), new DatabaseEntry(optional.get()[0]));
            if (status == OperationStatus.NOTFOUND) {
                rollback();
                return Optional.absent();
            }
        } catch (DeleteConstraintException e) {
            // fine.
            System.out.println("sdjgsdg "  +  new RowKey(optional.get()[0]) + e.getMessage());
        }

        if (status == OperationStatus.NOTFOUND) {
            rollback();
            return Optional.absent();
        }
        return  Optional.fromNullable((E) getSerializer(entityClass).deserializeEntity(optional.get()));
    }

    public <E> Query<E> select(final Class<E> entityClass, final Criteria criteria) {
        return new DefaultQuery<>(entityClass, this, criteria);
    }

    public <E> Query<E> select(Class<E> entityClass) {
        return new DefaultQuery<>(entityClass, this);
    }

    public void deleteAll(Class<?> entityClass) {
        try(Cursor cursor = openPrimaryCursor()) {
            DatabaseEntry firstKey = new DatabaseEntry(RowKey.getMinId(entityClass).getKey());
            deleteAll(cursor, firstKey);
        }
    }

    public void deleteAll() {
        try(Cursor cursor = openForeignCursor()) {
            DatabaseEntry firstKey = new DatabaseEntry(RowKey.getMinId().getKey());
            deleteAll(cursor, firstKey);
        }
        try(Cursor cursor = openPrimaryCursor()) {
            DatabaseEntry firstKey = new DatabaseEntry(RowKey.getMinId().getKey());
            deleteAll(cursor, firstKey);
        }
    }

    private void deleteAll(Cursor cursor, DatabaseEntry firstKey) {
        if (cursor.getSearchKeyRange(firstKey, new DatabaseEntry(), LockMode.DEFAULT) == OperationStatus.SUCCESS) {
            cursor.delete();
        }
        while (cursor.getNextNoDup(firstKey, new DatabaseEntry(), LockMode.DEFAULT) == OperationStatus.SUCCESS) {
            cursor.delete();
            while (cursor.getNextDup(firstKey, new DatabaseEntry(), LockMode.DEFAULT) == OperationStatus.SUCCESS){
                cursor.delete();
            }
        }
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
