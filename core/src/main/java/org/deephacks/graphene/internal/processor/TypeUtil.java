package org.deephacks.graphene.internal.processor;

import org.deephacks.graphene.Schema.KeySchema;
import org.deephacks.graphene.internal.serialization.BytesUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.lang.reflect.Method;

public class TypeUtil {
  public static String packageNameOf(TypeElement type) {
    while (true) {
      Element enclosing = type.getEnclosingElement();
      if (enclosing instanceof PackageElement) {
        return ((PackageElement) enclosing).getQualifiedName().toString();
      }
      type = (TypeElement) enclosing;
    }
  }

  public static int getSize(String type) {
    if (type.equals("byte[]")) {
      return 16;
    }
    String bufType = getSimpleClassName(type);
    bufType = toBufType(bufType);
    switch (bufType) {
      case "byte":
        return 1;
      case "short":
        return 2;
      case "integer":
        return 4;
      case "int":
        return 4;
      case "long":
        return 8;
      case "float":
        return 4;
      case "double":
        return 8;
      case "string":
        // UUID fits in 36 bytes
        return 36;
      case "boolean":
        return 1;
      case "char":
        return 2;
      case "character":
        return 2;
    }
    switch (type) {
      case "java.time.LocalDateTime":
        return BytesUtils.LOCAL_DATE_TIME_BYTES;
    }
    throw new IllegalArgumentException("Type not recognized " + type);
  }

  public static String classNameOf(TypeElement type) {
    return type.getQualifiedName().toString();
  }

  public static String simpleClassNameOf(TypeElement type) {
    return type.getSimpleName().toString();
  }

  public static KeySchema getKeySchema(Class<?> cls) {
    try {
      Class<?> generatedClass = getGeneratedEntity(cls);
      Method method = generatedClass.getDeclaredMethod("keySchema");
      method.setAccessible(true);
      return (KeySchema) method.invoke(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static Class<?> getGeneratedEntity(Class<?> cls) {
    String generatedClassName;
    int idx = cls.getName().indexOf('$');
    if (idx < 0) {
      generatedClassName = cls.getPackage().getName() + ".Entity_" + cls.getSimpleName();
    } else {
      String className = cls.getName().substring(idx + 1, cls.getName().length());
      generatedClassName = cls.getPackage().getName() + ".Entity_" + className;
    }
    try {
      return Class.forName(generatedClassName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static String toCapitalizedBufType(String type) {
    type = getSimpleClassName(type);
    type = toBufType(type);
    type = Character.toUpperCase(type.charAt(0)) + type.substring(1, type.length());
    return type;
  }

  private static String getSimpleClassName(String type) {
    int i = type.indexOf("<");
    // extract generic type argument
    if (i != -1) {
      type = type.substring(i + 1, type.length() - 1);
    }

    int idx = type.lastIndexOf(".");
    if (idx != -1) {
      type = type.substring(type.lastIndexOf(".") + 1, type.length());
    }
    return type;
  }

  private static String toBufType(String type) {
    if (type.equals("Character")) {
      type = "char";
    } else if (type.equals("Integer")) {
      return "int";
    }
    if (type.endsWith("[]")) {
      type = type.substring(0, type.length() - 2);
    }

    type = type.toLowerCase();
    switch (type) {
      case "byte":
      case "short":
      case "integer":
      case "int":
      case "long":
      case "float":
      case "double":
      case "string":
      case "boolean":
      case "char":
      case "character":
        return type;
      default:
        return "object";
    }
  }
}
