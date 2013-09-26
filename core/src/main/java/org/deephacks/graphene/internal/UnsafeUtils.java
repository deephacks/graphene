package org.deephacks.graphene.internal;

import org.deephacks.graphene.internal.EntityClassWrapper.EntityFieldWrapper;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
                        new Class[] { Object.class, long.class, Object.class, long.class, long.class });
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
            EntityFieldWrapper id = classWrapper.getId();
            set(id, rowKey.getInstance());
        }

        UnsafeEntityObjectWrapper(Object object) {
            super(object);
            classWrapper = UnsafeEntityClassWrapper.get(object.getClass());
        }

        public void set(EntityFieldWrapper field, Object value) {
            try {
                long offset = classWrapper.getOffset(field.getName());
                Class<?> cls = field.getType();
                if (field.isPrimitive()) {
                    if(byte.class.isAssignableFrom(cls)) {
                        unsafe.putByte(object, offset, (byte) value);
                    } else if (short.class.isAssignableFrom(cls)) {
                        unsafe.putShort(object, offset, (short) value);
                    } else if (int.class.isAssignableFrom(cls)) {
                        unsafe.putInt(object, offset, (int) value);
                    } else if (long.class.isAssignableFrom(cls)) {
                        unsafe.putLong(object, offset, (long) value);
                    } else if (float.class.isAssignableFrom(cls)) {
                        unsafe.putFloat(object, offset, (float) value);
                    } else if (double.class.isAssignableFrom(cls)) {
                        unsafe.putDouble(object, offset, (double) value);
                    } else if (boolean.class.isAssignableFrom(cls)) {
                        unsafe.putBoolean(object, offset, (boolean) value);
                    } else if (char.class.isAssignableFrom(cls)) {
                        unsafe.putChar(object, offset, (char) value);
                    } else {
                        throw new UnsupportedOperationException("Did not recognize " + cls);
                    }
                } else if (field.getType().isEnum()) {
                    if (field.isCollection()) {
                        Collection<Enum> enums = toEnums((Collection) value, field.getType());
                        unsafe.putObject(object, offset, enums);
                    } else {
                        Enum anEnum = Enum.valueOf((Class<? extends Enum>) field.getType(), value.toString());
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

        public Object getValue(EntityFieldWrapper field) {
            try {
                long offset = classWrapper.getOffset(field.getName());
                Class<?> cls = field.getType();
                if (Reflections.isPrimitive(cls)) {
                    if(byte.class.isAssignableFrom(cls)) {
                        return unsafe.getByte(object, offset);
                    } else if (short.class.isAssignableFrom(cls)) {
                        return unsafe.getShort(object, offset);
                    } else if (int.class.isAssignableFrom(cls)) {
                        return unsafe.getInt(object, offset);
                    } else if (long.class.isAssignableFrom(cls)) {
                        return unsafe.getLong(object, offset);
                    } else if (float.class.isAssignableFrom(cls)) {
                        return unsafe.getFloat(object, offset);
                    } else if (double.class.isAssignableFrom(cls)) {
                        return unsafe.getDouble(object, offset);
                    } else if (boolean.class.isAssignableFrom(cls)) {
                        return unsafe.getBoolean(object, offset);
                    } else if (char.class.isAssignableFrom(cls)) {
                        return unsafe.getChar(object, offset);
                    } else {
                        throw new UnsupportedOperationException("Did not recognize " + cls);
                    }
                } else if (field.getType().isEnum()) {
                    return unsafe.getObject(object, offset);
                } else if (field.isReference()) {
                    Object ref = unsafe.getObject(object, offset);
                    if (ref == null) {
                        return null;
                    }
                    if (ref instanceof Collection) {
                        ArrayList<String> instanceIds = new ArrayList<>();
                        for (Object instance : (Collection) ref) {
                            UnsafeEntityClassWrapper refClass = UnsafeEntityClassWrapper.get(instance.getClass());
                            EntityFieldWrapper id = refClass.getId();
                            String instanceId = String.valueOf(id.getField().get(instance));
                            instanceIds.add(instanceId);
                        }
                        return instanceIds;
                    } else {
                        UnsafeEntityClassWrapper refClass = UnsafeEntityClassWrapper.get(ref.getClass());
                        EntityFieldWrapper id = refClass.getId();
                        String instanceId = String.valueOf(id.getField().get(ref));
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

        UnsafeEntityClassWrapper(Class<?> cls) {
            super(cls);
            for (EntityFieldWrapper field : fields.values()) {
                long offset = unsafe.objectFieldOffset(field.getField());
                offsets.put(field.getName(), offset);
            }
            for (EntityFieldWrapper field : references.values()) {
                long offset = unsafe.objectFieldOffset(field.getField());
                offsets.put(field.getName(), offset);
            }
            long offset = unsafe.objectFieldOffset(id.getField());
            offsets.put(id.getName(), offset);
        }

        public long getOffset(String fieldName) {
            return offsets.get(fieldName);
        }

        public static UnsafeEntityClassWrapper get(Class<?> cls) {
            if (catalog.containsKey(cls)) {
                return catalog.get(cls);
            }
            catalog.put(cls, new UnsafeEntityClassWrapper(cls));
            return catalog.get(cls);
        }
    }
}