package org.deephacks.graphene;

import org.deephacks.graphene.internal.serialization.Bytes;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * UniqueIds are used when beans are serialized into binary form. Every persistent bean manager implementation
 * should provide an implementation that is registered using the standard java ServiceLoader mechanism.
 * <field>
 * Instance ids and schema/property names are mapped to a unique id numbers in order to save space and
 * decrease serialization latency. Schema and property names are always cached in memory to speedup lookup.
 * Instance ids can also be cached but this decision is taken by the implementation.
 */
public class UniqueIds {
  private static final String SCHEMA_DATABASE_NAME = "graphene.schemas";
  private static final String INSTANCE_DATABASE_NAME = "graphene.instances";
  private final KeyValueStore instances;
  private final KeyValueStore schemas;
  private final Graphene graphene;
  private final byte NAME_PREFIX = 0;
  private final byte ID_PREFIX = 1;
  protected boolean shouldCacheInstance = true;

  private static final ConcurrentHashMap<String, Long> instanceIdCache = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<Long, String> instanceNameCache = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<String, Integer> schemaIdCache = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<Integer, String> schemaNameCache = new ConcurrentHashMap<>();

  public UniqueIds(Graphene graphene, KeyValueStore instances, KeyValueStore schemas) {
    this.schemas = schemas;
    this.instances = instances;
    this.graphene = graphene;
    for (Entry<String, byte[]> entry : listSchemas().entrySet()) {
      schemaIdCache.put(entry.getKey(), (int) Bytes.getLong(entry.getValue()));
      schemaNameCache.put((int) Bytes.getLong(entry.getValue()), entry.getKey());
    }
  }

  public byte[] getMaxSchemaWidth() {
    return Bytes.fromInt(-1);
  }

  public byte[] getMinSchemaWidth() {
    return Bytes.fromInt(0);
  }

  public byte[] getMaxInstanceWidth() {
    return Bytes.fromLong(-1);
  }

  public byte[] getMinInstanceWidth() {
    return Bytes.fromLong(0);
  }

  public int getSchemaId(final Class<?> cls) {
    return getSchemaId(cls.getName());
  }

  public int getSchemaId(final String name) {
    Integer id = schemaIdCache.get(name);
    if (id != null) {
      return id;
    } else {
      id = getSchemaIdFromStorage(name);
      schemaIdCache.put(name, id);
      schemaNameCache.put(id, name);
    }
    return id;
  }

  public String getSchemaName(final int id) {
    String name = schemaNameCache.get(id);
    if (name != null) {
      return name;
    } else {
      name = getSchemaNameFromStorage(id);
      if (Guavas.isNullOrEmpty(name)) {
        throw new IllegalStateException("Could not map id " + id + " to a name");
      }
      schemaIdCache.put(name, id);
      schemaNameCache.put(id, name);
    }
    return name;
  }

  public long getInstanceId(final String name) {
    Long id = instanceIdCache.get(name);
    if (id != null) {
      return id;
    } else {
      id = getInstanceIdFromStorage(name);
      if (shouldCacheInstance) {
        instanceIdCache.put(name, id);
        instanceNameCache.put(id, name);
      }
    }
    return id;
  }

  public String getInstanceName(final long id) {
    String name = instanceNameCache.get(id);
    if (!Guavas.isNullOrEmpty(name)) {
      return name;
    } else {
      name = getInstanceNameFromStorage(id);
      if (Guavas.isNullOrEmpty(name)) {
        throw new IllegalStateException("Could not map id " + id + " to a name");
      }
      if (shouldCacheInstance) {
        instanceIdCache.put(name, id);
        instanceNameCache.put(id, name);
      }
    }
    return name;
  }

  protected String getSchemaNameFromStorage(int id) {
    Optional<byte[]> optionalName = schemas.get(getIdKey(id));
    if (optionalName.isPresent()) {
      return new String(optionalName.get(), UTF_8);
    }
    throw new IllegalArgumentException("Key does not exist in storage " + id);
  }

