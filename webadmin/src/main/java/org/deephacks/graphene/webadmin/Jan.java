package org.deephacks.graphene.webadmin;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Indexer;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by stoffe on 5/10/14.
 */
public class Jan {
  public static void main(String[] args) throws IOException {
    Indexer indexer = new Indexer();
    ClassInfo index = indexer.index(new FileInputStream("target/classes/org/deephacks/graphene/webadmin/E.class"));
    System.out.println(index);

  }
}
