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
}
