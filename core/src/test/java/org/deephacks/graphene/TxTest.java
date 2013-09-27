package org.deephacks.graphene;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class TxTest extends BaseTest {

    /**
     * Test that put can be rolled back from a get.
     */
    @Test
    public void test_put_get_rollback() {
        assertFalse(repository.get(defaultValues("a1", A.class), A.class).isPresent());
        assertTrue(repository.put(defaultValues("a1", A.class)));
        repository.rollback();
        assertFalse(repository.get("a1", A.class).isPresent());
    }

    /**
     * Test that puts can be rolled back.
     */
    @Test
    public void test_put_select_rollback() {
        int numInstances = 10;
        // reverse order which instances are inserted to check that sorted order is respected
        for (int i = numInstances; i > 0; i--) {
            repository.put(defaultValues("a" + i, A.class));
            repository.put(defaultValues("b" + i, B.class));
            repository.put(defaultValues("c" + i, C.class));
        }

        try (ResultSet<A> result = repository.select(A.class).retrieve()) {
            assertThat(Lists.newArrayList(result).size(), is(numInstances));
        }
        try (ResultSet<B> result = repository.select(B.class).retrieve()) {
            assertThat(Lists.newArrayList(result).size(), is(numInstances));
        }
        try (ResultSet<C> result = repository.select(C.class).retrieve()) {
            assertThat(Lists.newArrayList(result).size(), is(numInstances));
        }
        repository.rollback();
        try (ResultSet<A> result = repository.select(A.class).retrieve()) {
            assertThat(Lists.newArrayList(result).size(), is(0));
        }
        try (ResultSet<B> result = repository.select(B.class).retrieve()) {
            assertThat(Lists.newArrayList(result).size(), is(0));
        }
        try (ResultSet<C> result = repository.select(C.class).retrieve()) {
            assertThat(Lists.newArrayList(result).size(), is(0));
        }
    }

    /**
     * Test that puts and deletes can be rolled back.
     */
    @Test
    public void test_put_delete_rollback() {
        int numInstances = 10;
        // reverse order which instances are inserted to check that sorted order is respected
        for (int i = numInstances; i > 0; i--) {
            repository.put(defaultValues("a" + i, A.class));
        }
        repository.commit();

        assertTrue(repository.put(defaultValues("a100", A.class)));
        assertTrue(repository.get("a100", A.class).isPresent());
        assertTrue(repository.delete("a1", A.class).isPresent());
        assertFalse(repository.get("a1", A.class).isPresent());

        // undo put (a100) and delete (a1) operation above
        repository.rollback();

        assertTrue(repository.get("a1", A.class).isPresent());
        assertFalse(repository.get("a100", A.class).isPresent());
    }

    /**
     * Test that operations between different threads/transactions are not visible
     * to other concurrent operations.
     */
    @Test
    public void test_tx_isolation() {
        // TODO:
    }
}
