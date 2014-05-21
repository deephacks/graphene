package org.deephacks.graphene;

import org.deephacks.graphene.internal.Bytes;
import org.deephacks.graphene.internal.EntityValidator;
import org.deephacks.graphene.internal.Serializer;
import org.deephacks.graphene.internal.Serializer.UnsafeSerializer;
import org.fusesource.lmdbjni.Database;
import org.fusesource.lmdbjni.Env;
import org.fusesource.lmdbjni.Transaction;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Graphene {
  private static final Handle<Graphene> INSTANCE = new Handle<>();
  public static final String TMP_DIR = System.getProperty("java.io.tmpdir");
  public static final String DEFAULT_GRAPHENE_DIR_NAME = "graphene.env";
  public static File DEFAULT_ENV_FILE = new File(TMP_DIR, DEFAULT_GRAPHENE_DIR_NAME);

  private Env env;

  private static final Handle<Database> primary = new Handle<>();
  private String primaryName = "graphene.primary";

  private static final Handle<Database> secondary = new Handle<>();
  private String secondaryName = "graphene.secondary";

  private static final Handle<Database> sequence = new Handle<>();
  private String sequenceName = "graphene.sequence";

  private static final Handle<Database> schema = new Handle<>();
  private static final String schemaName = "graphene.schema";

  private static final Handle<Database> instances = new Handle<>();
  private static final String instanceName = "graphene.instance";

  private Serializer defaultSerializer;
  private final Map<Class<?>, Serializer> serializers = new HashMap<>();

  private Optional<EntityValidator> validator;

  private Graphene() {
    DEFAULT_ENV_FILE.mkdirs();
    env = new Env();
    env.setMapSize(4_294_967_296L);
    env.setMaxDbs(10);
    env.open(DEFAULT_ENV_FILE.getPath());
    TransactionManager.env = env;
    try {
      validator = Optional.of(new EntityValidator());
    } catch (Throwable e) {
      validator = Optional.empty();
    }
    getPrimary();
    getSecondary();
    getInstance();
    getSequence();
    getSchema();
  }

  private Graphene(Env env, String primaryName, String secondaryName) {
    Guavas.checkNotNull(env);
    Guavas.checkNotNull(primaryName);
    Guavas.checkNotNull(secondary);
    this.env = env;
    this.primaryName = primaryName;
    this.secondaryName = secondaryName;
    TransactionManager.env = env;
    try {
      validator = Optional.of(new EntityValidator());
    } catch (Throwable e) {
      validator = Optional.empty();
    }
  }

  public static Handle<Graphene> create() {
    if (INSTANCE.get() != null) {
      throw new IllegalStateException("Graphene have already been created. Close it first.");
    }
    INSTANCE.set(new Graphene());
    init();
    return INSTANCE;
  }

  public static Handle<Graphene> create(Env env, String primaryName, String secondaryName) {
    if (INSTANCE.get() != null) {
      throw new IllegalStateException("Graphene have already been created. Close it first.");
    }
    INSTANCE.set(new Graphene(env, primaryName, secondaryName));
    init();
    return INSTANCE;
  }

  public static Handle<Graphene> get() {
    if (INSTANCE.get() != null) {
      return INSTANCE;
    }
    INSTANCE.set(new Graphene());
    return INSTANCE;
  }

  static void init() {
    INSTANCE.get().getPrimary();
    INSTANCE.get().getSecondary();
    INSTANCE.get().getSchema();
    INSTANCE.get().getInstance();
  }

  public Env getEnv() {
    return env;
  }

  public Handle<Database> getPrimary() {
    if (primary.get() == null) {
      primary.set(env.openDatabase(primaryName));
    }
    return primary;
  }

  public Handle<Database> getSecondary() {
    if (secondary.get() == null) {
      Database db = env.openDatabase(secondaryName);
      secondary.set(db);
    }
    return secondary;
  }

  public long increment(byte[] key) {
    synchronized (INSTANCE) {
      Transaction tx = TransactionManager.getInternalTx();
      byte[] seq;
      if (tx == null) {
        seq = getSequence().get().get(key);
      } else {
        seq = getSequence().get().get(tx, key);
      }
      long num = 0;
      if (seq != null) {
        num = Bytes.getLong(seq) + 1;
      }
      if (tx == null) {
        getSequence().get().put( key, Bytes.fromLong(num));
      } else {
        getSequence().get().put(tx, key, Bytes.fromLong(num));
      }
      return num;
    }
  }

  public Handle<Database> getSchema() {
    if (schema.get() == null) {
      schema.set(env.openDatabase(schemaName));
    }
    return schema;
  }

  public Handle<Database> getInstance() {
    if (instances.get() == null) {
      instances.set(env.openDatabase(instanceName));
    }
    return instances;
  }

  public Handle<Database> getSequence() {
    if (sequence.get() == null) {
      sequence.set(env.openDatabase(sequenceName));
    }
    return sequence;
  }

  public Optional<EntityValidator> getValidator() {
    return validator;
  }

  public void registerSerializer(Class<?> entityClass, Serializer serializer) {
    serializers.put(entityClass, serializer);
  }

  public Serializer getSerializer(Class<?> entityClass) {
    Serializer serializer = serializers.get(entityClass);
    if (serializer != null) {
      return serializer;
    }
    if (defaultSerializer == null) {
      defaultSerializer = new UnsafeSerializer();
    }
    return defaultSerializer;
  }

  public void close() {
    getSchema().get().close();
    getInstance().get().close();
    getSecondary().get().close();
    getPrimary().get().close();
    getSequence().get().close();
    INSTANCE.set(null);
    primary.set(null);
    secondary.set(null);
    schema.set(null);
    instances.set(null);
  }
}
