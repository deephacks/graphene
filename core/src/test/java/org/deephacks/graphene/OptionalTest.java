package org.deephacks.graphene;

import org.deephacks.graphene.BuilderProxy.Builder;
import org.deephacks.graphene.Entities.OptionalValues;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;

public class OptionalTest extends BaseTest {

  @Test
  public void test_optional() {
    OptionalValues o = new Builder<>(OptionalValues.class)
            .set(OptionalValues::getId, "1")
            .build().get();
    graphene.put(o);
    OptionalValues result = graphene.get("1", OptionalValues.class).get();
    assertFalse(result.getListValue().isPresent());
    assertFalse(result.getSetValue().isPresent());
    assertFalse(result.getMapValue().isPresent());
    //assertFalse(result.getValueArray().isPresent());
  }
}
