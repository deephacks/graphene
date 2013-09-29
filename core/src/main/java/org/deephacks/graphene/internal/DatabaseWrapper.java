package org.deephacks.graphene.internal;

import com.google.common.base.Optional;
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

public class DatabaseWrapper {
    private final Handle<Graphene> graphene = Graphene.get();
    private final Handle<Database> db;
    public DatabaseWrapper(Handle<Database> db) {
        this.db = db;
    }

    public Optional<byte[]> get(byte[] key) {
        Transaction tx = graphene.get().getTx();
        DatabaseEntry dbKey = new DatabaseEntry(key);
        DatabaseEntry dbValue = new DatabaseEntry();
        if (OperationStatus.NOTFOUND == db.get().get(tx, dbKey, dbValue, LockMode.RMW)) {
            return Optional.absent();
        }
        return Optional.fromNullable(dbValue.getData());
    }

    public boolean put(byte[] key, byte[] value) {
        Transaction tx = graphene.get().getTx();
        DatabaseEntry dbKey = new DatabaseEntry(key);
        DatabaseEntry dbValue = new DatabaseEntry(value);
        if (OperationStatus.KEYEXIST == db.get().putNoOverwrite(tx, dbKey, dbValue)) {
            return false;
        }
        return true;
    }

    public Map<byte[], byte[]> listAll() {
        Map<byte[], byte[]> map = new HashMap<>();
        try(Cursor cursor = db.get().openCursor(graphene.get().getTx(), null)) {
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
        try(Cursor cursor = db.get().openCursor(graphene.get().getTx(), null)) {
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
