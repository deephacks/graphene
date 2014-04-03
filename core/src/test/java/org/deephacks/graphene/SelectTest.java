package org.deephacks.graphene;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.deephacks.graphene.TransactionManager.withTx;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class SelectTest extends BaseTest {

  /**
   * Test that it is possible to select all instances of a specific type.
   */
  @Test
  public void test_select_all() {
    withTx(tx -> {
      int numInstances = 10;
      for (int i = 0; i < numInstances; i++) {
        repository.put(buildA("a" + i));
        repository.put(buildB("b" + i));
        repository.put(buildC("c" + i));
      }
      assertSelectAll(A.class, "a", numInstances);
      assertSelectAll(B.class, "b", numInstances);
      assertSelectAll(C.class, "c", numInstances);
    });
  }

  /**
   * Test that we can select instances based on single buildEmbedded data.
   */
  @Test
  public void test_select_single_embedded() {
    withTx(tx -> {
      int numInstances = 10;
      String value = UUID.randomUUID().toString();
      ArrayList<A> instances = new ArrayList<>();
      for (int i = 0; i < numInstances; i++) {
        A a = buildA("" + i, value);
        instances.add(a);
        repository.put(a);
      }

      List<A> result = repository.stream(A.class)
              .filter(a -> a.getEmbedded().getStringValue().equals(value))
              .collect(Collectors.toList());

      for (int i = 0; i < result.size(); i++) {
        A expected = instances.get(i);
        assertEquals(expected, result.get(i));
      }
    });
  }

  /**
   * Test that we can select instances based on single reference data.
   */
  @Test
  public void test_select_single_reference() {
    LinkedHashMap<String, StandardProperties> map = defaultReferences();
    withTx(tx -> {
      map.values().forEach(repository::put);
      List<B> result = repository.stream(B.class)
              .filter(b -> b.getA() != null && b.getA().getStringValue().equals("value"))
              .collect(Collectors.toList());
      for (B b : result) {
        StandardProperties expected = map.get(b.getId());
        assertEquals(b, expected);
      }
      assertThat(result.size(), is(2));
    });
  }

  /**
   * Test that we can select instances based on single reference key data.
   */
  @Test
  public void test_select_single_reference_key() {
    LinkedHashMap<String, StandardProperties> map = defaultReferences();
    withTx(tx -> {
      map.values().forEach(repository::put);
      List<B> result = repository.stream(B.class)
              .filter(b -> b.getA() != null && b.getA().getId().equals("a1"))
              .collect(Collectors.toList());
      for (B b : result) {
        StandardProperties expected = map.get(b.getId());
        assertEquals(b, expected);
      }
      assertThat(result.size(), is(2));
    });
  }

  /**
   * Test that it is possible to restrict min and max keys from a select and
   * that keys are given in insert order.
   */
  @Test
  public void test_select_min_max() {
    /*
    withTx(tx -> {
      int numInstances = 10;
      for (int i = 0; i < numInstances; i++) {
        repository.put(buildA("a" + i));
        repository.put(buildB("b" + i));
        repository.put(buildC("c" + i));
      }
      try (ResultSet<B> resultSet = repository.select(B.class).setFirstResult("b3").setLastResult("b6").retrieve()) {
        List<B> result = Guavas.newArrayList(resultSet);
        assertThat(result.size(), is(4));
        // assert in key sort (not insert) order.
        assertEquals(buildB("b3"), result.get(0));
        assertEquals(buildB("b4"), result.get(1));
        assertEquals(buildB("b5"), result.get(2));
        assertEquals(buildB("b6"), result.get(3));
      }
    });
    */
  }

  /**
   * Test that it is possible to restrict min and max key and max results from
   * a select and that keys are given in sorted order.
   */
  @Test
  public void test_select_min_max_and_max_results() {
    /*
    withTx(tx -> {
      int numInstances = 10;
      // reverse order which instances are inserted to check that sorted order is respected
      for (int i = 0; i < numInstances; i++) {
        repository.put(buildA("a" + i));
        repository.put(buildB("b" + i));
        repository.put(buildC("c" + i));
      }
      ResultSet<B> resultSet = repository.select(B.class).setFirstResult("b3").setLastResult("b6").setMaxResults(2).retrieve();
      List<B> result = Guavas.newArrayList(resultSet);
      resultSet.close();
      assertThat(result.size(), is(2));
      // assert in key sort (not insert) order and that only two first result is returned
      assertEquals(buildB("b3"), result.get(0));
      assertEquals(buildB("b4"), result.get(1));
    });
    */
  }

  /**
   * Test a query with OR predicate
   */
  @Test
  public void test_select_or_predicate() {
    withTx(tx -> {
      A a1 = buildA("a1", "v1");
      if (!repository.put(a1)) {
        throw new IllegalStateException("Could not create");
      }
      A a2 = buildA("a2", "v2");
      if (!repository.put(a2)) {
        throw new IllegalStateException("Could not create");
      }
      List<A> result = repository.stream(A.class)
              .filter(a -> a.getStringValue().contains("v1") || a.getStringValue().contains("v2"))
              .collect(Collectors.toList());
      assertThat(result.size(), is(2));
    });
  }

  /**
   * Test a query with NOT predicate
   */
  @Test
  public void test_select_not_predicate() {
    withTx(tx -> {
      A a1 = buildA("a1", "v1");
      repository.put(a1);
      A a2 = buildA("a2", "v2");
      repository.put(a2);
      A a3 = buildA("a3", "a3");
      repository.put(a3);
      List<A> result = repository.stream(A.class)
              .filter(a -> !a.getStringValue().contains("v"))
              .collect(Collectors.toList());
      assertThat(result.size(), is(1));
      assertEquals(a3, result.get(0));
    });
  }

  /**
   * Test a query with AND predicate
   */
  @Test
  public void test_select_and_predicate() {
    withTx(tx -> {
      A a1 = buildA("a1", "v1");
      repository.put(a1);
      A a2 = buildA("a2", "v2");
      repository.put(a2);
      List<A> result = repository.stream(A.class)
              .filter(a -> a.getStringValue().contains("v1") && a.getIntValue() < 0)
              .collect(Collectors.toList());
      assertThat(result.size(), is(1));
    });
  }

  /**
   * Test that we can combine min, max, results and predicates.
   */
  @Test
  public void test_select_min_max_and_max_results_predicate() {
    /*
    withTx(tx -> {
      int numInstances = 10;
      // reverse order which instances are inserted to check that sorted order is respected
      for (int i = 0; i < numInstances; i++) {
        final A a = buildA("a" + i);
        repository.put(a);
        final B b = buildB("b" + i, "b" + i);
        repository.put(b);
        final C c = buildC("c" + i);
        repository.put(c);
      }
      ResultSet<B> resultSet = repository.select(B.class,
              field("stringValue").is(equal("b4")))
              .setFirstResult("b3").setLastResult("b6").setMaxResults(2).retrieve();
      List<B> result = Guavas.newArrayList(resultSet);
      resultSet.close();
      assertThat(result.size(), is(1));
      B b4 = buildB("b4", "b4");
      assertEquals(b4, result.get(0));
    });
    */
  }

  /**
   * Test that we can cant select elements that are outside first/last key span.
   */
  @Test
  public void test_select_predicate_outside_min_max() {
    withTx(tx -> {
      int numInstances = 10;
      // reverse order which instances are inserted to check that sorted order is respected
      for (int i = numInstances; i > -1; i--) {
        final A a = buildA("a" + i);
        repository.put(a);
        final B b = buildB("b" + i);
        repository.put(b);
        final C c = buildC("c" + i);
        repository.put(c);
      }
      List<B> result = repository.stream(B.class)
              .filter(b -> b.getStringValue().equals("b1"))
              .limit(2)
              .collect(Collectors.toList());
    /*
    ResultSet<B> resultSet = repository.select(B.class,
            field("stringValue").is(equal("b1")))
            .setFirstResult("b3").setLastResult("b6").setMaxResults(2).retrieve();
            */
      assertThat(result.size(), is(0));
    });
  }

  private void assertSelectAll(Class<? extends StandardProperties> entityClass, String prefix, int numInstances) {
    final List<? extends StandardProperties> resultSet = repository.selectAll(entityClass);
    final ArrayList<? extends StandardProperties> objects = Guavas.newArrayList(resultSet);
    for (int i = 0; i < objects.size(); i++) {
      StandardProperties expected;
      if (entityClass.equals(A.class)) {
        expected = buildA(prefix + i);
      } else if (entityClass.equals(B.class)) {
        expected = buildB(prefix + i);
      } else if (entityClass.equals(C.class)) {
        expected = buildC(prefix + i);
      } else {
        throw new IllegalArgumentException("Did not recognize class " + entityClass);
      }
      assertEquals(expected, objects.get(i));
    }
    assertThat(objects.size(), is(numInstances));
  }
}
