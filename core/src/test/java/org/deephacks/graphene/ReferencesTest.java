package org.deephacks.graphene;

import com.google.common.base.Optional;
import com.sleepycat.je.ForeignConstraintException;
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
        for (A a : map.values()) {
            repository.put(a);
            repository.commit();
        }

        Optional result = repository.get("b1", B.class);
        assertReflectionEquals(map.get("b1"), result.get(), LENIENT_ORDER);

        result = repository.get("b2", B.class);
        assertReflectionEquals(map.get("b2"), result.get(), LENIENT_ORDER);

        result = repository.get("c1", C.class);
        assertReflectionEquals(map.get("c1"), result.get(), LENIENT_ORDER);
    }

    /**
     * Test that instance that have references to non-existing instance cannot
     * be created.
     */
    @Test
    public void test_missing_references() {
        LinkedHashMap<String,A> map = defaultReferences();
        try {
            repository.put(map.get("b2"));
            fail("Should violate constraint");
        } catch (ForeignConstraintException e) {
            repository.rollback();
            assertTrue(true);
        }
        assertFalse(repository.get(map.get("b2").getId(), B.class).isPresent());
    }

    /**
     * Test that instances that other have references to cannot be deleted.
     */
    @Test
    public void test_referential_integrity_delete_constraint() {
        LinkedHashMap<String,A> map = defaultReferences();
        for (A a : map.values()) {
            repository.put(a);
            repository.commit();
        }
        try {
            repository.delete(map.get("a2").getId(), A.class);
            fail("Should not be possible to delete instance that are referenced by others");
        } catch (DeleteConstraintException e) {
            assertTrue(true);
        }
    }

    /**
     * Test that an existing delete constraint can be fixed by deleting instances
     * that reference others.
     */
    //@Test
    public void test_fixing_delete_constraint() {
        LinkedHashMap<String,A> map = defaultReferences();

        // create instance without references and create
        // instances that reference them afterwards in order
        // to not violate referential integrity
        for (A a : map.values()) {
            repository.put(a);
            repository.commit();
        }

        // now we delete instances in the reverse order in order
        // to not violate delete constraint.

        A instance = map.get("c1");
        repository.delete(instance.getId(), instance.getClass());

        instance = map.get("b1");
        try {
            repository.delete(instance.getId(), instance.getClass());
            fail("c2 should still have references to b1");
        } catch (DeleteConstraintException e) {

        }


    }

}
