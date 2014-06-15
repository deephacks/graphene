package org.deephacks.graphene;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@SuppressWarnings("all")
public class BuilderProxy<T> {
  private Object object;
  private Class<T> cls;

  public BuilderProxy(T object) {
    this.object = object;
    this.cls = cls;
  }

  public <T> T get(String property) {
    String method = "get" + Character.toLowerCase(property.charAt(0)) + (property.length() > 1 ? property.substring(1) : "");
    try {
      return (T) cls.getMethod(method).invoke(object);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public T get() {
    return (T) object;
  }

  public static class Builder<T> {

    private Object builder;
    private Class<T> cls;
    private Class<T> builderClass;
    private AtomicReference<String> propertyName = new AtomicReference<>();
    private T proxy;
    private Object value;
    private String prefix = "with";
    public Builder(Class<T> cls) {
      try {
        this.cls = (Class<T>) cls;
        Class<?> enclosingClass = cls.getEnclosingClass();
        if (enclosingClass != null) {
          String packageName = cls.getPackage().getName();
          String className = cls.getSimpleName();
          this.builderClass = (Class<T>) Class.forName(packageName + "." + className +  "Builder");
        } else {
          this.builderClass = (Class<T>) Class.forName(cls.getCanonicalName() +  "Builder");
        }

        this.builder = builderClass.newInstance();
        this.proxy = createProxy();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public Builder(Class<T> cls, Package pkg) {
      try {
        this.cls = (Class<T>) cls;
        this.builderClass = (Class<T>) Class.forName(pkg.getName() + "." + cls.getSimpleName() + "Builder");
        this.builder = builderClass.newInstance();
        this.proxy = createProxy();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public <U> Builder<T> set(Function<T, U> property, U value) {
      try {
        String propertyName = getPropertyName(property, value);
        for (Method m : builderClass.getMethods()) {
          String methodName = m.getName();
          if (methodName.equalsIgnoreCase(prefix + propertyName)) {
            if (value instanceof Optional) {
              Optional optional = (Optional) value;
              m.invoke(builder, optional.get());
            } else {
              m.invoke(builder, value);
            }
            return this;
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      throw new RuntimeException("Could not find property " + propertyName);
    }

    private <R> String getPropertyName(Function<T, R> method, final R value) throws NoSuchMethodException {
      this.value = value;
      method.apply(proxy);
      return propertyName.get();
    }

    private <R> T createProxy() {
      return (T) java.lang.reflect.Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{ cls },
                  new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                      String methodName = method.getName();
                      if (methodName.startsWith("get")) {
                        propertyName.set(Character.toLowerCase(methodName.charAt(3)) + (methodName.length() > 4 ? methodName.substring(4) : ""));
                      }
                      return value;
                    }
                  });
    }

    public BuilderProxy<T> build() {
      try {
        return new BuilderProxy(builderClass.getMethod("build").invoke(builder));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final Map<String, Class<?>> ALL_PRIMITIVE_TYPES = new HashMap<>();

  static {
    for (Class<?> primitiveNumber : Arrays.asList(byte.class, short.class,
            int.class, long.class, float.class, double.class)) {
      ALL_PRIMITIVE_TYPES.put(primitiveNumber.getName(), primitiveNumber);
    }
    for (Class<?> primitive : Arrays.asList(char.class, boolean.class)) {
      ALL_PRIMITIVE_TYPES.put(primitive.getName(), primitive);
    }
  }

  public static boolean isPrimitive(Class<?> type) {
    return ALL_PRIMITIVE_TYPES.containsKey(type.getName());
  }
}
