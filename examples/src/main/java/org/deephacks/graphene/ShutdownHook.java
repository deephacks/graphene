package org.deephacks.graphene;

class ShutdownHook {

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