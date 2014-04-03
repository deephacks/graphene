package org.deephacks.graphene;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;

import java.util.Stack;
import java.util.function.Function;

public class TransactionManager {
  private static final Handle<Graphene> graphene = Graphene.get();
  private static final TransactionManager tm = graphene.get().getTransactionManager();
  private static final ThreadLocal<Stack<Tx>> threadLocal = new ThreadLocal<>();
  private final Environment environment;


  public TransactionManager(Environment environment) {
    this.environment = environment;
  }

  public static <T> T withTxReturn(Function<Tx, T> function) {
    Tx tx = tm.beginTransaction();
    try {
      T result = function.apply(tx);
      switch (tx.getTx().getState()) {
        case OPEN:
          tm.commit();
          break;
        case POSSIBLY_COMMITTED:
          break;
        case COMMITTED:
          break;
        case MUST_ABORT:
          tm.rollback();
          break;
        case ABORTED:
          tm.rollback();
          break;
      }
      return result;
    } catch (Throwable e) {
      try {
        tm.rollback();
      } catch (Throwable e1) {
        throw new IllegalStateException(e1);
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException(e);
    }
  }

  public static void withTx(Transactional transactional) {
    withTxReturn(tx -> {
      transactional.execute(tx);
      return null;
    });
  }

  public static interface Transactional {
    void execute(Tx tx);
  }


  Tx beginTransaction() {
    Transaction transaction = environment.beginTransaction(null, null);
    Tx tx = new Tx(transaction);
    push(tx);
    return tx;
  }

  void commit() {
    Tx tx = pop();
    if (tx == null) {
      return;
    }
    tx.commit();
  }

  void rollback() {
    Tx tx = pop();
    if (tx == null) {
      return;
    }
    tx.rollback();
  }

  void push(Tx value) {
    Stack<Tx> stack = threadLocal.get();
    if (stack == null) {
      stack = new Stack<>();
    }
    stack.push(value);
    threadLocal.set(stack);
  }

  Tx peek() {
    Stack<Tx> stack = threadLocal.get();
    if (stack == null || stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  Tx pop() {
    Stack<Tx> stack = threadLocal.get();
    if (stack == null || stack.isEmpty()) {
      return null;
    }
    return stack.pop();
  }

  void clear() {
    Stack<Tx> stack = threadLocal.get();
    stack.clear();
    threadLocal.set(null);
  }

  void push(Cursor cursor) {
    Tx tx = peek();
    if (tx == null) {
      throw new NullPointerException("No active transaction!");
    }
    tx.push(cursor);
  }

  public Transaction getInternalTx() {
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
      if (tx.isValid()) {
        tx.commit();
      }
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
