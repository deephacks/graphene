package org.deephacks.graphene;

public class Handle<T> {
    private T instance;

    public Handle(T instance) {
        Guavas.checkNotNull(instance);
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
