package org.deephacks.graphene.internal;

import java.io.Serializable;
import java.util.Comparator;

public class FastKeyComparator implements Comparator<byte[]>, Serializable {
    @Override
    public int compare(byte[] o1, byte[] o2) {
        return BytesUtils.compareTo(o1, 0, o1.length, o2, 0, o2.length);
    }
}