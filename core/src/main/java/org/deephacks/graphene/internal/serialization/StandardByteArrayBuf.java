package org.deephacks.graphene.internal.serialization;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.deephacks.graphene.internal.serialization.BytesUtils.*;

public class StandardByteArrayBuf extends Buf {
  private StandardByteArrayInputStream in;
  private StandardByteArrayOutputStream out;

  public StandardByteArrayBuf(StandardByteArrayInputStream in) {
    this.in = in;
  }

  public StandardByteArrayBuf(StandardByteArrayOutputStream out) {
    this.out = out;
  }

  @Override
  public Buf setPosition(int position) throws IOException {
    if (in != null) {
      in.position(position);
    }
    return this;
  }

  @Override
  public int getPosition() throws IOException {
    if (in != null) {
      return in.getPosition();
    } else {
      return out.getPosition();
    }
  }

  @Override
  public Buf writeBoolean(boolean o) throws IOException {
    out.write(BytesUtils.toByte(o));
    return this;
  }

  @Override
  public Buf writeByte(byte o) throws IOException {
    out.write(o);
    return this;
  }

  @Override
  public Buf writeShort(short o) throws IOException {
    out.write(Bytes.fromShort(o));
    return this;
  }

  @Override
  public Buf writeChar(char c) throws IOException {
    out.write(Bytes.fromInt(c));
    return this;
  }

  @Override
  public Buf writeInt(int o) throws IOException {
    out.write(Bytes.fromInt(o));
    return this;
  }

  @Override
  public Buf writeLong(long o) throws IOException {
    out.write(Bytes.fromLong(o));
    return this;
  }

  @Override
  public Buf writeFloat(float o) throws IOException {
    out.write(Bytes.fromFloat(o));
    return this;
  }

  @Override
  public Buf writeDouble(double o) throws IOException {
    out.write(Bytes.fromDouble(o));
    return this;
  }

  @Override
  public Buf writeBytes(byte[] o) throws IOException {
    out.write(o);
    return this;
  }

  @Override
  public Buf writeString(String o) throws IOException {
    byte[] bytes = o.getBytes(StandardCharsets.UTF_8);
    out.write(Bytes.fromInt(bytes.length));
    out.write(bytes);
    return this;
  }

  @Override
  public Buf writeObject(Object object) throws IOException {
    Class<?> cls = object.getClass();
    if (Date.class == cls) {
      long time = ((Date) object).getTime();
      writeLong(time);
    } else if (BigDecimal.class == cls) {
      writeString(object.toString());
    } else if (BigInteger.class == cls) {
      writeString(object.toString());
    } else if (object instanceof Enum) {
      writeString(object.toString());
    } else if (LocalDateTime.class == cls) {
      writeBytes(BytesUtils.writeBytes((LocalDateTime) object));
    } else if (ZonedDateTime.class == cls) {
      writeBytes(BytesUtils.writeBytes((ZonedDateTime) object));
    } else if (LocalDate.class == cls) {
      writeBytes(BytesUtils.toBytes((LocalDate) object));
    } else if (LocalTime.class == cls) {
      writeBytes(BytesUtils.toBytes((LocalTime) object));
    } else if (Instant.class == cls) {
      writeBytes(BytesUtils.toBytes((Instant) object));
    } else if (Period.class == cls) {
      writeBytes(BytesUtils.toBytes((Period) object));
    } else if (Duration.class == cls) {
      writeBytes(BytesUtils.toBytes((Duration) object));
    } else {
      throw new IllegalArgumentException("Did not recognize type " + cls);
    }
    return this;
  }

  @Override
  public boolean readBoolean() throws IOException {
    return BytesUtils.getBoolean((byte) in.read());
  }

  @Override
  public byte readByte() throws IOException {
    return (byte) in.read();
  }

  @Override
  public short readShort() throws IOException {
    byte[] bytes = new byte[2];
    in.read(bytes);
    return Bytes.getShort(bytes);
  }

  @Override
  public int readInt() throws IOException {
    byte[] bytes = new byte[4];
    in.read(bytes);
    return Bytes.getInt(bytes);
  }

  @Override
  public long readLong() throws IOException {
    byte[] bytes = new byte[8];
    in.read(bytes);
    return Bytes.getLong(bytes);
  }

  @Override
  public float readFloat() throws IOException {
    byte[] bytes = new byte[4];
    in.read(bytes);
    return BytesUtils.getFloat(bytes, 0);
  }

  @Override
  public double readDouble() throws IOException {
    byte[] bytes = new byte[8];
    in.read(bytes);
    return BytesUtils.getDouble(bytes, 0);
  }

  @Override
  public char readChar() throws IOException {
    byte[] bytes = new byte[4];
    in.read(bytes);
    return (char) Bytes.getInt(bytes);
  }

  @Override
  public String readString() throws IOException {
    int size = readInt();
    byte[] bytes = readBytes(size);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Override
  public byte[] readBytes(int num) throws IOException {
    byte[] bytes = new byte[num];
    in.read(bytes);
    return bytes;
  }

  @Override
  public Object readObject(Class<?> type) throws IOException {
    if (type == Date.class) {
      return new Date(readLong());
    } else if (type == BigDecimal.class) {
      return new BigDecimal(readString());
    } else if (type == BigInteger.class) {
      return new BigInteger(readString());
    } else if (type.isEnum()) {
      return Enum.valueOf((Class) type, readString());
    } else if (type == LocalDateTime.class) {
      return BytesUtils.getLocalDateTime(readBytes(LOCAL_DATE_TIME_BYTES));
    } else if (type == ZonedDateTime.class) {
      return BytesUtils.getZonedDateTime(readBytes(ZONED_DATE_TIME_BYTES));
    } else if (type == LocalDate.class) {
      return BytesUtils.getLocalDate(readBytes(LOCAL_DATE_BYTES), 0);
    } else if (type == LocalTime.class) {
      return BytesUtils.getLocalTime(readBytes(LOCAL_TIME_BYTES), 0);
    } else if (type == Instant.class) {
      return BytesUtils.getInstant(readBytes(INSTANT_BYTES));
    } else if (type == Period.class) {
      return BytesUtils.getPeriod(readBytes(PERIOD_BYTES));
    } else if (type == Duration.class) {
      return BytesUtils.getDuration(readBytes(DURATION_BYTES));
    } else {
      throw new IllegalArgumentException("Did not recognize type " + type);
    }
  }

  @Override
  public byte[] getByteArray() {
    return out.toByteArray();
  }

  @Override
  public Buf copy(byte[] bytes) {
    if (in != null) {
      return new StandardByteArrayBuf(in.copy(bytes));
    } else {
      return new StandardByteArrayBuf(out.copy(bytes));
    }
  }

  @Override
  public Buf copy() {
    if (in != null) {
      return new StandardByteArrayBuf(in.copy());
    } else {
      return new StandardByteArrayBuf(out.copy());
    }
  }

  @Override
  public void close() throws Exception {

  }
}
