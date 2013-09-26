package org.deephacks.confit.internal.berkeley;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import org.deephacks.graphene.internal.FastKeyComparator;

import java.io.File;
import java.util.Arrays;

public class BareTest {
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
        Transaction tx = env.beginTransaction(null, null);
        DatabaseEntry k = new DatabaseEntry(new byte[]{1});
        DatabaseEntry v = new DatabaseEntry(new byte[]{1});
        db.put(tx, k, v);
        tx.commit();
        tx = env.beginTransaction(null, null);
        DatabaseEntry v1 = new DatabaseEntry(new byte[]{1});
        OperationStatus status = db.get(tx, k, v1, LockMode.DEFAULT);
        System.out.println(Arrays.toString(v1.getData()));
        tx.commit();
        tx = env.beginTransaction(null, null);
        status = db.get(tx, k, v1, LockMode.DEFAULT);
        System.out.println(Arrays.toString(v1.getData()));
        status = db.delete(tx, k);
        System.out.println(status);
        status = db.get(tx, k, v1, LockMode.DEFAULT);
        System.out.println(status);
        tx.abort();
        tx = env.beginTransaction(null, null);
        v1 = new DatabaseEntry();
        status = db.get(tx, k, v1, LockMode.DEFAULT);
        System.out.println(Arrays.toString(v1.getData()));
    }
}
