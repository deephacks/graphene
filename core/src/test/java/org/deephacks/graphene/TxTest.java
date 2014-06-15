package org.deephacks.graphene;

import org.deephacks.graphene.Entities.A;
import org.deephacks.graphene.Entities.B;
import org.deephacks.graphene.Entities.C;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.deephacks.graphene.Entities.buildA;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class TxTest extends BaseTest {

   //Test that put can be rolled back from a get.
  @Test
  public void test_put_get_rollback() {
    graphene.withTxWrite(tx -> {
      assertFalse(tx.get("a1", A.class).isPresent());
      assertTrue(tx.put(buildA("a1")));
      tx.rollback();
    });
    assertFalse(graphene.get("a1", A.class).isPresent());
  }

  // Test that puts can be rolled back.
  @Test
  public void test_put_select_rollback() {
    graphene.withTxWrite(tx -> {
      int numInstances = 10;
      // reverse order which instances are inserted to check that sorted order is respected
      for (int i = numInstances; i > 0; i--) {
        tx.put(buildA("a" + i));
        //tx.put(buildB("b" + i));
        //tx.put(buildC("c" + i));
      }

      assertThat(tx.selectAll(A.class).size(), is(numInstances));
      //assertThat(tx.selectAll(B.class).size(), is(numInstances));
      //assertThat(tx.selectAll(C.class).size(), is(numInstances));

      tx.rollback();
    });

    graphene.withTxRead(tx -> {
      assertThat(tx.selectAll(A.class).size(), is(0));
      assertThat(tx.selectAll(B.class).size(), is(0));
      assertThat(tx.selectAll(C.class).size(), is(0));
    });
  }

   // Test that puts and deletes can be rolled back.
  @Test
  public void test_put_delete_rollback() {
    graphene.joinTxWrite(tx -> {
      int numInstances = 10;
      // reverse order which instances are inserted to check that sorted order is respected
      for (int i = numInstances; i > 0; i--) {
        graphene.put(buildA("a" + i));
      }
    });
    graphene.withTxWrite(tx -> {
      assertTrue(tx.put(buildA("a100")));
      assertTrue(tx.get("a100", A.class).isPresent());
      assertTrue(tx.delete("a1", A.class).isPresent());
      assertFalse(tx.get("a1", A.class).isPresent());
      // undo put (a100) and delete (a1) operation above
      tx.rollback();
    });
    graphene.withTxRead(tx -> {
      assertTrue(tx.get("a1", A.class).isPresent());
      assertFalse(tx.get("a100", A.class).isPresent());
    });
  }

   // Test that operations between different threads/transactions are not visible
   // to other concurrent operations.
  @Test
  public void test_tx_write_isolation() throws Exception {
    for (int i = 0; i < 100; i++) {
      final SynchronousQueue<String> queue = new SynchronousQueue<>();
      graphene.withTxWrite(tx -> {
        tx.delete("a", A.class);
      });
      final A a1 = buildA("a", "a1");
      CompletableFuture<Void> f1 = runAsync(() -> graphene.withTxWrite(tx -> {
        try {
          tx.put(a1);
          // signal t2 wake up
          queue.put("wakeup");
          // sleep a little while to make sure that there is
          // a chance that t2 calls get before t1 does commit
          Thread.sleep(10);
          queue.put("wakeup");
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }));

      CompletableFuture<Void> f2 = runAsync(() -> graphene.withTxRead(tx -> {
        try {
          // wait for put(a1)
          queue.take();
          // t2 read tx should not see t1 since we have repeatable read
          assertFalse(tx.get("a", A.class).isPresent());
          queue.take();
          // let t1 commit
          Thread.sleep(10);
          // t2 read tx should not see t1 since we have repeatable read
          assertFalse(tx.get("a", A.class).isPresent());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }));

      allOf(f1, f2).exceptionally(throwable -> {
        throw new RuntimeException(throwable);
      }).get();
      // t1 commit - should see value now
      assertTrue(graphene.get("a", A.class).isPresent());
    }
  }


   // Test that highly concurrent transactions does not break when accessing the same type.
  @Test
  public void test_concurrent_txs_put_and_get_same_entity() throws InterruptedException, ExecutionException {
    final A instance = buildA("a1");
    final int numRounds = 500;
    final AtomicInteger counter = new AtomicInteger(0);
    final CountDownLatch latch = new CountDownLatch(numRounds);

    Runnable runnable = () -> graphene.withTxWrite(tx -> {
      try {
        tx.put(instance);
        assertEquals(instance, tx.get("a1", A.class).get());
        counter.incrementAndGet();
      } finally {
        latch.countDown();
      }
    });

    CompletableFuture<Void> f = runAsync(runnable);
    for (int i = 0; i < numRounds - 1; i++) {
      f = f.thenRunAsync(runnable);
    }
    f.exceptionally(throwable -> { throw new RuntimeException(throwable);} ).get();
    assertThat(counter.get(), is(numRounds));
  }
}
