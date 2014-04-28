package org.deephacks.graphene;

import org.deephacks.graphene.internal.UniqueIds;
import org.deephacks.vals.VirtualValue;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.deephacks.graphene.TransactionManager.withTx;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

public class QueryTest extends BaseTest {
  List<Person> persons = new ArrayList<>();

  @Before
  public void before() {
    withTx(tx -> {
      UniqueIds ids = new UniqueIds();
      repository.deleteAll(Person.class);
      ids.deleteAll();
    });
    int num = 0;
    for (char i = 'a'; i <= 'c'; i++) {
      for (char j = 'a'; j <= 'c'; j++) {
        for (char k = 'a'; k <= 'c'; k++) {
          String name = Character.toString(i) + Character.toString(j) + Character.toString(k);
          String id = String.valueOf(num++);
          Person p = new PersonBuilder()
                  .withId(id)
                  .withForeName(name)
                  .withSureName(name)
                  .build();
          persons.add(p);
        }
      }
    }
    ArrayList<Person> list = new ArrayList<>(persons);
    Collections.shuffle(list);
    list.forEach(repository::put);
  }

  @Test
  public void test_eq() {
    List<Person> result = repository.query("filter id == '0'", Person.class);
    assertThat(result.size(), is(1));
    assertThat(result.get(0), is(persons.get(0)));
  }

  @Test
  public void test_not_eq() {
    List<Person> result = repository.query("filter foreName != 'aaa' ordered foreName", Person.class);
    assertThat(result.size(), is(persons.size() - 1));
    assertThat(result.get(result.size() - 1), is(persons.get(persons.size() - 1)));
  }

  @Test
  public void test_gt() {
    List<Person> result = repository.query("filter foreName > 'ccb'", Person.class);
    assertThat(result.size(), is(1));
    assertThat(result.get(result.size() - 1), is(persons.get(persons.size() - 1)));
  }

  @Test
  public void test_gt_eq() {
    List<Person> result = repository.query("filter foreName >= 'ccb' ordered foreName", Person.class);
    assertThat(result.size(), is(2));
    assertThat(result.get(result.size() - 2), is(persons.get(persons.size() - 2)));
  }

  @Test
  public void test_lt() {
    List<Person> result = repository.query("filter foreName < 'aab'", Person.class);
    assertThat(result.size(), is(1));
    assertThat(result.get(0), is(persons.get(0)));
  }

  @Test
  public void test_lt_eq() {
    List<Person> result = repository.query("filter foreName <= 'aab' ordered foreName", Person.class);
    assertThat(result.size(), is(2));
    assertThat(result.get(1), is(persons.get(1)));
  }

  @Test
  public void test_ordering() {
    List<Person> result = repository.query("limit 1 ordered foreName", Person.class);
    assertThat(result.get(0), is(persons.get(0)));

    result = repository.query("limit 1 reversed foreName", Person.class);
    assertThat(result.get(0), is(persons.get(persons.size() - 1)));
  }

  @Test
  public void test_limit_skip() {
    List<Person> result = repository.query("limit 1 skip 1 ordered id", Person.class);
    assertThat(result.get(0), is(persons.get(1)));
  }

  @Test
  public void test_startsWith() {
    List<Person> result = repository.query("filter foreName startsWith 'a' ordered id", Person.class);
    result.forEach(p -> assertThat(p.getForeName().charAt(0), is('a')));
  }

  @Test
  public void test_endsWith() {
    List<Person> result = repository.query("filter foreName endsWith 'a' ordered id", Person.class);
    result.forEach(p -> assertThat(p.getForeName().charAt(2), is('a')));
  }

  @Test
  public void test_contains() {
    List<Person> result = repository.query("filter foreName contains 'a' ordered id", Person.class);
    result.forEach(p -> assertThat(p.getForeName(), containsString("a")));
  }

  @Test
  public void test_regexp() {
    List<Person> result = repository.query("filter foreName regExp '^.a.$' ordered id", Person.class);
    result.forEach(p -> assertThat(p.getForeName().charAt(1), is('a')));
  }


  @VirtualValue
  public static interface Person {
    @Id
    String getId();

    String getForeName();

    String getSureName();

  }
}
