package org.deephacks.graphene.cdi.users;

import org.deephacks.graphene.Entity;
import org.deephacks.graphene.Graphene;
import org.deephacks.graphene.Key;
import org.deephacks.graphene.cdi.Transaction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.deephacks.graphene.cdi.TransactionAttribute.REQUIRES_NEW_READ;

@ApplicationScoped
@Transaction
public class Users {

  @Inject
  private Graphene graphene;

  public void createUser(User user) {
    graphene.put(user);
  }

  @Transaction(REQUIRES_NEW_READ)
  public User get(String ssn) {
    return graphene.get(ssn, User.class).get();
  }

  @Entity
  public static interface User {
    @Key String getSsn();
    String getFullName();
  }
}