package org.deephacks.graphene;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Optional;

import static org.deephacks.graphene.TransactionManager.withTx;
import static org.junit.Assert.*;

public class ReferencesTest extends BaseTest {

  /**
   * Test that references (single and collection) are fetched eagerly
   * through multiple levels.
   */
  @Test
  public void test_references_with_multiple_levels() {
    LinkedHashMap<String, StandardProperties> map = defaultReferences();
    withTx(tx -> {
      map.values().forEach(repository::put);
      Optional result = repository.get("b1", B.class);
      assertEquals(map.get("b1"), result.get());

      result = repository.get("b2", B.class);
      assertEquals(map.get("b2"), result.get());

      result = repository.get("c1", C.class);
      assertEquals(map.get("c1"), result.get());
    });
  }

  /**
   * Test that instance that have references to non-existing instance cannot
   * be created.
   */
  @Test
  public void test_missing_references() {
    LinkedHashMap<String, StandardProperties> map = defaultReferences();
    withTx(tx -> {
      try {
        repository.put(map.get("b2"));
        fail("Should violate constraint");
      } catch (ForeignKeyConstraintException e) {
        assertTrue(true);
      }
    });
    assertFalse(repository.get(map.get("b2").getId(), B.class).isPresent());
  }

  /**
   * Test that instances that other have references to cannot be deleted.
   */
  @Test
  public void test_referential_integrity_delete_constraint() {
    withTx(tx -> {
      LinkedHashMap<String, StandardProperties> map = defaultReferences();
      map.values().forEach(repository::put);
      try {
        repository.delete(map.get("a2").getId(), A.class);
        fail("Should not be possible to delete instance that are referenced by others");
      } catch (DeleteConstraintException e) {
        assertTrue(true);
      }
    });
  }

  /**
   * Test that an existing delete constraint can be fixed by deleting instances
   * that reference others.
   */
  @Test
  public void test_fixing_delete_constraint() {
    LinkedHashMap<String, StandardProperties> map = defaultReferences();
    withTx(tx -> {
      // create instance without references and create
      // instances that reference them afterwards in order
      // to not violate referential integrity
      map.values().forEach(repository::put);

      StandardProperties instance = map.get("c1");
      repository.delete(instance.getId(), instance.getClass());
      // make sure to commit! otherwise the following
      // DeleteConstraintException will rollback this delete
    });

    withTx(tx -> {
      try {
        StandardProperties instance = map.get("b1");
        repository.delete(instance.getId(), B.class);
        fail("c2 should have a reference to b1");
      } catch (DeleteConstraintException e) {
        tx.rollback();
      }
    });
    withTx(tx -> {
      StandardProperties instance = map.get("c2");
      repository.delete(instance.getId(), instance.getClass());

      instance = map.get("b1");
      repository.delete(instance.getId(), instance.getClass());
      // make sure to commit! otherwise the following
      // DeleteConstraintException will rollback this delete
    });
    withTx(tx -> {
      try {
        StandardProperties instance = map.get("a1");
        repository.delete(instance.getId(), A.class);
        fail("b2 should have a reference to a1");
      } catch (DeleteConstraintException e) {
        tx.rollback();
      }
    });
    withTx(tx -> {
      StandardProperties instance = map.get("b2");
      repository.delete(instance.getId(), instance.getClass());
      instance = map.get("a1");
      repository.delete(instance.getId(), instance.getClass());
      instance = map.get("a2");
      repository.delete(instance.getId(), instance.getClass());
      repository.delete(instance.getId(), instance.getClass());
      instance = map.get("a3");
      repository.delete(instance.getId(), instance.getClass());
    });
  }

}
