package org.deephacks.graphene;

import org.deephacks.graphene.internal.KeyInterface;
import org.deephacks.graphene.internal.serialization.Buf;
import org.deephacks.graphene.internal.serialization.BufAllocator;
import org.deephacks.graphene.internal.serialization.KeySerialization.KeyReader;
import org.deephacks.graphene.internal.serialization.KeySerialization.KeyWriter;
import org.deephacks.graphene.internal.serialization.ValueSerialization.ValueReader;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Schema<T> {

  private KeySchema keySchema;
  private Class<T> cls;
  private BufAllocator bufAllocator;
  private UniqueIds uniqueIds;
  private Constructor<T> constructor;
  private int schemaId;
  private byte[] minKey;
  private byte[] maxKey;
  private KeyWriter keyWriter;

  public Schema(Class<T> cls, KeySchema keySchema, BufAllocator bufAllocator, UniqueIds uniqueIds) {
    this.keySchema = keySchema;
    this.uniqueIds = uniqueIds;
    this.schemaId = uniqueIds.getSchemaId(cls);
    this.cls = cls;
    this.bufAllocator = bufAllocator;
    this.minKey = appendSchemaId(getKeySchema().getMinKey(), getSchemaId());
    this.maxKey = appendSchemaId(getKeySchema().getMaxKey(), getSchemaId());
    try {
      this.constructor = cls.getDeclaredConstructor(KeyReader.class, ValueReader.class);
      this.constructor.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private byte[] appendSchemaId(byte[] key, int schemaId) {
    try {
      Buf buf = bufAllocator.allocateOutput(key.length);
      buf.writeInt(schemaId);
      buf.writeBytes(key);
      return buf.getByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public KeySchema getKeySchema() {
    return keySchema;
  }

  public Class<T> getClazz() {
    return cls;
  }

  public int getSchemaId() {
    return schemaId;
  }

  public byte[] getMinKey() {
    return minKey;
  }

  public byte[] getMaxKey() {
    return maxKey;
  }

  public T getEntity(byte[][] bytes) {
     KeyReader keyReader = new KeyReader(bufAllocator.allocateInput(bytes[0]), keySchema);
    ValueReader valueReader = new ValueReader(bufAllocator.allocateInput(bytes[1]), uniqueIds);
    try {
      return constructor.newInstance(keyReader, valueReader);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  public KeyInterface getKey(Object key) {
    if (key instanceof KeyInterface) {
      return (KeyInterface) key;
    }
    int size = keySchema.getFirstKeyPart().getSize();
    final Class<?> cls = key.getClass();
    return (keyWriter, schemaId) -> {
      keyWriter.writeInt(schemaId);
      if (cls.isAssignableFrom(String.class)) {
        keyWriter.writeStringBytes(key.toString(), size);
      } else if (cls.isAssignableFrom(byte.class) || cls.isAssignableFrom(Byte.class)) {
        keyWriter.writeByte((byte) key);
      } else if (cls.isAssignableFrom(short.class) || cls.isAssignableFrom(Short.class)) {
        keyWriter.writeShort((short) key);
      } else if (cls.isAssignableFrom(int.class) || cls.isAssignableFrom(Integer.class)) {
        keyWriter.writeInt((int) key);
      } else if (cls.isAssignableFrom(long.class) || cls.isAssignableFrom(Long.class)) {
        keyWriter.writeLong((long) key);
      } else if (cls.isAssignableFrom(float.class) || cls.isAssignableFrom(Float.class)) {
        keyWriter.writeFloat((float) key);
      } else if (cls.isAssignableFrom(double.class) || cls.isAssignableFrom(Double.class)) {
        keyWriter.writeDouble((double) key);
      } else if (cls.isAssignableFrom(boolean.class) || cls.isAssignableFrom(Boolean.class)) {
        keyWriter.writeBoolean((boolean) key);
      } else if (cls.isAssignableFrom(char.class) || cls.isAssignableFrom(Character.class)) {
        keyWriter.writeChar((char) key);
      } else if (cls.isAssignableFrom(byte[].class)) {
        keyWriter.writeBytes((byte[]) key);
      } else {
        keyWriter.writeObject(key);
      }
      return keyWriter.getBytes();
    };
 }

  public KeyWriter getKeyWriter() {
    return new KeyWriter(bufAllocator.allocateOutput(getKeySchema().size()), getKeySchema());
  }

  public static class KeySchema {
    private static final int SCHEMA_ID_SIZE = 4;
    private final Map<String, KeyPart> keyParts = new HashMap<>();
    private final byte[] minKey;
    private final byte[] maxKey;

    public KeySchema(List<KeyPart> parts) {
      int size = 0;
      for (KeyPart part : parts) {
        size += part.getSize();
        keyParts.put(part.getName(), part);
      }
      // 4 more bytes for the schema id
      this.minKey = new byte[size];
      this.maxKey = new byte[size];
      Arrays.fill(maxKey, (byte) 0xFF);
    }

    public int size() {
      return minKey.length;
    }

    public byte[] getMinKey() {
      return minKey;
    }

    public byte[] getMaxKey() {
      return maxKey;
    }

    public KeyPart getKeyPart(String name) {
      return keyParts.get(name);
    }

    public KeyPart getFirstKeyPart() {
      return keyParts.values().iterator().next();
    }

    public static class KeyPart {
      private String name;
      private int size;
      private Class<?> type;
      private int bytesPosition;

      public KeyPart(String name, Class<?> type, int size, int bytesPosition) {
        this.name = name;
        this.size = size;
        this.bytesPosition = bytesPosition + SCHEMA_ID_SIZE;
        this.type = type;
      }

      public Class<?> getType() {
        return type;
      }

      public int getBytesPosition() {
        return bytesPosition;
      }

      public String getName() {
        return name;
      }

      public int getSize() {
        return size;
      }

    }
  }

  public static class FieldSchema {

    public FieldSchema() {
    }
  }


}
