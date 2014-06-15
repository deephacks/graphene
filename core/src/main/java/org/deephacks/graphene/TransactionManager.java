package org.deephacks.graphene;

import org.deephacks.graphene.Transaction.Transactional;
import org.fusesource.lmdbjni.Cursor;
import org.fusesource.lmdbjni.Env;

import java.util.Stack;
import java.util.function.Function;
import java.util.function.Supplier;

class TransactionManager {
  private static ThreadLocal<Stack<Transaction>> threadLocal = new ThreadLocal<>();
  private final Graphene graphene;
  private final Env env;

  TransactionManager(Graphene graphene) {
    this.graphene = graphene;
    this.env = graphene.getEnv();
  }

  <T> T withTxReturn(boolean readOnly, Function<Transaction, T> function) {
    Transaction tx = beginTransaction(readOnly);
    try {
      T result = function.apply(tx);
      commit();
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

  <T> T withTxReadReturn(Function<Transaction, T> function) {
    return withTxReturn(true, function);
  }

  <T> T withTxWriteReturn(Function<Transaction, T> function) {
    return withTxReturn(false, function);
  }

  <T> T joinTxReadReturn(Function<Transaction, T> function) {
    Transaction tx = peek();
    if (tx == null) {
      return withTxReturn(true, function);
    } else {
      return function.apply(tx);
    }
  }

  <T> T joinTxWriteReturn(Function<Transaction, T> function) {
    Transaction tx = peek();
    if (tx == null) {
      return withTxWriteReturn(function);
    } else {
      if (tx.isReadOnly()) {
        throw new IllegalStateException("Cannot upgrade a read transaction to be writable.");
      } else {
        return function.apply(tx);
      }
    }
  }

  void withTxRead(Transactional transactional) {
    withTxReturn(true, tx -> {
      transactional.execute(tx);
      return null;
    });
  }

  void withTxWrite(Transactional transactional) {
    withTxReturn(false, tx -> {
      transactional.execute(tx);
      return null;
    });
  }

  void joinTxRead(Transactional transactional) {
    Transaction tx = peek();
    if (tx == null) {
      withTxRead(transactional);
    } else {
      if (!tx.isReadOnly()) {
        throw new IllegalStateException("Cannot downgrade a write transaction into read only.");
      } else {
        transactional.execute(tx);
      }
    }
  }

  void joinTxWrite(Transactional transactional) {
    Transaction tx = peek();
    if (tx == null) {
      withTxWrite(transactional);
    } else {
      if (tx.isReadOnly()) {
        throw new IllegalStateException("Cannot upgrade a read transaction to be writable.");
      } else {
        transactional.execute(tx);
      }
    }
  }

  <T> T inTxRead(Supplier<T> supplier) {
    return withTxReturn(true, tx -> supplier.get());
  }

  <T> T inTxWrite(Supplier<T> supplier) {
    return withTxReturn(false, tx -> supplier.get());
  }

  <T> T joinTxRead(Supplier<T> supplier) {
    Transaction tx = peek();
    if (tx == null) {
      return inTxRead(supplier);
    } else {
      return supplier.get();
    }
  }

  Transaction tx = peek();

  <T> T joinTxWrite(Supplier<T> supplier) {
    if (tx == null) {
      return inTxWrite(supplier);
    } else {
      return supplier.get();
    }
  }


  Transaction beginTransaction(boolean readOnly) {
    Transaction tx = new Transaction(graphene, env.createTransaction(readOnly), readOnly);
    push(tx);
    return tx;
  }

  Transaction joinReadWithWriteTransaction(org.fusesource.lmdbjni.Transaction readTx) {
    org.fusesource.lmdbjni.Transaction writeTx = env.createTransaction(readTx, false);
    Transaction tx = new Transaction(graphene, writeTx, false);
    push(tx);
    return tx;
  }

  void commit() {
    Transaction tx = pop();
    if (tx == null) {
      return;
    }
    tx.commit();
  }

  void rollback() {
    Transaction tx = pop();
    if (tx == null) {
      return;
    }
    tx.rollback();
  }

  void push(Transaction value) {
    Stack<Transaction> stack = threadLocal.get();
    if (stack == null) {
      stack = new Stack<>();
    }
    stack.push(value);
    threadLocal.set(stack);
  }

  Transaction peek() {
    Stack<Transaction> stack = threadLocal.get();
    if (stack == null || stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  Transaction pop() {
    Stack<Transaction> stack = threadLocal.get();
    if (stack == null || stack.isEmpty()) {
      return null;
    }
    return stack.pop();
  }

  void clear() {
    Stack<Transaction> stack = threadLocal.get();
    stack.clear();
    threadLocal.set(null);
  }

  void push(Cursor cursor) {
    Transaction tx = peek();
    if (tx == null) {
      throw new NullPointerException("No active transaction!");
    }
    tx.push(cursor);
  }

  org.fusesource.lmdbjni.Transaction getInternalTx() {
    Transaction tx = peek();
    if (tx == null) {
      return null;
    }
    return tx.getTx();
  }
}
