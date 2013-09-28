package org.deephacks.graphene.manual;


import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryMultiKeyCreator;
import org.deephacks.graphene.internal.FastKeyComparator;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

public class SecTest {
    public static void main(String[] args) {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        Environment env = new Environment(new File("/tmp/txtest"), envConfig);

        DatabaseConfig primaryConfig = new DatabaseConfig();
        primaryConfig.setTransactional(true);
        primaryConfig.setAllowCreate(true);
        primaryConfig.setSortedDuplicates(false);
        primaryConfig.setBtreeComparator(new FastKeyComparator());
        primaryConfig.setKeyPrefixing(true);
        Database db = env.openDatabase(null, "p", primaryConfig);
        SecondaryConfig secConf = new SecondaryConfig();
        secConf.setTransactional(true);
        secConf.setAllowCreate(true);
        secConf.setSortedDuplicates(true);
        secConf.setBtreeComparator(new FastKeyComparator());
        secConf.setKeyPrefixing(true);
        secConf.setForeignKeyDatabase(db);

        final DatabaseEntry k1 = new DatabaseEntry(new byte[]{1});
        final DatabaseEntry k2 = new DatabaseEntry(new byte[]{2});
        final DatabaseEntry k3 = new DatabaseEntry(new byte[]{3});
        final DatabaseEntry v = new DatabaseEntry(new byte[] {0});


        secConf.setMultiKeyCreatorVoid(new SecondaryMultiKeyCreator() {
            @Override
            public void createSecondaryKeys(SecondaryDatabase secondary, DatabaseEntry key, DatabaseEntry data, Set<DatabaseEntry> results) {
                if (key.getData()[0] == 2 || key.getData()[0] == 3) {
                    System.out.println(Arrays.toString(key.getData()));
                    results.add(k1);
                    results.add(k1);
                    results.add(k1);
                }

            }
        });
        env.openSecondaryDatabase(null, "s", db, secConf);
        db.put(null, k1, v);
        db.put(null, k2, v);
        db.put(null, k3, v);

        db.delete(null, k3);
        db.delete(null, k1);
        db.delete(null, k2);



    }
}
