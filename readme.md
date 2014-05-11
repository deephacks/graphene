Graphene
========
[![Build Status](https://travis-ci.org/deephacks/graphene.svg?branch=master)](https://travis-ci.org/deephacks/graphene)

Simple and lightweight object persistence framework.

========
#### Define entities

Create an interface and annotate it with @Entity and @VirtualValue. All non-void, parameterless, getter methods on this interface will be treated as properties, each having same type as the return type of the method. A builder class is generated automatically at compile time. 

See https://github.com/deephacks/vals for more information.

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
#### Default values

Default getter methods returning values are treated as a default values. They are used as fallback values, merged with the entity if no values already exist.

```java
@Entity @VirtualValue
interface Account { 
  @Id String getId(); 
  default Long getAmount() { return 0; }
}
```

========
#### Put entity

```java
EntityRepository repository = new EntityRepository();
repository.put(user);
```

========
#### Put without overwrite

Put an entity if it does not exist. If the entity exist, nothing will be written.

```java
EntityRepository repository = new EntityRepository();
repository.putNoOverwrite(user);
```

========
#### Get entity

A specific an entity is fetched by providing its @Id.

```java
Optional<User> user = repository.get("12345", User.class);
```
========
#### Update entity

Entities are updated by replacing the existing entity entirely with the provided entity.

```java
User user = repository.get("12345", User.class).get();
User updated = user.copy().withName("Eric").build();
repository.put(updated);
```
========
#### Delete entity

Deleting an entity does not cascade with regards to references that entities may have. 

```java
repository.delete("12345", User.class);
```

========
#### Validation

TBD


========
#### Embedded entities

Embedded entities does not have identity and are stored as values as part of other entities.

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

References are allowed to be circular and can be single valued, a List or a Map with a string key (the id).
Referential checks are made to make sure that entities exist, or throw an exception otherwise. 
The same is true when trying to delete entities already referenced by others.

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
#### Durability

TDB

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
