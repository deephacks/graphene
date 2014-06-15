package org.deephacks.graphene.cdi;

import org.deephacks.graphene.Graphene;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@ApplicationScoped
public class Application {

  @Inject
  private Graphene graphene;

  @Produces
  @Singleton
  public static Graphene produceGraphene() {
    return Graphene.builder().build();
  }

  @PreDestroy
  public void closing() {
    System.out.println("Closing graphene...");
    graphene.close();
    System.out.println("Closing graphene success");
  }
}
