package org.deephacks.graphene;


import org.deephacks.graphene.Entities.A;
import org.deephacks.graphene.Entities.B;
import org.deephacks.graphene.Entities.C;
import org.deephacks.graphene.otherpackage.OtherPackageValue;
import org.junit.Before;

public class BaseTest {
  static {
    CompilerUtils.compile(OtherPackageValue.class, TopEntity.class, Entities.class);
  }

  protected static final Graphene graphene = Graphene.builder().build();

  static {
    ShutdownHook.install(new Thread("ShutdownHook") {
      @Override
      public void run() {
        try {
          graphene.close();
        } catch (Exception e) {
          throw new RuntimeException();
        }
      }
    });
  }

  @Before
  public void before() {
    graphene.deleteAll(C.class);
    graphene.deleteAll(B.class);
    graphene.deleteAll(A.class);
  }

  static class ShutdownHook {

    static void install(final Thread threadToJoin) {
      Thread thread = new ShutdownHookThread(threadToJoin);
      Runtime.getRuntime().addShutdownHook(thread);
    }

    private static class ShutdownHookThread extends Thread {
      private final Thread threadToJoin;

      private ShutdownHookThread(final Thread threadToJoin) {
        super("ShutdownHook: " + threadToJoin.getName());
        this.threadToJoin = threadToJoin;
      }

      @Override
      public void run() {
        shutdown(threadToJoin, 30000);
      }
    }

    public static void shutdown(final Thread t, final long joinwait) {
      if (t == null)
        return;
      t.start();
      while (t.isAlive()) {
        try {
          t.join(joinwait);
        } catch (InterruptedException e) {
        }
      }
    }
  }
}
