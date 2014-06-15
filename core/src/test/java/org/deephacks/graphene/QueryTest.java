package org.deephacks.graphene;

import org.deephacks.graphene.BuilderProxy.Builder;
import org.deephacks.graphene.Entities.Person;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.StringContains.containsString;

public class QueryTest extends BaseTest {
  List<Person> persons = new ArrayList<>();

  @Before
  public void before() {
    graphene.withTxWrite(tx -> {
      tx.deleteAll(Person.class);
      int num = 0;
      for (char i = 'a'; i <= 'c'; i++) {
        for (char j = 'a'; j <= 'c'; j++) {
          for (char k = 'a'; k <= 'c'; k++) {
            String name = Character.toString(i) + Character.toString(j) + Character.toString(k);
            String id = String.valueOf(num++);
            Person p = new Builder<>(Person.class)
                    .set(Person::getId, id)
                    .set(Person::getForeName, name)
                    .set(Person::getSureName, name)
                    .build().get();
            tx.put(p);
            persons.add(p);
          }
        }
      }
    });
  }

  @Test
  public void test_eq() {
    List<Person> result = graphene.query("filter id == '0'", Person.class);
    assertThat(result.size(), is(1));
    assertThat(result.get(0), is(persons.get(0)));
  }

  @Test
  public void test_not_eq() {
    List<Person> result = graphene.query("filter foreName != 'aaa' ordered foreName", Person.class);
    assertThat(result.size(), is(persons.size() - 1));
    assertThat(result.get(result.size() - 1), is(persons.get(persons.size() - 1)));
  }

  @Test
  public void test_gt() {
    List<Person> result = graphene.query("filter foreName > 'ccb'", Person.class);
    assertThat(result.size(), is(1));
    assertThat(result.get(result.size() - 1), is(persons.get(persons.size() - 1)));
  }

  @Test
  public void test_gt_eq() {
    List<Person> result = graphene.query("filter foreName >= 'ccb' ordered foreName", Person.class);
    assertThat(result.size(), is(2));
    assertThat(result.get(result.size() - 2), is(persons.get(persons.size() - 2)));
  }

  @Test
  public void test_lt() {
    List<Person> result = graphene.query("filter foreName < 'aab'", Person.class);
    assertThat(result.size(), is(1));
    assertThat(result.get(0), is(persons.get(0)));
  }

  @Test
  public void test_lt_eq() {
    List<Person> result = graphene.query("filter foreName <= 'aab' ordered foreName", Person.class);
    assertThat(result.size(), is(2));
    assertThat(result.get(1), is(persons.get(1)));
  }

  @Test
  public void test_ordering() {
    List<Person> result = graphene.query("limit 1 ordered foreName", Person.class);
    assertThat(result.get(0), is(persons.get(0)));

    result = graphene.query("limit 1 reversed foreName", Person.class);
    assertThat(result.get(0), is(persons.get(persons.size() - 1)));
  }

  @Test
  public void test_limit_skip() {
    List<Person> result = graphene.query("limit 1 skip 1 ordered id", Person.class);
    assertThat(result.get(0), is(persons.get(1)));
  }

  @Test
  public void test_startsWith() {
    List<Person> result = graphene.query("filter foreName startsWith 'a' ordered id", Person.class);
    result.forEach(field -> assertThat(field.getForeName().charAt(0), is('a')));
  }

  @Test
  public void test_endsWith() {
    List<Person> result = graphene.query("filter foreName endsWith 'a' ordered id", Person.class);
    result.forEach(field -> assertThat(field.getForeName().charAt(2), is('a')));
  }

  @Test
  public void test_contains() {
    List<Person> result = graphene.query("filter foreName contains 'a' ordered id", Person.class);
    result.forEach(field -> assertThat(field.getForeName(), containsString("a")));
  }

  @Test
  public void test_regexp() {
    List<Person> result = graphene.query("filter foreName regExp '^.a.$' ordered id", Person.class);
    result.forEach(field -> assertThat(field.getForeName().charAt(1), is('a')));
  }

}
