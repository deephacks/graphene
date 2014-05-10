package org.deephacks.graphene;


import org.deephacks.graphene.internal.UniqueIds;
import org.deephacks.vals.VirtualValue;
import org.junit.Before;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.deephacks.graphene.TransactionManager.withTx;

public class BaseTest {
  protected static final EntityRepository repository = new EntityRepository();

  static {
    ShutdownHook.install(new Thread("ShutdownHook") {
      @Override
      public void run() {
        try {
          Graphene.get().get().close();
        } catch (Exception e) {
          throw new RuntimeException();
        }
      }
    });
  }

  @Before
  public void before() {
    withTx(tx -> {
      UniqueIds ids = new UniqueIds();
      repository.deleteAll(C.class);
      repository.deleteAll(B.class);
      repository.deleteAll(A.class);
      ids.deleteAll();
    });
  }

  static class ShutdownHook {

    static void install(final Thread threadToJoin) {
      Thread thread = new ShutdownHookThread(threadToJoin);
      Runtime.getRuntime().addShutdownHook(thread);
    }

    private static class ShutdownHookThread extends Thread {
      private final Thread threadToJoin;

      private ShutdownHookThread(final Thread threadToJoin) {
        super("ShutdownHook: " + threadToJoin.getName());
        this.threadToJoin = threadToJoin;
      }

      @Override
      public void run() {
        shutdown(threadToJoin, 30000);
      }
    }

