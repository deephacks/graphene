package org.deephacks.graphene;

import com.google.common.base.Preconditions;

public class Handle<T> {
    private T instance;

    public Handle(T instance) {
        Preconditions.checkNotNull(instance);
        this.instance = instance;
    }

    public Handle() {

    }

    public T get() {
        return instance;
    }

    public void set(T instance) {
        this.instance = instance;
    }
}
