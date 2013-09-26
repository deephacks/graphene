package org.deephacks.confit.internal.berkeley;


import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.deephacks.confit.internal.berkeley.TestData.A;
import org.deephacks.confit.internal.berkeley.TestData.B;
import org.deephacks.confit.internal.berkeley.TestData.C;
import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.ResultSet;
import org.deephacks.graphene.internal.UniqueIds;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.deephacks.confit.internal.berkeley.TestData.defaultValues;
import static org.deephacks.graphene.Criteria.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

public class EntityRepositoryTest {
    private final EntityRepository repository = new EntityRepository();

    @Before
    public void before() {
        repository.deleteAll();
        UniqueIds ids = new UniqueIds();
        ids.deleteAll();
        repository.commit();
        assertThat(repository.countAll(), is(0L));
    }
    /**
     * Test that it is possible to put, get and delete instances of
     * different types.
     */
    @Test
    public void test_basic_put_get_delete() {
        //putAndGetAssert(defaultValues("a", A.class));
        //putAndGetAssert(defaultValues("b", B.class));
        putAndGetAssert(defaultValues("c", C.class));
    }

    /**
     * Test that it is possible to overwrite existing values with
     * other values as well as nullifying them.
     */
    @Test
    public void test_put_overwrite_with_value_and_nulls() {
        A a = defaultValues("a", A.class);
        repository.put(a);

        a.setStringValue("newValue");
        a.setFloatValues(null);
        a.setIntValue(null);
        repository.put(a);

        Optional<A> result = repository.get("a", A.class);
        assertReflectionEquals(result.get(), a, LENIENT_ORDER);

    }

    /**
     * Test that it is not possible to overwrite existing values with putNoOverwrite.
     */
    @Test
    public void test_put_noOverwrite() {
        A a = defaultValues("a", A.class);
        repository.putNoOverwrite(a);
        a.setStringValue("newValue");
        a.setFloatValues(null);
        a.setIntValue(null);
        assertFalse(repository.putNoOverwrite(a));
        a = defaultValues("a", A.class);
        Optional<A> result = repository.get("a", A.class);
        assertReflectionEquals(a, result.get(), LENIENT_ORDER);
    }

    /**
     * Test that no instance is returned when trying to delete non existing instance.
     */
    @Test
    public void test_delete_non_existin_instances() {
        final Optional<A> a = repository.delete(UUID.randomUUID().toString(), A.class);
        assertFalse(a.isPresent());

    }
    /**
     * Test that it is possible to select all instances of a specific type.
     */
    @Test
    public void test_select_all() {
        int numInstances = 10;
        for (int i = 0; i < numInstances; i++) {
            repository.put(defaultValues("a" + i, A.class));
            repository.put(defaultValues("b" + i, B.class));
            repository.put(defaultValues("c" + i, C.class));
        }
        assertSelectAll(A.class, "a", numInstances);
        assertSelectAll(B.class, "b", numInstances);
        assertSelectAll(C.class, "c", numInstances);
    }

    /**
     * Test that it is possible to restrict min and max keys from a select and
     * that keys are given in insert order.
     */
    @Test
    public void test_select_min_max() {
        int numInstances = 10;
        for (int i = 0; i < numInstances; i++) {
            repository.put(defaultValues("a" + i, A.class));
            repository.put(defaultValues("b" + i, B.class));
            repository.put(defaultValues("c" + i, C.class));
        }
        try (ResultSet<B> resultSet = repository.select(B.class).setFirstResult("b3").setLastResult("b6").retrieve()) {
            List<B> result = Lists.newArrayList(resultSet);
            assertThat(result.size(), is(4));
            // assert in key sort (not insert) order.
            assertReflectionEquals(defaultValues("b3", B.class), result.get(0), LENIENT_ORDER);
            assertReflectionEquals(defaultValues("b4", B.class), result.get(1), LENIENT_ORDER);
            assertReflectionEquals(defaultValues("b5", B.class), result.get(2), LENIENT_ORDER);
            assertReflectionEquals(defaultValues("b6", B.class), result.get(3), LENIENT_ORDER);
        }
    }

    /**
     * Test that it is possible to restrict min and max key and max results from
     * a select and that keys are given in sorted order.
     */
    @Test
    public void test_select_min_max_and_max_results() {
        int numInstances = 10;
        // reverse order which instances are inserted to check that sorted order is respected
        for (int i = 0; i < numInstances; i++) {
            repository.put(defaultValues("a" + i, A.class));
            repository.put(defaultValues("b" + i, B.class));
            repository.put(defaultValues("c" + i, C.class));
        }
        ResultSet<B> resultSet = repository.select(B.class).setFirstResult("b3").setLastResult("b6").setMaxResults(2).retrieve();
        List<B> result = Lists.newArrayList(resultSet);
        resultSet.close();
        assertThat(result.size(), is(2));
        // assert in key sort (not insert) order and that only two first result is returned
        assertReflectionEquals(defaultValues("b3", B.class), result.get(0), LENIENT_ORDER);
        assertReflectionEquals(defaultValues("b4", B.class), result.get(1), LENIENT_ORDER);
    }

