package org.deephacks.graphene;

import org.deephacks.graphene.BuilderProxy.Builder;
import org.deephacks.graphene.Entities.ByteArrayKey;
import org.deephacks.graphene.Entities.ObjectKey;
import org.deephacks.graphene.Entities.ObjectKeyEntity;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class PrimaryKeyTest extends BaseTest {

  @Before
  public void before() {
    graphene.deleteAll(ObjectKeyEntity.class);
  }

  // the entity specify a key that is exactly 3 bytes wide.
  // if the key stored key would be shorter, null bytes would be
  // appended to end of key (when fetched) and the test would fail!
  @Test
  public void test_byte_array_key_and_without_values() {
    ByteArrayKey object = new Builder<>(ByteArrayKey.class)
            .set(ByteArrayKey::getKey, new byte[]{1, 2, 3}).build().get();
    graphene.put(object);
    ByteArrayKey result = graphene.get(new byte[]{1, 2, 3}, ByteArrayKey.class).get();
    assertThat(result, is(object));
  }


  @Test
  public void test_get_object_key_without_values() {
    ObjectKeyEntity object = create("1", "2014-01-01T10:00");
    graphene.put(object);

    ObjectKeyEntity result = graphene.get(object.getKey(), ObjectKeyEntity.class).get();
    assertThat(result, is(object));
  }

  @Test
  public void test_list_object_key_in_sorted_order() {
    List<ObjectKeyEntity> list = new ArrayList<>();
    list.add(create("2", "2015-01-01T10:00"));
    list.add(create("2", "2014-01-01T10:00"));
    list.add(create("1", "2016-01-01T10:00"));
    graphene.putAll(list);

    List<ObjectKeyEntity> result = graphene.list(ObjectKeyEntity.class);
    assertThat(result.get(0), is(list.get(2)));
    assertThat(result.get(1), is(list.get(1)));
    assertThat(result.get(2), is(list.get(0)));
  }


  private ObjectKeyEntity create(String partition, String time) {
    LocalDateTime localDateTime = LocalDateTime.parse(time);
    ObjectKey key = new Builder<>(ObjectKey.class)
            .set(ObjectKey::getPartition, partition)
            .set(ObjectKey::getTime, localDateTime)
            .build().get();

    ObjectKeyEntity object = new Builder<>(ObjectKeyEntity.class)
            .set(ObjectKeyEntity::getKey, key).build().get();
    return object;
  }

}
