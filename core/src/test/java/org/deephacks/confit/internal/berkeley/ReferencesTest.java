package org.deephacks.confit.internal.berkeley;

import com.google.common.base.Optional;
import org.deephacks.confit.internal.berkeley.TestData.A;
import org.deephacks.confit.internal.berkeley.TestData.B;
import org.deephacks.confit.internal.berkeley.TestData.C;
import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.internal.UniqueIds;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

public class ReferencesTest {
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
     * Test that references (single and collection) are fetched eagerly
     * through multiple levels.
     */
    @Test
    public void test_references_with_multiple_levels() {
        LinkedHashMap<String,A> map = TestData.defaultReferences();
        for (A a : map.values()) {
            repository.put(a);
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
    }

    /**
     * Test that instances that other have references to cannot be deleted.
     */
    @Test
    public void test_referential_integrity() {
    }
}
