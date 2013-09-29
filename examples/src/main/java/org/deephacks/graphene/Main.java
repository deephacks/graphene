package org.deephacks.graphene;

import org.jboss.weld.environment.se.Weld;

import javax.enterprise.inject.spi.CDI;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Weld weld = new Weld();
        weld.initialize();

        PersonService personService = CDI.current().select(PersonService.class).get();
        CountryService countryService = CDI.current().select(CountryService.class).get();

        Country country = new Country("Sweden");
        countryService.create(country);

        Person person = new Person("111", "Kristoffer", "Sjögren", country, new Address("street", "12345"));
        personService.create(person);
        List<Person> persons = personService.selectSurname("Sjögren");
        System.out.println(persons);
    }
}
