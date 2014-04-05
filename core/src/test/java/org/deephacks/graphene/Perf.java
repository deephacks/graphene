package org.deephacks.graphene;

import org.deephacks.graphene.BaseTest.A;

import java.util.ArrayList;
import java.util.List;

import static org.deephacks.graphene.BaseTest.buildA;
import static org.deephacks.graphene.TransactionManager.withTx;

public class Perf {
  public static void main(String[] args) throws Exception {
    EntityRepository repository = new EntityRepository();
    List<A> as = new ArrayList<>();
    for (int i = 0; i < 10000; i++) {
      as.add(buildA("a"+i));
    }
    System.out.println("--");
    System.in.read();
    long time = System.currentTimeMillis();
    withTx(tx -> {
      for (A a : as) {
        repository.put(a);
      }
    });

    System.out.println((System.currentTimeMillis() - time));
  }
}
