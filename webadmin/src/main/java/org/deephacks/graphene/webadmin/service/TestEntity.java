package org.deephacks.graphene.webadmin.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.deephacks.graphene.Entity;
import org.deephacks.graphene.Id;
import org.deephacks.vals.VirtualValue;

@Entity @VirtualValue @JsonDeserialize(builder = TestEntityBuilder.class)
public interface TestEntity {

  @Id
  String getId();

  String getName();

}
