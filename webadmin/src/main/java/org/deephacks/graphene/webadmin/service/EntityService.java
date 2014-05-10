package org.deephacks.graphene.webadmin.service;

import org.deephacks.graphene.EntityRepository;

import java.util.List;

public class EntityService {

  private EntityRepository repository = new EntityRepository();


  public <T> List<T> list(Class<T> cls) {
    return repository.selectAll(cls);
  }

  public void put(Object object) {
    repository.put(object);
  }

}
