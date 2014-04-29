package org.deephacks.graphene.cdi;

import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.cdi.Accounts.Account;
import org.deephacks.graphene.cdi.Users.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.Assert.fail;
import static org.deephacks.graphene.TransactionManager.withTx;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

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

  private User u1 = new UserBuilder().withSsn("1").withFullName("1").build();
  private User u2 = new UserBuilder().withSsn("2").withFullName("2").build();
  private Account a1;
  private Account a2;

  @Before
  public void before() {
    withTx(tx -> {
      repository.deleteAll(Account.class);
      repository.deleteAll(User.class);
    });
    withTx(tx -> {
      users.createUser(u1);
      users.createUser(u2);
    });

    withTx(tx -> {
      u1 = users.get(u1.getSsn());
      u2 = users.get(u2.getSsn());
      a1 = accounts.createAccount(u1);
      a2 = accounts.createAccount(u2);
    });
  }

  /**
   * Test that transaction interceptor is able to join transactions between cdi services.
   */

  @Test
  public void test_join_transaction() {
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

  /**
   * Test that concurrent transfers adds up in the end.
   */
  @Test
  public void test_concurrency() throws InterruptedException {
    int amount = 1000;
    bank.deposit(a1, amount);
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
    final CountDownLatch latch = new CountDownLatch(amount);
    for (int i = 0; i < amount; i++) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          bank.transfer(u1, u2, 1);
          latch.countDown();
        }
      });
    }
    latch.await();
    assertThat(accounts.getAccount(u1).getBalance(), is(0));
    assertThat(accounts.getAccount(u2).getBalance(), is(amount));
  }

  /**
   * Test that requires new methods are executed in a new transaction that
   * commit or abort within that method call.
   */
  @Test
  public void test_requires_new_tx() throws InterruptedException {

  }

}
