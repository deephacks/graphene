package org.deephacks.confit.internal.berkeley;

import com.google.common.collect.Lists;
import org.deephacks.graphene.Id;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestData {

    public static <T extends A> T defaultValues(String id, Class<T> cls) {
        final A a;
        try {
            a = (A) cls.getConstructor(String.class).newInstance(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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

    public static class A {
        @Id
        private String id;

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
            return "A{" + id + "}";
        }
    }

    public static class B extends A {

        public B(String id) {
            super(id);
        }

    }

    public static class C extends A {

        public C(String id) {
            super(id);
        }
    }
}
