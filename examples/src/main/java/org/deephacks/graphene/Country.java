package org.deephacks.graphene;

@Entity
public class Country {
    @Id
    private String country;

    public Country(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return "Country{" +
                "country='" + country + '\'' +
                '}';
    }
}
