package org.deephacks.graphene.webadmin.service;

import org.deephacks.graphene.Entity;
import org.deephacks.graphene.Id;
import org.deephacks.vals.VirtualValue;

@Entity @VirtualValue
public interface TestEntity {

  @Id
  String getId();

  String getName();

}
