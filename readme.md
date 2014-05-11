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

Generified collections of these types are also supported.

- java.lang.String (UTF-8 encoded)
- java.lang.Character (2 bytes)
- java.lang.Byte (1 byte)
- java.lang.Short (2 bytes)
- java.lang.Integer (4 bytes)
- java.lang.Long (8 bytes)
- java.lang.Float (4 bytes)
- java.lang.Double (8 bytes)
- java.lang.Boolean (1 byte)
- java.math.BigDecimal (UTF-8 encoded)
- java.math.BigInteger (UTF-8 encoded)
- java.time.LocalTime (16 bytes, hour + minute + second + nano)
- java.time.LocalDate (12 bytes, year + month + day)
- java.time.LocalDateTime (28 bytes, LocalDate + LocalTime)
- java.time.ZonedDateTime (32 bytes, LocalDate + LocalTime + Offset)
- java.time.Instant (12 bytes, seconds + nanos)
- java.time.Period (12 bytes, years + months + days)
- java.time.Duration (12 bytes, seconds + nanos)
- Any enum type (UTF-8 encoded)

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
