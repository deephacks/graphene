package org.deephacks.graphene.cdi;

import org.deephacks.graphene.Entity;
import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.Id;
import org.deephacks.graphene.cdi.Users.User;
import org.deephacks.vals.VirtualValue;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
@Transaction
public class Accounts {

  @Inject
  private EntityRepository repository;

  public Account lockAccount(User user) {
    Optional<Account> opt = repository.getForUpdate(user.getSsn(), Account.class);
    return opt.get();

  }

  public Account getAccount(User user) {
    Optional<Account> opt = repository.get(user.getSsn(), Account.class);
    return opt.get();
  }

  public Account createAccount(User user) {
    Account account = new AccountBuilder()
            .withSsn(user.getSsn())
            .withUser(user)
            .build();
    repository.put(account);
    return getAccount(user);
  }

  public void save(Account account) {
    repository.put(account);
  }

  @Entity
  @VirtualValue
  public static interface Account {

    @Id
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

