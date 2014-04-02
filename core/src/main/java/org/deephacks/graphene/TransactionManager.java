package org.deephacks.graphene;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;

import java.util.Stack;

public class TransactionManager {
  private static final ThreadLocal<Stack<Tx>> threadLocal = new ThreadLocal<>();
  private final Environment environment;

  public TransactionManager(Environment environment) {
    this.environment = environment;
  }

  public Tx beginTransaction() {
    Transaction transaction = environment.beginTransaction(null, null);
    Tx tx = new Tx(transaction);
    push(tx);
    return tx;
  }

  public void commit() {
    Tx tx = pop();
    if (tx == null) {
      return;
    }
    tx.commit();
  }

  public void rollback() {
    Tx tx = pop();
    if (tx == null) {
      return;
    }
    tx.rollback();
  }

  public void push(Tx value) {
    Stack<Tx> stack = threadLocal.get();
    if (stack == null) {
      stack = new Stack<>();
    }
    stack.push(value);
    threadLocal.set(stack);
  }

  public Tx peek() {
    Stack<Tx> stack = threadLocal.get();
    if (stack == null || stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  public Tx pop() {
    Stack<Tx> stack = threadLocal.get();
    if (stack == null || stack.isEmpty()) {
      return null;
    }
    return stack.pop();
  }

  public void clear() {
    Stack<Tx> stack = threadLocal.get();
    stack.clear();
    threadLocal.set(null);
  }

  public void push(Cursor cursor) {
    Tx tx = peek();
    if (tx == null) {
      throw new NullPointerException("No active transaction!");
    }
    tx.push(cursor);
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
