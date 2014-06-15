package org.deephacks.graphene;

import org.deephacks.graphene.otherpackage.OtherPackageValue;

@Entity
public interface TopEntity {
  @Key
  String getId();

  OtherPackageValue getValue();
}
