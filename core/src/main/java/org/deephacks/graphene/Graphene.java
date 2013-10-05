package org.deephacks.graphene;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.ForeignKeyDeleteAction;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.Sequence;
import com.sleepycat.je.SequenceConfig;
import org.deephacks.graphene.EntityRepository.KeyCreator;
import org.deephacks.graphene.internal.EntityValidator;
import org.deephacks.graphene.internal.FastKeyComparator;
import org.deephacks.graphene.internal.Serializer;
import org.deephacks.graphene.internal.Serializer.UnsafeSerializer;
import org.deephacks.graphene.internal.UniqueIds;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Graphene {
    private static final Handle<Graphene> INSTANCE = new Handle<>();
    private static TransactionManager TM;
    public static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    public static final String DEFAULT_GRAPHENE_DIR_NAME = "graphene.env";
    public static File DEFAULT_ENV_FILE = new File(TMP_DIR, DEFAULT_GRAPHENE_DIR_NAME);

    private Environment env;

    private static final Handle<Database> primary = new Handle<>();
    private DatabaseConfig primaryConfig;
    private String primaryName = "graphene.primary";

    private static final Handle<SecondaryDatabase> secondary = new Handle<>();
    private SecondaryConfig secondaryConfig;
    private String secondaryName = "graphene.secondary";

    private static final Handle<Database> sequence = new Handle<>();
    private DatabaseConfig sequenceConfig;
    private String sequenceName = "graphene.sequence";
    private static Sequence SEQUENCE;

    private static final Handle<Database> schemas = new Handle<>();
    private static final String schemaName = "graphene.schema";

    private static final Handle<Database> instances = new Handle<>();
    private static final String instanceName = "graphene.instance";

    private static final Map<Integer, Handle<Sequence>> sequences = new HashMap<>();

    private Serializer defaultSerializer;
    private final Map<Class<?>, Serializer> serializers = new HashMap<>();

    private Optional<EntityValidator> validator;

    private Graphene() {
        DEFAULT_ENV_FILE.mkdirs();
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        envConfig.setDurability(Durability.COMMIT_SYNC);
        envConfig.setLockTimeout(1, TimeUnit.SECONDS);
        System.out.println(DEFAULT_ENV_FILE.getAbsolutePath());
        env = new Environment(DEFAULT_ENV_FILE, envConfig);
        TM = new TransactionManager(env);
        try {
            validator = Optional.of(new EntityValidator());
        } catch (Throwable e) {
            validator = Optional.absent();
        }
    }

    private Graphene(Environment env, String primaryName, String secondaryName) {
        Preconditions.checkNotNull(env);
        Preconditions.checkNotNull(primaryName);
        Preconditions.checkNotNull(secondary);
        Preconditions.checkArgument(env.getConfig().getTransactional(), "Environment must be transactional");
        this.env = env;
        this.primaryName = primaryName;
        this.secondaryName = secondaryName;
        TM = new TransactionManager(env);
        try {
            validator = Optional.of(new EntityValidator());
        } catch (Throwable e) {
            validator = Optional.absent();
        }
    }

    private Graphene(Environment env, String primaryName, DatabaseConfig primaryConfig, String secondaryName, SecondaryConfig secondaryConfig) {
        Preconditions.checkNotNull(env);
        Preconditions.checkNotNull(primaryName);
        Preconditions.checkNotNull(secondaryName);
        Preconditions.checkArgument(env.getConfig().getTransactional(), "Environment must be transactional");
        this.env = env;
        this.primaryConfig = primaryConfig;
        this.primaryName = primaryName;
        this.secondaryConfig = secondaryConfig;
        this.secondaryName = secondaryName;
        TM = new TransactionManager(env);
        try {
            validator = Optional.of(new EntityValidator());
        } catch (Throwable e) {
            validator = Optional.absent();
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

    public static Handle<Graphene> create(Environment env, String primaryName, String secondaryName) {
        if (INSTANCE.get() != null) {
            throw new IllegalStateException("Graphene have already been created. Close it first.");
        }
        INSTANCE.set(new Graphene(env, primaryName, secondaryName));
        init();
        return INSTANCE;
    }

    public static Handle<Graphene> create(Environment env, String primaryName, DatabaseConfig primaryConfig, String secondaryName, SecondaryConfig secondaryConfig) {
        if (INSTANCE.get() != null) {
            throw new IllegalStateException("Graphene have already been created. Close it first.");
        }
        INSTANCE.set(new Graphene(env, primaryName, primaryConfig, secondaryName, secondaryConfig));
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
        INSTANCE.get().getSchemas();
        INSTANCE.get().getInstances();
    }

    public Environment getEnv() {
        return env;
    }

    public Handle<Database> getPrimary() {
        Preconditions.checkArgument(getPrimaryConfig().getTransactional(), "Primary must be transactional");
        if (primary.get() == null) {
            primary.set(env.openDatabase(null, primaryName, primaryConfig));
        }
        return primary;
    }

    public DatabaseConfig getPrimaryConfig() {
        if (primaryConfig == null) {
            primaryConfig = new DatabaseConfig();
            primaryConfig.setTransactional(true);
            primaryConfig.setAllowCreate(true);
            primaryConfig.setSortedDuplicates(false);
            primaryConfig.setBtreeComparator(new FastKeyComparator());
            primaryConfig.setKeyPrefixing(true);
        }
        return primaryConfig;
    }

    public Handle<SecondaryDatabase> getSecondary() {
        Preconditions.checkArgument(getSecondaryConfig().getTransactional(), "Secondary must be transactional");
        if (secondary.get() == null) {
            SecondaryDatabase db = env.openSecondaryDatabase(null, secondaryName, getPrimary().get(), secondaryConfig);
            secondary.set(db);
        }
        return secondary;
    }

    public SecondaryConfig getSecondaryConfig() {
        if (secondaryConfig == null) {
            secondaryConfig = new SecondaryConfig();
            secondaryConfig.setAllowCreate(true);
            secondaryConfig.setTransactional(true);
            secondaryConfig.setKeyPrefixing(true);
            secondaryConfig.setMultiKeyCreator(new KeyCreator());
            secondaryConfig.setForeignKeyDatabase(getPrimary().get());
            secondaryConfig.setForeignKeyDeleteAction(ForeignKeyDeleteAction.ABORT);
            secondaryConfig.setBtreeComparator(new FastKeyComparator());
            secondaryConfig.setSortedDuplicates(true);
        }
        return secondaryConfig;
    }


    public long increment(byte[] key) {
        SequenceConfig config = new SequenceConfig();
        config.setAllowCreate(true);

        try (Sequence seq = getSequence().get().openSequence(TM.peek(), new DatabaseEntry(key), config)) {
            return seq.get(TM.peek(), 1);
        }
    }

    public Handle<Database> getSchemas() {
        if (schemas.get() == null) {
            DatabaseConfig config = new DatabaseConfig();
            config.setTransactional(true);
            config.setAllowCreate(true);
            config.setSortedDuplicates(false);
            schemas.set(env.openDatabase(null, schemaName, config));
        }
        return schemas;
    }

    public Handle<Database> getInstances() {
        if (instances.get() == null) {
            DatabaseConfig config = new DatabaseConfig();
            config.setTransactional(true);
            config.setAllowCreate(true);
            config.setSortedDuplicates(false);
            instances.set(env.openDatabase(null, instanceName, config));
        }
        return instances;
    }

    public Handle<Database> getSequence() {
        if (sequence.get() == null) {
            sequenceConfig = new DatabaseConfig();
            sequenceConfig.setTransactional(true);
            sequenceConfig.setAllowCreate(true);
            sequenceConfig.setSortedDuplicates(false);
            sequenceConfig.setBtreeComparator(new FastKeyComparator());
            sequenceConfig.setKeyPrefixing(true);
            sequence.set(env.openDatabase(null, sequenceName, sequenceConfig));
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
        if (defaultSerializer == null){
            defaultSerializer = new UnsafeSerializer();
        }
        return defaultSerializer;
    }

    public TransactionManager getTransactionManager() {
        return TM;
    }

    public void close() {
        getSchemas().get().close();
        getInstances().get().close();
        getSecondary().get().close();
        getPrimary().get().close();
        getSequence().get().close();
        INSTANCE.set(null);
        primaryConfig = null;
        secondaryConfig = null;
        primary.set(null);
        secondary.set(null);
        schemas.set(null);
        instances.set(null);
        SEQUENCE.close();
    }

    public void closeAndDelete() {
        UniqueIds.clear();
        close();
        remove(schemaName);
        remove(instanceName);
        remove(secondaryName);
        remove(primaryName);
    }

    private void remove(String name) {
        try {
            env.removeDatabase(null, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
