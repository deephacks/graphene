package org.deephacks.graphene.internal;

import org.deephacks.graphene.Embedded;
import org.deephacks.graphene.Guavas;
import org.deephacks.graphene.Id;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.deephacks.graphene.internal.Reflections.findAnnotation;
import static org.deephacks.graphene.internal.Reflections.getParameterizedType;

public class EntityClassWrapper {
  private static final Map<Class<?>, Class<?>> virtualMapping = new HashMap<>();
  private static final Map<Class<?>, EntityClassWrapper> catalog = new HashMap<>();
  private EntityMethodWrapper id;
  private Map<String, EntityMethodWrapper> methods = new HashMap<>();
  private Map<String, EntityMethodWrapper> references = new HashMap<>();
  private Map<String, EntityMethodWrapper> embedded = new HashMap<>();
  private final Class<?> virtualClass;

  private EntityClassWrapper(Class<?> cls) {
    this.virtualClass = cls;
    Map<String, Method> map = Reflections.findGetterMethods(cls);
    for (String methodName : map.keySet()) {
      String name = getNameFromMethod(map.get(methodName));
      methods.put(name, new EntityMethodWrapper(map.get(methodName), false));
    }

    Map<Method, Annotation> annotation = Reflections.findGetterMethods(cls, Id.class);
    if (annotation.size() != 0) {
      this.id = new EntityMethodWrapper(annotation.keySet().iterator().next(), false);
      methods.remove(id.getName());
    }
    // embedded fields must be check first because if field is Entity
    // we must still treat it as embedded, not as an Entity
    for (EntityMethodWrapper method : Guavas.newArrayList(methods.values())) {
      Class<?> type = method.getType();
      Embedded embedded = type.getAnnotation(Embedded.class);
      if (embedded != null) {
        methods.remove(method.getName());
        this.embedded.put(method.getName(), new EntityMethodWrapper(method.getMethod(), true));
      }
    }

    for (EntityMethodWrapper method : Guavas.newArrayList(methods.values())) {
      Class<?> type = method.getType();
      Id id = findAnnotation(type, Id.class);
      if (id != null) {
        methods.remove(method.getName());
        references.put(method.getName(), new EntityMethodWrapper(method.getMethod(), true));
      }
    }
  }

  public static EntityClassWrapper get(Class<?> cls) {
    cls = getVirtualValueClass(cls);
    if (catalog.containsKey(cls)) {
      return catalog.get(cls);
    }
    catalog.put(cls, new EntityClassWrapper(cls));
    return catalog.get(cls);
  }

  private static Class<?> getVirtualValueClass(Class<?> clazz) {
    Class<?> cls = virtualMapping.get(clazz);
    if (cls != null) {
      return cls;
    }
    if (clazz.getAnnotation(Embedded.class) != null) {
      virtualMapping.put(clazz, clazz);
      return clazz;
    }
    for (Method m : clazz.getMethods()) {
      if (m.isAnnotationPresent(Id.class)) {
        virtualMapping.put(clazz, clazz);
        return clazz;
      }
    }
    for (Class<?> ifClazz : clazz.getInterfaces()) {
      if (ifClazz.getAnnotation(Embedded.class) != null) {
        virtualMapping.put(clazz, ifClazz);
        return ifClazz;
      }
      for (Method m : ifClazz.getMethods()) {
        if (m.isAnnotationPresent(Id.class)) {
          virtualMapping.put(clazz, ifClazz);
          return ifClazz;
        }
      }
    }
    throw new IllegalArgumentException("Class " + clazz + " must be a @VirtualValue interface and have one @Id method.");
  }

  public EntityMethodWrapper getId() {
    return id;
  }

  public Map<String, EntityMethodWrapper> getMethods() {
    return methods;
  }

    public Map<String, EntityMethodWrapper> getReferences() {
    return references;
  }

  public boolean isReference(String name) {
    return references.containsKey(name);
  }

  public boolean isMethod(String name) {
    return methods.containsKey(name);
  }

  public Map<String, EntityMethodWrapper> getEmbedded() {
    return embedded;
  }

