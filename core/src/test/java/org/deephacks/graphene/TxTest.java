package org.deephacks.graphene;

import org.deephacks.graphene.internal.UniqueIds;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.deephacks.graphene.EntityRepository.withTx;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class TxTest extends BaseTest {

  /**
   * Test that put can be rolled back from a get.
   */
  @Test
  public void test_put_get_rollback() {
    withTx(tx -> {
      assertFalse(repository.get(buildA("a1"), A.class).isPresent());
      assertTrue(repository.put(buildA("a1")));
      tx.rollback();
    });
    assertFalse(repository.get("a1", A.class).isPresent());
  }

  /**
   * Test that puts can be rolled back.
   */
  @Test
  public void test_put_select_rollback() {
    withTx(tx -> {
      int numInstances = 10;
      // reverse order which instances are inserted to check that sorted order is respected
      for (int i = numInstances; i > 0; i--) {
        repository.put(buildA("a" + i));
        repository.put(buildB("b" + i));
        repository.put(buildC("c" + i));
      }

      assertThat(repository.selectAll(A.class).size(), is(numInstances));
      assertThat(repository.selectAll(B.class).size(), is(numInstances));
      assertThat(repository.selectAll(C.class).size(), is(numInstances));

      tx.rollback();
    });

    withTx(tx -> {
      new UniqueIds().printAllSchemaAndInstances();
      assertThat(repository.selectAll(A.class).size(), is(0));
      assertThat(repository.selectAll(B.class).size(), is(0));
      assertThat(repository.selectAll(C.class).size(), is(0));
    });
  }

  /**
   * Test that puts and deletes can be rolled back.
   */
  @Test
  public void test_put_delete_rollback() {
    withTx(tx -> {
      int numInstances = 10;
      // reverse order which instances are inserted to check that sorted order is respected
      for (int i = numInstances; i > 0; i--) {
        repository.put(buildA("a" + i));
      }
    });
    withTx(tx -> {
      assertTrue(repository.put(buildA("a100")));
      assertTrue(repository.get("a100", A.class).isPresent());
      assertTrue(repository.delete("a1", A.class).isPresent());
      assertFalse(repository.get("a1", A.class).isPresent());
      // undo put (a100) and delete (a1) operation above
      tx.rollback();
    });
    withTx(tx -> {
      assertTrue(repository.get("a1", A.class).isPresent());
      assertFalse(repository.get("a100", A.class).isPresent());
    });
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
      withTx(tx -> {
        repository.delete("a", A.class);
      });
      final A a1 = buildA("a", "a1");
      failure = null;
      Thread t1 = new Thread(() -> withTx(tx -> {
        try {
          repository.put(a1);
          // signal t2 wake up
          queue.put("wakeup");
          // sleep a little while to make sure that there is
          // a chance that t2 calls get before t1 does commit
          Thread.sleep(10);
        } catch (Exception e) {
          tx.rollback();
          failure = e;
          throw new RuntimeException(e);
        } finally {
          latch.countDown();
        }
      }), "Thread 1");
      Thread t2 = new Thread(() -> withTx(tx -> {
        try {
          // wait for a1.put(a)
          queue.take();
          // t2 should be forced to wait until t1 does commit
          assertTrue(repository.get("a", A.class).isPresent());
        } catch (Exception e) {
          tx.rollback();
          failure = e;
          throw new RuntimeException(e);
        } finally {
          latch.countDown();
        }
      }), "Thread 2");
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
    final A instance = buildA("a1");
    final int numRounds = 500;
    final AtomicInteger counter = new AtomicInteger(0);
    final CountDownLatch latch = new CountDownLatch(numRounds);
    for (int i = 0; i < numRounds; i++) {
      executor.execute(() -> withTx(tx -> {
        try {
          repository.put(instance);
          assertEquals(instance, repository.getForUpdate("a1", A.class).get());
          counter.incrementAndGet();
        } catch (Exception e) {
          tx.rollback();
          throw new RuntimeException(e);
        } finally {
          latch.countDown();
        }
      }));
    }
    latch.await();
    assertThat(counter.get(), is(numRounds));
  }
}
