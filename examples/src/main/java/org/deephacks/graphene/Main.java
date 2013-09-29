package org.deephacks.graphene;

import org.jboss.weld.environment.se.Weld;

import javax.enterprise.inject.spi.CDI;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        startCdi();
        PersonService personService = CDI.current().select(PersonService.class).get();
        CountryService countryService = CDI.current().select(CountryService.class).get();

        Country country = new Country("Sweden");
        countryService.create(country);

        Person person = new Person("111", "Kristoffer", "Sjögren", country, new Address("street", "12345"));
        personService.create(person);
        List<Person> persons = personService.selectSurname("Sjögren");
        System.out.println(persons);
    }

    public static void startCdi() {
        final Weld weld = new Weld();
        weld.initialize();

        ShutdownHook.install(new Thread("ShutdownHook") {
            @Override
            public void run() {
                try {
                    weld.shutdown();
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            }
        });
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
