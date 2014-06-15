package org.deephacks.graphene.cdi;

import org.deephacks.graphene.Entity;
import org.deephacks.graphene.Graphene;
import org.deephacks.graphene.Key;
import org.deephacks.graphene.cdi.Users.User;
import org.deephacks.vals.VirtualValue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
@Transaction
public class Accounts {

  @Inject
  private Graphene graphene;

  public Account lockAccount(User user) {
    // How to lock?
    //Optional<Account> opt = graphene.getForUpdate(user.getSsn(), Account.class);
    return null;//opt.get();

  }

  public Account getAccount(User user) {
    Optional<Account> opt = graphene.get(user.getSsn(), Account.class);
    return opt.get();
  }

  public Account createAccount(User user) {
    Account account = new AccountBuilder()
            .withSsn(user.getSsn())
            .withUser(user)
            .build();
    graphene.put(account);
    return getAccount(user);
  }

  public void save(Account account) {
    graphene.put(account);
  }

  @Entity
  @VirtualValue
  public static interface Account {

    @Key
    String getSsn();

    User getUser();

    default Integer getBalance() {
      return 0;
    }

    static void postConstruct(Account account) {
      if (account.getBalance() < 0) {
        throw new IllegalArgumentException("balance must be positive");
      }
    }

    default Account withdraw(int amount) {
      return AccountBuilder.builderFrom(this).withBalance(getBalance() - amount).build();
    }

    default Account deposit(int amount) {
      return AccountBuilder.builderFrom(this).withBalance(getBalance() + amount).build();
    }
  }
}

