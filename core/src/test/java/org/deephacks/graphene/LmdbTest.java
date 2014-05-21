package org.deephacks.graphene;

import org.fusesource.lmdbjni.Constants;
import org.fusesource.lmdbjni.Cursor;
import org.fusesource.lmdbjni.Database;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.Env;
import org.fusesource.lmdbjni.LMDBException;
import org.fusesource.lmdbjni.Transaction;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LmdbTest {

  @Test
  public void test_lmdb() throws InterruptedException {
    System.out.println(System.getProperty("java.library.path"));

    String path = "/tmp";
    Env env = new Env();
    env.setMapSize(4_294_967_296L);
    env.setMaxDbs(2);

    // env.open(path, 0x10000);
    env.open(path);

    Database db = env.openDatabase("foo");
    create(db, env);
    list(db, env);
    delete(db, env);
    list(db, env);
    db.close();
    env.close();
  }

  public static void create(Database db, Env env) {
    List<byte[]> list = new ArrayList<>();
    for (int i = 0; i < 100_000; i++) {
      list.add(UUID.randomUUID().toString().getBytes());
      if (i % 10000 == 0) {
        // byte[] result = db.get(tx, bytes);
        System.out.println(i);
      }
    }
    System.out.println(list.size());
    long before = System.currentTimeMillis();
    try {
      Transaction tx = env.createTransaction();
      int i = 1;
      for (byte[] bytes : list) {
        db.put(tx, bytes, bytes);
        if (i++ % 10000 == 0) {
          System.out.println(i + " " + new String(bytes));
        }
      }
      tx.commit();
      System.out.println((System.currentTimeMillis() - before));
    } catch (LMDBException e) {
      System.out.println(e.getErrorCode());
    }
  }

  public static void list(Database db, Env env) {
    Transaction tx = env.createTransaction(true);
    Cursor cursor = db.openCursor(tx);
    int i = 1;
    long before = System.currentTimeMillis();
    for (Entry entry = cursor.get(Constants.FIRST); entry != null; entry = cursor.get(Constants.NEXT)) {
      byte[] key = entry.getKey();
      if (i++ % 10000 == 0) {
        System.out.println(i + " " + new String(key));
      }
    }
    System.out.println((System.currentTimeMillis() - before));
    tx.commit();
  }

  public static void delete(Database db, Env env) {
    Transaction tx = env.createTransaction();
    Cursor cursor = db.openCursor(tx);
    int i = 1;
    long before = System.currentTimeMillis();
    for (Entry entry = cursor.get(Constants.FIRST); entry != null; entry = cursor.get(Constants.NEXT)) {
      cursor.delete();
      if (i++ % 10000 == 0) {
        System.out.println(i + " delete");
      }
    }
    System.out.println((System.currentTimeMillis() - before));
    tx.commit();
  }
}
