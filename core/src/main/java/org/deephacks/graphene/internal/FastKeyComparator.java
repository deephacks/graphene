package org.deephacks.graphene.internal;

import org.deephacks.graphene.internal.serialization.BytesUtils;

import java.io.Serializable;
import java.util.Comparator;

public class FastKeyComparator implements Comparator<byte[]>, Serializable {
  @Override
  public int compare(byte[] o1, byte[] o2) {
    return BytesUtils.compareTo(o1, 0, o1.length, o2, 0, o2.length);
  }

  public static boolean equals(byte[] o1, byte[] o2) {
    return BytesUtils.compareTo(o1, 0, o1.length, o2, 0, o2.length) == 0;
  }

  public static boolean withinKeyRange(byte[] key, byte[] firstKey, byte[] lastKey) {
    if (BytesUtils.compareTo(firstKey, 0, firstKey.length, key, 0, key.length) > 0) {
      return false;
    }
    if (BytesUtils.compareTo(lastKey, 0, lastKey.length, key, 0, key.length) < 0) {
      return false;
    }
    return true;
  }
}