package org.deephacks.graphene.webadmin;

import org.deephacks.graphene.Entity;
import org.deephacks.graphene.Id;
import org.deephacks.vals.VirtualValue;

@VirtualValue @Entity
public interface E {
  @Id
  String getId();

}
