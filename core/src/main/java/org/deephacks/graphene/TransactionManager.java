package org.deephacks.graphene;

import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class TransactionManager {
    private static final ThreadLocal<Map<Class<?>, Stack<Transaction>>> threadLocal = new ThreadLocal<>();
    private final Environment environment;

    public TransactionManager(Environment environment) {
        this.environment = environment;
    }

    public Transaction beginTransaction() {
        Transaction tx = environment.beginTransaction(null, null);
        push(tx);
        return tx;
    }

    public void commit() {
        Transaction tx = pop();
        if (tx == null) {
            return;
        }
        tx.commit();
    }

    public void rollback() {
        Transaction tx = pop();
        if (tx == null) {
            return;
        }
        tx.abort();
    }

    public void push(Transaction value) {
        Map<Class<?>, Stack<Transaction>> map = threadLocal.get();
        if (map == null) {
            map = new HashMap<>();
        }
        Stack<Transaction> stack = map.get(Transaction.class);
        if (stack == null) {
            stack = new Stack<>();
        }
        stack.push(value);
        map.put(Transaction.class, stack);
        threadLocal.set(map);
    }

    public Transaction peek() {
        Map<Class<?>, Stack<Transaction>> map = threadLocal.get();
        if (map == null) {
            return null;
        }
        Stack<Transaction> stack = map.get(Transaction.class);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    public Transaction pop() {
        Map<Class<?>, Stack<Transaction>> map = threadLocal.get();
        if (map == null) {
            return null;
        }
        Stack<Transaction> stack = map.get(Transaction.class);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.pop();
    }

    public void clear() {
        Map<Class<?>, Stack<Transaction>> map = threadLocal.get();
        if (map == null) {
            return;
        }
        map.remove(Transaction.class);
    }
}
