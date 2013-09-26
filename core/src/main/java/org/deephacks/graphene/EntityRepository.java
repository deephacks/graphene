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
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryMultiKeyCreator;
import com.sleepycat.je.Transaction;
import org.deephacks.graphene.Query.DefaultQuery;
import org.deephacks.graphene.internal.RowKey;
import org.deephacks.graphene.internal.Serializer;
import org.deephacks.graphene.internal.UniqueIds;

import java.util.Set;

public class EntityRepository {
    private final Handle<Graphene> graphene = Graphene.get();
    private final Handle<Database> db;
    private final Handle<SecondaryDatabase> secondary;

    public EntityRepository() {
        this.db = graphene.get().getPrimary();
        this.secondary = graphene.get().getSecondary();

    }

    public <E> Optional<E> get(Object key, Class<E> entityClass) {
        byte[] dataKey = getSerializer(entityClass).serializeRowKey(new RowKey(entityClass, key));
        DatabaseEntry entryKey = new DatabaseEntry(dataKey);
        DatabaseEntry entryValue = new DatabaseEntry();
        if (OperationStatus.SUCCESS == db.get().get(getTx(), entryKey, entryValue, LockMode.DEFAULT)) {
            byte[][] kv = new byte[][]{ entryKey.getData(), entryValue.getData()};
            return Optional.fromNullable((E) getSerializer(entityClass).deserializeEntity(kv));
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
        return OperationStatus.SUCCESS == db.get().put(getTx(), key, value);
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
        try {
            final Optional<E> optional = get(key, entityClass);
            if(!optional.isPresent()) {
                return Optional.absent();
            }
            byte[] dataKey = getSerializer(entityClass).serializeRowKey(new RowKey(entityClass, key));
            OperationStatus status = db.get().delete(getTx(), new DatabaseEntry(dataKey));
            if (status == OperationStatus.NOTFOUND) {
                return Optional.absent();
            }
            return Optional.fromNullable(optional.get());
        } catch (com.sleepycat.je.DeleteConstraintException e) {
            throw new DeleteConstraintException(e);
        }
    }

    public <E> Query<E> select(final Class<E> entityClass, final Criteria criteria) {
        return new DefaultQuery<>(entityClass, this, criteria);
    }

    public <E> Query<E> select(Class<E> entityClass) {
        return new DefaultQuery<>(entityClass, this);
    }

    public void deleteAll() {
        try(Cursor cursor = openCursor()) {
            DatabaseEntry firstKey = new DatabaseEntry(RowKey.getMinId().getKey());

            if (cursor.getSearchKeyRange(firstKey, new DatabaseEntry(), LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                cursor.delete();
            }

            while (cursor.getNextNoDup(firstKey, new DatabaseEntry(), LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                cursor.delete();
            }
        }
    }

    public <E> long count(Class<E> entityClass) {
        return db.get().count();
    }

    public long countAll() {
        return db.get().count();
    }

    Cursor openCursor() {
        return db.get().openCursor(getTx(), null);
    }

    public Transaction getTx() {
        return graphene.get().getTx();
    }

    public void commit() {
        graphene.get().commit();
    }

    public void abort() {
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
/*            BeanId beanId = BeanId.read(key.getData());
            Schema schema = schemaManager.getSchema(beanId.getSchemaName());
            ValueReader reader = new ValueReader(data.getData());
            int[][] header = reader.getHeader();
            for (String referenceName : schema.getReferenceNames()) {
                int refId = uniqueIds.getSchemaId(referenceName);
                Object value = reader.getValue(refId, header);
                if (value != null) {
                    String schemaName = schema.getReferenceSchemaName(referenceName);
                    int schemaId = uniqueIds.getSchemaId(schemaName);
                    if (value instanceof Collection) {
                        for (Long id : (Collection<Long>) value) {
                            results.add(new DatabaseEntry(BinaryBeanId.toKey(schemaId, id)));
                        }
                    } else {
                        results.add(new DatabaseEntry(BinaryBeanId.toKey(schemaId, (long) value)));
                    }
                }
            }
        */
        }
    }
}
