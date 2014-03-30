package org.deephacks.graphene;

import java.util.ArrayList;
import java.util.Collections;

public class Guavas {
  public static <T> T checkNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }
  public static void checkArgument(boolean expression, String msg) {
    if (!expression) {
      throw new IllegalArgumentException(msg);
    }
  }
  public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
    checkNotNull(elements);
    ArrayList<E> list = new ArrayList<>();
    elements.forEach(list::add);
    return list;
  }
  public static <E> ArrayList<E> newArrayList(E... elements) {
    checkNotNull(elements);
    ArrayList<E> list = new ArrayList<>();
    Collections.addAll(list, elements);
    return list;
  }

  public static boolean isNullOrEmpty(String string) {
    return string == null || string.length() == 0;
  }
}
