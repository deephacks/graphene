package org.deephacks.graphene;

import org.deephacks.graphene.internal.RowKey;

public class ForeignKeyConstraintException extends RuntimeException {

    public ForeignKeyConstraintException(String message) {
        super(message);
    }

    public ForeignKeyConstraintException(byte[] primaryKey, byte[] secondary) {
        super("Primary " + new RowKey(primaryKey) + " Secondary "
                + new RowKey(secondary));
    }
}
