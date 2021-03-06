/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.graphene.internal.serialization;

import sun.misc.Unsafe;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BytesUtils {
  public static final Charset UTF_8 = Charset.forName("UTF-8");
  public static final int LONG_BYTES = Long.SIZE / Byte.SIZE;

  /**
   * Adds a big-endian 4-byte integer to a sorted array of bytes.
   *
   * @param arr This byte array is assumed to be sorted array
   *            of signed ints.
   * @param n   value to add.
   * @return new array with the added value.
   */
  public static byte[] add(byte[] arr, int n) {
    int index = binarySearch(arr, n);
    byte[] arr2 = new byte[arr.length + 4];
    System.arraycopy(arr, 0, arr2, 0, index);
    System.arraycopy(arr, index, arr2, index + 4, arr.length - index);
    Bytes.setInt(arr2, n, index);
    return arr2;
  }

  /**
   * Remove a big-endian 4-byte integer to a sorted array of bytes.
   *
   * @param arr This byte array is assumed to be sorted array
   *            of signed ints.
   * @param n   value to remove.
   * @return new array with the added value.
   */
  public static byte[] remove(byte[] arr, int n) {
    int index = binarySearch(arr, n);
    byte[] arr2 = new byte[arr.length - 4];
    System.arraycopy(arr, 0, arr2, 0, index);
    System.arraycopy(arr, index + 4, arr2, index, arr.length - index - 4);
    return arr2;
  }

  /**
   * Search for a big-endian 4-byte integer in a array of bytes.
   *
   * @param a   array of containing only big-endian 4-byte integers.
   * @param key the value to seach for.
   * @return the index found.
   */
  public static int binarySearch(byte[] a, int key) {
    int low = 0;
    int high = a.length;

    while (low < high) {
      int mid = (low + high) >>> 1;
      if (mid % 4 != 0) {
        if (high == a.length) {
          mid = low;
        } else {
          mid = high;
        }
      }
      int midVal = Bytes.getInt(a, mid);

      if (midVal < key)
        low = mid + 4;
      else if (midVal > key)
        high = mid - 4;
      else
        return mid; // key found
    }
    if (low == a.length) {
      return low;
    }
    return key > Bytes.getInt(a, low) ? low + 4 : low;

  }

  public static void write(DataOutput out, Object value) {
    try {
      if (value instanceof Byte) {
        out.write(DataType.BYTE.getId());
        out.write((byte) value);
      } else if (value instanceof Short) {
        out.write(DataType.SHORT.getId());
        out.writeShort((short) value);
      } else if (value instanceof Integer) {
        out.write(DataType.INTEGER.getId());
        out.writeInt((int) value);
      } else if (value instanceof Long) {
        out.write(DataType.LONG.getId());
        out.writeLong((long) value);
      } else if (value instanceof Float) {
        out.write(DataType.FLOAT.getId());
        out.writeFloat((float) value);
      } else if (value instanceof Double) {
        out.write(DataType.DOUBLE.getId());
        out.writeDouble((double) value);
      } else if (value instanceof Boolean) {
        out.write(DataType.BOOLEAN.getId());
        out.writeBoolean((boolean) value);
      } else if (value instanceof String) {
        out.write(DataType.STRING.getId());
        out.writeUTF((String) value);
      } else if (value instanceof Character) {
        out.write(DataType.CHAR.getId());
        out.writeChar((char) value);
      } else {
        throw new UnsupportedOperationException("Did not recognize type " + value.getClass());
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static Object read(DataInput in) {
    try {
      DataType type = DataType.getDataType(in.readByte());
      switch (type) {
        case BYTE:
          return in.readByte();
        case SHORT:
          return in.readShort();
        case INTEGER:
          return in.readInt();
        case LONG:
          return in.readLong();
        case FLOAT:
          return in.readFloat();
        case DOUBLE:
          return in.readDouble();
        case BOOLEAN:
          return in.readBoolean();
        case STRING:
          return in.readUTF();
        case CHAR:
          return in.readChar();
        case BYTE_LIST:
          throw new UnsupportedOperationException("Did not recognize type " + type);
        case SHORT_LIST:
          throw new UnsupportedOperationException("Did not recognize type " + type);
        case INTEGER_LIST:
          throw new UnsupportedOperationException("Did not recognize type " + type);
        case LONG_LIST:
          throw new UnsupportedOperationException("Did not recognize type " + type);
        case FLOAT_LIST:
          throw new UnsupportedOperationException("Did not recognize type " + type);
        case DOUBLE_LIST:
          throw new UnsupportedOperationException("Did not recognize type " + type);
        case BOOLEAN_LIST:
          throw new UnsupportedOperationException("Did not recognize type " + type);
        case STRING_LIST:
          throw new UnsupportedOperationException("Did not recognize type " + type);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    throw new UnsupportedOperationException("Did not recognize type");
  }

  public static byte toByte(boolean value) {
    return value ? (byte) 1 : (byte) 0;
  }

  public static byte[] toBytes(long[] values) {
    ByteBuffer buf = allocate(values.length * 8);
    LongBuffer longBuffer = buf.asLongBuffer();
    longBuffer.put(values);
    return buf.array();
  }

  public static byte[] toBytes(int[] values) {
    byte[][] bytes = new byte[values.length][];
    int size = 0;
    for (int i = 0; i < values.length; i++) {
      bytes[i] = VarInt32.write(values[i]);
      size += bytes[i].length;
    }
    return write(bytes, size);
  }

  public static byte[] toBytes(short[] values) {
    byte[][] bytes = new byte[values.length][];
    int size = 0;
    for (int i = 0; i < values.length; i++) {
      bytes[i] = VarInt32.write(values[i]);
      size += bytes[i].length;
    }
    return write(bytes, size);
  }

  private static byte[] write(byte[][] bytes, int size) {
    ByteBuffer buf = allocate(size);
    for (byte[] b : bytes) {
      buf.put(b);
    }
    return buf.array();
  }


  public static byte[] toBytes(byte[] values) {
    ByteBuffer buf = allocate(values.length);
    buf.put(values);
    return buf.array();
  }

  public static byte[] toBytes(boolean[] values) {
    byte[] bytes = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      bytes[i] = toByte(values[i]);
    }
    return bytes;
  }

  public static byte[] toBytes(float[] values) {
    ByteBuffer buf = allocate(values.length * 4);
    buf.asFloatBuffer().put(values);
    return buf.array();
  }

  public static byte[] toBytes(double[] values) {
    ByteBuffer buf = allocate(values.length * 8);
    buf.asDoubleBuffer().put(values);
    return buf.array();
  }

  public static byte[] toBytes(String[] strings) {
    List<byte[]> stringBytes = new ArrayList<>();
    int size = 0;
    for (String str : strings) {
      byte[] bytes = str.getBytes(UTF_8);
      byte[] bytesLength = VarInt32.write(bytes.length);
      ByteBuffer buffer = allocate(bytes.length + bytesLength.length);
      buffer.put(bytesLength);
      buffer.put(bytes);
      stringBytes.add(buffer.array());
      size += bytesLength.length + bytes.length;
    }
    ByteBuffer buffer = allocate(size + 4);

    buffer.put(VarInt32.write(strings.length));
    for (byte[] bytes : stringBytes) {
      buffer.put(bytes);
    }
    return buffer.array();
  }

  public static boolean[] toBooleans(byte[] value, int offset, int num) {
    boolean[] values = new boolean[num];
    int idx = 0;
    for (int i = offset; i < num + offset; i++) {
      values[idx++] = getBoolean(value[i]);
    }
    return values;
  }

  public static boolean[] toBooleans(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    return toBooleans(value, offset, length);
  }

  public static List<Boolean> toBooleanList(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    boolean[] values = toBooleans(value, offset, length);
    ArrayList<Boolean> list = new ArrayList<>();
    for (Boolean v : values) {
      list.add(v);
    }
    return list;
  }

  public static long[] toLongs(byte[] value, int offset, int num) {
    long[] values = new long[num];
    int idx = 0;
    for (int i = offset; i < offset + (num * 8); i += 8) {
      values[idx++] = Bytes.getLong(value, i);
    }
    return values;
  }

  public static long[] toLongs(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    return toLongs(value, offset, length);
  }

  public static List<Long> toLongList(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    long[] values = toLongs(value, offset, length);
    ArrayList<Long> list = new ArrayList<>();
    for (Long v : values) {
      list.add(v);
    }
    return list;
  }

  public static int[] toInts(byte[] value, int offset, int num) {
    int[] values = new int[num];
    int idx = 0;
    for (int i = 0; i < num; i++) {
      int intValue = VarInt32.read(value, offset);
      offset += VarInt32.size(intValue);
      values[idx++] = intValue;
    }
    return values;
  }

  public static int[] toInts(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    return toInts(value, offset, length);
  }

  public static List<Integer> toIntList(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    int[] values = toInts(value, offset, length);
    ArrayList<Integer> list = new ArrayList<>();
    for (Integer v : values) {
      list.add(v);
    }
    return list;
  }

  public static char[] toChars(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    return toChars(value, offset, length);
  }

  public static char[] toChars(byte[] value, int offset, int num) {
    char[] values = new char[num];
    int idx = 0;
    for (int i = 0; i < num; i++) {
      char intValue = (char) VarInt32.read(value, offset);
      offset += VarInt32.size(intValue);
      values[idx++] = intValue;
    }
    return values;
  }

  public static List<Character> toCharList(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    char[] values = toChars(value, offset, length);
    ArrayList<Character> list = new ArrayList<>();
    for (Character v : values) {
      list.add(v);
    }
    return list;
  }

  public static short[] toShorts(byte[] value, int offset, int num) {
    short[] values = new short[num];
    int idx = 0;
    for (int i = 0; i < num; i++) {
      int intValue = VarInt32.read(value, offset);
      offset += VarInt32.size(intValue);
      values[idx++] = (short) intValue;
    }
    return values;
  }

  public static short[] toShorts(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    return toShorts(value, offset, length);
  }

  public static List<Short> toShortList(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    short[] values = toShorts(value, offset, length);
    ArrayList<Short> list = new ArrayList<>();
    for (short v : values) {
      list.add(v);
    }
    return list;
  }

  public static byte[] toBytes(byte[] value, int offset, int num) {
    byte[] values = new byte[num];
    int idx = 0;
    System.arraycopy(value, offset, values, 0, num);
    return values;
  }

  public static byte[] toBytes(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    return toBytes(value, offset, length);
  }

  public static byte[][] toBytesList(byte[] data, int offset) {
    int length = VarInt32.read(data, offset);
    offset += VarInt32.size(length);
    byte[][] byteArrays = new byte[length][];
    for (int i = 0; i < length; i++) {
      int l = VarInt32.read(data, offset);
      offset += VarInt32.size(l);
      byte[] bytes = toBytes(data, offset, l);
      offset += bytes.length;
      byteArrays[i] = bytes;
    }
    return byteArrays;
  }

  public static List<Byte> toByteList(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    byte[] bytes = toBytes(value, offset, length);
    ArrayList<Byte> list = new ArrayList<>();
    for (byte v : bytes) {
      list.add(v);
    }
    return list;
  }

  public static double[] toDoubles(byte[] value, int offset, int num) {
    double[] values = new double[num];
    int idx = 0;
    for (int i = offset; i < offset + (num * 8); i += 8) {
      values[idx++] = getDouble(value, i);
    }
    return values;
  }

  public static double[] toDoubles(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    return toDoubles(value, offset, length);
  }

  public static List<Double> toDoubleList(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    double[] values = toDoubles(value, offset, length);
    ArrayList<Double> list = new ArrayList<>();
    for (double v : values) {
      list.add(v);
    }
    return list;
  }

  public static float[] toFloats(byte[] value, int offset, int num) {
    float[] values = new float[num];
    int idx = 0;
    for (int i = offset; i < offset + (num * 4); i += 4) {
      values[idx++] = getFloat(value, i);
    }
    return values;
  }

  public static float[] toFloats(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    return toFloats(value, offset, length);
  }

  public static List<Float> toFloatList(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    float[] values = toFloats(value, offset, length);
    ArrayList<Float> list = new ArrayList<>();
    for (float v : values) {
      list.add(v);
    }
    return list;
  }

  public static String[] toStrings(byte[] value, int offset, int length) {
    ArrayList<String> values = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      int size = VarInt32.read(value, offset);
      offset += VarInt32.size(size);
      byte[] bytes = new byte[size];
      System.arraycopy(value, offset, bytes, 0, size);
      values.add(new String(bytes));
      offset += size;
    }
    return values.toArray(new String[values.size()]);
  }

  public static String[] toStrings(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    return toStrings(value, offset, length);
  }

  public static List<String> toStringList(byte[] value, int offset) {
    int length = VarInt32.read(value, offset);
    offset += VarInt32.size(length);
    String[] values = toStrings(value, offset, length);
    ArrayList<String> list = new ArrayList<>();
    Collections.addAll(list, values);
    return list;
  }

  public static short getShort(final byte[] b, final int offset) {
    return (short) (b[offset] << 8 | b[offset + 1] & 0xFF);
  }

  public static float getFloat(final byte[] b, final int offset) {
    return Float.intBitsToFloat(Bytes.getInt(b, offset));
  }

  public static double getDouble(final byte[] b, final int offset) {
    return Double.longBitsToDouble(Bytes.getLong(b, offset));
  }

  public static String getString(final byte[] b, int offset) {
    int length = VarInt32.read(b, offset);
    offset += VarInt32.size(length);
    byte[] bytes = new byte[length];
    System.arraycopy(b, offset, bytes, 0, length);
    return new String(bytes);
  }

  public static final int LOCAL_TIME_BYTES = 4 + 4 + 4 + 4;

  public static byte[] toBytes(LocalTime localTime) {
    int hour = localTime.getHour();
    int min = localTime.getMinute();
    int sec = localTime.getSecond();
    int nano = localTime.getNano();
    byte[] bytes = new byte[LOCAL_TIME_BYTES];
    System.arraycopy(Bytes.fromInt(hour), 0, bytes, 0, 4);
    System.arraycopy(Bytes.fromInt(min), 0, bytes, 4, 4);
    System.arraycopy(Bytes.fromInt(sec), 0, bytes, 8, 4);
    System.arraycopy(Bytes.fromInt(nano), 0, bytes, 12, 4);
    return bytes;
  }

  public static LocalTime getLocalTime(byte[] value, int offset) {
    int hour = Bytes.getInt(value, offset);
    int min = Bytes.getInt(value, 4 + offset);
    int sec = Bytes.getInt(value, 8 + offset);
    int nano = Bytes.getInt(value, 12 + offset);
    return LocalTime.of(hour, min, sec, nano);
  }

  public static final int LOCAL_DATE_BYTES = 4 + 4 + 4;

  public static byte[] toBytes(LocalDate localDate) {
    int year = localDate.getYear();
    int month = localDate.getMonthValue();
    int day = localDate.getDayOfMonth();
    byte[] bytes = new byte[LOCAL_DATE_BYTES];
    System.arraycopy(Bytes.fromInt(year), 0, bytes, 0, 4);
    System.arraycopy(Bytes.fromInt(month), 0, bytes, 4, 4);
    System.arraycopy(Bytes.fromInt(day), 0, bytes, 8, 4);
    return bytes;
  }

  public static LocalDate getLocalDate(byte[] bytes, int offset) {
    int year = Bytes.getInt(bytes, offset);
    int month = Bytes.getInt(bytes, 4 + offset);
    int day = Bytes.getInt(bytes, 8 + offset);
    return LocalDate.of(year, month, day);
  }

  public static final int INSTANT_BYTES = 8 + 4;

  public static byte[] toBytes(Instant instant) {
    long seconds = instant.getLong(ChronoField.INSTANT_SECONDS);
    int nanos = instant.get(ChronoField.NANO_OF_SECOND);
    byte[] bytes = new byte[INSTANT_BYTES];
    System.arraycopy(Bytes.fromLong(seconds), 0, bytes, 0, 8);
    System.arraycopy(Bytes.fromInt(nanos), 0, bytes, 8, 4);
    return bytes;
  }

  public static Instant getInstant(byte[] bytes) {
    long seconds = Bytes.getLong(bytes);
    int nanos = Bytes.getInt(bytes, 8);
    return Instant.ofEpochSecond(seconds, nanos);
  }

  public static final int PERIOD_BYTES = 4 + 4 + 4;

  public static byte[] toBytes(Period instant) {
    int years = instant.getYears();
    int months = instant.getMonths();
    int days = instant.getDays();

    byte[] bytes = new byte[PERIOD_BYTES];
    System.arraycopy(Bytes.fromInt(years), 0, bytes, 0, 4);
    System.arraycopy(Bytes.fromInt(months), 0, bytes, 4, 4);
    System.arraycopy(Bytes.fromInt(days), 0, bytes, 8, 4);
    return bytes;
  }

  public static Period getPeriod(byte[] bytes) {
    int years = Bytes.getInt(bytes);
    int months = Bytes.getInt(bytes, 4);
    int days = Bytes.getInt(bytes, 8);
    return Period.of(years, months, days);
  }

  public static final int DURATION_BYTES = 8 + 4;

  public static byte[] toBytes(Duration duration) {
    long seconds = duration.getSeconds();
    int nano = duration.getNano();
    byte[] bytes = new byte[DURATION_BYTES];
    System.arraycopy(Bytes.fromLong(seconds), 0, bytes, 0, 8);
    System.arraycopy(Bytes.fromInt(nano), 0, bytes, 8, 4);
    return bytes;
  }

  public static Duration getDuration(byte[] bytes) {
    long seconds = Bytes.getLong(bytes);
    int nano = Bytes.getInt(bytes, 8);
    return Duration.ofSeconds(seconds, nano);
  }


  public static final int LOCAL_DATE_TIME_BYTES = LOCAL_DATE_BYTES + LOCAL_TIME_BYTES;

  public static byte[] writeBytes(LocalDateTime localDateTime) {
    LocalDate localDate = localDateTime.toLocalDate();
    LocalTime localTime = localDateTime.toLocalTime();
    byte[] dateBytes = BytesUtils.toBytes(localDate);
    byte[] timeBytes = BytesUtils.toBytes(localTime);
    byte[] dateTimeBytes = new byte[LOCAL_DATE_TIME_BYTES];
    System.arraycopy(dateBytes, 0, dateTimeBytes, 0, dateBytes.length);
    System.arraycopy(timeBytes, 0, dateTimeBytes, dateBytes.length, timeBytes.length);
    return dateTimeBytes;
  }

  public static LocalDateTime getLocalDateTime(byte[] bytes) {
    LocalDate localDate = BytesUtils.getLocalDate(bytes, 0);
    LocalTime localTime = BytesUtils.getLocalTime(bytes, LOCAL_DATE_BYTES);
    return LocalDateTime.of(localDate, localTime);
  }

  public static final int ZONED_OFFSET_BYTES = 4;

  public static byte[] writeBytes(ZoneOffset zoneOffset) {
    int totalSeconds = zoneOffset.getTotalSeconds();
    return Bytes.fromInt(totalSeconds);
  }

  public static ZoneOffset getZoneOffset(byte[] bytes, int offset) {
    return ZoneOffset.ofTotalSeconds(Bytes.getInt(bytes, offset));
  }

  public static final int ZONED_DATE_TIME_BYTES = LOCAL_DATE_TIME_BYTES + ZONED_OFFSET_BYTES;

  public static byte[] writeBytes(ZonedDateTime zonedDateTime) {
    LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
    ZoneOffset offset = zonedDateTime.getOffset();
    ZoneId zone = zonedDateTime.getZone();
    if (!(zone instanceof ZoneOffset)) {
      // ZoneRegion is not supported since it is encoded as
      // a string i.e. Australia/Sydney and ZonedDateTime
      // must be written on a predefined number of bytes
      throw new IllegalArgumentException("ZonedDateTime with ZoneRegion is not supported.");
    }
    byte[] localDateTimeBytes = BytesUtils.writeBytes(localDateTime);
    byte[] offsetBytes = BytesUtils.writeBytes(offset);
    byte[] zonedDateTimeBytes = new byte[ZONED_DATE_TIME_BYTES];
    System.arraycopy(localDateTimeBytes, 0, zonedDateTimeBytes, 0, localDateTimeBytes.length);
    System.arraycopy(offsetBytes, 0, zonedDateTimeBytes, localDateTimeBytes.length, offsetBytes.length);
    return zonedDateTimeBytes;
  }

  public static ZonedDateTime getZonedDateTime(byte[] bytes) {
    LocalDate localDate = BytesUtils.getLocalDate(bytes, 0);
    LocalTime localTime = BytesUtils.getLocalTime(bytes, LOCAL_DATE_BYTES);
    ZoneOffset offset = BytesUtils.getZoneOffset(bytes, LOCAL_DATE_BYTES + LOCAL_TIME_BYTES);
    return ZonedDateTime.of(localDate, localTime, offset);
  }

  public static boolean getBoolean(byte b) {
    return b != 0;
  }

  public static ByteBuffer allocate(int length) {
    // return PooledByteBufAllocator.DEFAULT.directBuffer(length).nioBuffer();
    // return Unpooled.buffer(length).nioBuffer();
    return ByteBuffer.allocate(length);
  }

  static final Unsafe theUnsafe;

  /**
   * The offset to the first element in a byte array.
   */
  static final int BYTE_ARRAY_BASE_OFFSET;

  static {
    theUnsafe = (Unsafe) AccessController.doPrivileged(
            new PrivilegedAction<Object>() {
              @Override
              public Object run() {
                try {
                  Field f = Unsafe.class.getDeclaredField("theUnsafe");
                  f.setAccessible(true);
                  return f.get(null);
                } catch (NoSuchFieldException e) {
                  // It doesn't matter what we throw;
                  // it's swallowed in getBestComparer().
                  throw new Error();
                } catch (IllegalAccessException e) {
                  throw new Error();
                }
              }
            }
    );

    BYTE_ARRAY_BASE_OFFSET = theUnsafe.arrayBaseOffset(byte[].class);

    // sanity check - this should never fail
    if (theUnsafe.arrayIndexScale(byte[].class) != 1) {
      throw new AssertionError();
    }
  }

  public static int compareTo(byte[] buffer1, byte[] buffer2) {
    return compareTo(buffer1, 0, buffer1.length, buffer2, 0, buffer2.length);
  }

  /**
   * Lexicographically compare two arrays.
   *
   * @param buffer1 left operand
   * @param buffer2 right operand
   * @param offset1 Where to beginTransaction comparing in the left buffer
   * @param offset2 Where to beginTransaction comparing in the right buffer
   * @param length1 How much to compare from the left buffer
   * @param length2 How much to compare from the right buffer
   * @return 0 if equal, less 0 if left is less than right, etc.
   */
  public static int compareTo(byte[] buffer1, int offset1, int length1,
                              byte[] buffer2, int offset2, int length2) {
    // Short circuit equal case
    if (buffer1 == buffer2 &&
            offset1 == offset2 &&
            length1 == length2) {
      return 0;
    }
    int minLength = Math.min(length1, length2);
    int minWords = minLength / LONG_BYTES;
    int offset1Adj = offset1 + BYTE_ARRAY_BASE_OFFSET;
    int offset2Adj = offset2 + BYTE_ARRAY_BASE_OFFSET;

    /*
     * Compare 8 bytes at a time. Benchmarking shows comparing 8 bytes at a
     * time is no slower than comparing 4 bytes at a time even on 32-bit.
     * On the other hand, it is substantially faster on 64-bit.
     */
    for (int i = 0; i < minWords * LONG_BYTES; i += LONG_BYTES) {
      long lw = theUnsafe.getLong(buffer1, offset1Adj + (long) i);
      long rw = theUnsafe.getLong(buffer2, offset2Adj + (long) i);
      long diff = lw ^ rw;

      if (diff != 0) {
        if (!littleEndian) {
          return lessThanUnsigned(lw, rw) ? -1 : 1;
        }

        // Use binary search
        int n = 0;
        int y;
        int x = (int) diff;
        if (x == 0) {
          x = (int) (diff >>> 32);
          n = 32;
        }

        y = x << 16;
        if (y == 0) {
          n += 16;
        } else {
          x = y;
        }

        y = x << 8;
        if (y == 0) {
          n += 8;
        }
        return (int) (((lw >>> n) & 0xFFL) - ((rw >>> n) & 0xFFL));
      }
    }

    // The epilogue to cover the last (minLength % 8) elements.
    for (int i = minWords * LONG_BYTES; i < minLength; i++) {
      int result = compare(
              buffer1[offset1 + i],
              buffer2[offset2 + i]);
      if (result != 0) {
        return result;
      }
    }
    return length1 - length2;
  }

  static final boolean littleEndian =
          ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);

  /**
   * Returns true if x1 is less than x2, when both values are treated as
   * unsigned.
   */
  static boolean lessThanUnsigned(long x1, long x2) {
    return (x1 + Long.MIN_VALUE) < (x2 + Long.MIN_VALUE);
  }

  public static String toStringBinary(final byte[] b) {
    if (b == null)
      return "null";
    return toStringBinary(b, 0, b.length);
  }

  public static String toStringBinary(byte[] b, int off, int len) {
    StringBuilder result = new StringBuilder();
    // Just in case we are passed a 'len' that is > buffer length...
    if (off >= b.length) return result.toString();
    if (off + len > b.length) len = b.length - off;
    for (int i = off; i < off + len; ++i) {
      int ch = b[i] & 0xFF;
      if ((ch >= '0' && ch <= '9')
              || (ch >= 'A' && ch <= 'Z')
              || (ch >= 'a' && ch <= 'z')
              || " `~!@#$%^&*()-_=+[]{}|;:'\",.<>/?".indexOf(ch) >= 0) {
        result.append((char) ch);
      } else {
        result.append(String.format("\\x%02X", ch));
      }
    }
    return result.toString();
  }

  public static enum DataType {
    BYTE(1), SHORT(2), INTEGER(3), LONG(4), FLOAT(5), DOUBLE(6), BOOLEAN(7), STRING(8),
    CHAR(9), BYTE_ARRAY(10), ENUM(11), BIG_INTEGER(12), BIG_DECIMAL(13), DATE(14),
    LOCAL_DATE_TIME(15), ZONED_DATE_TIME(16), LOCAL_DATE(17), LOCAL_TIME(18), INSTANT(19),
    PERIOD(20), DURATION(21),

    BYTE_LIST(30), SHORT_LIST(31), INTEGER_LIST(32), LONG_LIST(33), FLOAT_LIST(34),
    DOUBLE_LIST(35), BOOLEAN_LIST(36), STRING_LIST(37), CHAR_LIST(38), BYTE_ARRAY_LIST(39),

    // special treatment
    OBJECT_LIST(50), OBJECT(51);
    private int id;

    DataType(int id) {
      this.id = id;
    }

    private static final Map<Byte, DataType> idToEnumMap = new HashMap<>();

    static {
      for (DataType type : DataType.values()) {
        idToEnumMap.put(type.getId(), type);
      }
    }

    public byte getId() {
      return (byte) id;
    }

    public static DataType getDataType(byte id) {
      return idToEnumMap.get(id);
    }

    public static DataType getDataType(Class<?> cls) {
      if (Byte.class.isAssignableFrom(cls)) {
        return BYTE;
      } else if (byte[].class.isAssignableFrom(cls)) {
        return BYTE_ARRAY;
      } else if (byte.class.isAssignableFrom(cls)) {
        return BYTE;
      } else if (Short.class.isAssignableFrom(cls)) {
        return SHORT;
      } else if (short.class.isAssignableFrom(cls)) {
        return SHORT;
      } else if (Integer.class.isAssignableFrom(cls)) {
        return INTEGER;
      } else if (int.class.isAssignableFrom(cls)) {
        return INTEGER;
      } else if (Long.class.isAssignableFrom(cls)) {
        return LONG;
      } else if (long.class.isAssignableFrom(cls)) {
        return LONG;
      } else if (Float.class.isAssignableFrom(cls)) {
        return FLOAT;
      } else if (float.class.isAssignableFrom(cls)) {
        return FLOAT;
      } else if (Double.class.isAssignableFrom(cls)) {
        return DOUBLE;
      } else if (double.class.isAssignableFrom(cls)) {
        return DOUBLE;
      } else if (Boolean.class.isAssignableFrom(cls)) {
        return BOOLEAN;
      } else if (boolean.class.isAssignableFrom(cls)) {
        return BOOLEAN;
      } else if (String.class.isAssignableFrom(cls)) {
        return STRING;
      } else if (char.class.isAssignableFrom(cls)) {
        return CHAR;
      } else if (Character.class.isAssignableFrom(cls)) {
        return CHAR;
      } else if (Enum.class.isAssignableFrom(cls)) {
        return ENUM;
      } else if (BigDecimal.class.isAssignableFrom(cls)) {
        return BIG_DECIMAL;
      } else if (BigInteger.class.isAssignableFrom(cls)) {
        return BIG_INTEGER;
      } else if (Date.class.isAssignableFrom(cls)) {
        return DATE;
      } else if (LocalDateTime.class.isAssignableFrom(cls)) {
        return LOCAL_DATE_TIME;
      } else if (ZonedDateTime.class.isAssignableFrom(cls)) {
        return ZONED_DATE_TIME;
      } else if (LocalDate.class.isAssignableFrom(cls)) {
        return LOCAL_DATE;
      } else if (LocalTime.class.isAssignableFrom(cls)) {
        return LOCAL_TIME;
      } else if (Instant.class.isAssignableFrom(cls)) {
        return INSTANT;
      } else if (Period.class.isAssignableFrom(cls)) {
        return PERIOD;
      } else if (Duration.class.isAssignableFrom(cls)) {
        return DURATION;
      } else {
        return OBJECT;
      }
    }
  }

  public static int compare(byte a, byte b) {
    return toInt(a) - toInt(b);
  }

  public static int toInt(byte value) {
    return value & 0xFF;
  }
}
