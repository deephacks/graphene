package org.deephacks.graphene;

import org.jboss.weld.environment.se.Weld;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.util.List;

public class Main {

    @Inject
    private PersonService personService;

    @Inject
    private CountryService countryService;

    public static void main(String[] args) {
        startCdi();
        Main main = CDI.current().select(Main.class).get();
        main.run();
    }

    void run() {
        Country country = new Country("Sweden");
        countryService.create(country);

        Person person = new Person("111", "Kristoffer", "Sjögren", country, new Address("street", "12345"));
        personService.create(person);
        List<Person> persons = personService.selectSurname("Sjögren");
        System.out.println(persons);

    }

    static void startCdi() {
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
}