  public boolean isEmbedded(String name) {
    return embedded.containsKey(name);
  }

  @Override
  public String toString() {
    return virtualClass.getName();
  }

  public EntityMethodWrapper getReference(String methodName) {
    return references.get(methodName);
  }

  public EntityMethodWrapper getEmbedded(String methodName) {
    return embedded.get(methodName);
  }

  public Class<?> getVirtualClass() {
    return virtualClass;
  }

  private static String getNameFromMethod(Method method) {
    String name = method.getName();
    name = name.substring(3, name.length());
    name = Character.toLowerCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : "");
    return name;
  }

  public static class EntityMethodWrapper {
    private Method method;
    private String name;
    private boolean isCollection;
    private boolean isMap;
    private boolean isReference;
    private List<String> enums;
    private final Class<?> type;
    private final boolean isPrimitive;
    private final boolean isBasicType;
    private final boolean isEnum;

    EntityMethodWrapper(Method method, boolean isReference) {
      this.method = method;
      this.name = getNameFromMethod(method);
      this.isCollection = Collection.class.isAssignableFrom(method.getReturnType());
      this.isMap = Map.class.isAssignableFrom(method.getReturnType());
      this.isReference = isReference;
      this.type = calculateType();
      this.isPrimitive = Types.isPrimitive(type);
      this.isBasicType = Types.isBasicType(type);
      this.isEnum = type.isEnum();
      this.enums = calculateEnums();
    }

    public Class<?> getType() {
      return type;
    }

    private Class<?> calculateType() {
      if (!isCollection) {
        return method.getReturnType();
      }
      List<Class<?>> p = getParameterizedType(method);
      if (p.size() == 0) {
        throw new UnsupportedOperationException("Collection of method [" + method
                + "] does not have parameterized arguments, which is not allowed.");
      }
      return p.get(0);
    }

    private List<String> calculateEnums() {
      if (!isCollection) {
        if (method.getReturnType().isEnum()) {
          List<String> s = new ArrayList<>();
          for (Object o : method.getReturnType().getEnumConstants()) {
            s.add(o.toString());
          }
          return s;
        } else {
          return new ArrayList<>();
        }
      }
      List<Class<?>> p = getParameterizedType(method);
      if (p.size() == 0) {
        throw new UnsupportedOperationException("Collection of method [" + method
                + "] does not have parameterized arguments, which is not allowed.");
      }
      if (p.get(0).isEnum()) {
        List<String> s = new ArrayList<>();
        for (Object o : p.get(0).getEnumConstants()) {
          s.add(o.toString());
        }
        return s;
      }
      return new ArrayList<>();
    }

    public List<Class<?>> getMapParamTypes() {
      if (!isMap) {
        throw new UnsupportedOperationException("Method return type [" + method + "] is not a map.");
      }
      List<Class<?>> p = getParameterizedType(method);
      if (p.size() == 0) {
        throw new UnsupportedOperationException("Map of method return type [" + method
                + "] does not have parameterized arguments, which is not allowed.");
      }
      return p;
    }

    public boolean isCollection() {
      return isCollection;
    }

    public boolean isMap() {
      return isMap;
    }

    public boolean isFinal() {
      return Modifier.isFinal(method.getModifiers());
    }

    public boolean isStatic() {
      return Modifier.isStatic(method.getModifiers());
    }

    public boolean isTransient() {
      return Modifier.isTransient(method.getModifiers());
    }

    public List<String> getEnums() {
      return enums;
    }

    public String getName() {
      return name;
    }

    public Method getMethod() {
      return method;
    }

    public boolean isPrimitive() {
      return isPrimitive;
    }

    public boolean isReference() {
      return isReference;
    }

    public boolean isBasicType() {
      return isBasicType;
    }

    public boolean isEnum() {
      return isEnum;
    }

    public Object getAnnotation(Class<? extends Annotation> annotation) {
      return method.getAnnotation(annotation);
    }

    @Override
    public String toString() {
      return String.valueOf(method);
    }
  }

}