    /**
     * Test a query with OR predicate
     */
    @Test
    public void test_select_or_predicate() {
        A a1 = defaultValues("a1", A.class);
        a1.setStringValue("v1");
        if (!repository.put(a1)) {
            throw new IllegalStateException("Could not create");
        }
        A a2 = defaultValues("a2", A.class);
        a2.setStringValue("v2");
        if (!repository.put(a2)) {
            throw new IllegalStateException("Could not create");
        }
        try (ResultSet<A> resultSet = repository.select(A.class,
                field("stringValue").is(contains("v1")).or(field("stringValue").is(equal("v2"))))
                .retrieve()) {
            List<A> result = Lists.newArrayList(resultSet);
            assertThat(result.size(), is(2));
        }
    }

    /**
     * Test a query with NOT predicate
     */
    @Test
    public void test_select_not_predicate() {
        A a1 = defaultValues("a1", A.class);
        a1.setStringValue("v1");
        repository.put(a1);
        A a2 = defaultValues("a2", A.class);
        a2.setStringValue("v2");
        repository.put(a2);
        A a3 = defaultValues("a3", A.class);
        a3.setStringValue("a3");
        repository.put(a3);
        try (ResultSet<A> resultSet = repository.select(A.class,
                        field("stringValue").not(contains("v"))).retrieve()) {
            List<A> result = Lists.newArrayList(resultSet);
            assertThat(result.size(), is(1));
            assertReflectionEquals(a3, result.get(0), LENIENT_ORDER);
        }
    }

    /**
     * Test a query with AND predicate
     */
    @Test
    public void test_select_and_predicate() {
        A a1 = defaultValues("a1", A.class);
        a1.setStringValue("v1");
        repository.put(a1);
        A a2 = defaultValues("a2", A.class);
        a2.setStringValue("v2");
        a2.setIntValue(1);
        repository.put(a2);
        try (ResultSet<A> resultSet = repository.select(A.class,
                field("stringValue").is(contains("v")).and(field("intValue").is(largerThan(0))))
                .retrieve()) {
            List<A> result = Lists.newArrayList(resultSet);
            assertThat(result.size(), is(1));
        }
    }

    /**
     * Test that we can combine min, max, results and predicates.
     */
    @Test
    public void test_select_min_max_and_max_results_predicate() {
        int numInstances = 10;
        // reverse order which instances are inserted to check that sorted order is respected
        for (int i = 0; i < numInstances; i++) {
            final A a = defaultValues("a" + i, A.class);
            repository.put(a);
            final B b = defaultValues("b" + i, B.class);
            b.setStringValue("b" + i);
            repository.put(b);
            final C c = defaultValues("c" + i, C.class);
            repository.put(c);
        }
        ResultSet<B> resultSet = repository.select(B.class,
                field("stringValue").is(equal("b4")))
                .setFirstResult("b3").setLastResult("b6").setMaxResults(2).retrieve();
        List<B> result = Lists.newArrayList(resultSet);
        resultSet.close();
        assertThat(result.size(), is(1));
        B b4 = defaultValues("b4", B.class);
        b4.setStringValue("b4");
        assertReflectionEquals(b4, result.get(0), LENIENT_ORDER);
    }

    /**
     * Test that we can cant select elements that are outside first/last key span.
     */
    @Test
    public void test_select_predictae_outside_min_max() {
        int numInstances = 10;
        // reverse order which instances are inserted to check that sorted order is respected
        for (int i = numInstances; i > -1; i--) {
            final A a = defaultValues("a" + i, A.class);
            repository.put(a);
            final B b = defaultValues("b" + i, B.class);
            b.setStringValue("b" + i);
            repository.put(b);
            final C c = defaultValues("c" + i, C.class);
            repository.put(c);
        }
        ResultSet<B> resultSet = repository.select(B.class,
                field("stringValue").is(equal("b1")))
                .setFirstResult("b3").setLastResult("b6").setMaxResults(2).retrieve();
        List<B> result = Lists.newArrayList(resultSet);
        resultSet.close();
        assertThat(result.size(), is(0));
    }


    private void assertSelectAll(Class<? extends A> entityClass, String prefix, int numInstances) {
        final ResultSet<? extends A> resultSet = repository.select(entityClass).retrieve();
        final ArrayList<? extends A> objects = Lists.newArrayList(resultSet);
        resultSet.close();
        for (int i = 0; i < objects.size(); i++) {
            A expected = defaultValues(prefix + i, entityClass);
            assertReflectionEquals(expected, objects.get(i), LENIENT_ORDER);
        }
        assertThat(objects.size(), is(numInstances));
    }

    public <T extends A> void putAndGetAssert(T object) {
        repository.put(object);
        Optional<? extends A> optional = repository.get(object.getId(), object.getClass());
        assertReflectionEquals(object, optional.get(), LENIENT_ORDER);
        optional = repository.delete(object.getId(), object.getClass());
        assertTrue(optional.isPresent());
        assertReflectionEquals(object, optional.get(), LENIENT_ORDER);
        optional = repository.get(object.getId(), object.getClass());
        assertFalse(optional.isPresent());
    }
}
