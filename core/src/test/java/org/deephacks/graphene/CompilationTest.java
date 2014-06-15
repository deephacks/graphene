package org.deephacks.graphene;

import org.deephacks.graphene.BuilderProxy.Builder;
import org.deephacks.graphene.otherpackage.OtherPackageValue;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CompilationTest extends BaseTest {

  public void test_missing_key() {

  }

  public void test_multiple_simple_key_annotations() {

  }

  public void test_default_values() {

  }

  @Test
  public void test_top_entity() {
    OtherPackageValue value = new Builder<>(OtherPackageValue.class)
            .set(OtherPackageValue::getValue, "value")
            .build().get();

    TopEntity top = new Builder<>(TopEntity.class)
            .set(TopEntity::getId, "1")
            .set(TopEntity::getValue, value)
            .build().get();
    graphene.put(top);
    Optional<TopEntity> optional = graphene.get("1", TopEntity.class);
    assertThat(top, is(optional.get()));
  }
}
