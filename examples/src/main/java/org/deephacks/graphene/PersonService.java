package org.deephacks.graphene;

import com.google.common.collect.Lists;
import org.deephacks.graphene.cdi.Transaction;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

import static org.deephacks.graphene.Criteria.containsNoCase;
import static org.deephacks.graphene.Criteria.field;

@Transaction
@ApplicationScoped
public class PersonService {
    private static final EntityRepository repository = new EntityRepository();

    public void create(Person person) {
        repository.put(person);
    }

    public void delete(String ssn) {
        repository.delete(ssn, Person.class);
    }

    public List<Person> selectSurname(String surname) {
        try (ResultSet<Person> result = repository.select(Person.class, field("surname").is(containsNoCase(surname))).retrieve()) {
            return Lists.newArrayList(result);
        }
    }
}
