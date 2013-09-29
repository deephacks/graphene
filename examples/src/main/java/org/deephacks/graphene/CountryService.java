package org.deephacks.graphene;


import org.deephacks.graphene.cdi.Transaction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Transaction
@ApplicationScoped
public class CountryService {

    @Inject
    private EntityRepository repository;

    public void create(Country country) {
        repository.put(country);
    }
}
