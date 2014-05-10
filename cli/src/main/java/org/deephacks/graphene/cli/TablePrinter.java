package org.deephacks.graphene.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class TablePrinter {
  private List<Header> headers = new ArrayList<>();
  private List<List<String>> values = new ArrayList<>();

  public void addHeader(String name, int width) {
    headers.add(new Header(name, width));
  }
  public void addValues(List<String> values) {
    this.values.add(values);
  }

  public void print() {
    String rowSeparator = createRowSeparator();
    String rowForHeaders = rowForValues(headers);
    PrintStream printStream = System.out;
    printStream.println(rowSeparator);
    printStream.println(rowForHeaders);
    printStream.println(rowSeparator);

    for (List<?> row : values) {
      String rowText = rowForValues(row);
      printStream.println(rowText);
    }
    printStream.println(rowSeparator);
  }

  private String rowForValues(List<?> values) {
    StringBuilder sb = new StringBuilder();
    sb.append("|");

    for (int index = 0; index < headers.size(); index++) {
      Object value = values.get(index);
      int length = headers.get(index).getWidth();
      String output = value.toString();
      if (value instanceof Header) {
        output = ((Header) value).getName();
      }
      String padded = StringUtils.fix(output, length);
      sb.append(padded);
      sb.append("|");
    }
    return sb.toString();
  }

  private String createRowSeparator() {
    List<Integer> lengths = getLengths();
    String[] segments = new String[lengths.size()];
    for (int index = 0; index < lengths.size(); index++) {
      segments[index] = StringUtils.repeat("-", lengths.get(index));
    }
    return StringUtils.join(segments, "+", "+", "+");
  }

  private List<Integer> getLengths() {
    List<Integer> lengths = new ArrayList<>();
    for (Header header : headers) {
      lengths.add(header.getWidth());
    }
    return lengths;
  }

  public static class Header {
    private String name;
    private int width;

    public Header(String name, int width) {
      this.name = name;
      this.width = width;
    }

    public String getName() {
      return name;
    }

    public int getWidth() {
      return width;
    }
  }

  private static class StringUtils {

    public static String repeat(char character, int times) {
      return repeat(String.valueOf(character), times);
    }

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

    public static String padAndAdd(String input, int length, String sides) {
      return sides + pad(input, length) + sides;
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

    public static String repeatAndJoin(String text, int width, String connector, int times) {
      String connectee = repeat(text, width);
      String[] strings = new String[times];
      for (int index = 0; index < times; index++) {
        strings[index] = connectee;
      }
      return join(strings, connector);
    }
  }
}
