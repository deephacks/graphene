package org.deephacks.graphene;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.deephacks.graphene.Criteria.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

public class SelectTest extends BaseTest {

  /**
   * Test that it is possible to select all instances of a specific type.
   */
  @Test
  public void test_select_all() {
    repository.beginTransaction();
    int numInstances = 10;
    for (int i = 0; i < numInstances; i++) {
      repository.put(buildA("a" + i));
      repository.put(buildB("b" + i));
      repository.put(buildC("c" + i));
    }
    assertSelectAll(A.class, "a", numInstances);
    assertSelectAll(B.class, "b", numInstances);
    assertSelectAll(C.class, "c", numInstances);
    repository.commit();
  }

  /**
   * Test that we can select instances based on single buildEmbedded data.
   */
  @Test
  public void test_select_single_embedded() {
    repository.beginTransaction();
    int numInstances = 10;
    String value = UUID.randomUUID().toString();
    ArrayList<A> instances = new ArrayList<>();
    for (int i = 0; i < numInstances; i++) {
      A a = buildA("" + i, value);
      instances.add(a);
      repository.put(a);
    }
    try (ResultSet<A> resultSet = repository.select(A.class, field("buildEmbedded.stringValue").is(equal(value))).retrieve()) {
      ArrayList<A> objects = Guavas.newArrayList(resultSet);
      for (int i = 0; i < objects.size(); i++) {
        A expected = instances.get(i);
        assertReflectionEquals(expected, objects.get(i), LENIENT_ORDER);
      }
    }
    repository.commit();
  }

  /**
   * Test that we can select instances based on single reference data.
   */
  @Test
  public void test_select_single_reference() {
    LinkedHashMap<String, StandardProperties> map = defaultReferences();
    repository.beginTransaction();
    map.values().forEach(repository::put);
    repository.commit();
    repository.beginTransaction();
    ArrayList<B> objects;
    try (ResultSet<B> resultSet = repository.select(B.class, field("a.stringValue").is(equal("value"))).retrieve()) {
      objects = Guavas.newArrayList(resultSet);
      for (int i = 0; i < objects.size(); i++) {
        StandardProperties expected = map.get(objects.get(i).getId());
        assertReflectionEquals(objects.get(i), expected, LENIENT_ORDER);
      }
    }
    assertThat(objects.size(), is(2));
    repository.commit();
  }

  /**
   * Test that we can select instances based on single reference key data.
   */
  @Test
  public void test_select_single_reference_key() {
    LinkedHashMap<String, StandardProperties> map = defaultReferences();
    repository.beginTransaction();
    map.values().forEach(repository::put);
    repository.commit();
    repository.beginTransaction();
    ArrayList<B> objects;
    try (ResultSet<B> resultSet = repository.select(B.class, field("a.id").is(equal("a1"))).retrieve()) {
      objects = Guavas.newArrayList(resultSet);
      for (int i = 0; i < objects.size(); i++) {
        StandardProperties expected = map.get(objects.get(i).getId());
        assertReflectionEquals(objects.get(i), expected, LENIENT_ORDER);
      }
    }
    assertThat(objects.size(), is(2));
    repository.commit();
  }

  /**
   * Test that it is possible to restrict min and max keys from a select and
   * that keys are given in insert order.
   */
  @Test
  public void test_select_min_max() {
    repository.beginTransaction();
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
      assertReflectionEquals(buildB("b3"), result.get(0), LENIENT_ORDER);
      assertReflectionEquals(buildB("b4"), result.get(1), LENIENT_ORDER);
      assertReflectionEquals(buildB("b5"), result.get(2), LENIENT_ORDER);
      assertReflectionEquals(buildB("b6"), result.get(3), LENIENT_ORDER);
    }
    repository.commit();
  }

  /**
   * Test that it is possible to restrict min and max key and max results from
   * a select and that keys are given in sorted order.
   */
  @Test
  public void test_select_min_max_and_max_results() {
    repository.beginTransaction();
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
    assertReflectionEquals(buildB("b3"), result.get(0), LENIENT_ORDER);
    assertReflectionEquals(buildB("b4"), result.get(1), LENIENT_ORDER);
    repository.commit();
  }

  /**
   * Test a query with OR predicate
   */
  @Test
  public void test_select_or_predicate() {
    repository.beginTransaction();
    A a1 = buildA("a1", "v1");
    if (!repository.put(a1)) {
      throw new IllegalStateException("Could not create");
    }
    A a2 = buildA("a2", "v2");
    if (!repository.put(a2)) {
      throw new IllegalStateException("Could not create");
    }
    try (ResultSet<A> resultSet = repository.select(A.class,
            field("stringValue").is(contains("v1")).or(field("stringValue").is(equal("v2"))))
            .retrieve()) {
      List<A> result = Guavas.newArrayList(resultSet);
      assertThat(result.size(), is(2));
    }
    repository.commit();
  }

  /**
   * Test a query with NOT predicate
   */
  @Test
  public void test_select_not_predicate() {
    repository.beginTransaction();
    A a1 = buildA("a1", "v1");
    repository.put(a1);
    A a2 = buildA("a2", "v2");
    repository.put(a2);
    A a3 = buildA("a3", "a3");
    repository.put(a3);
    try (ResultSet<A> resultSet = repository.select(A.class,
            field("stringValue").not(contains("v"))).retrieve()) {
      List<A> result = Guavas.newArrayList(resultSet);
      assertThat(result.size(), is(1));
      assertReflectionEquals(a3, result.get(0), LENIENT_ORDER);
    }
    repository.commit();
  }

  /**
   * Test a query with AND predicate
   */
  @Test
  public void test_select_and_predicate() {
    repository.beginTransaction();
    A a1 = buildA("a1", "v1");
    repository.put(a1);
    A a2 = buildA("a2", "v2");
    repository.put(a2);
    try (ResultSet<A> resultSet = repository.select(A.class,
            field("stringValue").is(contains("v")).and(field("intValue").is(largerThan(0))))
            .retrieve()) {
      List<A> result = Guavas.newArrayList(resultSet);
      assertThat(result.size(), is(1));
    }
    repository.commit();
  }

  /**
   * Test that we can combine min, max, results and predicates.
   */
  @Test
  public void test_select_min_max_and_max_results_predicate() {
    repository.beginTransaction();
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
    assertReflectionEquals(b4, result.get(0), LENIENT_ORDER);
    repository.commit();
  }

  /**
   * Test that we can cant select elements that are outside first/last key span.
   */
  @Test
  public void test_select_predicate_outside_min_max() {
    repository.beginTransaction();
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
    ResultSet<B> resultSet = repository.select(B.class,
            field("stringValue").is(equal("b1")))
            .setFirstResult("b3").setLastResult("b6").setMaxResults(2).retrieve();
    List<B> result = Guavas.newArrayList(resultSet);
    resultSet.close();
    assertThat(result.size(), is(0));
    repository.commit();
  }

  private void assertSelectAll(Class<? extends StandardProperties> entityClass, String prefix, int numInstances) {
    final ResultSet<? extends StandardProperties> resultSet = repository.select(entityClass).retrieve();
    final ArrayList<? extends StandardProperties> objects = Guavas.newArrayList(resultSet);
    resultSet.close();
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
      assertReflectionEquals(expected, objects.get(i), LENIENT_ORDER);
    }
    assertThat(objects.size(), is(numInstances));
  }
}
