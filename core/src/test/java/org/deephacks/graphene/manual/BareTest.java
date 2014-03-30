package org.deephacks.graphene.manual;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Sequence;
import com.sleepycat.je.SequenceConfig;
import com.sleepycat.je.Transaction;
import org.deephacks.graphene.BaseTest.A;
import org.deephacks.graphene.BaseTest.B;
import org.deephacks.graphene.BaseTest.C;
import org.deephacks.graphene.internal.FastKeyComparator;
import org.deephacks.graphene.internal.RowKey;
import org.deephacks.graphene.internal.UniqueIds;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bare Berkeley DB manual tests
 */
public class BareTest {
  public static void main(String[] args) throws Exception {
    UniqueIds ids = new UniqueIds();
    ids.listSchemas();

    final EnvironmentConfig envConfig = new EnvironmentConfig();
    envConfig.setAllowCreate(true);
    envConfig.setTransactional(true);
    final Environment env = new Environment(new File("/tmp/txtest"), envConfig);

    DatabaseConfig primaryConfig = new DatabaseConfig();
    primaryConfig.setTransactional(true);
    primaryConfig.setAllowCreate(true);
    primaryConfig.setSortedDuplicates(false);
    primaryConfig.setBtreeComparator(new FastKeyComparator());
    primaryConfig.setKeyPrefixing(true);
    final Database db = env.openDatabase(null, "p", primaryConfig);


    final ExecutorService executorService = Executors.newFixedThreadPool(10);
    for (int i = 0; i < 1000; i++) {
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          final Transaction tx = env.beginTransaction(null, null);
          SequenceConfig c = new SequenceConfig();
          c.setAllowCreate(true);
          final byte[] key = new byte[]{1};
          try (Sequence seq = db.openSequence(null, new DatabaseEntry(key), c)) {
            System.out.println(seq.get(null, 10));
          }
          tx.commit();
        }
      });
    }
    Thread.sleep(10000);
    List<byte[]> list = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      list.add(new RowKey(A.class, "" + i).getKey());
      list.add(new RowKey(B.class, "" + i).getKey());
      list.add(new RowKey(C.class, "" + i).getKey());
    }
    Collections.shuffle(list);
    for (byte[] b : list) {
      System.out.println(Arrays.toString(b));
      db.put(null, new DatabaseEntry(b), new DatabaseEntry(b));
    }
    Cursor cursor = db.openCursor(null, null);
    DatabaseEntry key = new DatabaseEntry();
    OperationStatus status = cursor.getFirst(key, new DatabaseEntry(), LockMode.DEFAULT);
    System.out.println("---");
    while ((status = cursor.getNext(key, new DatabaseEntry(), LockMode.DEFAULT)) == OperationStatus.SUCCESS) {
      System.out.println(Arrays.toString(key.getData()));
    }

  }
}
