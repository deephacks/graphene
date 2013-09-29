package org.deephacks.graphene.cdi;

import com.google.common.base.Optional;
import org.deephacks.graphene.Entity;
import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.Id;
import org.deephacks.graphene.cdi.Users.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@Transaction
public class Accounts {

    @Inject
    private EntityRepository repository;

    public Account getAccount(User user) {
        Optional<Account> opt = repository.get(user.getSsn(), Account.class);
        return opt.get();
    }

    public Account createAccount(User user) {
        Account account = new Account(user.getSsn());
        account.setUser(user);
        repository.put(account);
        return repository.get(user.getSsn(), Account.class).get();
    }

    public void save(Account account) {
        repository.put(account);
    }


    @Entity
    public static class Account {

        @Id
        private String ssn;

        private User user;

        private int balance = 0;

        public Account(String ssn) {
            this.ssn = ssn;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }

        public int getBalance() {
            return balance;
        }

        public void withdraw(int amount) {
            balance -= amount;
            if (balance < 0){
                throw new IllegalStateException("Not enough money on account");
            }
        }

        public void deposit(int amount) {
            balance += amount;
        }
    }
}

