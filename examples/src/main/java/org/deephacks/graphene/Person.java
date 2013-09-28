package org.deephacks.graphene;

@Entity
public class Person {
    @Id
    private String ssn;

    private Country country;

    @Embedded
    private Address address;

    private String forename;
    private String surname;

    public Person(String ssn, String forename, String surname, Country country,  Address address) {
        this.ssn = ssn;
        this.forename = forename;
        this.surname = surname;
        this.address = address;
        this.country = country;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getForename() {
        return forename;
    }

    public void setForename(String forename) {
        this.forename = forename;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return "Person{" +
                "ssn='" + ssn + '\'' +
                ", country=" + country +
                ", address=" + address +
                ", forename='" + forename + '\'' +
                ", surname='" + surname + '\'' +
                '}';
    }
}
