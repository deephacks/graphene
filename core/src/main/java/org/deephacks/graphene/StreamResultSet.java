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

public class StreamResultSet<T> implements Iterable<T>, Closeable {
  private static final Handle<Graphene> graphene = Graphene.get();
  private int maxResult = Integer.MAX_VALUE;
  private int matches = 0;
  private final Cursor cursor;
  private final Serializer serializer;
  private DatabaseEntry key;
  private DatabaseEntry value;
  private byte[] first;
  private byte[] last;
  private boolean lastReached = false;

  public StreamResultSet(Class<T> entityClass, Cursor cursor) {
    this.key = new DatabaseEntry();
    this.serializer = graphene.get().getSerializer(entityClass);
    this.first = serializer.serializeRowKey(RowKey.getMinId(entityClass));
    this.key.setData(this.first);
    this.value = new DatabaseEntry();
    cursor.getSearchKeyRange(key, value, LockMode.RMW);
    this.last = serializer.serializeRowKey(RowKey.getMaxId(entityClass));
    this.cursor = cursor;
  }

  @Override
  public Iterator<T> iterator() {
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

  class ByteIteratorWrapper<E> implements Iterator<E> {

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
    public E next() {
      byte[][] data = iterator.next();
      return (E) serializer.deserializeEntity(data);
    }

    @Override
    public void remove() {
      iterator.remove();
    }
  }
}
