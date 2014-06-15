package org.deephacks.graphene;

import deephacks.streamql.Query;
import org.deephacks.graphene.Transaction.Transactional;
import org.deephacks.graphene.internal.EntityInterface;
import org.deephacks.graphene.internal.EntityValidator;
import org.deephacks.graphene.internal.FastKeyComparator;
import org.deephacks.graphene.internal.KeyInterface;
import org.deephacks.graphene.internal.serialization.BufAllocator;
import org.deephacks.graphene.internal.serialization.Bytes;
import org.deephacks.graphene.internal.serialization.KeySerialization.KeyWriter;
import org.deephacks.graphene.internal.serialization.UnsafeBufAllocator;
import org.deephacks.graphene.internal.serialization.ValueSerialization.ValueWriter;
import org.fusesource.lmdbjni.Constants;
import org.fusesource.lmdbjni.Cursor;
import org.fusesource.lmdbjni.Database;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.Env;
import org.fusesource.lmdbjni.GetOp;
import org.fusesource.lmdbjni.SeekOp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Graphene {
  private static final AtomicReference<Graphene> INSTANCE = new AtomicReference<>();
  private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
  private static final String DEFAULT_GRAPHENE_DIR_NAME = "graphene.env";
  private static final File DEFAULT_ENV_FILE = new File(TMP_DIR, DEFAULT_GRAPHENE_DIR_NAME);
  private static SchemaRepository SCHEMA_REPOSITORY;

  private static final String primaryName = "graphene.primary";
  private final Database primary;

  private static final String secondaryName = "graphene.secondary";
  private final Database secondary;

  private static final String sequenceName = "graphene.sequence";
  private final Database sequence;


  private static final String schemaName = "graphene.schema";
  private final Database schema;

  private static final String instanceName = "graphene.instance";
  private final Database instances;

  private final Env env;
  private final Optional<EntityValidator> validator;
  private final BufAllocator bufAllocator;
  private final UniqueIds uniqueIds;
  private final TransactionManager txManager;
  private final Set<Class> entities = new HashSet<>();

  public static Builder builder() {
    return new Builder();
  }

  private Graphene(Builder builder) {
    synchronized (INSTANCE) {
      if (INSTANCE.get() != null) {
        throw new IllegalStateException("Graphene have already been created. Close it first.");
      }
      File dir = builder.dir.orElse(DEFAULT_ENV_FILE);
      dir.mkdirs();
      Long size = builder.dbSizeInBytes.orElse(4_294_967_296L);
      this.bufAllocator = builder.bufAllocator.orElse(new UnsafeBufAllocator());
      this.env = new Env();
      this.env.setMapSize(size);
      this.env.setMaxDbs(10);
      this.env.open(dir.getPath());
      this.txManager = new TransactionManager(this);
      Optional<EntityValidator> maybeAbsent = Optional.empty();
      try {
        maybeAbsent = Optional.of(new EntityValidator());
      } catch (Throwable e) {
        // no validator found on classpath
      }
      validator = maybeAbsent;
      this.primary = env.openDatabase(primaryName);
      this.secondary = env.openDatabase(secondaryName);
      this.sequence = env.openDatabase(sequenceName);
      this.schema = env.openDatabase(schemaName);
      this.instances = env.openDatabase(instanceName);
      this.uniqueIds = new UniqueIds(this, new KeyValueStore(this, instances), new KeyValueStore(this, schema));
      Graphene.SCHEMA_REPOSITORY = new SchemaRepository(bufAllocator, uniqueIds);
      INSTANCE.set(this);
    }
  }

  public <E> boolean putAll(final List<E> entities) {
    Guavas.checkNotNull(entities);
    List<byte[][]> kvs = new ArrayList<>();
    for (E entity : entities) {
      EntityInterface iface = (EntityInterface) entity;
      if (validator.isPresent()) {
        validator.get().validate(entity);
      }
      Class<?> entityClass = entity.getClass();
      Schema<?> schema = SCHEMA_REPOSITORY.getSchema(entityClass);
      try {
        KeyWriter keyWriter = new KeyWriter(bufAllocator.allocateOutput(schema.getKeySchema().size()), schema.getKeySchema());
        ValueWriter valueWriter = new ValueWriter(bufAllocator.allocateOutput(), bufAllocator.allocateOutput(), uniqueIds);
        int schemaId = uniqueIds.getSchemaId(schema.getClazz());
        final byte[][] data = iface.serialize(keyWriter, valueWriter, schemaId);
        if (data == null || data.length != 2) {
          throw new IllegalArgumentException("Could not serialize type");
        }
        if (data[0].length == 0 && iface.isEmbedded()) {
          String msg = "Cannot store @Embedded classes " + entityClass.getName();
          throw new IllegalArgumentException(msg);
        }
        kvs.add(data);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    joinTxWrite(tx -> {
      for (byte[][] kv : kvs) {
        primary.put(tx.getTx(), kv[0], kv[1], Constants.NOOVERWRITE);
      }
    });
    return true;
  }

  /**
   * Put provided instance in storage, overwrite if instance already exist.
   *
   * @param entity instance to be written
   * @param <E>    instance type
   * @return true if writing the instance was successful
   */
  public <E> boolean put(final E entity) {
    return putAll(Arrays.asList(entity));
  }

  /**
   * Get an instance from storage and acquire a shared read lock on this instance.
   * The lock will be held until the transaction commit or rollback.
   *
   * @param key         primary key of instance
   * @param entityClass instance type
   * @param <E>         instance type
   * @return the instance if it exist, otherwise absent
   */
  public <E> Optional<E> get(Object key, Class<E> entityClass) {
    Schema<E> schema = SCHEMA_REPOSITORY.getSchema(entityClass);
    return joinTxReadReturn(transaction -> {
      Optional<byte[][]> optional = getKv(key, schema);
      if (optional.isPresent()) {
        return Optional.ofNullable(schema.getEntity(optional.get()));
      }
      return Optional.empty();
    });
  }

  /**
   * Select and return all instances.
   *
   * @param entityClass the type which is target for selection
   * @param <E>         instance type
   * @return instances match criteria
   */
  public <E> List<E> selectAll(Class<E> entityClass) {
    return joinTxReadReturn(tx -> {
      try (Stream<E> stream = tx.stream(entityClass)) {
        return stream.collect(Collectors.toList());
      }
    });
  }

  /**
   * Put an instance if it does not exist. If the instance exist, nothing will be written.
   *
   * @param entity instance to be written
   * @param <E>    instance type
   * @return true if the instance did not exist, false otherwise.
   */
  public <E> boolean putNoOverwrite(E entity) {
    Guavas.checkNotNull(entity);
    EntityInterface iface = (EntityInterface) entity;
    if (validator.isPresent()) {
      validator.get().validate(entity);
    }
    try {
      Class<?> entityClass = entity.getClass();
      Schema<?> schema = SCHEMA_REPOSITORY.getSchema(entityClass);
      KeyWriter keyWriter = schema.getKeyWriter();
      ValueWriter valueWriter = new ValueWriter(bufAllocator.allocateOutput(), bufAllocator.allocateOutput(), uniqueIds);
      int schemaId = uniqueIds.getSchemaId(schema.getClazz());
      final byte[][] data = iface.serialize(keyWriter, valueWriter, schemaId);
      if (data == null || data.length != 2) {
        throw new IllegalArgumentException("Could not serialize type");
      }
      if (data[0].length == 0 && iface.isEmbedded()) {
        String msg = "Cannot store @Embedded classes " + entityClass.getName();
        throw new IllegalArgumentException(msg);
      }
      return joinTxWriteReturn(tx -> primary.put(tx.getTx(), data[0], data[1], Constants.NOOVERWRITE) == null);
    } catch (IOException e) {
      // better exception!
      throw new RuntimeException(e);
    }
  }

  /**
   * Delete an instance with a specific primary key.
   *
   * @param key         primary key to delete
   * @param entityClass instance type to delete
   * @param <E>         instance type
   * @return the instance if it was deleted, otherwise absent
   * @throws DeleteConstraintException if another instance have a reference on the deleted instance
   */
  public <E> Optional<E> delete(Object key, Class<E> entityClass) throws DeleteConstraintException {
    Schema<E> schema = SCHEMA_REPOSITORY.getSchema(entityClass);
    try {
      return joinTxWriteReturn(tx -> {
        final Optional<byte[][]> optional = getKv(key, schema);
        if (!optional.isPresent()) {
          return Optional.empty();
        }
        if (!primary.delete(tx.getTx(), optional.get()[0])) {
          return Optional.empty();
        }
        return Optional.ofNullable(schema.getEntity(optional.get()));
      });
    } catch (DeleteConstraintException e) {
        /* FIXME
        throw new org.deephacks.graphene.DeleteConstraintException(
                new RowKey(e.getPrimaryKey().getData()) + " have a reference to " + new RowKey(e.getSecondaryKey().getData()), e);
        */
      throw new RuntimeException(e);
    }
  }

  public <E> void deleteAll(Class<E> entityClass) {
    Schema<E> schema = SCHEMA_REPOSITORY.getSchema(entityClass);
    try {
      joinTxWrite(tx -> {
        byte[] firstKey = schema.getMinKey();
        byte[] lastKey = schema.getMaxKey();
        try (Cursor c = primary.openCursor(tx.getTx())) {
          Entry entry = c.seek(SeekOp.RANGE, firstKey);
          // this is a awkward, keys are first collected then deleted
          // ideally entries should be deleted as the cursor is forwarded
          List<byte[]> keys = new ArrayList<>();
          while (entry  != null) {
            if (!FastKeyComparator.withinKeyRange(entry.getKey(), firstKey, lastKey)) {
              break;
            }
            keys.add(entry.getKey());
            // c.delete() would be more appropriate but this seems to mess
            // up the position of the cursor.
            entry = c.get(GetOp.NEXT);
          }
          for (byte[] k : keys) {
            c.seek(SeekOp.KEY, k);
            c.delete();
          }
        }
      });
    } catch (DeleteConstraintException e)
    {
      /* FIXME
      throw new org.deephacks.graphene.DeleteConstraintException(
              new RowKey(e.getPrimaryKey().getData()) + " have a reference to " + new RowKey(e.getSecondaryKey().getData()), e);
              */
    }
  }

  private void printKey(String m, byte[] key) {
    byte[] array = new byte[2];
    System.arraycopy(key, 4, array, 0, 2);
    String name = uniqueIds.getSchemaName(key[0]);
    System.out.println("> " + m + " " + name + " " + new String(array) + " " + Arrays.toString(key));
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> query(String query, Class<T> cls) {
    return joinTxReadReturn(tx -> {
      final Cursor cursor = primary.openCursor(tx.getTx());
      Query<T> q = Query.parse(query, cls);
      Schema<?> schema = SCHEMA_REPOSITORY.getSchema(q.getType());
      StreamResultSet objects = new StreamResultSet<>(schema, cursor);
      Spliterator spliterator = objects.spliterator();
      try (Stream stream = StreamSupport.stream(spliterator, false)) {
        tx.push(cursor);
        return q.collect(stream);
      }
    });
  }


  private <E> Optional<byte[][]> getKv(Object key, Schema<E> schema) {
    KeyInterface iface = schema.getKey(key);
    try {
      KeyWriter keyWriter = schema.getKeyWriter();
      final byte[] dataKey = iface.serializeKey(keyWriter, schema.getSchemaId());
      return joinTxReadReturn(tx -> {
        byte[] value;
        if ((value = primary.get(tx.getTx(), dataKey)) != null) {
          byte[][] kv = new byte[][]{dataKey, value};
          return Optional.ofNullable(kv);
        }
        return Optional.empty();
      });
    } catch (IOException e) {
      // TODO: fix proper exception
      throw new RuntimeException(e);
    }
  }

  public <T> T withTxReadReturn(Function<Transaction, T> function) {
    return txManager.withTxReadReturn(function);
  }

  public <T> T withTxWriteReturn(Function<Transaction, T> function) {
    return txManager.withTxWriteReturn(function);
  }

  public <T> T joinTxReadReturn(Function<Transaction, T> function) {
    return txManager.joinTxReadReturn(function);
  }

  public <T> T joinTxWriteReturn(Function<Transaction, T> function) {
    return txManager.joinTxWriteReturn(function);
  }

  public void withTxRead(Transactional transactional) {
    txManager.withTxRead(transactional);
  }

  public void withTxWrite(Transactional transactional) {
    txManager.withTxWrite(transactional);
  }


  public void joinTxRead(Transactional transactional) {
    txManager.joinTxRead(transactional);
  }

  public void joinTxWrite(Transactional transactional) {
    txManager.joinTxWrite(transactional);
  }


  public <T> T inTxRead(Supplier<T> supplier) {
    return txManager.inTxRead(supplier);
  }

  public <T> T inTxWrite(Supplier<T> supplier) {
    return txManager.inTxWrite(supplier);
  }

  public void close() {
    schema.close();
    instances.close();
    secondary.close();
    primary.close();
    sequence.close();
    INSTANCE.set(null);
  }

  Env getEnv() {
    return env;
  }

  long increment(byte[] key) {
    return joinTxWriteReturn(tx -> {
      byte[] seq = sequence.get(tx.getTx(), key);
      long num = 0;
      if (seq != null) {
        num = Bytes.getLong(seq) + 1;
      }
      sequence.put(tx.getTx(), key, Bytes.fromLong(num));
      return num;
    });
  }

  TransactionManager getTxManager() {
    return txManager;
  }

  Optional<EntityValidator> getValidator() {
    return validator;
  }

  UniqueIds getUniqueIds() {
    return uniqueIds;
  }

  BufAllocator getBufAllocator() {
    return bufAllocator;
  }

  public Cursor openPrimaryCursor(org.fusesource.lmdbjni.Transaction tx) {
    return primary.openCursor(tx);
  }

  public <T> Schema<T> getSchema(Class<T> cls) {
    return SCHEMA_REPOSITORY.getSchema(cls);
  }

  public void get(String s) {
  }

  public static class Builder {
    private Optional<BufAllocator> bufAllocator = Optional.empty();
    private Optional<Long> dbSizeInBytes = Optional.empty();
    private Optional<File> dir = Optional.empty();

    public Builder withDbSize(Long dbSizeInBytes) {
      this.dbSizeInBytes = Optional.ofNullable(dbSizeInBytes);
      return this;
    }

    public Builder withDir(File dir) {
      this.dir = Optional.ofNullable(dir);
      return this;
    }

    public Builder withBufAllocator(BufAllocator bufAllocator) {
      this.bufAllocator = Optional.ofNullable(bufAllocator);
      return this;
    }

    public synchronized Graphene build() {
      return new Graphene(this);
    }
  }

}
