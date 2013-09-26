package org.deephacks.graphene.internal;


import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Arrays;

public class RowKey implements Comparable<RowKey>, Serializable {
    private static final UniqueIds ids  = new UniqueIds();
    private byte[] key;
    private Class<?> cls;

    public RowKey(Class<?> cls, String id) {
        this.cls = cls;
        int schemaId = ids.getSchemaId(cls.getName());
        long instanceId = ids.getInstanceId(id);
        this.key = toKey(schemaId, instanceId);
    }

    public RowKey(byte[] key) {
        this.key = key;
    }

    public RowKey(Class<?> entity, Object key) {
        Preconditions.checkNotNull(key);
        int schemaId = ids.getSchemaId(entity.getName());
        long instanceId = ids.getInstanceId(key.toString());
        this.key = toKey(schemaId, instanceId);
    }

    public Class<?> getCls() {
        if (cls != null) {
            return cls;
        }
        String className = ids.getSchemaName(getClassId());
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        return cls;
    }

    public byte[] getKey() {
        return key;
    }

    public String getInstance() {
        return ids.getInstanceName(getInstanceId());
    }

    public static byte[] toKey(int schemaId, long instanceId) {
        byte[] s = Bytes.fromInt(schemaId);
        byte[] i = Bytes.fromLong(instanceId);
        return new byte[] {s[0], s[1], s[2], s[3], i[0], i[1], i[2], i[3], i[4], i[5], i[6], i[7]};
    }

    public static RowKey getMinId(Class<?> cls) {
        byte[] key = toKey(ids.getSchemaId(cls.getName()), 0);
        return new RowKey(key);
    }

    public static RowKey getMinId() {
        byte[] key = toKey(0, 0);
        return new RowKey(key);
    }

    public static RowKey getMaxId(Class<?> cls) {
        byte[] key = toKey(ids.getSchemaId(cls.getName()), -1);
        return new RowKey(key);
    }

    public static RowKey getMaxId() {
        byte[] key = toKey(-1, -1);
        return new RowKey(key);
    }

    private byte[] getClassBytes() {
        return new byte[] { key[0], key[1], key[2], key[3] };
    }

    private int getClassId() {
        return Bytes.getInt(getClassBytes());
    }

    private byte[] getInstanceBytes() {
        return new byte[] { key[4], key[5], key[6], key[7], key[8], key[9], key[10], key[11] };
    }

    private long getInstanceId() {
        return Bytes.getLong(getInstanceBytes());
    }

    @Override
    public int compareTo(RowKey o) {
        return BytesUtils.compareTo(key, 0, key.length, o.getKey(), 0, o.getKey().length);
    }

    @Override
    public String toString() {
        return Arrays.toString(key);
    }
}