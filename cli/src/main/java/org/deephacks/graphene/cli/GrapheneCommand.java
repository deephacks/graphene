package org.deephacks.graphene.cli;


import jline.TerminalFactory;
import jline.console.ConsoleReader;
import org.deephacks.graphene.Graphene;
import org.deephacks.graphene.Schema;
import org.deephacks.graphene.internal.gql.IllegalQueryException;
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
 * graphene gql "filter city.name contains 'holm' ordered streetName, streetNumber" 'org.deephacks.graphene.Entities$Street'
 */
public class GrapheneCommand {

  private static final DecimalFormat format = new DecimalFormat("#.##");
  private static final Graphene graphene;
  private static final ConsoleReader console;
  static {
    graphene = Graphene.builder().build();
    try {
      console = new ConsoleReader();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ShutdownHook.install(new Thread() {
      @Override
      public void run() {
        try {
          TerminalFactory.get().restore();
          graphene.close();
        } catch (Exception e) {
          // ignore
        }
      }
    });
  }

  @CliCmd
  public void query(String query, String className) {
    graphene.withTxRead(tx -> {
      try {
        Class<?> cls = Class.forName(className);
        Schema<?> schema = graphene.getSchema(cls);
        long before = System.currentTimeMillis();
        List<?> result = tx.query(query, schema);
        long took = System.currentTimeMillis() - before;
        TablePrinter tp = new TablePrinter();
        for (String header : getHeaders(cls)) {
          tp.addHeader(Character.toLowerCase(header.charAt(0)) + header.substring(1, header.length()));
        }
        for (Object entity : result) {
          tp.addRow(getValues(entity));
        }
        tp.print();
        System.out.println(result.size() + " rows in set (" + format.format((double) took / 1000) + " sec)");
      } catch (IllegalQueryException e) {
        System.out.println("Illegal gql " + query);
      } catch (ClassNotFoundException e) {
        System.out.println("Class not recognized " + className);
      } catch (Throwable e) {
        e.printStackTrace();
      }
    });
  }

  @CliCmd
  public void console() {
    while (true) {
      try {
        console.setPrompt("$ ");
        String line;
        while ((line = console.readLine()) != null) {
          if ("exit".equalsIgnoreCase(line.trim())) {
            return;
          } else if ("list".equalsIgnoreCase(line.trim())) {
            graphene.listSchema().forEach(System.out::println);
            continue;
          }
          String[] split = line.split(" ");
          String query = Arrays.asList(split).stream().limit(split.length - 1).collect(Collectors.joining(" "));
          query(query, split[split.length - 1]);
        }
      } catch (Exception e) {
        e.printStackTrace();
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
