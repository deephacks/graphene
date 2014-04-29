package org.deephacks.graphene.cdi;

import org.deephacks.graphene.Entity;
import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.Id;
import org.deephacks.vals.VirtualValue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.deephacks.graphene.cdi.TransactionAttribute.REQUIRES_NEW;

@ApplicationScoped
@Transaction
public class Users {

    @Inject
    private EntityRepository repository;

    public void createUser(User user) {
        repository.put(user);
    }

    @Transaction(REQUIRES_NEW)
    public User get(String ssn) {
        return repository.get(ssn, User.class).get();
    }

    @Entity @VirtualValue
    public static interface User {
        @Id
        String getSsn();
        String getFullName();
    }
}
