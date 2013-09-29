package org.deephacks.graphene.cdi;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ThreadLocalManager {
    private static final ThreadLocal<Map<Class<?>, Stack<Object>>> threadLocal = new ThreadLocal<>();

    public static <T> void push(Class<T> cls, T value) {
        Map<Class<?>, Stack<Object>> map = threadLocal.get();
        if (map == null) {
            map = new HashMap<Class<?>, Stack<Object>>();
        }
        Stack<Object> stack = map.get(cls);
        if (stack == null) {
            stack = new Stack<Object>();
        }
        stack.push(value);
        map.put(cls, stack);
        threadLocal.set(map);
    }

    public static <T> T peek(Class<T> cls) {
        Map<Class<?>, Stack<Object>> map = threadLocal.get();
        if (map == null) {
            return null;
        }
        Stack<Object> stack = map.get(cls);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return cls.cast(stack.peek());
    }

    public static <T> T pop(Class<T> cls) {
        Map<Class<?>, Stack<Object>> map = threadLocal.get();
        if (map == null) {
            return null;
        }
        Stack<Object> stack = map.get(cls);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return cls.cast(stack.pop());
    }

    public static <T> void clear(Class<T> cls) {
        Map<Class<?>, Stack<Object>> map = threadLocal.get();
        if (map == null) {
            return;
        }
        map.remove(cls);
    }

    public static void clear() {
        threadLocal.set(null);
    }
}
