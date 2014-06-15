package org.deephacks.graphene;


import org.deephacks.graphene.Entities.A;
import org.deephacks.graphene.Entities.Identity;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.deephacks.graphene.Entities.buildA;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Test serialization of type instances, including all different types that
 * are supported like primitive and object types, enums, custom and buildEmbedded types.
 */
public class BasicTest extends BaseTest {
  @Test
  public void test_put_and_delete_without_tx() {
    putAndGetAssert(buildA("a"), A.class);
    //putAndGetAssert(buildB("b"), B.class);
    //putAndGetAssert(buildC("c"), C.class);
  }


  @Test
  public void test_select() {
    graphene.put(buildA("a1"));
    graphene.put(buildA("a2"));
    graphene.put(buildA("a3"));
    assertThat(graphene.selectAll(A.class).size(), is(3));

    graphene.withTxRead(tx -> {
      List<A> result = tx.stream(A.class).filter(a -> a.getId().endsWith("2")).collect(Collectors.toList());
      assertThat(result.size(), is(1));
    });
  }


  // Test that it is possible to put, get and delete instances of
  // different types.
  @Test
  public void test_basic_put_get_delete() {
    graphene.withTxWrite(tx -> {
      putAndGetAssert(buildA("a"), A.class);
      //putAndGetAssert(buildB("b"), B.class);
      //putAndGetAssert(buildC("c"), C.class);
    });
  }


  // Test that it is not possible to overwrite existing values with putNoOverwrite.
  @Test
  public void test_put_noOverwrite() {
    graphene.withTxWrite(tx -> {
      A a = buildA("a");
      tx.putNoOverwrite(a);
      assertFalse(tx.putNoOverwrite(a));
      a = buildA("a");
      Optional<A> result = tx.get("a", A.class);
      assertEquals(a, result.get());
    });
  }


  // Test that no instance is returned when trying to delete non existing instance.
  @Test
  public void test_delete_non_existin_instances() {
    graphene.withTxWrite(tx -> {
      final Optional<A> a = tx.delete(UUID.randomUUID().toString(), A.class);
      assertFalse(a.isPresent());
    });
  }

  public <T extends Identity> void putAndGetAssert(T object, Class<T> cls) {
    graphene.put(object);
    Optional<? extends Identity> optional = graphene.get(object.getId(), cls);
    assertEquals(object.toString(), optional.get().toString());
    assertTrue(object.equals(optional.get()));
    assertTrue(optional.get().equals(object));
    assertEquals(object.hashCode(), optional.get().hashCode());
    optional = graphene.delete(object.getId(), cls);
    assertTrue(optional.isPresent());
    assertEquals(object, optional.get());
    optional = graphene.get(object.getId(), cls);
    assertFalse(optional.isPresent());
  }
}

