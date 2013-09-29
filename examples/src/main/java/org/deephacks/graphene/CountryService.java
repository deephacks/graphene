package org.deephacks.graphene;


import org.deephacks.graphene.cdi.Transaction;

import javax.enterprise.context.ApplicationScoped;

@Transaction
@ApplicationScoped
public class CountryService {
    private static final EntityRepository repository = new EntityRepository();

    public void create(Country country) {
        repository.put(country);
    }
}
