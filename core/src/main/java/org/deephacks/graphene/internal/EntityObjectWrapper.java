package org.deephacks.graphene.internal;


import org.deephacks.graphene.internal.EntityClassWrapper.EntityFieldWrapper;

public class EntityObjectWrapper {
    protected Object object;
    protected RowKey rowKey;
    protected EntityClassWrapper classWrapper;

    protected EntityObjectWrapper(Object object) {
        this.object = object;
        this.classWrapper = EntityClassWrapper.get(object.getClass());
        try {
            // embedded entities may not have an id
            if (classWrapper.getId() != null) {
                final Object o = classWrapper.getId().getField().get(object);
                if (o == null) {
                    throw new IllegalArgumentException("Entity of type ["+object.getClass()+"] lacks a String id.");
                }
                this.rowKey = new RowKey(object.getClass(), o.toString());
            }
        } catch (IllegalAccessException e) {
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

    public EntityFieldWrapper getField(String fieldName) {
        EntityFieldWrapper field = classWrapper.getFields().get(fieldName);
        if (field != null) {
            return field;
        }
        if (classWrapper.getId().getName().equals(fieldName)) {
            return classWrapper.getId();
        }
        throw new IllegalStateException("Did not recognize field " + fieldName);
    }

    public EntityFieldWrapper getReference(String fieldName) {
        EntityFieldWrapper field = classWrapper.getReferences().get(fieldName);
        if (field != null) {
            return field;
        }
        if (classWrapper.getId().getName().equals(fieldName)) {
            return classWrapper.getId();
        }
        throw new IllegalStateException("Did not recognize field " + fieldName);
    }

    public EntityFieldWrapper getEmbedded(String fieldName) {
        EntityFieldWrapper field = classWrapper.getEmbedded().get(fieldName);
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
        return classWrapper.getFields().containsKey(fieldName);
    }

    public boolean isEmbedded(String fieldName) {
        return classWrapper.getEmbedded().containsKey(fieldName);
    }

    @Override
    public String toString() {
        return rowKey.getCls() + " " + rowKey.getInstance();
    }

}
