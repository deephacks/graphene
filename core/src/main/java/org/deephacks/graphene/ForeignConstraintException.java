package org.deephacks.graphene;

public class ForeignConstraintException extends RuntimeException {
    public ForeignConstraintException(String message) {
        super(message);
    }
}
