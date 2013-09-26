package org.deephacks.graphene.internal;

import org.deephacks.graphene.Id;
import org.deephacks.graphene.Reference;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.deephacks.graphene.internal.Reflections.getParameterizedType;

public class EntityClassWrapper {
    private static final Map<Class<?>, EntityClassWrapper> catalog = new HashMap<>();
    protected EntityFieldWrapper id;
    protected Map<String, EntityFieldWrapper> fields = new HashMap<>();
    protected Map<String, EntityFieldWrapper> references = new HashMap<>();

    protected EntityClassWrapper(Class<?> cls) {
        Map<String, Field> map = Reflections.findFields(cls);
        for (String fieldName : map.keySet()) {
            fields.put(fieldName, new EntityFieldWrapper(map.get(fieldName)));
        }

        Map<Field, Annotation> annotation = Reflections.findFields(cls, Id.class);
        if (annotation.size() == 0) {
            throw new IllegalArgumentException("No id on " + cls);
        }
        this.id = new EntityFieldWrapper(annotation.keySet().iterator().next());
        fields.remove(id.getName());

        for (Field field : map.values()) {
            if (field.getAnnotation(Reference.class) != null) {
                fields.remove(field.getName());
                references.put(field.getName(), new EntityFieldWrapper(field));
            }
        }

    }

    public EntityFieldWrapper getId() {
        return id;
    }

    public Map<String, EntityFieldWrapper> getFields() {
        return fields;
    }


    public static EntityClassWrapper get(Class<?> cls) {
        if (catalog.containsKey(cls)) {
            return catalog.get(cls);
        }
        catalog.put(cls, new EntityClassWrapper(cls));
        return catalog.get(cls);
    }

    public static class EntityFieldWrapper {
        private Field field;
        private boolean isCollection;
        private boolean isMap;
        private boolean primitive;

        EntityFieldWrapper(Field field) {
            this.field = field;
            this.isCollection = Collection.class.isAssignableFrom(field.getType());
            this.isMap = Map.class.isAssignableFrom(field.getType());
        }

        public Class<?> getType() {
            if (!isCollection) {
                return field.getType();
            }
            List<Class<?>> p = getParameterizedType(field);
            if (p.size() == 0) {
                throw new UnsupportedOperationException("Collection of field [" + field
                        + "] does not have parameterized arguments, which is not allowed.");
            }
            return p.get(0);
        }

        public List<Class<?>> getMapParamTypes() {
            if (!isMap) {
                throw new UnsupportedOperationException("Field [" + field + "] is not a map.");
            }
            List<Class<?>> p = getParameterizedType(field);
            if (p.size() == 0) {
                throw new UnsupportedOperationException("Map of field [" + field
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
            return Modifier.isFinal(field.getModifiers());
        }

        public boolean isStatic() {
            return Modifier.isStatic(field.getModifiers());
        }

        public boolean isTransient() {
            return Modifier.isTransient(field.getModifiers());
        }

        public List<String> getEnums() {
            if (!isCollection) {
                if (field.getType().isEnum()) {
                    List<String> s = new ArrayList<>();
                    for (Object o : field.getType().getEnumConstants()) {
                        s.add(o.toString());
                    }
                    return s;
                } else {
                    return new ArrayList<>();
                }
            }
            List<Class<?>> p = getParameterizedType(field);
            if (p.size() == 0) {
                throw new UnsupportedOperationException("Collection of field [" + field
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

        public String getName() {
            return field.getName();
        }

        public Field getField() {
            return field;
        }

        public boolean isPrimitive() {
            return Reflections.isPrimitive(field.getType());
        }
    }
}
