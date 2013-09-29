package org.deephacks.graphene.cdi;

import org.jboss.weld.environment.se.Weld;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import javax.enterprise.inject.spi.CDI;

public class CdiRunner extends BlockJUnit4ClassRunner {
    private Class<?> cls;
    private static Weld weld;

    public CdiRunner(final Class<?> cls) throws InitializationError {
        super(cls);
        this.cls = cls;
        if (weld != null) {
            return;
        }
        ShutdownHook.install(new Thread(new Runnable() {
            @Override
            public void run() {
                weld.shutdown();
            }
        }));
        weld = new Weld();
        try {
            weld.initialize();
        } catch (Exception e) {
            throw new InitializationError(e);
        }
    }

    @Override
    protected Object createTest() throws Exception {
        try {
            return CDI.current().select(cls).get();
        } catch (Exception e) {
            throw e;
        }
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


