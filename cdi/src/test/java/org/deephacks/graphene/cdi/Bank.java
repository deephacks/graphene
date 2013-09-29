package org.deephacks.graphene.cdi;

import org.deephacks.graphene.cdi.Accounts.Account;
import org.deephacks.graphene.cdi.Users.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@Transaction
public class Bank {

    @Inject
    private Accounts accounts;

    public void transfer(User from, User to, int amount) {
        Account fromAccount = accounts.getAccount(from);
        Account toAccount = accounts.getAccount(to);

        deposit(toAccount, amount);
        withdraw(fromAccount, amount);
    }

    public void deposit(Account account, int amount) {
        account = accounts.getAccount(account.getUser());
        account.deposit(amount);
        accounts.save(account);
    }

    public void withdraw(Account account, int amount) {
        account = accounts.getAccount(account.getUser());
        account.withdraw(amount);
        accounts.save(account);
    }

}