package org.deephacks.graphene;

import org.deephacks.graphene.BuilderProxy.Builder;
import org.deephacks.graphene.Entities.ByteArrayKey;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PrimaryKeyTest extends BaseTest {

  // the entity specify a key that is exactly 3 bytes wide.
  // if the key stored key would be shorter, null bytes would be
  // appended to end of key (when fetched) and the test would fail!
  @Test
  public void test_byte_array_key_and_no_values() {
    ByteArrayKey object = new Builder<>(ByteArrayKey.class)
            .set(ByteArrayKey::getKey, new byte[]{1, 2, 3}).build().get();
    graphene.put(object);
    ByteArrayKey result = graphene.get(new byte[]{1, 2, 3}, ByteArrayKey.class).get();
    assertThat(result, is(object));
  }

  public void test_object_key() {

  }
}
