package org.deephacks.graphene.internal;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.deephacks.graphene.Graphene;
import org.deephacks.graphene.Handle;

import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * UniqueIds are used when beans are serialized into binary form. Every persistent bean manager implementation
 * should provide an implementation that is registered using the standard java ServiceLoader mechanism.
 *
 * Instance ids and schema/property names are mapped to a unique id numbers in order to save space and
 * decrease serialization latency. Schema and property names are always cached in memory to speedup lookup.
 * Instance ids can also be cached but this decision is taken by the implementation.
 */
public class UniqueIds {
    private static final String SCHEMA_DATABASE_NAME = "graphene.schemas";
    private static final String INSTANCE_DATABASE_NAME = "graphene.instances";
    private final DatabaseWrapper instances;
    private final DatabaseWrapper schemas;
    private final byte NAME_PREFIX = 0;
    private final byte ID_PREFIX = 1;
    private final Handle<Graphene> graphene = Graphene.get();
    protected boolean shouldCacheInstance = true;

    private static final ConcurrentHashMap<String, Long> instanceIdCache = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, String> instanceNameCache = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Integer> schemaIdCache = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Integer, String> schemaNameCache = new ConcurrentHashMap<>();

    public UniqueIds() {
        this.schemas = new DatabaseWrapper(graphene.get().getSchemas()) ;
        this.instances  = new DatabaseWrapper(graphene.get().getInstances()) ;
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
        if (!Strings.isNullOrEmpty(name)) {
            return name;
        } else {
            name = getSchemaNameFromStorage(id);
            if (Strings.isNullOrEmpty(name)) {
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
        if (!Strings.isNullOrEmpty(name)) {
            return name;
        } else {
            name = getInstanceNameFromStorage(id);
            if (Strings.isNullOrEmpty(name)) {
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
        throw new IllegalArgumentException("Id does not exist in storage " + id);
    }

    protected int getSchemaIdFromStorage(String name) {
        byte[] nameBytes = name.getBytes(UTF_8);
        schemas.get(getNameKey(nameBytes));
        Optional <byte[]> optionalId = schemas.get(getNameKey(nameBytes));
        if (optionalId.isPresent()) {
            return Long.valueOf(Bytes.getLong(optionalId.get())).intValue();
        }
        byte[] key = SCHEMA_DATABASE_NAME.getBytes(UTF_8);

        long id = graphene.get().getSequence(key).get().get(graphene.get().getTx(), 1);
        byte[] nameKey = getNameKey(nameBytes);
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
        throw new IllegalArgumentException("Id does not exist in storage " + id);
    }

    protected long getInstanceIdFromStorage(String name) {
        byte[] nameBytes = name.getBytes(UTF_8);
        Optional <byte[]> optionalId = instances.get(getNameKey(nameBytes));
        if (optionalId.isPresent()) {
            return Bytes.getLong(optionalId.get());
        }
        byte[] key = INSTANCE_DATABASE_NAME.getBytes(UTF_8);
        long id = graphene.get().getSequence(key).get().get(graphene.get().getTx(), 1);
        byte[] nameKey = getNameKey(nameBytes);
        byte[] idKey = getIdKey(id);
        instances.put(nameKey, Bytes.fromLong(id));
        instances.put(idKey, nameBytes);
        return id;
    }
    private byte[] getIdKey(long id) {
        byte[] k = Bytes.fromLong(id);
        return new byte[] { ID_PREFIX, k[0], k[1], k[2], k[3], k[4], k[5], k[6], k[7]};
    }

    private byte[] getNameKey(byte[] name) {
        byte[] key = new byte[name.length + 1];
        key[0] = NAME_PREFIX;
        System.arraycopy(name, 0, key, 1, name.length);
        return key;
    }

    public static void clear() {
        instanceIdCache.clear();
        instanceNameCache.clear();
        schemaIdCache.clear();
        schemaNameCache.clear();
    }
}