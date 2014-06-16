package org.deephacks.graphene.internal.gql;

public class IllegalQueryException extends RuntimeException {
  public IllegalQueryException(String message) {
    super(message);
  }

  public IllegalQueryException(String message, Throwable cause) {
    super(message, cause);
  }
}
