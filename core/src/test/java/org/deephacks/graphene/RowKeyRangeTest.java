package org.deephacks.graphene;


import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class RowKeyRangeTest {

  @Test
  public void testUnion() {
    RowKeyRange r1 = getKeyRange("C", true, "E", true);
    RowKeyRange r2  = getKeyRange("D", true, "F", true);
    RowKeyRange expected = getKeyRange("C", true, "F", true);

    assertThat(r1.union(r2), is(expected));
    assertThat(r2.union(r1), is(expected));
  }

  @Test
  public void testIntersect() {
    RowKeyRange r1 = getKeyRange("C", true, "E", true);
    RowKeyRange r2 = getKeyRange("D", true, "F", true);
    RowKeyRange expected = getKeyRange("D", true, "E", true);

    assertThat(r1.intersect(r2), is(expected));
    assertThat(r2.intersect(r1), is(expected));
  }

  public static RowKeyRange getKeyRange(String k1, boolean lowerInclusive, String k2, boolean upperInclusive) {
    return RowKeyRange.getKeyRange(toBytes(k1), lowerInclusive, toBytes(k2), upperInclusive);
  }

  public static byte[] toBytes(String s) {
    return s.getBytes();
  }
}