    public static void shutdown(final Thread t, final long joinwait) {
      if (t == null)
        return;
      t.start();
      while (t.isAlive()) {
        try {
          t.join(joinwait);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public static A buildA(String id, String... value) {
    return new ABuilder()
            .withId(id)
            .withBytePrimitive((byte) 1)
            .withShortPrimitive((short) 123)
            .withIntPrimitive(Integer.MAX_VALUE)
            .withLongPrimitive(Long.MAX_VALUE)
            .withFloatPrimitive(Float.MAX_VALUE)
            .withDoublePrimitive(Double.MAX_VALUE)
            .withBoolPrimitive(true)
            .withCharPrimitive('g')
            .withByteValue((byte) 2)
            .withByteValues(Arrays.asList((byte) 1, (byte) 2, (byte) 3))
            .withShortValue((short) 3)
            .withShortValues(Guavas.newArrayList((short) 1, (short) 2, (short) 3))
            .withIntValue(Integer.MIN_VALUE)
            .withIntValues(Guavas.newArrayList(1, 2, 3))
            .withLongValue(Long.MIN_VALUE)
            .withLongValues(Guavas.newArrayList(1L, 2L, 3L))
            .withFloatValue(Float.MIN_VALUE)
            .withFloatValues(Guavas.newArrayList(Float.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE))
            .withDoubleValue(Double.MIN_VALUE)
            .withDoubleValues(Guavas.newArrayList(Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE))
            .withBigDecimal(new BigDecimal("0127392183723987.12938712983723976"))
            .withBigDecimals(Guavas.newArrayList(new BigDecimal("0127392183723987.12938712983723976"), new BigDecimal("0127392183723987.12938712983723976")))
            .withBigInteger(new BigInteger("012739218372398712938712983723976"))
            .withBigIntegers(Guavas.newArrayList(new BigInteger("012739218372398712938712983723976"), new BigInteger("012739218372398712938712983723976")))
            .withBoolValue(false)
            .withBoolValues(Guavas.newArrayList(true, false, true))
            .withCharValue('q')
            .withCharValues(Guavas.newArrayList('a', 'b', 'c'))
            .withStringValue(value.length == 0 ? "value" : value[0])
            .withStringValues(Guavas.newArrayList("1a", "2b", "3c"))
            .withEmbedded(buildEmbedded())
            .withEmbeddedList(Guavas.newArrayList(buildEmbedded(), buildEmbedded(), buildEmbedded()))
            .withEnumValue(TimeUnit.DAYS)
            .withEnumList(Guavas.newArrayList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS))
            .withDate(new Date(1))
            .withDateList(Guavas.newArrayList(new Date(1), new Date(2)))
            .withLocalDateTime(LocalDateTime.parse("2014-04-01T12:00"))
            .withLocalDateTimeList(Guavas.newArrayList(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .withPeriod(Period.parse("P1Y2M3D"))
            .withPeriods(Guavas.newArrayList(Period.parse("P1Y2M3D"), Period.parse("P1Y2M3D")))
            .withDuration(Duration.parse("PT15M"))
            .withDurations(Guavas.newArrayList(Duration.parse("PT15M"), Duration.parse("PT15M")))
            .build();
  }


  public static B buildB(String id, A a, List<A> listOfA) {
    return builderB(id)
            .withA(a)
            .withListOfA(listOfA)
            .build();
  }

  public static B buildB(String id, String... value) {
    return builderB(id)
            .build();
  }

  public static BBuilder builderB(String id, String... value) {
    return new BBuilder()
            .withId(id)
            .withBytePrimitive((byte) 1)
            .withShortPrimitive((short) 123)
            .withIntPrimitive(Integer.MAX_VALUE)
            .withLongPrimitive(Long.MAX_VALUE)
            .withFloatPrimitive(Float.MAX_VALUE)
            .withDoublePrimitive(Double.MAX_VALUE)
            .withBoolPrimitive(true)
            .withCharPrimitive('g')
            .withByteValue((byte) 2)
            .withByteValues(Arrays.asList((byte) 1, (byte) 2, (byte) 3))
            .withShortValue((short) 3)
            .withShortValues(Guavas.newArrayList((short) 1, (short) 2, (short) 3))
            .withIntValue(Integer.MIN_VALUE)
            .withIntValues(Guavas.newArrayList(1, 2, 3))
            .withLongValue(Long.MIN_VALUE)
            .withLongValues(Guavas.newArrayList(1L, 2L, 3L))
            .withFloatValue(Float.MIN_VALUE)
            .withFloatValues(Guavas.newArrayList(Float.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE))
            .withDoubleValue(Double.MIN_VALUE)
            .withDoubleValues(Guavas.newArrayList(Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE))
            .withBoolValue(false)
            .withBoolValues(Guavas.newArrayList(true, false, true))
            .withCharValue('q')
            .withCharValues(Guavas.newArrayList('a', 'b', 'c'))
            .withStringValue(value.length == 0 ? "value" : value[0])
            .withStringValues(Guavas.newArrayList("1a", "2b", "3c"))
            .withBigDecimal(new BigDecimal("0127392183723987.12938712983723976"))
            .withBigDecimals(Guavas.newArrayList(new BigDecimal("0127392183723987.12938712983723976"), new BigDecimal("0127392183723987.12938712983723976")))
            .withBigInteger(new BigInteger("012739218372398712938712983723976"))
            .withBigIntegers(Guavas.newArrayList(new BigInteger("012739218372398712938712983723976"), new BigInteger("012739218372398712938712983723976")))
            .withEnumValue(TimeUnit.DAYS)
            .withEnumList(Guavas.newArrayList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS))
            .withEmbedded(buildEmbedded())
            .withEmbeddedList(Guavas.newArrayList(buildEmbedded(), buildEmbedded(), buildEmbedded()))
            .withDate(new Date(1))
            .withDateList(Guavas.newArrayList(new Date(1), new Date(2)))
            .withPeriod(Period.parse("P1Y2M3D"))
            .withPeriods(Guavas.newArrayList(Period.parse("P1Y2M3D"), Period.parse("P1Y2M3D")))
            .withDuration(Duration.parse("PT15M"))
            .withDurations(Guavas.newArrayList(Duration.parse("PT15M"), Duration.parse("PT15M")))
            .withLocalDateTime(LocalDateTime.parse("2014-04-01T12:00"))
            .withLocalDateTimeList(Guavas.newArrayList(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")));
  }

  public static C buildC(String id, B b, List<B> listOfB) {
    return builderC(id)
            .withB(b)
            .withListOfB(listOfB)
            .build();
  }

  public static C buildC(String id) {
    return builderC(id)
            .build();
  }

  public static CBuilder builderC(String id) {
    return new CBuilder()
            .withId(id)
            .withBytePrimitive((byte) 1)
            .withShortPrimitive((short) 123)
            .withIntPrimitive(Integer.MAX_VALUE)
            .withLongPrimitive(Long.MAX_VALUE)
            .withFloatPrimitive(Float.MAX_VALUE)
            .withDoublePrimitive(Double.MAX_VALUE)
            .withBoolPrimitive(true)
            .withCharPrimitive('g')
            .withByteValue((byte) 2)
            .withByteValues(Arrays.asList((byte) 1, (byte) 2, (byte) 3))
            .withShortValue((short) 3)
            .withShortValues(Guavas.newArrayList((short) 1, (short) 2, (short) 3))
            .withIntValue(Integer.MIN_VALUE)
            .withIntValues(Guavas.newArrayList(1, 2, 3))
            .withLongValue(Long.MIN_VALUE)
            .withLongValues(Guavas.newArrayList(1L, 2L, 3L))
            .withFloatValue(Float.MIN_VALUE)
            .withFloatValues(Guavas.newArrayList(Float.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE))
            .withDoubleValue(Double.MIN_VALUE)
            .withDoubleValues(Guavas.newArrayList(Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE))
            .withBigDecimal(new BigDecimal("0127392183723987.12938712983723976"))
            .withBigDecimals(Guavas.newArrayList(new BigDecimal("0127392183723987.12938712983723976"), new BigDecimal("0127392183723987.12938712983723976")))
            .withBigInteger(new BigInteger("012739218372398712938712983723976"))
            .withBigIntegers(Guavas.newArrayList(new BigInteger("012739218372398712938712983723976"), new BigInteger("012739218372398712938712983723976")))
            .withBoolValue(false)
            .withBoolValues(Guavas.newArrayList(true, false, true))
            .withCharValue('q')
            .withCharValues(Guavas.newArrayList('a', 'b', 'c'))
            .withStringValue("value")
            .withStringValues(Guavas.newArrayList("1a", "2b", "3c"))
            .withEnumValue(TimeUnit.DAYS)
            .withEnumList(Guavas.newArrayList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS))
            .withEmbedded(buildEmbedded())
            .withEmbeddedList(Guavas.newArrayList(buildEmbedded(), buildEmbedded(), buildEmbedded()))
            .withDate(new Date(1))
            .withDateList(Guavas.newArrayList(new Date(1), new Date(2)))
            .withPeriod(Period.parse("P1Y2M3D"))
            .withPeriods(Guavas.newArrayList(Period.parse("P1Y2M3D"), Period.parse("P1Y2M3D")))
            .withDuration(Duration.parse("PT15M"))
            .withDurations(Guavas.newArrayList(Duration.parse("PT15M"), Duration.parse("PT15M")))
            .withLocalDateTime(LocalDateTime.parse("2014-04-01T12:00"))
            .withLocalDateTimeList(Guavas.newArrayList(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")));
  }


  public static Embedded buildEmbedded() {
    return new EmbeddedBuilder()
            .withId("e")
            .withBytePrimitive((byte) 12)
            .withShortPrimitive((short) 323)
            .withIntPrimitive(Integer.MAX_VALUE)
            .withLongPrimitive(Long.MAX_VALUE)
            .withFloatPrimitive(Float.MAX_VALUE)
            .withDoublePrimitive(Double.MAX_VALUE)
            .withBoolPrimitive(true)
            .withCharPrimitive('g')
            .withByteValue((byte) 2)
            .withByteValues(Arrays.asList((byte) 1, (byte) 2, (byte) 3))
            .withShortValue((short) 3)
            .withShortValues(Guavas.newArrayList((short) 1, (short) 2, (short) 3))
            .withIntValue(Integer.MIN_VALUE)
            .withIntValues(Guavas.newArrayList(1, 2, 3))
            .withLongValue(Long.MIN_VALUE)
            .withLongValues(Guavas.newArrayList(1L, 2L, 3L))
            .withFloatValue(Float.MIN_VALUE)
            .withFloatValues(Guavas.newArrayList(Float.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE))
            .withDoubleValue(Double.MIN_VALUE)
            .withDoubleValues(Guavas.newArrayList(Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE))
            .withBigDecimal(new BigDecimal("0127392183723987.12938712983723976"))
            .withBigDecimals(Guavas.newArrayList(new BigDecimal("0127392183723987.12938712983723976"), new BigDecimal("0127392183723987.12938712983723976")))
            .withBigInteger(new BigInteger("012739218372398712938712983723976"))
            .withBigIntegers(Guavas.newArrayList(new BigInteger("012739218372398712938712983723976"), new BigInteger("012739218372398712938712983723976")))
            .withDoubleValues(Guavas.newArrayList(Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE))
            .withBoolValue(false)
            .withBoolValues(Guavas.newArrayList(true, false, true))
            .withCharValue('q')
            .withCharValues(Guavas.newArrayList('a', 'b', 'c'))
            .withStringValue("value")
            .withStringValues(Guavas.newArrayList("1a", "2b", "3c"))
            .withEnumValue(TimeUnit.DAYS)
            .withEnumList(Guavas.newArrayList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS))
            .withPeriod(Period.parse("P1Y2M3D"))
            .withPeriods(Guavas.newArrayList(Period.parse("P1Y2M3D"), Period.parse("P1Y2M3D")))
            .withDuration(Duration.parse("PT15M"))
            .withDurations(Guavas.newArrayList(Duration.parse("PT15M"), Duration.parse("PT15M")))
            .withDate(new Date(1))
            .withDateList(Guavas.newArrayList(new Date(1), new Date(2)))
            .withLocalDateTime(LocalDateTime.parse("2014-04-01T12:00"))
            .withLocalDateTimeList(Guavas.newArrayList(LocalDateTime.parse("2014-04-01T12:00"), LocalDateTime.parse("2014-04-02T12:00")))
            .build();
  }


  public static LinkedHashMap<String, StandardProperties> defaultReferences() {
    LinkedHashMap<String, StandardProperties> map = new LinkedHashMap<>();
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

  public static interface StandardProperties {
    String getId();

    byte getBytePrimitive();

    short getShortPrimitive();

    int getIntPrimitive();

    long getLongPrimitive();

    float getFloatPrimitive();

    double getDoublePrimitive();

    boolean getBoolPrimitive();

    char getCharPrimitive();

    Byte getByteValue();

    List<Byte> getByteValues();

    Short getShortValue();

    List<Short> getShortValues();

    Integer getIntValue();

    List<Integer> getIntValues();

    Long getLongValue();

    List<Long> getLongValues();

    Float getFloatValue();

    List<Float> getFloatValues();

    Double getDoubleValue();

    List<Double> getDoubleValues();

    BigDecimal getBigDecimal();

    List<BigDecimal> getBigDecimals();

    BigInteger getBigInteger();

    List<BigInteger> getBigIntegers();

    Boolean getBoolValue();

    List<Boolean> getBoolValues();

    Character getCharValue();

    List<Character> getCharValues();

    String getStringValue();

    List<String> getStringValues();

    TimeUnit getEnumValue();

    List<TimeUnit> getEnumList();

    Date getDate();

    List<Date> getDateList();

    LocalDateTime getLocalDateTime();

    List<LocalDateTime> getLocalDateTimeList();

    Period getPeriod();

    List<Period> getPeriods();

    Duration getDuration();

    List<Duration> getDurations();
  }

  @VirtualValue
  public static interface A extends StandardProperties {
    @Override
    @Id
    String getId();

    Embedded getEmbedded();
    List<Embedded> getEmbeddedList();
  }

  @VirtualValue
  public static interface B extends StandardProperties {

    @Override
    @Id
    String getId();

    @Nullable
    A getA();

    @Nullable
    List<A> getListOfA();

    Embedded getEmbedded();
    List<Embedded> getEmbeddedList();
  }

  @VirtualValue
  public static interface C extends StandardProperties {

    @Override
    @Id
    String getId();

    @Nullable
    B getB();

    @Nullable
    public List<B> getListOfB();

    Embedded getEmbedded();
    List<Embedded> getEmbeddedList();
  }

  @VirtualValue
  @org.deephacks.graphene.Embedded
  public static interface Embedded extends StandardProperties {
  }
}
