Graphene
========
[![Build Status](https://travis-ci.org/deephacks/graphene.svg?branch=master)](https://travis-ci.org/deephacks/graphene)

Simple and lightweight object persistence framework.

========
#### Entity

A builder class is generated automatically at compile time.

```java
@Entity @VirtualValue
interface User { 
  @Id String getSsn(); 
  String getName(); 
  UserBuilder copy() { return UserBuilder.builderFrom(this); }
}

User user = new UserBuilder().withSsn("12345").withName("James").build();
```

========
#### Put entity

```java
EntityRepository repository = new EntityRepository();
repository.put(user);

```
========
#### Get entity

```java
Optional<User> user = repository.get("12345", User.class);
```
========
#### Update entity

```java
User user = repository.get("12345", User.class).get();
User updated = user.copy().withName("Eric").build();
repository.put(updated);
```
========
#### Delete entity

```java
repository.delete("12345", User.class);
```
========
#### Embedded entities

```java
@Entity @VirtualValue
interface User { 
  @Id String getSsn(); 
  Address getAddress(); 
}

@Embedded @VirtualValue
interface Address { 
  String getStreet(); 
}

```

========
#### Entity references with referential integrity

```java
@Entity @VirtualValue
interface Category { 
  @Id String getName(); 
  List<Item> getItems();
}

@Entity @VirtualValue
interface Item {
  @Id String getId();
  String getDescription();
}

```

========
#### Type-safe cursor queries

```java
List<User> result = repository.stream(User.class)
                              .filter(b -> b.getName().equals("James"))
                              .collect(Collectors.toList());
```

========
#### Query language

```java
List<User> result = repository.query("filter name == 'James' ordered name", User.class);
```

========
#### Composite Id

TBD

========
#### Cursor seek and pagination

TBD

========
#### Ordering

TBD


========
#### Transactions

```java

withTx(tx -> {
  repository.put(user1);
  repository.put(user2);
  
  joinTx(tx -> {
    repository.put(user3);
  });  

  tx.rollback(); // or throw exception
});

```

========
#### Supported basic types

Generified collection types of these is also supported.

- java.lang.String
- java.lang.Character
- java.lang.Byte
- java.lang.Short
- java.lang.Integer
- java.lang.Long
- java.lang.Float
- java.lang.Double
- java.lang.Boolean
- java.math.BigDecimal
- java.math.BigInteger
- java.time.LocalTime
- java.time.LocalDate
- java.time.LocalDateTime
- java.time.ZonedDateTime
- java.time.Instant
- java.time.Period
- java.time.Duration
- Any enum type

========
#### Console

Install the console using the following steps.

```sh

wget http://search.maven.org/remotecontent?filepath=org/deephacks/graphene/graphene-cli/0.3.2/graphene-cli-0.3.2.tar.gz
tar xvf graphene-cli-0.3.2.tar.gz -C /usr/local
ln -fs /usr/local/graphene-cli-0.3.2 /usr/local/graphene
alias graphene="/usr/local/graphene/bin/graphene"
```

Copy jar files containing @Entity classes to '/usr/local/graphene/lib' and start the console.

```sh
$ graphene console
```
Execute queries from within the console.

```sh
$ filter city.name contains 'holm' ordered streetName org.deephacks.graphene.Entities$Street

+----+--------+----------+----------+------------+
|city|location|postalCode|streetName|streetNumber|
+----+--------+----------+----------+------------+
| .. | ..     | ..       | ..       | ..         |
| .. | ..     | ..       | ..       | ..         |
+----+--------+----------+----------+------------+
19 rows in set (0.01 sec)
```
