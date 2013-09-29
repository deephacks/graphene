package org.deephacks.graphene;

import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;

import java.util.Stack;

public class TransactionManager {
    private static final ThreadLocal<Stack<Transaction>> threadLocal = new ThreadLocal<>();
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
        Stack<Transaction> stack = threadLocal.get();
        if (stack == null) {
            stack = new Stack<>();
        }
        stack.push(value);
        threadLocal.set(stack);
    }

    public Transaction peek() {
        Stack<Transaction> stack = threadLocal.get();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    public Transaction pop() {
        Stack<Transaction> stack = threadLocal.get();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.pop();
    }

    public void clear() {
        Stack<Transaction> stack = threadLocal.get();
        stack.clear();
        threadLocal.set(null);
    }
}
