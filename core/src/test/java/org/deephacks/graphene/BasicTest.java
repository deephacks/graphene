package org.deephacks.graphene;


import com.google.common.base.Optional;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

/**
 * Test serialization of entity instances, including all different types that
 * are supported like primitive and object types, enums, custom and embedded types.
 */
public class BasicTest extends BaseTest {

    /**
     * Test that it is possible to put, get and delete instances of
     * different types.
     */
    @Test
    public void test_basic_put_get_delete() {
        repository.beginTransaction();
        putAndGetAssert(defaultValues("a", A.class));
        putAndGetAssert(defaultValues("b", B.class));
        putAndGetAssert(defaultValues("c", C.class));
        repository.commit();
    }

    /**
     * Test that it is possible to overwrite existing values with
     * other values as well as nullifying them.
     */
    @Test
    public void test_put_overwrite_with_value_and_nulls() {
        repository.beginTransaction();
        A a = defaultValues("a", A.class);
        repository.put(a);

        a.setStringValue("newValue");
        a.setFloatValues(null);
        a.setIntValue(null);
        repository.put(a);

        Optional<A> result = repository.get("a", A.class);
        assertReflectionEquals(result.get(), a, LENIENT_ORDER);
        repository.commit();
    }

    /**
     * Test that it is not possible to overwrite existing values with putNoOverwrite.
     */
    @Test
    public void test_put_noOverwrite() {
        repository.beginTransaction();
        A a = defaultValues("a", A.class);
        repository.putNoOverwrite(a);
        a.setStringValue("newValue");
        a.setFloatValues(null);
        a.setIntValue(null);
        assertFalse(repository.putNoOverwrite(a));
        a = defaultValues("a", A.class);
        Optional<A> result = repository.get("a", A.class);
        assertReflectionEquals(a, result.get(), LENIENT_ORDER);
        repository.commit();
    }

    /**
     * Test that no instance is returned when trying to delete non existing instance.
     */
    @Test
    public void test_delete_non_existin_instances() {
        repository.beginTransaction();
        final Optional<A> a = repository.delete(UUID.randomUUID().toString(), A.class);
        assertFalse(a.isPresent());
        repository.commit();
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
