Graphene
========
[![Build Status](https://travis-ci.org/deephacks/graphene.svg?branch=master)](https://travis-ci.org/deephacks/graphene)

Simple and lightweight object persistence framework.

========


#### A builder class is generated automatically at compile time.
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
interface User { 
  @Id String getSsn(); 
  Address getAddress(); 
}

@Entity @VirtualValue
interface Address {
  @Id
  String getStreet(); 
}

```

========


#### Type-safe query
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

#### Console
```sh
$ graphene console
$ filter city.name contains 'holm' ordered streetName org.deephacks.graphene.Entities$Street

+----+--------+----------+----------+------------+
|city|location|postalCode|streetName|streetNumber|
+----+--------+----------+----------+------------+
| .. | ..     | ..       | ..       | ..         |
| .. | ..     | ..       | ..       | ..         |
+----+--------+----------+----------+------------+
19 rows in set (0.01 sec)
```
