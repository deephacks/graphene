package org.deephacks.graphene.internal;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtils {
  private static Unsafe unsafe;

  static {
    try {
      Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      unsafe = (Unsafe) unsafeField.get(null);
      // Ensure the unsafe supports list necessary methods to work around the mistake in the latest OpenJDK.
      // https://github.com/netty/netty/issues/1061
      // http://www.mail-archive.com/jdk6-dev@openjdk.java.net/msg00698.html
      try {
        unsafe.getClass().getDeclaredMethod(
                "copyMemory",
                new Class[]{Object.class, long.class, Object.class, long.class, long.class});
      } catch (NoSuchMethodError t) {
        throw t;
      }
    } catch (Throwable cause) {
      unsafe = null;
    }

  }

  public static Unsafe getUnsafe() {
    return unsafe;
  }

  /*
  static class UnsafeEntityObjectWrapper extends EntityObjectWrapper {
    UnsafeEntityClassWrapper classWrapper;

    UnsafeEntityObjectWrapper(RowKey rowKey) {
      super(rowKey);
      try {
        object = unsafe.allocateInstance(rowKey.getCls());
      } catch (InstantiationException e) {
        throw new IllegalStateException(e);
      }
      classWrapper = UnsafeEntityClassWrapper.get(object.getClass());
      EntityMethodWrapper id = classWrapper.getId();
      // embedded may not have id
      if (id != null) {
        set(id, rowKey.getInstance());
      }
    }

    UnsafeEntityObjectWrapper(Object object) {
      super(object);
      classWrapper = UnsafeEntityClassWrapper.get(object.getClass());
    }

    public void set(EntityMethodWrapper method, Object value) {
      try {
        long offset = classWrapper.getOffset(method.getName());
        Class<?> virtualClass = method.getType();
        if (method.isPrimitive()) {
          if (byte.class.isAssignableFrom(virtualClass)) {
            unsafe.putByte(object, offset, (byte) value);
          } else if (short.class.isAssignableFrom(virtualClass)) {
            unsafe.putShort(object, offset, (short) value);
          } else if (int.class.isAssignableFrom(virtualClass)) {
            unsafe.putInt(object, offset, (int) value);
          } else if (long.class.isAssignableFrom(virtualClass)) {
            unsafe.putLong(object, offset, (long) value);
          } else if (float.class.isAssignableFrom(virtualClass)) {
            unsafe.putFloat(object, offset, (float) value);
          } else if (double.class.isAssignableFrom(virtualClass)) {
            unsafe.putDouble(object, offset, (double) value);
          } else if (boolean.class.isAssignableFrom(virtualClass)) {
            unsafe.putBoolean(object, offset, (boolean) value);
          } else if (char.class.isAssignableFrom(virtualClass)) {
            unsafe.putChar(object, offset, (char) value);
          } else {
            throw new UnsupportedOperationException("Did not recognize " + virtualClass);
          }
        } else if (method.getType().isEnum()) {
          if (method.isCollection()) {
            Collection<Enum> enums = toEnums((Collection) value, method.getType());
            unsafe.putObject(object, offset, enums);
          } else {
            Enum anEnum = Enum.valueOf((Class<? extends Enum>) method.getType(), value.toString());
            unsafe.putObject(object, offset, anEnum);
          }
        } else {
          unsafe.putObject(object, offset, value);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private Collection<Enum> toEnums(Collection values, Class<?> type) {
      ArrayList<Enum> list = new ArrayList<>();
      for (Object e : values) {
        list.add(Enum.valueOf((Class<? extends Enum>) type, e.toString()));
      }
      return list;
    }

    public Object getValue(EntityMethodWrapper method) {
      try {
        long offset = classWrapper.getOffset(method.getName());

        if (method.isPrimitive()) {
          Class<?> virtualClass = method.getType();
          if (byte.class.isAssignableFrom(virtualClass)) {
            return unsafe.getByte(object, offset);
          } else if (short.class.isAssignableFrom(virtualClass)) {
            return unsafe.getShort(object, offset);
          } else if (int.class.isAssignableFrom(virtualClass)) {
            return unsafe.getInt(object, offset);
          } else if (long.class.isAssignableFrom(virtualClass)) {
            return unsafe.getLong(object, offset);
          } else if (float.class.isAssignableFrom(virtualClass)) {
            return unsafe.getFloat(object, offset);
          } else if (double.class.isAssignableFrom(virtualClass)) {
            return unsafe.getDouble(object, offset);
          } else if (boolean.class.isAssignableFrom(virtualClass)) {
            return unsafe.getBoolean(object, offset);
          } else if (char.class.isAssignableFrom(virtualClass)) {
            return unsafe.getChar(object, offset);
          } else {
            throw new UnsupportedOperationException("Did not recognize " + virtualClass);
          }
        } else if (method.isBasicType()) {
          return unsafe.getObject(object, offset);
        } else if (method.getType().isEnum()) {
          return unsafe.getObject(object, offset);
        } else if (method.getAnnotation(Embedded.class) != null) {
          return unsafe.getObject(object, offset);
        } else if (method.isReference()) {
          Object ref = unsafe.getObject(object, offset);
          if (ref == null) {
            return null;
          }
          if (ref instanceof Collection) {
            ArrayList<String> instanceIds = new ArrayList<>();
            for (Object instance : (Collection) ref) {
              UnsafeEntityClassWrapper refClass = UnsafeEntityClassWrapper.get(instance.getClass());
              EntityMethodWrapper id = refClass.getId();
              String instanceId = String.valueOf(id.getMethod().invoke(instance));
              instanceIds.add(instanceId);
            }
            return instanceIds;
          } else {
            UnsafeEntityClassWrapper refClass = UnsafeEntityClassWrapper.get(ref.getClass());
            EntityMethodWrapper id = refClass.getId();
            String instanceId = String.valueOf(id.getMethod().invoke(ref));
            return instanceId;
          }
        } else {
          return unsafe.getObject(object, offset);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public Object getObject() {
      return object;
    }
  }

  static class UnsafeEntityClassWrapper extends EntityClassWrapper {
    private Map<String, Long> offsets = new HashMap<>();
    private static final Map<Class<?>, UnsafeEntityClassWrapper> catalog = new HashMap<>();

    UnsafeEntityClassWrapper(Class<?> virtualClass) {
      super(virtualClass);
      for (EntityMethodWrapper method : methods.values()) {
        long offset = unsafe.objectFieldOffset(method.getMethod());
        offsets.put(method.getName(), offset);
      }
      for (EntityMethodWrapper method : references.values()) {
        long offset = unsafe.objectFieldOffset(method.getMethod());
        offsets.put(method.getName(), offset);
      }
      for (EntityMethodWrapper method : embedded.values()) {
        long offset = unsafe.objectFieldOffset(method.getMethod());
        offsets.put(method.getName(), offset);
      }
      // embedded entities may not have id
      if (id != null) {
        long offset = unsafe.objectFieldOffset(id.getMethod());
        offsets.put(id.getName(), offset);
      }
    }

    public long getOffset(String fieldName) {
      return offsets.get(fieldName);
    }

    public static UnsafeEntityClassWrapper get(Class<?> virtualClass) {
      if (catalog.containsKey(virtualClass)) {
        return catalog.get(virtualClass);
      }
      catalog.put(virtualClass, new UnsafeEntityClassWrapper(virtualClass));
      return catalog.get(virtualClass);
    }
  }
  */
}