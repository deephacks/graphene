package org.deephacks.graphene;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.deephacks.graphene.BuilderProxy.Builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Entities {

  public static interface StandardFields {

    byte getBytePrimitive();
    byte[] getBytePrimitiveArray();
    Byte getByteObject();
    List<Byte> getByteList();
    Set<Byte> getByteSet();
    Map<Byte, Byte> getByteMap();

    short getShortPrimitive();
    short[] getShortPrimitiveArray();
    Short getShortObject();
    List<Short> getShortList();
    Set<Short> getShortSet();
    Map<Short, Short> getShortMap();

    int getIntPrimitive();
    int[] getIntPrimitiveArray();
    Integer getIntegerObject();
    List<Integer> getIntegerList();
    Set<Integer> getIntegerSet();
    Map<Integer, Integer> getIntegerMap();

    long getLongPrimitive();
    long[] getLongPrimitiveArray();
    Long getLongObject();
    List<Long> getLongList();
    Set<Long> getLongSet();
    Map<Long, Long> getLongMap();


    float getFloatPrimitive();
    float[] getFloatPrimitiveArray();
    Float getFloatObject();
    List<Float> getFloatList();
    Set<Float> getFloatSet();
    Map<Float, Float> getFloatMap();

    double getDoublePrimitive();
    double[] getDoublePrimitiveArray();
    Double getDoubleObject();
    List<Double> getDoubleList();
    Set<Double> getDoubleSet();
    Map<Double, Double> getDoubleMap();

    boolean getBoolPrimitive();
    boolean[] getBoolPrimitiveArray();
    Boolean getBooleanObject();
    List<Boolean> getBooleanList();
    Set<Boolean> getBooleanSet();
    Map<Boolean, Boolean> getBooleanMap();

    char getCharPrimitive();
    char[] getCharPrimitiveArray();
    Character getCharacterObject();
    List<Character> getCharacterList();
    Set<Character> getCharacterSet();
    Map<Character, Character> getCharacterMap();

    String getString();
    List<String> getStringList();
    Set<String> getStringSet();
    Map<String, String> getStringMap();

    LocalDateTime getLocalDateTime();
    List<LocalDateTime> getLocalDateTimeList();
    Set<LocalDateTime> getLocalDateTimeSet();
    Map<LocalDateTime, LocalDateTime> getLocalDateTimeMap();

    BigDecimal getBigDecimal();
    List<BigDecimal> getBigDecimals();

    BigInteger getBigInteger();
    List<BigInteger> getBigIntegers();

    TimeUnit getEnumValue();
    List<TimeUnit> getEnumList();
    EnumSet<TimeUnit> getEnumSet();
    Map<TimeUnit, TimeUnit> getEnumMap();

    Date getDate();
    List<Date> getDateList();

    ZonedDateTime getZonedDateTime();
    List<ZonedDateTime> getZonedDateTimeList();

    LocalDate getLocalDate();
    List<LocalDate> getLocalDateList();

    LocalTime getLocalTime();
    List<LocalTime> getLocalTimeList();

    Instant getInstant();
    List<Instant> getInstantList();

    Period getPeriod();
    List<Period> getPeriods();

    Duration getDuration();
    List<Duration> getDurations();
  }

  public static interface Identity {
    @Key
    String getId();
  }

  @Entity
  public static interface A extends Identity, StandardFields {

    Value getEmbedded();
    List<Value> getEmbeddedList();
    Set<Value> getEmbeddedSet();
    Map<String, Value> getEmbeddedMap();
  }

  @Entity(builderPrefix = "with")
  public static interface B extends Identity, StandardFields {

    Optional<A> getA();
    Optional<List<A>> getListOfA();
    Optional<Set<A>> getSetOfA();
    Optional<Map<String, A>> getMapOfA();

    Value getEmbedded();
    List<Value> getEmbeddedList();
    Set<Value> getEmbeddedSet();
    Map<String, Value> getEmbeddedMap();
  }

  @Entity(builderPrefix = "with")
  public static interface C extends Identity, StandardFields {

    Optional<B> getB();
    Optional<List<B>> getListOfB();
    Optional<Set<B>> getSetOfB();
    Optional<Map<String, B>> getMapOfB();

    Value getEmbedded();
    List<Value> getEmbeddedList();
    Set<Value> getEmbeddedSet();
    Map<String, Value> getEmbeddedMap();
  }

  @Embedded(builderPrefix = "with")
  public static interface Value extends StandardFields {
  }

  @Entity
  public static interface G {
    @Key
    String getKey();

    ImmutableSet<String> getSet();
    ImmutableList<String> getList();
    ImmutableMap<String, String> getMap();
  }

  @Entity
  public static interface Person {
    @Key
    String getId();

    String getForeName();
    String getSureName();
  }

  @Entity
  public static interface DefaultValues {
    @Key
    String getId();

    default String getValue() {
      return "value";
    }
  }

  @Entity
  public static interface OptionalValues {
    @Key
    String getId();

    Optional<String> getValue();
    //Optional<byte[]> getValueArray();
    Optional<List<String>> getListValue();
    Optional<Map<String, String>> getMapValue();
    Optional<Set<String>> getSetValue();
  }

  @Entity
  public static interface ObjectKeyEntity {
    ObjectKey getKey();
    Optional<String> getValue();
    //Optional<byte[]> getValueArray();
    Optional<List<String>> getListValue();
    Optional<Map<String, String>> getMapValue();
    Optional<Set<String>> getSetValue();
  }

  @Key
  public static interface ObjectKey {
    @Key(position = 0)
    String getPartition();

    @Key(position = 1)
    LocalDateTime getTime();
  }

  @Entity
  public static interface ByteArrayKey {
    @Key(size = 3)
    byte[] getKey();
  }


  public static A buildA(String id, String... value) {
    return new Builder<>(A.class)
            .set(A::getId, id)
            .set(A::getBytePrimitive, (byte) 1)
            .set(A::getBytePrimitiveArray, new byte[]{(byte) 1, (byte) 2, (byte) 3})
            .set(A::getByteObject, (byte) 1)
            .set(A::getByteSet, Sets.newHashSet(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(A::getByteList, Arrays.asList(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(A::getByteMap, newHashMap().with(Byte.MAX_VALUE, Byte.MIN_VALUE).build())
            .set(A::getShortPrimitive, (short) 123)
            .set(A::getShortPrimitiveArray, new short[] {(short)1, (short)2, (short)12345})
            .set(A::getShortObject, (short) 1)
            .set(A::getShortSet, Sets.newHashSet(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(A::getShortList, Arrays.asList(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(A::getShortMap, newHashMap().with(Short.MIN_VALUE, Short.MAX_VALUE).build())
            .set(A::getIntPrimitive, Integer.MAX_VALUE)
            .set(A::getIntegerObject, Integer.MAX_VALUE)
            .set(A::getIntPrimitiveArray, new int[] {Integer.MAX_VALUE, 1, 1})
            .set(A::getIntegerSet, Sets.newHashSet(Integer.MAX_VALUE, Integer.MIN_VALUE))
            .set(A::getIntegerList, Arrays.asList(Integer.MIN_VALUE, Integer.MAX_VALUE))
            .set(A::getIntegerMap, newHashMap().with(Integer.MIN_VALUE, Integer.MAX_VALUE).build())
            .set(A::getLongPrimitive, Long.MAX_VALUE)
            .set(A::getLongObject, Long.MAX_VALUE)
            .set(A::getLongPrimitiveArray, new long[] {1L, Long.MAX_VALUE, Long.MIN_VALUE})
            .set(A::getLongSet, Sets.newHashSet(Long.MIN_VALUE, Long.MAX_VALUE))
            .set(A::getLongList, Arrays.asList(Long.MAX_VALUE, 0L, Long.MIN_VALUE))
            .set(A::getLongMap, newHashMap().with(Long.MAX_VALUE, Long.MIN_VALUE).build())
            .set(A::getFloatPrimitive, Float.MAX_VALUE)
            .set(A::getFloatObject, Float.MAX_VALUE)
            .set(A::getFloatPrimitiveArray, new float[]{1f, Float.MAX_VALUE, Float.MIN_VALUE})
            .set(A::getFloatSet, Sets.newHashSet(Float.MIN_VALUE, Float.MAX_VALUE))
            .set(A::getFloatList, Arrays.asList(Float.MAX_VALUE, 0f, Float.MIN_VALUE))
            .set(A::getFloatMap, newHashMap().with(Float.MAX_VALUE, Float.MIN_VALUE).build())
            .set(A::getDoublePrimitive, Double.MAX_VALUE)
            .set(A::getDoubleObject, Double.MAX_VALUE)
            .set(A::getDoublePrimitiveArray, new double[] {1d, Double.MAX_VALUE, Double.MIN_VALUE})
            .set(A::getDoubleSet, Sets.newHashSet(Double.MIN_VALUE, Double.MAX_VALUE))
            .set(A::getDoubleList, Arrays.asList(Double.MAX_VALUE, 0d, Double.MIN_VALUE))
            .set(A::getDoubleMap, newHashMap().with(Double.MAX_VALUE, Double.MIN_VALUE).build())
            .set(A::getBoolPrimitive, true)
            .set(A::getBooleanObject, true)
            .set(A::getBoolPrimitiveArray, new boolean[] {false, true, false})
            .set(A::getBooleanList, Arrays.asList(true, false))
            .set(A::getBooleanSet, Sets.<Boolean>newHashSet(Boolean.FALSE, Boolean.TRUE))
            .set(A::getBooleanMap, newHashMap().with(true, false).build())
            .set(A::getCharPrimitive, 'g')
            .set(A::getCharacterObject, 'g')
            .set(A::getCharPrimitiveArray, new char[]{'a', Character.MIN_VALUE, Character.MAX_VALUE})
            .set(A::getCharacterSet, Sets.newHashSet(Character.MIN_VALUE, Character.MAX_VALUE))
            .set(A::getCharacterList, Arrays.asList(Character.MAX_VALUE, Character.MIN_VALUE))
            .set(A::getCharacterMap, newHashMap().with(Character.MAX_VALUE, Character.MIN_VALUE).build())
            .set(A::getString, value.length == 0 ? "value" : value[0])
            .set(A::getStringList, Arrays.asList("1a", "2b", "3c"))
            .set(A::getStringSet, Sets.newHashSet("1a", "2b", "3d"))
            .set(A::getStringMap, newHashMap().with("a", "b").build())
            .set(A::getBigDecimal, new BigDecimal("0127392183723987.12938712983723976"))
            .set(A::getBigDecimals, Arrays.asList(new BigDecimal("0127392183723987.12938712983723976"), new BigDecimal("0127392183723987.12938712983723976")))
            .set(A::getBigInteger, new BigInteger("012739218372398712938712983723976"))
            .set(A::getBigIntegers, Arrays.asList(new BigInteger("012739218372398712938712983723976"), new BigInteger("012739218372398712938712983723976")))
            .set(A::getEmbedded, buildEmbedded(value))
            .set(A::getEmbeddedList, Arrays.asList(buildEmbedded(value), buildEmbedded(value), buildEmbedded(value)))
            .set(A::getEmbeddedSet, Sets.newHashSet(buildEmbedded(value), buildEmbedded(value), buildEmbedded(value)))
            .set(A::getEmbeddedMap, newHashMap().with("embedded", buildEmbedded(value)).build())
            .set(A::getEnumValue, TimeUnit.DAYS)
            .set(A::getEnumList, Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS))
            .set(A::getEnumSet, EnumSet.of(TimeUnit.MICROSECONDS, TimeUnit.HOURS))
            .set(A::getEnumMap, newHashMap().with(TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS).build())
            .set(A::getDate, new Date(1))
            .set(A::getDateList, Arrays.asList(new Date(1), new Date(2)))
            .set(A::getLocalDateTime, LocalDateTime.parse("2014-04-01T12:00"))
            .set(A::getLocalDateTimeList, Arrays.asList(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(A::getLocalDateTimeSet, Sets.newHashSet(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(A::getLocalDateTimeMap, newHashMap().with(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")).build())
            .set(A::getZonedDateTime, ZonedDateTime.parse("2007-12-03T10:15:30+01:00"))
            .set(A::getZonedDateTimeList, Arrays.asList(ZonedDateTime.parse("2007-12-03T10:15:30+01:00"), ZonedDateTime.parse("2007-12-03T10:15:30+01:00")))
            .set(A::getLocalDate, LocalDate.parse("2007-12-03"))
            .set(A::getLocalDateList, Arrays.asList(LocalDate.parse("2007-12-03"), LocalDate.parse("2007-12-03")))
            .set(A::getLocalTime, LocalTime.parse("10:15"))
            .set(A::getLocalTimeList, Arrays.asList(LocalTime.parse("10:15"), LocalTime.parse("10:15")))
            .set(A::getInstant, Instant.parse("2013-06-25T16:22:52.966Z"))
            .set(A::getInstantList, Arrays.asList(Instant.parse("2013-06-25T16:22:52.966Z"), Instant.parse("2013-06-25T16:22:52.966Z")))
            .set(A::getPeriod, Period.parse("P1Y2M3D"))
            .set(A::getPeriods, Arrays.asList(Period.parse("P1Y2M3D"), Period.parse("P1Y2M3D")))
            .set(A::getDuration, Duration.parse("PT15M"))
            .set(A::getDurations, Arrays.asList(Duration.parse("PT15M"), Duration.parse("PT15M"))).build().get();
  }


  public static B buildB(String id, A a, List<A> listOfA, String... value) {
    return builderB(id)
            .set(B::getId, id)
            .set(B::getA, Optional.of(a))
            .set(B::getListOfA, Optional.of(listOfA))
            .set(B::getBytePrimitive, (byte) 1)
            .set(B::getBytePrimitiveArray, new byte[]{(byte) 1, (byte) 2, (byte) 3})
            .set(B::getByteObject, (byte) 1)
            .set(B::getByteSet, Sets.newHashSet(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(B::getByteList, Arrays.asList(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(B::getByteMap, newHashMap().with(Byte.MAX_VALUE, Byte.MIN_VALUE).build())
            .set(B::getShortPrimitive, (short) 123)
            .set(B::getShortPrimitiveArray, new short[] {(short)1, (short)2, (short)12345})
            .set(B::getShortObject, (short) 1)
            .set(B::getShortSet, Sets.newHashSet(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(B::getShortList, Arrays.asList(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(B::getShortMap, newHashMap().with(Short.MIN_VALUE, Short.MAX_VALUE).build())
            .set(B::getIntPrimitive, Integer.MAX_VALUE)
            .set(B::getIntegerObject, Integer.MAX_VALUE)
            .set(B::getIntPrimitiveArray, new int[] {Integer.MAX_VALUE, 1, 1})
            .set(B::getIntegerSet, Sets.newHashSet(Integer.MAX_VALUE, Integer.MIN_VALUE))
            .set(B::getIntegerList, Arrays.asList(Integer.MIN_VALUE, Integer.MAX_VALUE))
            .set(B::getIntegerMap, newHashMap().with(Integer.MIN_VALUE, Integer.MAX_VALUE).build())
            .set(B::getLongPrimitive, Long.MAX_VALUE)
            .set(B::getLongObject, Long.MAX_VALUE)
            .set(B::getLongPrimitiveArray, new long[] {1L, Long.MAX_VALUE, Long.MIN_VALUE})
            .set(B::getLongSet, Sets.newHashSet(Long.MIN_VALUE, Long.MAX_VALUE))
            .set(B::getLongList, Arrays.asList(Long.MAX_VALUE, 0L, Long.MIN_VALUE))
            .set(B::getLongMap, newHashMap().with(Long.MAX_VALUE, Long.MIN_VALUE).build())
            .set(B::getFloatPrimitive, Float.MAX_VALUE)
            .set(B::getFloatObject, Float.MAX_VALUE)
            .set(B::getFloatPrimitiveArray, new float[]{1f, Float.MAX_VALUE, Float.MIN_VALUE})
            .set(B::getFloatSet, Sets.newHashSet(Float.MIN_VALUE, Float.MAX_VALUE))
            .set(B::getFloatList, Arrays.asList(Float.MAX_VALUE, 0f, Float.MIN_VALUE))
            .set(B::getFloatMap, newHashMap().with(Float.MAX_VALUE, Float.MIN_VALUE).build())
            .set(B::getDoublePrimitive, Double.MAX_VALUE)
            .set(B::getDoubleObject, Double.MAX_VALUE)
            .set(B::getDoublePrimitiveArray, new double[] {1d, Double.MAX_VALUE, Double.MIN_VALUE})
            .set(B::getDoubleSet, Sets.newHashSet(Double.MIN_VALUE, Double.MAX_VALUE))
            .set(B::getDoubleList, Arrays.asList(Double.MAX_VALUE, 0d, Double.MIN_VALUE))
            .set(B::getDoubleMap, newHashMap().with(Double.MAX_VALUE, Double.MIN_VALUE).build())
            .set(B::getBoolPrimitive, true)
            .set(B::getBooleanObject, true)
            .set(B::getBoolPrimitiveArray, new boolean[] {false, true, false})
            .set(B::getBooleanList, Arrays.asList(true, false))
            .set(B::getBooleanSet, Sets.<Boolean>newHashSet(Boolean.FALSE, Boolean.TRUE))
            .set(B::getBooleanMap, newHashMap().with(true, false).build())
            .set(B::getCharPrimitive, 'g')
            .set(B::getCharacterObject, 'g')
            .set(B::getCharPrimitiveArray, new char[]{'a', Character.MIN_VALUE, Character.MAX_VALUE})
            .set(B::getCharacterSet, Sets.newHashSet(Character.MIN_VALUE, Character.MAX_VALUE))
            .set(B::getCharacterList, Arrays.asList(Character.MAX_VALUE, Character.MIN_VALUE))
            .set(B::getCharacterMap, newHashMap().with(Character.MAX_VALUE, Character.MIN_VALUE).build())
            .set(B::getString, value.length == 0 ? "value" : value[0])
            .set(B::getStringList, Arrays.asList("1a", "2b", "3c"))
            .set(B::getStringSet, Sets.newHashSet("1a", "2b", "3d"))
            .set(B::getStringMap, newHashMap().with("a", "b").build())
            .set(B::getBigDecimal, new BigDecimal("0127392183723987.12938712983723976"))
            .set(B::getBigDecimals, Arrays.asList(new BigDecimal("0127392183723987.12938712983723976"), new BigDecimal("0127392183723987.12938712983723976")))
            .set(B::getBigInteger, new BigInteger("012739218372398712938712983723976"))
            .set(B::getBigIntegers, Arrays.asList(new BigInteger("012739218372398712938712983723976"), new BigInteger("012739218372398712938712983723976")))
            .set(B::getEmbedded, buildEmbedded(value))
            .set(B::getEmbeddedList, Arrays.asList(buildEmbedded(value), buildEmbedded(value), buildEmbedded(value)))
            .set(B::getEmbeddedSet, Sets.newHashSet(buildEmbedded(value), buildEmbedded(value), buildEmbedded(value)))
            .set(B::getEmbeddedMap, newHashMap().with("embedded", buildEmbedded(value)).build())
            .set(B::getEnumValue, TimeUnit.DAYS)
            .set(B::getEnumList, Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS))
            .set(B::getEnumSet, EnumSet.of(TimeUnit.MICROSECONDS, TimeUnit.HOURS))
            .set(B::getEnumMap, newHashMap().with(TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS).build())
            .set(B::getDate, new Date(1))
            .set(B::getDateList, Arrays.asList(new Date(1), new Date(2)))
            .set(B::getLocalDateTime, LocalDateTime.parse("2014-04-01T12:00"))
            .set(B::getLocalDateTimeList, Arrays.asList(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(B::getLocalDateTimeSet, Sets.newHashSet(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(B::getLocalDateTimeMap, newHashMap().with(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")).build())
            .set(B::getZonedDateTime, ZonedDateTime.parse("2007-12-03T10:15:30+01:00"))
            .set(B::getZonedDateTimeList, Arrays.asList(ZonedDateTime.parse("2007-12-03T10:15:30+01:00"), ZonedDateTime.parse("2007-12-03T10:15:30+01:00")))
            .set(B::getLocalDate, LocalDate.parse("2007-12-03"))
            .set(B::getLocalDateList, Arrays.asList(LocalDate.parse("2007-12-03"), LocalDate.parse("2007-12-03")))
            .set(B::getLocalTime, LocalTime.parse("10:15"))
            .set(B::getLocalTimeList, Arrays.asList(LocalTime.parse("10:15"), LocalTime.parse("10:15")))
            .set(B::getInstant, Instant.parse("2013-06-25T16:22:52.966Z"))
            .set(B::getInstantList, Arrays.asList(Instant.parse("2013-06-25T16:22:52.966Z"), Instant.parse("2013-06-25T16:22:52.966Z")))
            .set(B::getPeriod, Period.parse("P1Y2M3D"))
            .set(B::getPeriods, Arrays.asList(Period.parse("P1Y2M3D"), Period.parse("P1Y2M3D")))
            .set(B::getDuration, Duration.parse("PT15M"))
            .set(B::getDurations, Arrays.asList(Duration.parse("PT15M"), Duration.parse("PT15M"))).build().get();
  }

  public static B buildB(String id, String... value) {
    return builderB(id)
            .set(B::getId, id)
            .set(B::getBytePrimitive, (byte) 1)
            .set(B::getBytePrimitiveArray, new byte[]{(byte) 1, (byte) 2, (byte) 3})
            .set(B::getByteObject, (byte) 1)
            .set(B::getByteSet, Sets.newHashSet(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(B::getByteList, Arrays.asList(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(B::getByteMap, newHashMap().with(Byte.MAX_VALUE, Byte.MIN_VALUE).build())
            .set(B::getShortPrimitive, (short) 123)
            .set(B::getShortPrimitiveArray, new short[] {(short)1, (short)2, (short)12345})
            .set(B::getShortObject, (short) 1)
            .set(B::getShortSet, Sets.newHashSet(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(B::getShortList, Arrays.asList(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(B::getShortMap, newHashMap().with(Short.MIN_VALUE, Short.MAX_VALUE).build())
            .set(B::getIntPrimitive, Integer.MAX_VALUE)
            .set(B::getIntegerObject, Integer.MAX_VALUE)
            .set(B::getIntPrimitiveArray, new int[] {Integer.MAX_VALUE, 1, 1})
            .set(B::getIntegerSet, Sets.newHashSet(Integer.MAX_VALUE, Integer.MIN_VALUE))
            .set(B::getIntegerList, Arrays.asList(Integer.MIN_VALUE, Integer.MAX_VALUE))
            .set(B::getIntegerMap, newHashMap().with(Integer.MIN_VALUE, Integer.MAX_VALUE).build())
            .set(B::getLongPrimitive, Long.MAX_VALUE)
            .set(B::getLongObject, Long.MAX_VALUE)
            .set(B::getLongPrimitiveArray, new long[] {1L, Long.MAX_VALUE, Long.MIN_VALUE})
            .set(B::getLongSet, Sets.newHashSet(Long.MIN_VALUE, Long.MAX_VALUE))
            .set(B::getLongList, Arrays.asList(Long.MAX_VALUE, 0L, Long.MIN_VALUE))
            .set(B::getLongMap, newHashMap().with(Long.MAX_VALUE, Long.MIN_VALUE).build())
            .set(B::getFloatPrimitive, Float.MAX_VALUE)
            .set(B::getFloatObject, Float.MAX_VALUE)
            .set(B::getFloatPrimitiveArray, new float[]{1f, Float.MAX_VALUE, Float.MIN_VALUE})
            .set(B::getFloatSet, Sets.newHashSet(Float.MIN_VALUE, Float.MAX_VALUE))
            .set(B::getFloatList, Arrays.asList(Float.MAX_VALUE, 0f, Float.MIN_VALUE))
            .set(B::getFloatMap, newHashMap().with(Float.MAX_VALUE, Float.MIN_VALUE).build())
            .set(B::getDoublePrimitive, Double.MAX_VALUE)
            .set(B::getDoubleObject, Double.MAX_VALUE)
            .set(B::getDoublePrimitiveArray, new double[] {1d, Double.MAX_VALUE, Double.MIN_VALUE})
            .set(B::getDoubleSet, Sets.newHashSet(Double.MIN_VALUE, Double.MAX_VALUE))
            .set(B::getDoubleList, Arrays.asList(Double.MAX_VALUE, 0d, Double.MIN_VALUE))
            .set(B::getDoubleMap, newHashMap().with(Double.MAX_VALUE, Double.MIN_VALUE).build())
            .set(B::getBoolPrimitive, true)
            .set(B::getBooleanObject, true)
            .set(B::getBoolPrimitiveArray, new boolean[] {false, true, false})
            .set(B::getBooleanList, Arrays.asList(true, false))
            .set(B::getBooleanSet, Sets.<Boolean>newHashSet(Boolean.FALSE, Boolean.TRUE))
            .set(B::getBooleanMap, newHashMap().with(true, false).build())
            .set(B::getCharPrimitive, 'g')
            .set(B::getCharacterObject, 'g')
            .set(B::getCharPrimitiveArray, new char[]{'a', Character.MIN_VALUE, Character.MAX_VALUE})
            .set(B::getCharacterSet, Sets.newHashSet(Character.MIN_VALUE, Character.MAX_VALUE))
            .set(B::getCharacterList, Arrays.asList(Character.MAX_VALUE, Character.MIN_VALUE))
            .set(B::getCharacterMap, newHashMap().with(Character.MAX_VALUE, Character.MIN_VALUE).build())
            .set(B::getString, value.length == 0 ? "value" : value[0])
            .set(B::getStringList, Arrays.asList("1a", "2b", "3c"))
            .set(B::getStringSet, Sets.newHashSet("1a", "2b", "3d"))
            .set(B::getStringMap, newHashMap().with("a", "b").build())
            .set(B::getBigDecimal, new BigDecimal("0127392183723987.12938712983723976"))
            .set(B::getBigDecimals, Arrays.asList(new BigDecimal("0127392183723987.12938712983723976"), new BigDecimal("0127392183723987.12938712983723976")))
            .set(B::getBigInteger, new BigInteger("012739218372398712938712983723976"))
            .set(B::getBigIntegers, Arrays.asList(new BigInteger("012739218372398712938712983723976"), new BigInteger("012739218372398712938712983723976")))
            .set(B::getEmbedded, buildEmbedded(value))
            .set(B::getEmbeddedList, Arrays.asList(buildEmbedded(value), buildEmbedded(value), buildEmbedded(value)))
            .set(B::getEmbeddedSet, Sets.newHashSet(buildEmbedded(value), buildEmbedded(value), buildEmbedded(value)))
            .set(B::getEmbeddedMap, newHashMap().with("embedded", buildEmbedded(value)).build())
            .set(B::getEnumValue, TimeUnit.DAYS)
            .set(B::getEnumList, Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS))
            .set(B::getEnumSet, EnumSet.of(TimeUnit.MICROSECONDS, TimeUnit.HOURS))
            .set(B::getEnumMap, newHashMap().with(TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS).build())
            .set(B::getDate, new Date(1))
            .set(B::getDateList, Arrays.asList(new Date(1), new Date(2)))
            .set(B::getLocalDateTime, LocalDateTime.parse("2014-04-01T12:00"))
            .set(B::getLocalDateTimeList, Arrays.asList(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(B::getLocalDateTimeSet, Sets.newHashSet(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(B::getLocalDateTimeMap, newHashMap().with(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")).build())
            .set(B::getZonedDateTime, ZonedDateTime.parse("2007-12-03T10:15:30+01:00"))
            .set(B::getZonedDateTimeList, Arrays.asList(ZonedDateTime.parse("2007-12-03T10:15:30+01:00"), ZonedDateTime.parse("2007-12-03T10:15:30+01:00")))
            .set(B::getLocalDate, LocalDate.parse("2007-12-03"))
            .set(B::getLocalDateList, Arrays.asList(LocalDate.parse("2007-12-03"), LocalDate.parse("2007-12-03")))
            .set(B::getLocalTime, LocalTime.parse("10:15"))
            .set(B::getLocalTimeList, Arrays.asList(LocalTime.parse("10:15"), LocalTime.parse("10:15")))
            .set(B::getInstant, Instant.parse("2013-06-25T16:22:52.966Z"))
            .set(B::getInstantList, Arrays.asList(Instant.parse("2013-06-25T16:22:52.966Z"), Instant.parse("2013-06-25T16:22:52.966Z")))
            .set(B::getPeriod, Period.parse("P1Y2M3D"))
            .set(B::getPeriods, Arrays.asList(Period.parse("P1Y2M3D"), Period.parse("P1Y2M3D")))
            .set(B::getDuration, Duration.parse("PT15M"))
            .set(B::getDurations, Arrays.asList(Duration.parse("PT15M"), Duration.parse("PT15M"))).build().get();
  }

  public static Builder<B> builderB(String id, String... value) {
    return new Builder<>(B.class);
  }

  public static C buildC(String id, B b, List<B> listOfB, String... value) {
    return builderC(id)
            .set(C::getId, id)
            .set(C::getB, Optional.of(b))
            .set(C::getListOfB, Optional.of(listOfB))
            .set(C::getBytePrimitive, (byte) 1)
            .set(C::getBytePrimitiveArray, new byte[]{(byte) 1, (byte) 2, (byte) 3})
            .set(C::getByteObject, (byte) 1)
            .set(C::getByteSet, Sets.newHashSet(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(C::getByteList, Arrays.asList(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(C::getByteMap, newHashMap().with(Byte.MAX_VALUE, Byte.MIN_VALUE).build())
            .set(C::getShortPrimitive, (short) 123)
            .set(C::getShortPrimitiveArray, new short[] {(short)1, (short)2, (short)12345})
            .set(C::getShortObject, (short) 1)
            .set(C::getShortSet, Sets.newHashSet(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(C::getShortList, Arrays.asList(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(C::getShortMap, newHashMap().with(Short.MIN_VALUE, Short.MAX_VALUE).build())
            .set(C::getIntPrimitive, Integer.MAX_VALUE)
            .set(C::getIntegerObject, Integer.MAX_VALUE)
            .set(C::getIntPrimitiveArray, new int[] {Integer.MAX_VALUE, 1, 1})
            .set(C::getIntegerSet, Sets.newHashSet(Integer.MAX_VALUE, Integer.MIN_VALUE))
            .set(C::getIntegerList, Arrays.asList(Integer.MIN_VALUE, Integer.MAX_VALUE))
            .set(C::getIntegerMap, newHashMap().with(Integer.MIN_VALUE, Integer.MAX_VALUE).build())
            .set(C::getLongPrimitive, Long.MAX_VALUE)
            .set(C::getLongObject, Long.MAX_VALUE)
            .set(C::getLongPrimitiveArray, new long[] {1L, Long.MAX_VALUE, Long.MIN_VALUE})
            .set(C::getLongSet, Sets.newHashSet(Long.MIN_VALUE, Long.MAX_VALUE))
            .set(C::getLongList, Arrays.asList(Long.MAX_VALUE, 0L, Long.MIN_VALUE))
            .set(C::getLongMap, newHashMap().with(Long.MAX_VALUE, Long.MIN_VALUE).build())
            .set(C::getFloatPrimitive, Float.MAX_VALUE)
            .set(C::getFloatObject, Float.MAX_VALUE)
            .set(C::getFloatPrimitiveArray, new float[]{1f, Float.MAX_VALUE, Float.MIN_VALUE})
            .set(C::getFloatSet, Sets.newHashSet(Float.MIN_VALUE, Float.MAX_VALUE))
            .set(C::getFloatList, Arrays.asList(Float.MAX_VALUE, 0f, Float.MIN_VALUE))
            .set(C::getFloatMap, newHashMap().with(Float.MAX_VALUE, Float.MIN_VALUE).build())
            .set(C::getDoublePrimitive, Double.MAX_VALUE)
            .set(C::getDoubleObject, Double.MAX_VALUE)
            .set(C::getDoublePrimitiveArray, new double[] {1d, Double.MAX_VALUE, Double.MIN_VALUE})
            .set(C::getDoubleSet, Sets.newHashSet(Double.MIN_VALUE, Double.MAX_VALUE))
            .set(C::getDoubleList, Arrays.asList(Double.MAX_VALUE, 0d, Double.MIN_VALUE))
            .set(C::getDoubleMap, newHashMap().with(Double.MAX_VALUE, Double.MIN_VALUE).build())
            .set(C::getBoolPrimitive, true)
            .set(C::getBooleanObject, true)
            .set(C::getBoolPrimitiveArray, new boolean[] {false, true, false})
            .set(C::getBooleanList, Arrays.asList(true, false))
            .set(C::getBooleanSet, Sets.<Boolean>newHashSet(Boolean.FALSE, Boolean.TRUE))
            .set(C::getBooleanMap, newHashMap().with(true, false).build())
            .set(C::getCharPrimitive, 'g')
            .set(C::getCharacterObject, 'g')
            .set(C::getCharPrimitiveArray, new char[]{'a', Character.MIN_VALUE, Character.MAX_VALUE})
            .set(C::getCharacterSet, Sets.newHashSet(Character.MIN_VALUE, Character.MAX_VALUE))
            .set(C::getCharacterList, Arrays.asList(Character.MAX_VALUE, Character.MIN_VALUE))
            .set(C::getCharacterMap, newHashMap().with(Character.MAX_VALUE, Character.MIN_VALUE).build())
            .set(C::getString, value.length == 0 ? "value" : value[0])
            .set(C::getStringList, Arrays.asList("1a", "2b", "3c"))
            .set(C::getStringSet, Sets.newHashSet("1a", "2b", "3d"))
            .set(C::getStringMap, newHashMap().with("a", "b").build())
            .set(C::getBigDecimal, new BigDecimal("0127392183723987.12938712983723976"))
            .set(C::getBigDecimals, Arrays.asList(new BigDecimal("0127392183723987.12938712983723976"), new BigDecimal("0127392183723987.12938712983723976")))
            .set(C::getBigInteger, new BigInteger("012739218372398712938712983723976"))
            .set(C::getBigIntegers, Arrays.asList(new BigInteger("012739218372398712938712983723976"), new BigInteger("012739218372398712938712983723976")))
            .set(C::getEmbedded, buildEmbedded(value))
            .set(C::getEmbeddedList, Arrays.asList(buildEmbedded(value), buildEmbedded(value), buildEmbedded(value)))
            .set(C::getEmbeddedSet, Sets.newHashSet(buildEmbedded(value), buildEmbedded(value), buildEmbedded(value)))
            .set(C::getEmbeddedMap, newHashMap().with("embedded", buildEmbedded(value)).build())
            .set(C::getEnumValue, TimeUnit.DAYS)
            .set(C::getEnumList, Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS))
            .set(C::getEnumSet, EnumSet.of(TimeUnit.MICROSECONDS, TimeUnit.HOURS))
            .set(C::getEnumMap, newHashMap().with(TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS).build())
            .set(C::getDate, new Date(1))
            .set(C::getDateList, Arrays.asList(new Date(1), new Date(2)))
            .set(C::getLocalDateTime, LocalDateTime.parse("2014-04-01T12:00"))
            .set(C::getLocalDateTimeList, Arrays.asList(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(C::getLocalDateTimeSet, Sets.newHashSet(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(C::getLocalDateTimeMap, newHashMap().with(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")).build())
            .set(C::getZonedDateTime, ZonedDateTime.parse("2007-12-03T10:15:30+01:00"))
            .set(C::getZonedDateTimeList, Arrays.asList(ZonedDateTime.parse("2007-12-03T10:15:30+01:00"), ZonedDateTime.parse("2007-12-03T10:15:30+01:00")))
            .set(C::getLocalDate, LocalDate.parse("2007-12-03"))
            .set(C::getLocalDateList, Arrays.asList(LocalDate.parse("2007-12-03"), LocalDate.parse("2007-12-03")))
            .set(C::getLocalTime, LocalTime.parse("10:15"))
            .set(C::getLocalTimeList, Arrays.asList(LocalTime.parse("10:15"), LocalTime.parse("10:15")))
            .set(C::getInstant, Instant.parse("2013-06-25T16:22:52.966Z"))
            .set(C::getInstantList, Arrays.asList(Instant.parse("2013-06-25T16:22:52.966Z"), Instant.parse("2013-06-25T16:22:52.966Z")))
            .set(C::getPeriod, Period.parse("P1Y2M3D"))
            .set(C::getPeriods, Arrays.asList(Period.parse("P1Y2M3D"), Period.parse("P1Y2M3D")))
            .set(C::getDuration, Duration.parse("PT15M"))
            .set(C::getDurations, Arrays.asList(Duration.parse("PT15M"), Duration.parse("PT15M"))).build().get();
  }

  public static C buildC(String id) {
    return builderC(id)
            .set(C::getId, id)
            .set(C::getBytePrimitive, (byte) 1)
            .set(C::getBytePrimitiveArray, new byte[]{(byte) 1, (byte) 2, (byte) 3})
            .set(C::getByteObject, (byte) 1)
            .set(C::getByteSet, Sets.newHashSet(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(C::getByteList, Arrays.asList(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(C::getByteMap, newHashMap().with(Byte.MAX_VALUE, Byte.MIN_VALUE).build())
            .set(C::getShortPrimitive, (short) 123)
            .set(C::getShortPrimitiveArray, new short[] {(short)1, (short)2, (short)12345})
            .set(C::getShortObject, (short) 1)
            .set(C::getShortSet, Sets.newHashSet(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(C::getShortList, Arrays.asList(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(C::getShortMap, newHashMap().with(Short.MIN_VALUE, Short.MAX_VALUE).build())
            .set(C::getIntPrimitive, Integer.MAX_VALUE)
            .set(C::getIntegerObject, Integer.MAX_VALUE)
            .set(C::getIntPrimitiveArray, new int[] {Integer.MAX_VALUE, 1, 1})
            .set(C::getIntegerSet, Sets.newHashSet(Integer.MAX_VALUE, Integer.MIN_VALUE))
            .set(C::getIntegerList, Arrays.asList(Integer.MIN_VALUE, Integer.MAX_VALUE))
            .set(C::getIntegerMap, newHashMap().with(Integer.MIN_VALUE, Integer.MAX_VALUE).build())
            .set(C::getLongPrimitive, Long.MAX_VALUE)
            .set(C::getLongObject, Long.MAX_VALUE)
            .set(C::getLongPrimitiveArray, new long[] {1L, Long.MAX_VALUE, Long.MIN_VALUE})
            .set(C::getLongSet, Sets.newHashSet(Long.MIN_VALUE, Long.MAX_VALUE))
            .set(C::getLongList, Arrays.asList(Long.MAX_VALUE, 0L, Long.MIN_VALUE))
            .set(C::getLongMap, newHashMap().with(Long.MAX_VALUE, Long.MIN_VALUE).build())
            .set(C::getFloatPrimitive, Float.MAX_VALUE)
            .set(C::getFloatObject, Float.MAX_VALUE)
            .set(C::getFloatPrimitiveArray, new float[]{1f, Float.MAX_VALUE, Float.MIN_VALUE})
            .set(C::getFloatSet, Sets.newHashSet(Float.MIN_VALUE, Float.MAX_VALUE))
            .set(C::getFloatList, Arrays.asList(Float.MAX_VALUE, 0f, Float.MIN_VALUE))
            .set(C::getFloatMap, newHashMap().with(Float.MAX_VALUE, Float.MIN_VALUE).build())
            .set(C::getDoublePrimitive, Double.MAX_VALUE)
            .set(C::getDoubleObject, Double.MAX_VALUE)
            .set(C::getDoublePrimitiveArray, new double[] {1d, Double.MAX_VALUE, Double.MIN_VALUE})
            .set(C::getDoubleSet, Sets.newHashSet(Double.MIN_VALUE, Double.MAX_VALUE))
            .set(C::getDoubleList, Arrays.asList(Double.MAX_VALUE, 0d, Double.MIN_VALUE))
            .set(C::getDoubleMap, newHashMap().with(Double.MAX_VALUE, Double.MIN_VALUE).build())
            .set(C::getBoolPrimitive, true)
            .set(C::getBooleanObject, true)
            .set(C::getBoolPrimitiveArray, new boolean[] {false, true, false})
            .set(C::getBooleanList, Arrays.asList(true, false))
            .set(C::getBooleanSet, Sets.<Boolean>newHashSet(Boolean.FALSE, Boolean.TRUE))
            .set(C::getBooleanMap, newHashMap().with(true, false).build())
            .set(C::getCharPrimitive, 'g')
            .set(C::getCharacterObject, 'g')
            .set(C::getCharPrimitiveArray, new char[]{'a', Character.MIN_VALUE, Character.MAX_VALUE})
            .set(C::getCharacterSet, Sets.newHashSet(Character.MIN_VALUE, Character.MAX_VALUE))
            .set(C::getCharacterList, Arrays.asList(Character.MAX_VALUE, Character.MIN_VALUE))
            .set(C::getCharacterMap, newHashMap().with(Character.MAX_VALUE, Character.MIN_VALUE).build())
            .set(C::getString, "value")
            .set(C::getStringList, Arrays.asList("1a", "2b", "3c"))
            .set(C::getStringSet, Sets.newHashSet("1a", "2b", "3d"))
            .set(C::getStringMap, newHashMap().with("a", "b").build())
            .set(C::getBigDecimal, new BigDecimal("0127392183723987.12938712983723976"))
            .set(C::getBigDecimals, Arrays.asList(new BigDecimal("0127392183723987.12938712983723976"), new BigDecimal("0127392183723987.12938712983723976")))
            .set(C::getBigInteger, new BigInteger("012739218372398712938712983723976"))
            .set(C::getBigIntegers, Arrays.asList(new BigInteger("012739218372398712938712983723976"), new BigInteger("012739218372398712938712983723976")))
            .set(C::getEmbedded, buildEmbedded("value"))
            .set(C::getEmbeddedList, Arrays.asList(buildEmbedded("value"), buildEmbedded("value"), buildEmbedded("value")))
            .set(C::getEmbeddedSet, Sets.newHashSet(buildEmbedded("value"), buildEmbedded("value"), buildEmbedded("value")))
            .set(C::getEmbeddedMap, newHashMap().with("embedded", buildEmbedded("value")).build())
            .set(C::getEnumValue, TimeUnit.DAYS)
            .set(C::getEnumList, Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS))
            .set(C::getEnumSet, EnumSet.of(TimeUnit.MICROSECONDS, TimeUnit.HOURS))
            .set(C::getEnumMap, newHashMap().with(TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS).build())
            .set(C::getDate, new Date(1))
            .set(C::getDateList, Arrays.asList(new Date(1), new Date(2)))
            .set(C::getLocalDateTime, LocalDateTime.parse("2014-04-01T12:00"))
            .set(C::getLocalDateTimeList, Arrays.asList(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(C::getLocalDateTimeSet, Sets.newHashSet(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(C::getLocalDateTimeMap, newHashMap().with(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")).build())
            .set(C::getZonedDateTime, ZonedDateTime.parse("2007-12-03T10:15:30+01:00"))
            .set(C::getZonedDateTimeList, Arrays.asList(ZonedDateTime.parse("2007-12-03T10:15:30+01:00"), ZonedDateTime.parse("2007-12-03T10:15:30+01:00")))
            .set(C::getLocalDate, LocalDate.parse("2007-12-03"))
            .set(C::getLocalDateList, Arrays.asList(LocalDate.parse("2007-12-03"), LocalDate.parse("2007-12-03")))
            .set(C::getLocalTime, LocalTime.parse("10:15"))
            .set(C::getLocalTimeList, Arrays.asList(LocalTime.parse("10:15"), LocalTime.parse("10:15")))
            .set(C::getInstant, Instant.parse("2013-06-25T16:22:52.966Z"))
            .set(C::getInstantList, Arrays.asList(Instant.parse("2013-06-25T16:22:52.966Z"), Instant.parse("2013-06-25T16:22:52.966Z")))
            .set(C::getPeriod, Period.parse("P1Y2M3D"))
            .set(C::getPeriods, Arrays.asList(Period.parse("P1Y2M3D"), Period.parse("P1Y2M3D")))
            .set(C::getDuration, Duration.parse("PT15M"))
            .set(C::getDurations, Arrays.asList(Duration.parse("PT15M"), Duration.parse("PT15M")))
            .build().get();
  }

  public static Builder<C> builderC(String id, String... value) {
    return new Builder<>(C.class);
  }


  public static Value buildEmbedded(String... value) {
    return new Builder<>(Value.class)
            .set(Value::getBytePrimitive, (byte) 1)
            .set(Value::getBytePrimitiveArray, new byte[]{(byte) 1, (byte) 2, (byte) 3})
            .set(Value::getByteObject, (byte) 1)
            .set(Value::getByteSet, Sets.newHashSet(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(Value::getByteList, Arrays.asList(Byte.MAX_VALUE, Byte.MIN_VALUE))
            .set(Value::getByteMap, newHashMap().with(Byte.MAX_VALUE, Byte.MIN_VALUE).build())
            .set(Value::getShortPrimitive, (short) 123)
            .set(Value::getShortPrimitiveArray, new short[] {(short)1, (short)2, (short)12345})
            .set(Value::getShortObject, (short) 1)
            .set(Value::getShortSet, Sets.newHashSet(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(Value::getShortList, Arrays.asList(Short.MIN_VALUE, Short.MAX_VALUE))
            .set(Value::getShortMap, newHashMap().with(Short.MIN_VALUE, Short.MAX_VALUE).build())
            .set(Value::getIntPrimitive, Integer.MAX_VALUE)
            .set(Value::getIntegerObject, Integer.MAX_VALUE)
            .set(Value::getIntPrimitiveArray, new int[] {Integer.MAX_VALUE, 1, 1})
            .set(Value::getIntegerSet, Sets.newHashSet(Integer.MAX_VALUE, Integer.MIN_VALUE))
            .set(Value::getIntegerList, Arrays.asList(Integer.MIN_VALUE, Integer.MAX_VALUE))
            .set(Value::getIntegerMap, newHashMap().with(Integer.MIN_VALUE, Integer.MAX_VALUE).build())
            .set(Value::getLongPrimitive, Long.MAX_VALUE)
            .set(Value::getLongObject, Long.MAX_VALUE)
            .set(Value::getLongPrimitiveArray, new long[] {1L, Long.MAX_VALUE, Long.MIN_VALUE})
            .set(Value::getLongSet, Sets.newHashSet(Long.MIN_VALUE, Long.MAX_VALUE))
            .set(Value::getLongList, Arrays.asList(Long.MAX_VALUE, 0L, Long.MIN_VALUE))
            .set(Value::getLongMap, newHashMap().with(Long.MAX_VALUE, Long.MIN_VALUE).build())
            .set(Value::getFloatPrimitive, Float.MAX_VALUE)
            .set(Value::getFloatObject, Float.MAX_VALUE)
            .set(Value::getFloatPrimitiveArray, new float[]{1f, Float.MAX_VALUE, Float.MIN_VALUE})
            .set(Value::getFloatSet, Sets.newHashSet(Float.MIN_VALUE, Float.MAX_VALUE))
            .set(Value::getFloatList, Arrays.asList(Float.MAX_VALUE, 0f, Float.MIN_VALUE))
            .set(Value::getFloatMap, newHashMap().with(Float.MAX_VALUE, Float.MIN_VALUE).build())
            .set(Value::getDoublePrimitive, Double.MAX_VALUE)
            .set(Value::getDoubleObject, Double.MAX_VALUE)
            .set(Value::getDoublePrimitiveArray, new double[] {1d, Double.MAX_VALUE, Double.MIN_VALUE})
            .set(Value::getDoubleSet, Sets.newHashSet(Double.MIN_VALUE, Double.MAX_VALUE))
            .set(Value::getDoubleList, Arrays.asList(Double.MAX_VALUE, 0d, Double.MIN_VALUE))
            .set(Value::getDoubleMap, newHashMap().with(Double.MAX_VALUE, Double.MIN_VALUE).build())
            .set(Value::getBoolPrimitive, true)
            .set(Value::getBooleanObject, true)
            .set(Value::getBoolPrimitiveArray, new boolean[] {false, true, false})
            .set(Value::getBooleanList, Arrays.asList(true, false))
            .set(Value::getBooleanSet, Sets.<Boolean>newHashSet(Boolean.FALSE, Boolean.TRUE))
            .set(Value::getBooleanMap, newHashMap().with(true, false).build())
            .set(Value::getCharPrimitive, 'g')
            .set(Value::getCharacterObject, 'g')
            .set(Value::getCharPrimitiveArray, new char[]{'a', Character.MIN_VALUE, Character.MAX_VALUE})
            .set(Value::getCharacterSet, Sets.newHashSet(Character.MIN_VALUE, Character.MAX_VALUE))
            .set(Value::getCharacterList, Arrays.asList(Character.MAX_VALUE, Character.MIN_VALUE))
            .set(Value::getCharacterMap, newHashMap().with(Character.MAX_VALUE, Character.MIN_VALUE).build())
            .set(Value::getString, value.length == 0 ? "value" : value[0])
            .set(Value::getStringList, Arrays.asList("1a", "2b", "11"))
            .set(Value::getStringSet, Sets.newHashSet("1a", "ad", "3c"))
            .set(Value::getStringMap, newHashMap().with("aa", "bb").build())
            .set(Value::getBigDecimal, new BigDecimal("0127392183723987.12938712983723976"))
            .set(Value::getBigDecimals, Arrays.asList(new BigDecimal("0127392183723987.12938712983723976"), new BigDecimal("0127392183723987.12938712983723976")))
            .set(Value::getBigInteger, new BigInteger("012739218372398712938712983723976"))
            .set(Value::getBigIntegers, Arrays.asList(new BigInteger("012739218372398712938712983723976"), new BigInteger("012739218372398712938712983723976")))
            .set(Value::getEnumValue, TimeUnit.DAYS)
            .set(Value::getEnumList, Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS))
            .set(Value::getEnumSet, EnumSet.of(TimeUnit.MICROSECONDS, TimeUnit.HOURS))
            .set(Value::getEnumMap, newHashMap().with(TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS).build())
            .set(Value::getDate, new Date(1))
            .set(Value::getDateList, Arrays.asList(new Date(1), new Date(2)))
            .set(Value::getLocalDateTime, LocalDateTime.parse("2014-04-01T12:00"))
            .set(Value::getLocalDateTimeList, Arrays.asList(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(Value::getLocalDateTimeSet, Sets.newHashSet(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .set(Value::getLocalDateTimeMap, newHashMap().with(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")).build())
            .set(Value::getZonedDateTime, ZonedDateTime.parse("2007-12-03T10:15:30+01:00"))
            .set(Value::getZonedDateTimeList, Arrays.asList(ZonedDateTime.parse("2007-12-03T10:15:30+01:00"), ZonedDateTime.parse("2007-12-03T10:15:30+01:00")))
            .set(Value::getLocalDate, LocalDate.parse("2007-12-03"))
            .set(Value::getLocalDateList, Arrays.asList(LocalDate.parse("2007-12-03"), LocalDate.parse("2007-12-03")))
            .set(Value::getLocalTime, LocalTime.parse("10:15"))
            .set(Value::getLocalTimeList, Arrays.asList(LocalTime.parse("10:15"), LocalTime.parse("10:15")))
            .set(Value::getInstant, Instant.parse("2013-06-25T16:22:52.966Z"))
            .set(Value::getInstantList, Arrays.asList(Instant.parse("2013-06-25T16:22:52.966Z"), Instant.parse("2013-06-25T16:22:52.966Z")))
            .set(Value::getPeriod, Period.parse("P1Y2M3D"))
            .set(Value::getPeriods, Arrays.asList(Period.parse("P1Y2M3D"), Period.parse("P1Y2M3D")))
            .set(Value::getDuration, Duration.parse("PT15M"))
            .set(Value::getDurations, Arrays.asList(Duration.parse("PT15M"), Duration.parse("PT15M")))
            .build().get();
  }


  public static LinkedHashMap<String, StandardFields> defaultReferences() {
    LinkedHashMap<String, StandardFields> map = new LinkedHashMap<>();
    A a1 = buildA("a1");
    map.put("a1", a1);
    A a2 = buildA("a2");
    map.put("a2", a2);
    A a3 = buildA("a3");
    map.put("a3", a3);

    B b1 = buildB("b1", a1, Arrays.asList(a2, a3));
    map.put("b1", b1);

    B b2 = buildB("b2", a1, Arrays.asList(a2, a3));
    map.put("b2", b2);

    C c1 = buildC("c1", b1, Arrays.asList(b1, b2));
    map.put("c1", c1);

    C c2 = buildC("c2", b1, Arrays.asList(b1, b2));
    map.put("c2", c2);
    return map;
  }

  public static <K,V> MapBuilder<K,V> newHashMap(){
    return new MapBuilder<K,V>(new HashMap<K,V>());
  }


  public static class MapBuilder<K,V> {

    private Map<K,V> map;


    public MapBuilder(Map<K,V> map) {
      this.map = map;
    }

    public MapBuilder<K,V> with(K key, V value){
      map.put(key, value);
      return this;
    }

    public Map<K,V> build(){
      return map;
    }

  }

}
