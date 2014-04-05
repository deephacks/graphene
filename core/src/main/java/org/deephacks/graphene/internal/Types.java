package org.deephacks.graphene.internal;

import org.deephacks.graphene.internal.BytesUtils.DataType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Types {
  private static final Map<String, Class<?>> ALL_PRIMITIVE_TYPES = new HashMap<>();
  private static final Map<String, Class<?>> ALL_PRIMITIVE_NUMBERS = new HashMap<>();

  private static final Map<String, Class<?>> ALL_BASIC_OBJECT_TYPES = new HashMap<>();
  private static final Map<String, Class<?>> ALL_BASIC_OBJECT_NUMBERS = new HashMap<>();

  static {
    for (Class<?> primitiveNumber : Arrays.asList(byte.class, short.class,
            int.class, long.class, float.class, double.class)) {
      ALL_PRIMITIVE_NUMBERS.put(primitiveNumber.getName(), primitiveNumber);
      ALL_PRIMITIVE_TYPES.put(primitiveNumber.getName(), primitiveNumber);
    }
    for (Class<?> primitive : Arrays.asList(char.class, boolean.class)) {
      ALL_PRIMITIVE_TYPES.put(primitive.getName(), primitive);
    }
    for (Class<?> objectNumber : Arrays.asList(Byte.class, Short.class,
            Integer.class, Long.class, Float.class, Double.class)) {
      ALL_BASIC_OBJECT_NUMBERS.put(objectNumber.getName(), objectNumber);
      ALL_BASIC_OBJECT_TYPES.put(objectNumber.getName(), objectNumber);
    }
    for (Class<?> basicType : Arrays.asList(Character.class, Boolean.class, String.class)) {
      ALL_BASIC_OBJECT_TYPES.put(basicType.getName(), basicType);
    }

  }

  public static Class<?> getPrimitiveType(String className) {
    return ALL_PRIMITIVE_TYPES.get(className);
  }

  public static boolean isPrimitive(Class<?> type) {
    return ALL_PRIMITIVE_TYPES.containsKey(type.getName());
  }

  public static boolean isPrimitiveNumber(Class<?> type) {
    return ALL_PRIMITIVE_NUMBERS.containsKey(type.getName());
  }

  public static boolean isBasicType(Class<?> type) {
    return ALL_PRIMITIVE_TYPES.containsKey(type.getName()) || ALL_BASIC_OBJECT_TYPES.containsKey(type.getName());
  }

  public static boolean isBasicType(DataType type) {
    switch (type) {
      case BYTE:
      case SHORT:
      case INTEGER:
      case LONG:
      case FLOAT:
      case DOUBLE:
      case BOOLEAN:
      case STRING:
      case CHAR:
        return true;
      default:
        return false;
    }
  }

}
