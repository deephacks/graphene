package org.deephacks.graphene;

import org.fusesource.lmdbjni.Cursor;
import org.fusesource.lmdbjni.Env;
import org.fusesource.lmdbjni.Transaction;

import java.util.Stack;
import java.util.function.Function;
import java.util.function.Supplier;

public class TransactionManager {
  private static ThreadLocal<Stack<Tx>> threadLocal = new ThreadLocal<>();
  static Env env;

  public static <T> T withTxReturn(Function<Tx, T> function) {
    Tx tx = beginTransaction();
    try {
      T result = function.apply(tx);
      TransactionManager.commit();
      return result;
    } catch (Throwable e) {
      try {
        rollback();
      } catch (Throwable e1) {
        throw new IllegalStateException(e1);
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException(e);
    }
  }

  public static <T> T joinTxReturn(Function<Tx, T> function) {
    Tx tx = peek();
    if (tx == null) {
      return withTxReturn(function);
    } else {
      return function.apply(tx);
    }
  }

  public static void withTx(Transactional transactional) {
    withTxReturn(tx -> {
      transactional.execute(tx);
      return null;
    });
  }

  public static void joinTx(Transactional transactional) {
    Tx tx = peek();
    if (tx == null) {
      withTx(transactional);
    } else {
      transactional.execute(tx);
    }
  }

  public static <T> T inTx(Supplier<T> supplier) {
    return withTxReturn(tx -> supplier.get());
  }

  public static <T> T joinTx(Supplier<T> supplier) {
    Tx tx = peek();
    if (tx == null) {
      return inTx(supplier);
    } else {
      return supplier.get();
    }
  }

  public static interface Transactional {
    void execute(Tx tx);
  }


  static Tx beginTransaction() {
    Transaction transaction = env.createTransaction();
    Tx tx = new Tx(transaction);
    push(tx);
    return tx;
  }

  static void commit() {
    Tx tx = pop();
    if (tx == null) {
      return;
    }
    tx.commit();
  }

  static void rollback() {
    Tx tx = pop();
    if (tx == null) {
      return;
    }
    tx.rollback();
  }

  static void push(Tx value) {
    Stack<Tx> stack = threadLocal.get();
    if (stack == null) {
      stack = new Stack<>();
    }
    stack.push(value);
    threadLocal.set(stack);
  }

  static Tx peek() {
    Stack<Tx> stack = threadLocal.get();
    if (stack == null || stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  static Tx pop() {
    Stack<Tx> stack = threadLocal.get();
    if (stack == null || stack.isEmpty()) {
      return null;
    }
    return stack.pop();
  }

  static void clear() {
    Stack<Tx> stack = threadLocal.get();
    stack.clear();
    threadLocal.set(null);
  }

  static void push(Cursor cursor) {
    Tx tx = peek();
    if (tx == null) {
      throw new NullPointerException("No active transaction!");
    }
    tx.push(cursor);
  }

  public static Transaction getInternalTx() {
    Tx tx = peek();
    if (tx == null) {
      return null;
    }
    return tx.getTx();
  }

  public static class Tx {
    private Stack<Cursor> cursors = new Stack<>();
    private Transaction tx;

    private Tx(Transaction tx) {
      this.tx = tx;
    }

    public Transaction getTx() {
      return tx;
    }

    public void commit() {
      closeCursors();
      tx.commit();
    }

    public void rollback() {
      closeCursors();
      tx.abort();
    }

    public void push(Cursor cursor) {
      cursors.push(cursor);
    }

    private void closeCursors() {
      for (Cursor cursor : cursors) {
        try {
          cursor.close();
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
    }
  }
}
