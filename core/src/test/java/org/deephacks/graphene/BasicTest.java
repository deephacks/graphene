package org.deephacks.graphene;


import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Test serialization of entity instances, including all different types that
 * are supported like primitive and object types, enums, custom and buildEmbedded types.
 */
public class BasicTest extends BaseTest {

  /**
   * Test that it is possible to put, get and delete instances of
   * different types.
   */
  @Test
  public void test_basic_put_get_delete() {
    repository.withTx(tx -> {
      putAndGetAssert(buildA("a"), A.class);
      putAndGetAssert(buildB("b"), B.class);
      putAndGetAssert(buildC("c"), C.class);
    });
  }

  /**
   * Test that it is not possible to overwrite existing values with putNoOverwrite.
   */
  @Test
  public void test_put_noOverwrite() {
    repository.withTx(tx -> {
      A a = buildA("a");
      repository.putNoOverwrite(a);
      assertFalse(repository.putNoOverwrite(a));
      a = buildA("a");
      Optional<A> result = repository.get("a", A.class);
      assertEquals(a, result.get());
    });
  }

  /**
   * Test that no instance is returned when trying to delete non existing instance.
   */
  @Test
  public void test_delete_non_existin_instances() {
    repository.withTx(tx -> {
      final Optional<A> a = repository.delete(UUID.randomUUID().toString(), A.class);
      assertFalse(a.isPresent());
    });
  }

  public <T extends StandardProperties> void putAndGetAssert(T object, Class<T> cls) {
    repository.put(object);
    Optional<? extends StandardProperties> optional = repository.get(object.getId(), cls);
    assertEquals(object, optional.get());
    optional = repository.delete(object.getId(), cls);
    assertTrue(optional.isPresent());
    assertEquals(object, optional.get());
    optional = repository.get(object.getId(), cls);
    assertFalse(optional.isPresent());
  }
}
