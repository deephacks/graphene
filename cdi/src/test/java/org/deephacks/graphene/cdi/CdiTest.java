package org.deephacks.graphene.cdi;

import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.cdi.Accounts.Account;
import org.deephacks.graphene.cdi.Users.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(CdiRunner.class)
public class CdiTest {

    @Inject
    private EntityRepository repository;

    @Inject
    private Users users;

    @Inject
    private Accounts accounts;

    @Inject
    private Bank bank;

    @Before
    public void before() {
        repository.deleteAll(Account.class);
        repository.deleteAll(User.class);
        repository.commit();
    }

    /**
     * Test that transaction interceptor is able to join transactions between cdi services.
     */
    @Test
    public void test_join_transaction() {
        User u1 = new User("1", "1");
        User u2 = new User("2", "2");

        users.createUser(u1);
        users.createUser(u2);

        Account a1 = accounts.createAccount(u1);
        Account a2 = accounts.createAccount(u2);

        bank.deposit(a1, 10);
        bank.deposit(a2, 10);

        try {
            bank.transfer(u1, u2, 20);
            fail("Balance should be too small");
        } catch (Exception e) {
            // success - transaction was rolled back
        }
        // check that nothing was committed
        assertThat(accounts.getAccount(u1).getBalance(), is(10));
        assertThat(accounts.getAccount(u2).getBalance(), is(10));

        bank.transfer(u1, u2, 5);

        assertThat(accounts.getAccount(u1).getBalance(), is(5));
        assertThat(accounts.getAccount(u2).getBalance(), is(15));
    }
}
