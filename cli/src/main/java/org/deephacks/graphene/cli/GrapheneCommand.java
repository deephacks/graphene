package org.deephacks.graphene.cli;


import deephacks.streamql.IllegalQueryException;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import org.deephacks.graphene.Entity;
import org.deephacks.graphene.EntityRepository;
import org.deephacks.tools4j.cli.CliCmd;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Example:
 * graphene query "filter city.name contains 'holm' ordered streetName, streetNumber" 'org.deephacks.graphene.Entities$Street'
 */
public class GrapheneCommand {

  private static final DecimalFormat format = new DecimalFormat("#.##");

  @CliCmd
  public void query(String query, String className) {
    try {
      EntityRepository repository = new EntityRepository();
      Class<?> cls = Class.forName(className);
      if (cls.getDeclaredAnnotation(Entity.class) == null) {
        System.out.println(cls.getName() + ": not an @Entity");
        return;
      }
      long before = System.currentTimeMillis();
      List<?> result = repository.query(query, cls);
      long took = System.currentTimeMillis() - before;
      TablePrinter tp = new TablePrinter();
      for (String header : getHeaders(cls)) {
        tp.addHeader(Character.toLowerCase(header.charAt(0)) + header.substring(1, header.length()));
      }
      for (Object entity : result) {
        tp.addRow(getValues(entity));
      }
      tp.print();
      System.out.println(result.size() + " rows in set ("+ format.format((double) took / 1000) +" sec)");
    } catch (IllegalQueryException e) {
      System.out.println("Illegal query " + query);
    } catch (ClassNotFoundException e) {
      System.out.println("Class not recognized " + className);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  @CliCmd
  public void console() {
    while(true) {
      try {
        ConsoleReader console = new ConsoleReader();
        console.setPrompt("$ ");
        String line = null;
        while ((line = console.readLine()) != null) {
          String[] split = line.split(" ");
          String query = Arrays.asList(split).stream().limit(split.length - 1).collect(Collectors.joining(" "));
          query(query, split[split.length - 1]);
        }
      } catch(IOException e) {
        e.printStackTrace();
      } finally {
        try {
          TerminalFactory.get().restore();
        } catch(Exception e) {
          e.printStackTrace();
        }
      }
    }
  }


  private static List<String> getHeaders(Class<?> cls) throws InvocationTargetException, IllegalAccessException {
    return getMethods(cls).stream()
            .map(m -> m.getName().substring(3, m.getName().length()))
            .collect(Collectors.toList());
  }

  private static List<String> getValues(Object object) throws InvocationTargetException, IllegalAccessException {
    List<String> values = new ArrayList<>();
    for (Method m : getMethods(object.getClass())) {
      values.add(String.valueOf(m.invoke(object)));
    }
    return values;
  }

  private static List<Method> getMethods(Class<?> cls) {
    return Arrays.asList(cls.getDeclaredMethods()).stream()
            .sorted(Comparator.comparing(Method::getName))
            .filter(m -> m.getName().startsWith("get") && m.getParameterCount() == 0)
            .collect(Collectors.toList());
  }
}
