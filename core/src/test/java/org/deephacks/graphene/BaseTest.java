package org.deephacks.graphene;

import com.google.common.collect.Lists;
import org.deephacks.graphene.internal.UniqueIds;
import org.joda.time.DateTime;
import org.junit.Before;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

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
        repository.deleteAll(C.class);
        repository.deleteAll(B.class);
        repository.deleteAll(A.class);
        UniqueIds ids = new UniqueIds();
        ids.deleteAll();
        repository.commit();
        assertThat(repository.countAll(), is(0L));
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

    public static <T extends A> T defaultValues(String id, Class<T> cls) {
        final A a;
        try {
            a = (A) cls.getConstructor(String.class).newInstance(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        a.embedded = embedded();
        a.embeddedList = Lists.newArrayList(embedded(), embedded(), embedded());

        a.bytePrimitive = (byte) 1;
        a.shortPrimitive = 123;
        a.intPrimitive = Integer.MAX_VALUE;
        a.longPrimitive = Long.MAX_VALUE;
        a.floatPrimitive = Float.MAX_VALUE;
        a.doublePrimitive = Double.MAX_VALUE;
        a.boolPrimitive = true;
        a.charPrimitive = 'g';

        a.enumValue = TimeUnit.DAYS;
        a.enumValues = Lists.newArrayList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS);

        // FIXME
        // a.dateTime = new DateTime("2013-09-28");
        // a.dateTimes = Lists.newArrayList(new DateTime("2013-09-28"), new DateTime("2013-09-30"), new DateTime("2013-01-28"));

        a.byteValue = true;
        a.byteValues = Lists.newArrayList(true, false, true);

        a.shortValue = 123;
        a.shortValues = Lists.newArrayList((short) 1, (short) 2, (short) 3);

        a.intValue = Integer.MIN_VALUE;
        a.intValues = Lists.newArrayList(1, 2, 3);

        a.longValue = Long.MIN_VALUE;
        a.longValues = Lists.newArrayList(Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);

        a.floatValue = Float.MIN_VALUE;
        a.floatValues = Lists.newArrayList(Float.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE);

        a.doubleValue = Double.MIN_VALUE;
        a.doubleValues = Lists.newArrayList(Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE);

        a.boolValue = false;
        a.boolValues = Lists.newArrayList(true, false, true);

        a.charValue = 'q';
        a.charValues = Lists.newArrayList('a', 'b', 'c');

        a.stringValue = "value";
        a.stringValues = Lists.newArrayList("1a", "2b", "3c");

        return (T) a;
    }


    public static Embedded embedded() {
        Embedded embedded = new Embedded();
        embedded.bytePrimitive = (byte) 1;
        embedded.shortPrimitive = 123;
        embedded.intPrimitive = Integer.MAX_VALUE;
        embedded.longPrimitive = Long.MAX_VALUE;
        embedded.floatPrimitive = Float.MAX_VALUE;
        embedded.doublePrimitive = Double.MAX_VALUE;
        embedded.boolPrimitive = true;
        embedded.charPrimitive = 'g';

        embedded.enumValue = TimeUnit.DAYS;
        embedded.enumValues = Lists.newArrayList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MILLISECONDS);

        // FIXME
        // embedded.dateTime = new DateTime("2013-09-28");
        // embedded.dateTimes = Lists.newArrayList(new DateTime("2013-09-28"), new DateTime("2013-09-30"), new DateTime("2013-01-28"));

        embedded.byteValue = true;
        embedded.byteValues = Lists.newArrayList(true, false, true);

        embedded.shortValue = 123;
        embedded.shortValues = Lists.newArrayList((short) 1, (short) 2, (short) 3);

        embedded.intValue = Integer.MIN_VALUE;
        embedded.intValues = Lists.newArrayList(1, 2, 3);

        embedded.longValue = Long.MIN_VALUE;
        embedded.longValues = Lists.newArrayList(Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);

        embedded.floatValue = Float.MIN_VALUE;
        embedded.floatValues = Lists.newArrayList(Float.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE);

        embedded.doubleValue = Double.MIN_VALUE;
        embedded.doubleValues = Lists.newArrayList(Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE);

        embedded.boolValue = false;
        embedded.boolValues = Lists.newArrayList(true, false, true);

        embedded.charValue = 'q';
        embedded.charValues = Lists.newArrayList('a', 'b', 'c');

        embedded.stringValue = "value";
        embedded.stringValues = Lists.newArrayList("1a", "2b", "3c");

        return embedded;
    }


    public static LinkedHashMap<String, A> defaultReferences() {
        LinkedHashMap<String, A> map = new LinkedHashMap<>();
        A a1 = defaultValues("a1", A.class);
        map.put("a1", a1);
        A a2 = defaultValues("a2", A.class);
        map.put("a2", a2);
        A a3 = defaultValues("a3", A.class);
        map.put("a3", a3);

        B b1 = defaultValues("b1", B.class);
        b1.setA(a1);
        b1.setAvalues(Arrays.asList(a2, a3));
        map.put("b1", b1);

        B b2 = defaultValues("b2", B.class);
        b2.setA(a1);
        b2.setAvalues(Arrays.asList(a2, a3));
        map.put("b2", b2);

        C c1 = defaultValues("c1", C.class);
        c1.setB(b1);
        c1.setBvalues(Arrays.asList(b1, b2));
        map.put("c1", c1);

        C c2 = defaultValues("c2", C.class);
        c2.setB(b1);
        c2.setBvalues(Arrays.asList(b1, b2));
        map.put("c2", c2);

        return map;
    }

    @Entity
    public static class A {
        @Id
        private String id;

        @org.deephacks.graphene.Embedded
        private Embedded embedded;

        @org.deephacks.graphene.Embedded
        private List<Embedded> embeddedList;

        private byte bytePrimitive;
        private short shortPrimitive;
        private int intPrimitive;
        private long longPrimitive;
        private float floatPrimitive;
        private double doublePrimitive;
        private boolean boolPrimitive;
        private char charPrimitive;

        private TimeUnit enumValue;
        private List<TimeUnit> enumValues;

        private DateTime dateTime;
        private List<DateTime> dateTimes;

        private Boolean byteValue;
        private List<Boolean> byteValues;

        private Short shortValue;
        private List<Short> shortValues;

        private Integer intValue;
        private List<Integer> intValues;

        private Long longValue;
        private List<Long> longValues;

        private Float floatValue;
        private List<Float> floatValues;

        private Double doubleValue;
        private List<Double> doubleValues;

        private Boolean boolValue;
        private List<Boolean> boolValues;

        private Character charValue;
        private List<Character> charValues;

        private String stringValue;
        private List<String> stringValues;

        public A(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public byte getBytePrimitive() {
            return bytePrimitive;
        }

        public void setBytePrimitive(byte bytePrimitive) {
            this.bytePrimitive = bytePrimitive;
        }

        public short getShortPrimitive() {
            return shortPrimitive;
        }

        public void setShortPrimitive(short shortPrimitive) {
            this.shortPrimitive = shortPrimitive;
        }

        public int getIntPrimitive() {
            return intPrimitive;
        }

        public void setIntPrimitive(int intPrimitive) {
            this.intPrimitive = intPrimitive;
        }

        public long getLongPrimitive() {
            return longPrimitive;
        }

        public void setLongPrimitive(long longPrimitive) {
            this.longPrimitive = longPrimitive;
        }

        public float getFloatPrimitive() {
            return floatPrimitive;
        }

        public void setFloatPrimitive(float floatPrimitive) {
            this.floatPrimitive = floatPrimitive;
        }

        public double getDoublePrimitive() {
            return doublePrimitive;
        }

        public void setDoublePrimitive(double doublePrimitive) {
            this.doublePrimitive = doublePrimitive;
        }

        public boolean isBoolPrimitive() {
            return boolPrimitive;
        }

        public void setBoolPrimitive(boolean boolPrimitive) {
            this.boolPrimitive = boolPrimitive;
        }

        public char getCharPrimitive() {
            return charPrimitive;
        }

        public void setCharPrimitive(char charPrimitive) {
            this.charPrimitive = charPrimitive;
        }

        public Boolean getByteValue() {
            return byteValue;
        }

        public void setByteValue(Boolean byteValue) {
            this.byteValue = byteValue;
        }

        public List<Boolean> getByteValues() {
            return byteValues;
        }

        public void setByteValues(List<Boolean> byteValues) {
            this.byteValues = byteValues;
        }

        public Short getShortValue() {
            return shortValue;
        }

        public void setShortValue(Short shortValue) {
            this.shortValue = shortValue;
        }

        public List<Short> getShortValues() {
            return shortValues;
        }

        public void setShortValues(List<Short> shortValues) {
            this.shortValues = shortValues;
        }

        public Integer getIntValue() {
            return intValue;
        }

        public void setIntValue(Integer intValue) {
            this.intValue = intValue;
        }

        public List<Integer> getIntValues() {
            return intValues;
        }

        public void setIntValues(List<Integer> intValues) {
            this.intValues = intValues;
        }

        public Long getLongValue() {
            return longValue;
        }

        public void setLongValue(Long longValue) {
            this.longValue = longValue;
        }

        public List<Long> getLongValues() {
            return longValues;
        }

        public void setLongValues(List<Long> longValues) {
            this.longValues = longValues;
        }

        public Float getFloatValue() {
            return floatValue;
        }

        public void setFloatValue(Float floatValue) {
            this.floatValue = floatValue;
        }

        public List<Float> getFloatValues() {
            return floatValues;
        }

        public void setFloatValues(List<Float> floatValues) {
            this.floatValues = floatValues;
        }

        public Double getDoubleValue() {
            return doubleValue;
        }

        public void setDoubleValue(Double doubleValue) {
            this.doubleValue = doubleValue;
        }

        public List<Double> getDoubleValues() {
            return doubleValues;
        }

        public void setDoubleValues(List<Double> doubleValues) {
            this.doubleValues = doubleValues;
        }

        public Boolean getBoolValue() {
            return boolValue;
        }

        public void setBoolValue(Boolean boolValue) {
            this.boolValue = boolValue;
        }

        public List<Boolean> getBoolValues() {
            return boolValues;
        }

        public void setBoolValues(List<Boolean> boolValues) {
            this.boolValues = boolValues;
        }

        public Character getCharValue() {
            return charValue;
        }

        public void setCharValue(Character charValue) {
            this.charValue = charValue;
        }

        public List<Character> getCharValues() {
            return charValues;
        }

        public void setCharValues(List<Character> charValues) {
            this.charValues = charValues;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public List<String> getStringValues() {
            return stringValues;
        }

        public void setStringValues(List<String> stringValues) {
            this.stringValues = stringValues;
        }

        @Override
        public String toString() {
            return "{" + id + "}";
        }
    }

    @Entity
    public static class B extends A {

        private A a;

        private List<A> aValues;

        public B(String id) {
            super(id);
        }

        public A getA() {
            return a;
        }

        public void setA(A a) {
            this.a = a;
        }

        public List<A> getAvalues() {
            return aValues;
        }

        public void setAvalues(List<A> aValues) {
            this.aValues = aValues;
        }
    }

    @Entity
    public static class C extends A {

        private B b;

        private List<B> bValues;

        public C(String id) {
            super(id);
        }

        public B getB() {
            return b;
        }

        public void setB(B b) {
            this.b = b;
        }

        public List<B> getBvalues() {
            return bValues;
        }

        public void setBvalues(List<B> bValues) {
            this.bValues = bValues;
        }
    }


    public static class Embedded {

        private byte bytePrimitive;
        private short shortPrimitive;
        private int intPrimitive;
        private long longPrimitive;
        private float floatPrimitive;
        private double doublePrimitive;
        private boolean boolPrimitive;
        private char charPrimitive;

        private TimeUnit enumValue;
        private List<TimeUnit> enumValues;

        private DateTime dateTime;
        private List<DateTime> dateTimes;

        private Boolean byteValue;
        private List<Boolean> byteValues;

        private Short shortValue;
        private List<Short> shortValues;

        private Integer intValue;
        private List<Integer> intValues;

        private Long longValue;
        private List<Long> longValues;

        private Float floatValue;
        private List<Float> floatValues;

        private Double doubleValue;
        private List<Double> doubleValues;

        private Boolean boolValue;
        private List<Boolean> boolValues;

        private Character charValue;
        private List<Character> charValues;

        private String stringValue;
        private List<String> stringValues;


        public byte getBytePrimitive() {
            return bytePrimitive;
        }

        public void setBytePrimitive(byte bytePrimitive) {
            this.bytePrimitive = bytePrimitive;
        }

        public short getShortPrimitive() {
            return shortPrimitive;
        }

        public void setShortPrimitive(short shortPrimitive) {
            this.shortPrimitive = shortPrimitive;
        }

        public int getIntPrimitive() {
            return intPrimitive;
        }

        public void setIntPrimitive(int intPrimitive) {
            this.intPrimitive = intPrimitive;
        }

        public long getLongPrimitive() {
            return longPrimitive;
        }

        public void setLongPrimitive(long longPrimitive) {
            this.longPrimitive = longPrimitive;
        }

        public float getFloatPrimitive() {
            return floatPrimitive;
        }

        public void setFloatPrimitive(float floatPrimitive) {
            this.floatPrimitive = floatPrimitive;
        }

        public double getDoublePrimitive() {
            return doublePrimitive;
        }

        public void setDoublePrimitive(double doublePrimitive) {
            this.doublePrimitive = doublePrimitive;
        }

        public boolean isBoolPrimitive() {
            return boolPrimitive;
        }

        public void setBoolPrimitive(boolean boolPrimitive) {
            this.boolPrimitive = boolPrimitive;
        }

        public char getCharPrimitive() {
            return charPrimitive;
        }

        public void setCharPrimitive(char charPrimitive) {
            this.charPrimitive = charPrimitive;
        }

        public Boolean getByteValue() {
            return byteValue;
        }

        public void setByteValue(Boolean byteValue) {
            this.byteValue = byteValue;
        }

        public List<Boolean> getByteValues() {
            return byteValues;
        }

        public void setByteValues(List<Boolean> byteValues) {
            this.byteValues = byteValues;
        }

        public Short getShortValue() {
            return shortValue;
        }

        public void setShortValue(Short shortValue) {
            this.shortValue = shortValue;
        }

        public List<Short> getShortValues() {
            return shortValues;
        }

        public void setShortValues(List<Short> shortValues) {
            this.shortValues = shortValues;
        }

        public Integer getIntValue() {
            return intValue;
        }

        public void setIntValue(Integer intValue) {
            this.intValue = intValue;
        }

        public List<Integer> getIntValues() {
            return intValues;
        }

        public void setIntValues(List<Integer> intValues) {
            this.intValues = intValues;
        }

        public Long getLongValue() {
            return longValue;
        }

        public void setLongValue(Long longValue) {
            this.longValue = longValue;
        }

        public List<Long> getLongValues() {
            return longValues;
        }

        public void setLongValues(List<Long> longValues) {
            this.longValues = longValues;
        }

        public Float getFloatValue() {
            return floatValue;
        }

        public void setFloatValue(Float floatValue) {
            this.floatValue = floatValue;
        }

        public List<Float> getFloatValues() {
            return floatValues;
        }

        public void setFloatValues(List<Float> floatValues) {
            this.floatValues = floatValues;
        }

        public Double getDoubleValue() {
            return doubleValue;
        }

        public void setDoubleValue(Double doubleValue) {
            this.doubleValue = doubleValue;
        }

        public List<Double> getDoubleValues() {
            return doubleValues;
        }

        public void setDoubleValues(List<Double> doubleValues) {
            this.doubleValues = doubleValues;
        }

        public Boolean getBoolValue() {
            return boolValue;
        }

        public void setBoolValue(Boolean boolValue) {
            this.boolValue = boolValue;
        }

        public List<Boolean> getBoolValues() {
            return boolValues;
        }

        public void setBoolValues(List<Boolean> boolValues) {
            this.boolValues = boolValues;
        }

        public Character getCharValue() {
            return charValue;
        }

        public void setCharValue(Character charValue) {
            this.charValue = charValue;
        }

        public List<Character> getCharValues() {
            return charValues;
        }

        public void setCharValues(List<Character> charValues) {
            this.charValues = charValues;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public List<String> getStringValues() {
            return stringValues;
        }

        public void setStringValues(List<String> stringValues) {
            this.stringValues = stringValues;
        }

    }


}
