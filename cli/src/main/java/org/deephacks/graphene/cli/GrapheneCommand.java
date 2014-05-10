package org.deephacks.graphene.cli;


import deephacks.streamql.IllegalQueryException;
import org.deephacks.graphene.EntityRepository;
import org.deephacks.tools4j.cli.CliCmd;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GrapheneCommand {

  @CliCmd
  public void query(String query, String className) {
    try {
      EntityRepository repository = new EntityRepository();
      Class<?> cls = Class.forName(className);
      List<?> result = repository.query(query, cls);
      TablePrinter tp = new TablePrinter();
      for (String header : getHeaders(cls)) {
        tp.addHeader(header, 20);
      }
      for (Object entity : result) {
        tp.addValues(getValues(entity));
      }
      tp.print();
    } catch (IllegalQueryException e) {
      System.out.println("Illegal query " + query);
    } catch (ClassNotFoundException e) {
      System.out.println("Class not recognized " + className);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private static List<String> getHeaders(Class<?> cls) throws InvocationTargetException, IllegalAccessException {
    List<String> values = new ArrayList<>();
    for (Method m : cls.getDeclaredMethods()) {
      String name = m.getName();
      if (name.startsWith("get") && m.getParameterCount() == 0) {
        name = name.substring(3, name.length());
        values.add(name);
      }
    }
    return values;
  }

  private static List<String> getValues(Object object) throws InvocationTargetException, IllegalAccessException {
    List<String> values = new ArrayList<>();
    for (Method m : object.getClass().getDeclaredMethods()) {
      if (m.getName().startsWith("get") && m.getParameterCount() == 0) {
        values.add(String.valueOf(m.invoke(object)) );
      }
    }
    return values;
  }
}
