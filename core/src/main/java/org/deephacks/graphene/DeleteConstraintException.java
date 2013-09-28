package org.deephacks.graphene;

public class DeleteConstraintException extends RuntimeException {
    public DeleteConstraintException(String message) {
        super(message);
    }

    public DeleteConstraintException(String message, Throwable cause) {
        super(message, cause);
    }
}
