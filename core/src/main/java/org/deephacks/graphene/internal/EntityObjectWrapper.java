package org.deephacks.graphene.internal;


import org.deephacks.graphene.Embedded;
import org.deephacks.graphene.internal.EntityClassWrapper.EntityMethodWrapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class EntityObjectWrapper {
  private RowKey rowKey;
  private EntityClassWrapper classWrapper;
  private Map<String, Object> state = null;
  private Object object;
  public EntityObjectWrapper(Object object) {
    this.object = object;
    try {
      Field field = object.getClass().getDeclaredField("state");
      field.setAccessible(true);
      this.state = (Map<String, Object>) field.get(object);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    this.classWrapper = EntityClassWrapper.get(object.getClass());
    try {
      // embedded entities may not have an id
      if (classWrapper.getId() != null) {
        final Object o = classWrapper.getId().getMethod().invoke(object);
        if (o == null) {
          throw new IllegalArgumentException("Entity of type [" + object.getClass() + "] lacks a String id.");
        }
        this.rowKey = new RowKey(classWrapper.getVirtualClass(), o.toString());
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected EntityObjectWrapper(RowKey rowKey) {
    this.rowKey = rowKey;
    this.classWrapper = EntityClassWrapper.get(rowKey.getCls());
  }

  public RowKey getRowKey() {
    return rowKey;
  }

  public Object getValue(EntityMethodWrapper method) {
    try {

      if (method.isBasicType()) {
        return state.get(method.getName());
      } else if (method.getType().isEnum()) {
        return state.get(method.getName());
      } else if (method.getType().getAnnotation(Embedded.class) != null) {
        return state.get(method.getName());
      } else if (method.isReference()) {
        Object ref = state.get(method.getName());
        if (ref == null) {
          return null;
        }
        if (ref instanceof Collection) {
          ArrayList<String> instanceIds = new ArrayList<>();
          for (Object instance : (Collection) ref) {
            EntityClassWrapper refClass = EntityClassWrapper.get(instance.getClass());
            EntityMethodWrapper id = refClass.getId();
            String instanceId = String.valueOf(id.getMethod().invoke(instance));
            instanceIds.add(instanceId);
          }
          return instanceIds;
        } else {
          EntityClassWrapper refClass = EntityClassWrapper.get(ref.getClass());
          EntityMethodWrapper id = refClass.getId();
          String instanceId = String.valueOf(id.getMethod().invoke(ref));
          return instanceId;
        }
      } else {
        return state.get(method.getName());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public EntityMethodWrapper getMethod(String fieldName) {
    EntityMethodWrapper method = classWrapper.getMethods().get(fieldName);
    if (method != null) {
      return method;
    }
    if (classWrapper.getId().getName().equals(fieldName)) {
      return classWrapper.getId();
    }
    throw new IllegalStateException("Did not recognize field " + fieldName);
  }

  public EntityMethodWrapper getReference(String fieldName) {
    EntityMethodWrapper field = classWrapper.getReferences().get(fieldName);
    if (field != null) {
      return field;
    }
    if (classWrapper.getId().getName().equals(fieldName)) {
      return classWrapper.getId();
    }
    throw new IllegalStateException("Did not recognize field " + fieldName);
  }

  public EntityMethodWrapper getEmbedded(String fieldName) {
    EntityMethodWrapper field = classWrapper.getEmbedded().get(fieldName);
    if (field != null) {
      return field;
    }
    if (classWrapper.getId().getName().equals(fieldName)) {
      return classWrapper.getId();
    }
    throw new IllegalStateException("Did not recognize field " + fieldName);
  }

  public boolean isReference(String fieldName) {
    return classWrapper.getReferences().containsKey(fieldName);
  }

  public boolean isField(String fieldName) {
    return classWrapper.getMethods().containsKey(fieldName);
  }

  public boolean isEmbedded(String fieldName) {
    return classWrapper.getEmbedded().containsKey(fieldName);
  }

  @Override
  public String toString() {
    return rowKey.getCls() + " " + rowKey.getInstance();
  }

}
