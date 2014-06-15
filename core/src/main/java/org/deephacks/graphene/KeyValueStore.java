package org.deephacks.graphene;

import org.fusesource.lmdbjni.Constants;
import org.fusesource.lmdbjni.Cursor;
import org.fusesource.lmdbjni.Database;
import org.fusesource.lmdbjni.Entry;
import org.fusesource.lmdbjni.GetOp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KeyValueStore {
  private final Database db;
  private final Graphene graphene;

  public KeyValueStore(Graphene graphene, Database db) {
    this.db = db;
    this.graphene = graphene;
  }

  public Optional<byte[]> get(byte[] key) {
    return graphene.joinTxReadReturn(tx -> {
      byte[] value;
      if ((value = db.get(tx.getTx(), key)) == null) {
        return Optional.empty();
      }
      return Optional.ofNullable(value);
    });
  }

  public boolean put(byte[] key, byte[] value) {
    return graphene.joinTxWriteReturn(tx -> {
      db.put(tx.getTx(), key, value, Constants.NOOVERWRITE);
      return true;
    });
  }

  public Map<byte[], byte[]> listAll() {
    return graphene.joinTxWriteReturn(tx -> {
      Map<byte[], byte[]> map = new HashMap<>();
      try (Cursor cursor = db.openCursor(tx.getTx())) {
        Entry entry;
        for (entry = cursor.get(GetOp.FIRST); entry != null; entry = cursor.get(Constants.NEXT)) {
          map.put(entry.getKey(), entry.getValue());
        }
      }
      return map;
    });
  }

}
