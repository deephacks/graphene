package org.deephacks.graphene;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.deephacks.graphene.BuilderProxy.Builder;
import org.deephacks.graphene.Entities.G;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class GuavaTest extends BaseTest {

  @Test
  public void test_immutables() {
    ImmutableList<String> list = ImmutableList.<String>builder().add("1").add("2").add("3").build();
    ImmutableSet<String> set = ImmutableSet.<String>builder().add("1").add("2").add("3").build();
    ImmutableMap<String, String> map = ImmutableMap.<String, String>builder().put("1", "2").put("3", "4").put("5", "6").build();

    G g = new Builder<>(G.class)
              .set(G::getKey, "1")
              .set(G::getList, list)
              .set(G::getSet, set)
              .set(G::getMap, map)
              .build().get();
    graphene.put(g);
    G result = graphene.get("1", G.class).get();
    assertThat(result, is(g));
    assertThat(result.hashCode(), is(g.hashCode()));
    assertTrue(result.equals(g));
    assertTrue(g.equals(result));
  }

}