  protected int getSchemaIdFromStorage(String name) {
    byte[] nameBytes = name.getBytes(UTF_8);
    byte[] nameKey = getNameKey(nameBytes);
    Optional<byte[]> optionalId = schemas.get(nameKey);
    if (optionalId.isPresent()) {
      return Long.valueOf(Bytes.getLong(optionalId.get())).intValue();
    }
    byte[] key = SCHEMA_DATABASE_NAME.getBytes(UTF_8);
    long id = graphene.increment(key);
    byte[] idKey = getIdKey(id);
    schemas.put(nameKey, Bytes.fromLong(id));
    schemas.put(idKey, nameBytes);
    return (int) id;
  }

  protected String getInstanceNameFromStorage(long id) {
    Optional<byte[]> optionalName = instances.get(getIdKey(id));
    if (optionalName.isPresent()) {
      return new String(optionalName.get(), UTF_8);
    }
    throw new IllegalArgumentException("Key does not exist in storage " + id);
  }

  protected long getInstanceIdFromStorage(String name) {
    byte[] nameBytes = name.getBytes(UTF_8);
    Optional<byte[]> optionalId = instances.get(getNameKey(nameBytes));
    if (optionalId.isPresent()) {
      return Bytes.getLong(optionalId.get());
    }
    byte[] key = INSTANCE_DATABASE_NAME.getBytes(UTF_8);
    long id = graphene.increment(key);
    byte[] nameKey = getNameKey(nameBytes);
    byte[] idKey = getIdKey(id);
    instances.put(nameKey, Bytes.fromLong(id));
    instances.put(idKey, nameBytes);
    return id;
  }

  private byte[] getIdKey(long id) {
    byte[] k = Bytes.fromLong(id);
    return new byte[]{ID_PREFIX, k[0], k[1], k[2], k[3], k[4], k[5], k[6], k[7]};
  }

  private byte[] getNameKey(byte[] name) {
    byte[] key = new byte[name.length + 1];
    key[0] = NAME_PREFIX;
    System.arraycopy(name, 0, key, 1, name.length);
    return key;
  }

  public Map<String, byte[]> listSchemas() {
    Map<byte[], byte[]> map = schemas.listAll();
    TreeMap<String, byte[]> result = new TreeMap<>();

    for (byte[] key : map.keySet()) {
      if (key[0] == NAME_PREFIX) {
        byte[] name = new byte[key.length - 1];
        System.arraycopy(key, 1, name, 0, key.length - 1);
        for (byte[] key2 : map.keySet()) {
          byte[] name2 = map.get(key2);
          if (Arrays.equals(name2, map.get(key))) {
            result.put(new String(name), map.get(key));
          }
        }
      }
    }
    return result;
  }

  public Map<String, byte[]> listInstances() {
    Map<byte[], byte[]> map = instances.listAll();
    TreeMap<String, byte[]> result = new TreeMap<>();

    for (byte[] key : map.keySet()) {
      if (key[0] == NAME_PREFIX) {
        byte[] name = new byte[key.length - 1];
        System.arraycopy(key, 1, name, 0, key.length - 1);
        for (byte[] key2 : map.keySet()) {
          byte[] name2 = map.get(key2);
          if (Arrays.equals(name2, map.get(key))) {
            result.put(new String(name), map.get(key));
          }
        }
      }
    }
    return result;
  }

  public void printAllSchemaAndInstances() {
    System.out.println("Schemas:");
    Map<String, byte[]> map = listSchemas();
    for (String name : map.keySet()) {
      System.out.println(" " + Arrays.toString(map.get(name)) + " " + name);
    }
    System.out.println("Instances:");
    map = listInstances();
    for (String name : map.keySet()) {
      System.out.println(" " + Arrays.toString(map.get(name)) + " " + name);
    }
  }

  public static void clear() {
    instanceIdCache.clear();
    instanceNameCache.clear();
    schemaIdCache.clear();
    schemaNameCache.clear();
  }
}