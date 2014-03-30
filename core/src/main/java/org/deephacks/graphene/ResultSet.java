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
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import org.deephacks.graphene.internal.BytesUtils;
import org.deephacks.graphene.internal.FastKeyComparator;
import org.deephacks.graphene.internal.RowKey;
import org.deephacks.graphene.internal.Serializer;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Optional;

/**
 * A lazy evaluated result set iterator matching a certain query that created this
 * result set. Items that are not iterated are not evaluated.
 *
 * @param <T> type of instances
 */
public abstract class ResultSet<T> implements Iterable<T>, Closeable {

    public abstract Iterator<T> iterator();

    public abstract void close();

    static class DefaultResultSet<E> extends ResultSet<E> {
        private static final Handle<Graphene> graphene = Graphene.get();
        private int maxResult = Integer.MAX_VALUE;
        private int matches = 0;
        private final Cursor cursor;
        private final Optional<Criteria> criteria;
        private final Serializer serializer;
        private DatabaseEntry key;
        private DatabaseEntry value;
        private byte[] first;
        private byte[] last;
        private boolean lastReached = false;

        public DefaultResultSet(Class<?> entityClass, Object first, Object last, int maxResult, Optional<Criteria> criteria, Cursor cursor) {
            this.maxResult = maxResult;
            this.key = new DatabaseEntry();
            this.serializer = graphene.get().getSerializer(entityClass);
            if (first != null) {
                this.first = serializer.serializeRowKey(new RowKey(entityClass, first));
                this.key.setData(this.first);
                value = new DatabaseEntry();
            } else {
                this.first = serializer.serializeRowKey(RowKey.getMinId(entityClass));
                this.key.setData(this.first);
                value = new DatabaseEntry();
            }
            cursor.getSearchKeyRange(key, value, LockMode.RMW);
            if (last != null) {
                this.last = serializer.serializeRowKey(new RowKey(entityClass, last));
            } else {
                this.last = serializer.serializeRowKey(RowKey.getMaxId(entityClass));
            }
            this.cursor = cursor;
            this.criteria = criteria;

        }

        @Override
        public Iterator<E> iterator() {
            return new ByteIteratorWrapper<>(new ByteIterator().iterator(), serializer);
        }

        @Override
        public void close() {
            cursor.close();
        }

        class ByteIterator implements Iterable<byte[][]> {

            @Override
            public Iterator<byte[][]> iterator() {

                return new Iterator<byte[][]>() {
                    @Override
                    public boolean hasNext() {
                        if (matches == 0 && value != null && value.getData() != null) {
                            // the first value may already be fetched if
                            // cursor.getSearchKeyRange found a key
                            return true;
                        }
                        value = new DatabaseEntry();
                        boolean success = cursor.getNextNoDup(key, value, LockMode.RMW) == OperationStatus.SUCCESS;
                        if (!FastKeyComparator.withinKeyRange(key.getData(), first, last)) {
                            return false;
                        }
                        if (!success) {
                            return false;
                        }
                        if (last != null && BytesUtils.compareTo(key.getData(), 0, key.getData().length, last, 0, last.length) > 0) {
                            lastReached = true;
                        }
                        return matches < maxResult && !lastReached;
                    }

                    @Override
                    public byte[][] next() {
                        byte[] valueData = value.getData();
                        byte[] keyData = key.getData();
                        value = null;
                        return new byte[][]{ keyData, valueData };
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }

        class ByteIteratorWrapper<T> implements Iterator<T> {

            private final Iterator<byte[][]> iterator;
            private final Serializer serializer;

            public ByteIteratorWrapper(Iterator<byte[][]> iterator, Serializer serializer) {
                this.iterator = iterator;
                this.serializer = serializer;
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext() && matches++ < maxResult;
            }

            @Override
            public T next() {
                byte[][] data = iterator.next();
                return (T) serializer.deserializeEntity(data);
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        }
    }
}
