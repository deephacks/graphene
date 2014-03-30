package org.deephacks.graphene.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * General purpose class for commonly used reflection operations.
 *
 * @author Kristoffer Sjogren
 */
public final class Reflections {

  private static final Map<Class<?>, Map<String, Method>> methodCache = new HashMap<>();
  private static final Map<Class<?>, Map<Class<? extends Annotation>, Map<Method, Annotation>>> methodAnnotationCache = new HashMap<>();

  public static Class<?> forName(String className) {
    try {
      Class<?> primitive = Types.getPrimitiveType(className);
      return primitive != null ? primitive : Thread.currentThread()
              .getContextClassLoader().loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get list superclasses and interfaces recursively.
   *
   * @param clazz The class to beginTransaction the search with.
   * @return List of list super classes and interfaces of {@code clazz}. The list contains the class itself! The empty
   * list is returned if {@code clazz} is {@code null}.
   */
  public static List<Class<?>> computeClassHierarchy(Class<?> clazz) {
    List<Class<?>> classes = new ArrayList<>();
    computeClassHierarchy(clazz, classes);
    return classes;
  }

  /**
   * Get list superclasses and interfaces recursively.
   *
   * @param clazz   The class to beginTransaction the search with.
   * @param classes List of classes to which to add list found super classes and interfaces.
   */
  private static void computeClassHierarchy(Class<?> clazz, List<Class<?>> classes) {
    for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
      if (classes.contains(current)) {
        return;
      }
      classes.add(current);
      for (Class<?> currentInterface : current.getInterfaces()) {
        computeClassHierarchy(currentInterface, classes);
      }
    }
  }

  public static List<Class<?>> computeEnclosingClasses(Class<?> clazz) {
    List<Class<?>> classes = new ArrayList<>();
    computeEnclosingClasses(clazz, classes);
    return classes;
  }

  private static void computeEnclosingClasses(Class<?> clazz, List<Class<?>> classes) {
    for (Class<?> current = clazz; current != null; current = current.getEnclosingClass()) {
      if (classes.contains(current)) {
        return;
      }
      classes.add(current);
      for (Class<?> currentInterface : current.getInterfaces()) {
        computeEnclosingClasses(currentInterface, classes);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<? extends T> getComponentType(T[] a) {
    Class<?> k = a.getClass().getComponentType();
    return (Class<? extends T>) k; // unchecked cast
  }

  public static <T> T[] newArray(T[] a, int size) {
    return newArray(getComponentType(a), size);
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] newArray(Class<? extends T> k, int size) {
    if (k.isPrimitive())
      throw new IllegalArgumentException("Argument cannot be primitive: " + k);
    Object a = java.lang.reflect.Array.newInstance(k, size);
    return (T[]) a; // unchecked cast
  }

  public static <T> T cast(Class<T> clazz, Object object) {
    if (object == null) {
      return null;
    }
    if (clazz.isAssignableFrom(object.getClass())) {
      return clazz.cast(object);
    }
    return null;
  }

  public static Map<String, Method> findGetterMethods(final Class<?> clazz) {
    if (methodCache.get(clazz) != null) {
      return methodCache.get(clazz);
    }
    Map<String, Method> foundMethods = new HashMap<>();
    Class<?> searchType = clazz;
    //while (!Object.class.equals(searchType) && (searchType != null)) {
      Method[] methods = searchType.getMethods();
      for (Method method : methods) {
        if (!method.getName().startsWith("get")) {
          continue;
        }
        if (foundMethods.get(method.getName()) == null && !method.getName().equals("getClass")) {
          foundMethods.put(method.getName(), method);
        }
      }
      searchType = searchType.getSuperclass();
    //}
    methodCache.put(clazz, foundMethods);
    return foundMethods;
  }


  public static <T extends Annotation> Map<Method, Annotation> findGetterMethods(Class<?> cls, Class<T> annotationClass) {
    Map<Class<? extends Annotation>, Map<Method, Annotation>> annotations = methodAnnotationCache.get(cls);
    if (annotations != null) {
      Map<Method, Annotation> annotationMap = annotations.get(annotationClass);
      if (annotationMap != null) {
        return annotationMap;
      }
    }
    if (annotations == null) {
      annotations = new HashMap<>();
    }
    Map<Method, Annotation> annotationMap = new HashMap<>();
    for (Method method : findGetterMethods(cls).values()) {
      Annotation annotation = findAnnotation(method, annotationClass);
      if (annotation != null) {
        annotationMap.put(method, annotation);
      }
    }
    annotations.put(annotationClass, annotationMap);
    methodAnnotationCache.put(cls, annotations);
    return annotationMap;
  }


  public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
    A annotation = method.getAnnotation(annotationType);
    Class<?> cl = method.getDeclaringClass();
    if (annotation == null) {
      return searchOnInterfaces(method, annotationType, cl.getInterfaces());
    }
    return annotation;
  }

  public static <T extends Annotation> T findAnnotation(Class<?> cls, Class<T> annotationType) {
    for (Method method : cls.getMethods()) {
      T annotation = method.getAnnotation(annotationType);
      if (annotation != null) {
        return annotation;
      }
    }
    return null;
  }

  private static <A extends Annotation> A searchOnInterfaces(Method method, Class<A> annotationType, Class<?>[] ifcs) {
    A annotation = null;
    for (Class<?> iface : ifcs) {
      try {
        Method equivalentMethod = iface.getMethod(method.getName(), method.getParameterTypes());
        annotation = equivalentMethod.getAnnotation(annotationType);
      } catch (NoSuchMethodException ex) {
        // Skip this interface - it doesn't have the method...
      }
      if (annotation != null) {
        break;
      }
    }
    return annotation;
  }

  public static List<Class<?>> getParameterizedType(final Method method) {
    Type type = method.getGenericReturnType();

    if (!ParameterizedType.class.isAssignableFrom(type.getClass())) {

      // the field is it a raw type and does not have generic type
      // argument. Return empty list.
      return new ArrayList<>();
    }

    ParameterizedType ptype = (ParameterizedType) type;
    Type[] targs = ptype.getActualTypeArguments();
    List<Class<?>> classes = new ArrayList<>();
    for (Type aType : targs) {
      if (Class.class.isAssignableFrom(aType.getClass())) {
        classes.add((Class<?>) aType);
      } else if (WildcardType.class.isAssignableFrom(aType.getClass())) {
        // wild cards are not handled by this method
      } else if (TypeVariable.class.isAssignableFrom(aType.getClass())) {
        // type variables are not handled by this method
      }
    }
    return classes;
  }


  /**
   * Returns the parameterized type of a class, if exists. Wild cards, type
   * variables and raw types will be returned as an empty list.
   * <p>
   * If a field is of type Set<String> then java.lang.String is returned.
   * </p>
   * <p>
   * If a field is of type Map<String, Integer> then [java.lang.String,
   * java.lang.Integer] is returned.
   * </p>
   *
   * @param ownerClass the implementing target class to check against
   * @param ownerClass generic interface to resolve the type argument from
   * @return A list of classes of the parameterized type.
   */
  public static List<Class<?>> getParameterizedType(final Class<?> ownerClass,
                                                    Class<?> genericSuperClass) {
    Type[] types = null;
    if (genericSuperClass.isInterface()) {
      types = ownerClass.getGenericInterfaces();
    } else {
      types = new Type[]{ownerClass.getGenericSuperclass()};
    }

    List<Class<?>> classes = new ArrayList<>();
    for (Type type : types) {

      if (!ParameterizedType.class.isAssignableFrom(type.getClass())) {
        // the field is it a raw type and does not have generic type
        // argument. Return empty list.
        return new ArrayList<>();
      }

      ParameterizedType ptype = (ParameterizedType) type;
      Type[] targs = ptype.getActualTypeArguments();

      for (Type aType : targs) {

        classes.add(extractClass(ownerClass, aType));
      }
    }
    return classes;
  }

  public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
    try {
      Method method = clazz.getMethod(methodName, args);
      return Modifier.isStatic(method.getModifiers()) ? method : null;
    } catch (NoSuchMethodException ex) {
      return null;
    }
  }

  public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... paramTypes) {
    try {
      return clazz.getConstructor(paramTypes);
    } catch (NoSuchMethodException ex) {
      return null;
    }
  }

  private static Class<?> extractClass(Class<?> ownerClass, Type arg) {
    if (arg instanceof ParameterizedType) {
      return extractClass(ownerClass, ((ParameterizedType) arg).getRawType());
    } else if (arg instanceof GenericArrayType) {
      throw new UnsupportedOperationException("GenericArray types are not supported.");
    } else if (arg instanceof TypeVariable) {
      throw new UnsupportedOperationException("GenericArray types are not supported.");
    }
    return (arg instanceof Class ? (Class<?>) arg : Object.class);
  }
}