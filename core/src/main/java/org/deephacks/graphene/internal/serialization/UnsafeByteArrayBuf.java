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

public class UnsafeByteArrayBuf extends Buf {
  private UnsafeByteArrayInputStream in;
  private UnsafeByteArrayOutputStream out;

  public UnsafeByteArrayBuf(UnsafeByteArrayInputStream in) {
    this.in = in;
  }

  public UnsafeByteArrayBuf(UnsafeByteArrayOutputStream out) {
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
      return in.getPos();
    } else {
      return out.getPos();
    }
  }

  @Override
  public Buf writeBoolean(boolean o) throws IOException {
    out.writeBoolean(o);
    return this;
  }

  @Override
  public Buf writeByte(byte o) throws IOException {
    out.writeByte(o);
    return this;
  }

  @Override
  public Buf writeShort(short o) throws IOException {
    out.writeShort(o);
    return this;
  }

  @Override
  public Buf writeChar(char c) throws IOException {
    out.writeChar(c);
    return this;
  }

  @Override
  public Buf writeInt(int o) throws IOException {
    out.writeInt(o);
    return this;
  }

  @Override
  public Buf writeLong(long o) throws IOException {
    out.writeLong(o);
    return this;
  }

  @Override
  public Buf writeFloat(float o) throws IOException {
    out.writeFloat(o);
    return this;
  }

  @Override
  public Buf writeDouble(double o) throws IOException {
    out.writeDouble(o);
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
    out.writeInt(bytes.length);
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
    return in.readBoolean();
  }

  @Override
  public byte readByte() throws IOException {
    return in.readByte();
  }

  @Override
  public short readShort() throws IOException {
    return in.readShort();
  }

  @Override
  public int readInt() throws IOException {
    return in.readInt();
  }

  @Override
  public long readLong() throws IOException {
    return in.readLong();
  }

  @Override
  public float readFloat() throws IOException {
    return in.readFloat();
  }

  @Override
  public double readDouble() throws IOException {
    return in.readDouble();
  }

  @Override
  public char readChar() throws IOException {
    return in.readChar();
  }

  @Override
  public String readString() throws IOException {
    int size = in.readInt();
    byte[] bytes = new byte[size];
    in.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Override
  public byte[] readBytes(int num) throws IOException {
    byte[] bytes = new byte[num];
    in.readFully(bytes);
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
      return new UnsafeByteArrayBuf(in.copy(bytes));
    } else {
      return new UnsafeByteArrayBuf(out.copy(bytes));
    }
  }

  @Override
  public Buf copy() {
    if (in != null) {
      return new UnsafeByteArrayBuf(in.copy());
    } else {
      return new UnsafeByteArrayBuf(out.copy());
    }
  }

  @Override
  public void close() throws Exception {

  }
}
