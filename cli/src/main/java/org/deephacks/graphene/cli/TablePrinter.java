package org.deephacks.graphene.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class TablePrinter {
  private List<String> headers = new ArrayList<>();
  private List<List<String>> rows = new ArrayList<>();
  private List<Integer> widths = new ArrayList<>();

  public void addHeader(String name) {
    headers.add(name);
  }

  public void addRow(List<String> values) {
    rows.add(values);
  }

  public void print() {
    for (int i = 0; i < headers.size(); i++) {
      Integer width = headers.get(i).length();
      for (int j = 0; j < rows.size(); j++) {
        String value = rows.get(j).get(i);
        width = value.length() > width ? value.length() : width;
      }
      widths.add(width);
    }
    String rowSeparator = createRowSeparator();
    String rowForHeaders = rowForValues(headers);
    PrintStream printStream = System.out;
    printStream.println(rowSeparator);
    printStream.println(rowForHeaders);
    printStream.println(rowSeparator);

    for (List<?> row : rows) {
      String rowText = rowForValues(row);
      printStream.println(rowText);
    }
    printStream.println(rowSeparator);
  }

  private String rowForValues(List<?> values) {
    StringBuilder sb = new StringBuilder();
    sb.append("|");

    for (int index = 0; index < values.size(); index++) {
      Object value = values.get(index);
      String output = value.toString();
      String padded = StringUtils.fix(output, widths.get(index));
      sb.append(padded);
      sb.append("|");
    }
    return sb.toString();
  }

  private String createRowSeparator() {
    String[] segments = new String[headers.size()];
    for (int index = 0; index < headers.size(); index++) {
      segments[index] = StringUtils.repeat("-", widths.get(index));
    }
    return StringUtils.join(segments, "+", "+", "+");
  }

  private static class StringUtils {

    public static String repeat(String character, int times) {
      StringBuilder sb = new StringBuilder(times);
      for (int i = 0; i < times; i++) {
        sb.append(character);
      }
      return sb.toString();
    }

    public static String pad(String input, int length) {
      if (input.length() > length) throw new IllegalArgumentException("Input string is longer than length.");
      return input + repeat(" ", length - input.length());
    }

    public static String fix(String input, int length) {
      if (input.length() < length) {
        return pad(input, length);
      } else if (input.length() > length) {
        return input.substring(0, length);
      }
      return input;
    }

    public static String join(String[] strings, String inside, String start, String end) {
      return start + join(strings, inside) + end;
    }

    public static String join(String[] strings, String delimiter) {
      if (strings.length == 1) return strings[0];
      StringBuilder sb = new StringBuilder();

      for (int index = 0; index < strings.length - 1; index++) {
        sb.append(strings[index]);
        sb.append(delimiter);
      }
      sb.append(strings[strings.length - 1]);
      return sb.toString();
    }
  }
}
