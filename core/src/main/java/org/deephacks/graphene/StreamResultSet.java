package org.deephacks.graphene;

import org.deephacks.graphene.internal.FastKeyComparator;
import org.deephacks.graphene.internal.serialization.BytesUtils;
import org.fusesource.lmdbjni.Constants;
import org.fusesource.lmdbjni.Cursor;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.SeekOp;

import java.io.Closeable;
import java.util.Iterator;

public class StreamResultSet<T> implements Iterable<T>, Closeable {
  private int maxResult = Integer.MAX_VALUE;
  private int matches = 0;
  private final Cursor cursor;
  private byte[] key;
  private byte[] value;
  private byte[] firstKey;
  private byte[] lastKey;
  private boolean lastReached = false;
  private Schema<T> schema;

  public StreamResultSet(Schema<T> schema, Cursor cursor) {
    this.schema = schema;
    this.firstKey = schema.getMinKey();
    this.lastKey = schema.getMaxKey();
    this.key = this.firstKey;
    this.cursor = cursor;
    Entry entry = cursor.seek(SeekOp.RANGE, key);
    if (entry != null) {
      this.value = entry.getValue();
      this.key = entry.getKey();
    }
  }

  @Override
  public Iterator<T> iterator() {
    return new ByteIteratorWrapper<>(schema, new ByteIterator().iterator());
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
          if (matches == 0 && value != null) {
            // the firstKey value may already be fetched if
            // cursor.getSearchKeyRange found a key
            return FastKeyComparator.withinKeyRange(key, firstKey, lastKey);
          }
          Entry entry = cursor.get(Constants.NEXT);
          if (entry == null) {
            return false;
          }
          key = entry.getKey();
          value = entry.getValue();
          if (!FastKeyComparator.withinKeyRange(key, firstKey, lastKey)) {
            return false;
          }
          if (lastKey != null && BytesUtils.compareTo(key, 0, key.length, lastKey, 0, lastKey.length) > 0) {
            lastReached = true;
          }
          return matches < maxResult && !lastReached;
        }

        @Override
        public byte[][] next() {
          byte[][] bytes = { key, value };
          value = null;
          return bytes;
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
    private Schema<E> schema;
    public ByteIteratorWrapper(Schema<E> schema, Iterator<byte[][]> iterator) {
      this.iterator = iterator;
      this.schema = schema;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext() && matches++ < maxResult;
    }

    @Override
    public E next() {
      byte[][] data = iterator.next();
      return schema.getEntity(data);
    }

    @Override
    public void remove() {
      iterator.remove();
    }
  }
}
