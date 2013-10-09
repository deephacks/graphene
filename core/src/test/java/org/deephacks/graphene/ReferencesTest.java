package org.deephacks.graphene;

import com.google.common.base.Optional;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

public class ReferencesTest extends BaseTest {

    /**
     * Test that references (single and collection) are fetched eagerly
     * through multiple levels.
     */
    @Test
    public void test_references_with_multiple_levels() {

        LinkedHashMap<String,A> map = defaultReferences();
        repository.beginTransaction();
        for (A a : map.values()) {
            repository.put(a);
        }

        Optional result = repository.get("b1", B.class);
        assertReflectionEquals(map.get("b1"), result.get(), LENIENT_ORDER);

        result = repository.get("b2", B.class);
        assertReflectionEquals(map.get("b2"), result.get(), LENIENT_ORDER);

        result = repository.get("c1", C.class);
        assertReflectionEquals(map.get("c1"), result.get(), LENIENT_ORDER);
        repository.commit();
    }

    /**
     * Test that instance that have references to non-existing instance cannot
     * be created.
     */
    @Test
    public void test_missing_references() {
        repository.beginTransaction();
        LinkedHashMap<String,A> map = defaultReferences();
        try {
            repository.put(map.get("b2"));
            fail("Should violate constraint");
        } catch (ForeignKeyConstraintException e) {
            repository.rollback();
            assertTrue(true);
        }
        assertFalse(repository.get(map.get("b2").getId(), B.class).isPresent());
        repository.commit();
    }

    /**
     * Test that instances that other have references to cannot be deleted.
     */
    // @Test
    public void test_referential_integrity_delete_constraint() {
        repository.beginTransaction();
        LinkedHashMap<String,A> map = defaultReferences();
        for (A a : map.values()) {
            repository.put(a);
        }
        try {
            repository.delete(map.get("a2").getId(), A.class);
            fail("Should not be possible to delete instance that are referenced by others");
        } catch (DeleteConstraintException e) {
            assertTrue(true);
        }
        repository.commit();
    }

    /**
     * Test that an existing delete constraint can be fixed by deleting instances
     * that reference others.
     */
    @Test
    public void test_fixing_delete_constraint() {
        repository.beginTransaction();
        LinkedHashMap<String,A> map = defaultReferences();

        // create instance without references and create
        // instances that reference them afterwards in order
        // to not violate referential integrity
        for (A a : map.values()) {
            repository.put(a);
        }

        A instance = map.get("c1");
        repository.delete(instance.getId(), instance.getClass());
        // make sure to commit! otherwise the following
        // DeleteConstraintException will rollback this delete
        repository.commit();
        try {
            repository.beginTransaction();
            instance = map.get("b1");
            repository.delete(instance.getId(), instance.getClass());
            fail("c2 should have a reference to b1");
        } catch (DeleteConstraintException e) {
            repository.rollback();
        }
        repository.beginTransaction();
        instance = map.get("c2");
        repository.delete(instance.getId(), instance.getClass());

        instance = map.get("b1");
        repository.delete(instance.getId(), instance.getClass());
        // make sure to commit! otherwise the following
        // DeleteConstraintException will rollback this delete
        repository.commit();
        try {
            repository.beginTransaction();
            instance = map.get("a1");
            repository.delete(instance.getId(), instance.getClass());
            fail("b2 should have a reference to a1");
        } catch (DeleteConstraintException e) {
            repository.rollback();
        }
        repository.beginTransaction();
        instance = map.get("b2");
        repository.delete(instance.getId(), instance.getClass());
        instance = map.get("a1");
        repository.delete(instance.getId(), instance.getClass());
        instance = map.get("a2");
        repository.delete(instance.getId(), instance.getClass());
        repository.delete(instance.getId(), instance.getClass());
        instance = map.get("a3");
        repository.delete(instance.getId(), instance.getClass());
        repository.commit();
    }

}
