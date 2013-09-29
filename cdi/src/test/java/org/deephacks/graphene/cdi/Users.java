package org.deephacks.graphene.cdi;

import org.deephacks.graphene.Entity;
import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.Id;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class Users {

    @Inject
    private EntityRepository repository;

    @Transaction
    public void createUser(User user) {
        repository.put(user);
    }

    @Transaction
    public User get(String ssn) {
        return repository.get(ssn, User.class).get();
    }

    @Entity
    public static class User {
        @Id
        private String ssn;
        private String fullName;

        public User(String ssn, String fullName) {
            this.ssn = ssn;
        }

        public String getSsn() {
            return ssn;
        }

        public String getFullName() {
            return fullName;
        }
    }
}
