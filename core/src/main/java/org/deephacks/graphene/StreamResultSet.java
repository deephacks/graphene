package org.deephacks.graphene;

import org.deephacks.graphene.internal.BytesUtils;
import org.deephacks.graphene.internal.FastKeyComparator;
import org.deephacks.graphene.internal.RowKey;
import org.deephacks.graphene.internal.Serializer;
import org.fusesource.lmdbjni.Constants;
import org.fusesource.lmdbjni.Cursor;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.SeekOp;

import java.io.Closeable;
import java.util.Iterator;

public class StreamResultSet<T> implements Iterable<T>, Closeable {
  private static final Handle<Graphene> graphene = Graphene.get();
  private int maxResult = Integer.MAX_VALUE;
  private int matches = 0;
  private final Cursor cursor;
  private final Serializer serializer;
  private byte[] key;
  private byte[] value;
  private byte[] first;
  private byte[] last;
  private boolean lastReached = false;

  public StreamResultSet(Class<T> entityClass, Cursor cursor) {
    this.serializer = graphene.get().getSerializer(entityClass);
    this.first = serializer.serializeRowKey(RowKey.getMinId(entityClass));
    this.key = this.first;
    this.cursor = cursor;
    Entry entry = cursor.seek(SeekOp.KEY, key);
    if (entry != null) {
      this.value = entry.getValue();
      this.last = serializer.serializeRowKey(RowKey.getMaxId(entityClass));
    }
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
          if (matches == 0 && value != null) {
            // the first value may already be fetched if
            // cursor.getSearchKeyRange found a key
            return true;
          }
          Entry entry = cursor.get(Constants.NEXT);
          if (entry == null) {
            return false;
          }
          key = entry.getKey();
          value = entry.getValue();
          if (!FastKeyComparator.withinKeyRange(key, first, last)) {
            return false;
          }
          if (last != null && BytesUtils.compareTo(key, 0, key.length, last, 0, last.length) > 0) {
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
