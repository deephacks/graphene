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
            this.rowKey = new RowKey(object.getClass(), classWrapper.getId().getField().get(object));
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected EntityObjectWrapper(RowKey rowKey) {
        this.rowKey = rowKey;
        this.classWrapper = EntityClassWrapper.get(rowKey.getCls().get());
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

    public boolean isReference(String fieldName) {
        return classWrapper.getReferences().containsKey(fieldName);
    }

    public boolean isField(String fieldName) {
        return classWrapper.getFields().containsKey(fieldName);
    }

    @Override
    public String toString() {
        return rowKey.getCls() + " " + rowKey.getInstance();
    }
}
