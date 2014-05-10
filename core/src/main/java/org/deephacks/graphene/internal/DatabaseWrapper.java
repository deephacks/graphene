package org.deephacks.graphene.internal;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import org.deephacks.graphene.Graphene;
import org.deephacks.graphene.Handle;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.deephacks.graphene.TransactionManager.getInternalTx;

public class DatabaseWrapper {
  private final Handle<Graphene> graphene = Graphene.get();
  private final Handle<Database> db;

  public DatabaseWrapper(Handle<Database> db) {
    this.db = db;
  }

  public Optional<byte[]> get(byte[] key) {
    DatabaseEntry dbKey = new DatabaseEntry(key);
    DatabaseEntry dbValue = new DatabaseEntry();
    Transaction tx = getInternalTx();
    LockMode lockMode = tx == null ? LockMode.DEFAULT : LockMode.RMW ;
    if (OperationStatus.NOTFOUND == db.get().get(tx, dbKey, dbValue, lockMode)) {
      return Optional.empty();
    }
    return Optional.ofNullable(dbValue.getData());
  }

  public boolean put(byte[] key, byte[] value) {
    DatabaseEntry dbKey = new DatabaseEntry(key);
    DatabaseEntry dbValue = new DatabaseEntry(value);
    if (OperationStatus.KEYEXIST == db.get().putNoOverwrite(getInternalTx(), dbKey, dbValue)) {
      return false;
    }
    return true;
  }

  public Map<byte[], byte[]> listAll() {
    Map<byte[], byte[]> map = new HashMap<>();
    try (Cursor cursor = db.get().openCursor(getInternalTx(), null)) {
      DatabaseEntry firstKey = new DatabaseEntry(RowKey.getMinId().getKey());
      DatabaseEntry entry = new DatabaseEntry();
      if (cursor.getSearchKeyRange(firstKey, entry, LockMode.RMW) == OperationStatus.SUCCESS) {
        map.put(firstKey.getData(), entry.getData());
      }

      while (cursor.getNextNoDup(firstKey, entry, LockMode.RMW) == OperationStatus.SUCCESS) {
        map.put(firstKey.getData(), entry.getData());
      }
    }
    return map;
  }

  public void deleteAll() {
    try (Cursor cursor = db.get().openCursor(getInternalTx(), null)) {
      DatabaseEntry firstKey = new DatabaseEntry(RowKey.getMinId().getKey());

      if (cursor.getSearchKeyRange(firstKey, new DatabaseEntry(), LockMode.RMW) == OperationStatus.SUCCESS) {
        cursor.delete();
      }

      while (cursor.getNextNoDup(firstKey, new DatabaseEntry(), LockMode.RMW) == OperationStatus.SUCCESS) {
        cursor.delete();
      }
    }
  }
}
