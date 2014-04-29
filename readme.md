Graphene
========
[![Build Status](https://travis-ci.org/deephacks/graphene.svg?branch=master)](https://travis-ci.org/deephacks/graphene)

Simple and lightweight object persistence framework.

========


#### A builder class is generated automatically at compile time.
```java
@Entity @VirtualValue
interface User { @Id String getSsn(); String getName(); }

User user = new UserBuilder().withSsn("12345").withName("James").build();
```

#### Put entity

```java
EntityRepository repository = new EntityRepository();
repository.put(user);
```
========

#### Delete entity

```java
repository.delete(user);
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

