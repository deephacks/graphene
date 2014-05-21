package org.deephacks.graphene.internal;

import org.deephacks.graphene.Handle;
import org.fusesource.lmdbjni.Constants;
import org.fusesource.lmdbjni.Cursor;
import org.fusesource.lmdbjni.Database;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.SeekOp;
import org.fusesource.lmdbjni.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.deephacks.graphene.TransactionManager.getInternalTx;

public class DatabaseWrapper {
  private final Handle<Database> db;

  public DatabaseWrapper(Handle<Database> db) {
    this.db = db;
  }

  public Optional<byte[]> get(byte[] key) {
    Transaction tx = getInternalTx();
    byte[] value;
    if (tx == null) {
      if ((value = db.get().get(key)) == null) {
        return Optional.empty();
      }
    } else {
      if ((value = db.get().get(tx, key)) == null) {
        return Optional.empty();
      }
    }
    return Optional.ofNullable(value);
  }

  public boolean put(byte[] key, byte[] value) {
    Transaction tx = getInternalTx();
    if (tx == null) {
      db.get().put(key, value, Constants.NOOVERWRITE);
    } else {
      db.get().put(getInternalTx(), key, value, Constants.NOOVERWRITE);
    }
    return true;
  }

  public Map<byte[], byte[]> listAll() {
    Map<byte[], byte[]> map = new HashMap<>();
    try (Cursor cursor = db.get().openCursor(getInternalTx())) {
      byte[] firstKey = RowKey.getMinId().getKey();
      Entry entry;
      for (entry = cursor.seek(SeekOp.RANGE, firstKey); entry != null; entry = cursor.get(Constants.NEXT)) {
        map.put(entry.getKey(), entry.getValue());
      }
    }
    return map;
  }

  public void deleteAll() {
    try (Cursor cursor = db.get().openCursor(getInternalTx())) {
      byte[] firstKey = RowKey.getMinId().getKey();

      for (Entry entry = cursor.seek(SeekOp.RANGE, firstKey); entry != null; entry = cursor.get(Constants.NEXT)) {
        cursor.delete();
      }
    }
  }
}
