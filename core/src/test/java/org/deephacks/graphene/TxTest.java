package org.deephacks.graphene;

import com.google.common.collect.Lists;
import org.deephacks.graphene.internal.UniqueIds;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

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
        new UniqueIds().printAllSchemaAndInstances();
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

    static Exception failure;

    /**
     * Test that operations between different threads/transactions are not visible
     * to other concurrent operations.
     */
    @Test
    public void test_tx_write_isolation() throws Exception {
        for (int i = 0; i < 100; i++) {
            final CountDownLatch latch = new CountDownLatch(2);
            final SynchronousQueue<String> queue = new SynchronousQueue<>();
            repository.delete("a", A.class);
            repository.commit();
            final A a1 = defaultValues("a", A.class);
            a1.setStringValue("a1");
            failure = null;
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        repository.put(a1);
                        // signal t2 wake up
                        queue.put("wakeup");
                        // sleep a little while to make sure that there is
                        // a chance that t2 calls get before t1 does commit
                        Thread.sleep(10);
                        repository.commit();
                    } catch (Exception e) {
                        failure = e;
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            }, "Thread 1");
            Thread t2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // wait for a1.put(a)
                        queue.take();
                        // t2 should be forced to wait until t1 does commit
                        assertTrue(repository.get("a", A.class).isPresent());
                        repository.commit();
                    } catch (Exception e) {
                        failure = e;
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            }, "Thread 2");
            t1.start();
            t2.start();
            latch.await();
            if (failure != null) {
                throw failure;
            }
        }
    }

    /**
     * Check that two different read transactions does not compete for same lock.
     */
    @Test
    public void test_tx_read_concurrency() throws Exception {
        for (int i = 0; i < 2; i++) {
            final CountDownLatch latch = new CountDownLatch(2);
            final SynchronousQueue<String> queue = new SynchronousQueue<>();
            repository.put(defaultValues("a", A.class));
            repository.commit();
            failure = null;
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        repository.get("a", A.class);
                        queue.put("wake up");
                        // if there was a lock conflict t2 would get a
                        // timeout long before this sleep returns.
                        Thread.sleep(1000);
                        repository.commit();
                    } catch (Exception e) {
                        failure = e;
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            }, "Thread 1");
            Thread t2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        queue.take();
                        repository.get("a", A.class);
                        repository.commit();
                    } catch (Exception e) {
                        failure = e;
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            }, "Thread 2");
            t1.start();
            t2.start();
            latch.await();
            if (failure != null) {
                throw failure;
            }
        }
    }


    /**
     * Test that highly concurrent transactions does not break when accessing the same entity.
     */
    @Test
    public void test_concurrent_txs_put_and_get_same_entity() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
        final A instance = defaultValues("a1", A.class);
        final int numRounds = 500;
        final AtomicInteger counter = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(numRounds);
        for (int i = 0; i < numRounds; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        repository.put(instance);
                        repository.commit();
                        assertReflectionEquals(instance, repository.get("a1", A.class).get(), LENIENT_ORDER);
                        repository.commit();
                        counter.incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        latch.await();
        assertThat(counter.get(), is(numRounds));
    }
}
