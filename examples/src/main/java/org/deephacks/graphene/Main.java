package org.deephacks.graphene;

import com.google.common.collect.Lists;

public class Main {
    public static void main(String[] args) {
        EntityRepository repository = new EntityRepository();

        Country country = new Country("Sweden");
        repository.put(country);
        Person p = new Person("111", "Kristoffer", "Sjögren", country, new Address("street", "12345"));
        repository.put(p);
        repository.commit();

        try (ResultSet<Person> result = repository.select(Person.class).retrieve()) {
            System.out.println(Lists.newArrayList(result));
        }

        repository.delete("Sweden", Country.class);
    }
}
