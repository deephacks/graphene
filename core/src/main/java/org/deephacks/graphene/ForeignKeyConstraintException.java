package org.deephacks.graphene;

public class ForeignKeyConstraintException extends RuntimeException {

    public ForeignKeyConstraintException(String message) {
        super(message);
    }

    public ForeignKeyConstraintException(byte[] primaryKey, byte[] secondary) {
        super("Primary " + primaryKey + " Secondary "
                + secondary);
    }
}
