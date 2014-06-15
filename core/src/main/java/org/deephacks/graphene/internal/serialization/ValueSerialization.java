package org.deephacks.graphene.internal.serialization;

import org.deephacks.graphene.UniqueIds;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class ValueSerialization {

  public static class ValueReader {
    protected Buf buf;
    protected UniqueIds uniqueIds;
    private int headerLength = -1;
    private HashSet<String> readValues = new HashSet<>();
    private HashMap<String, Integer> valuePositions = new HashMap<>();

    protected ValueReader(Buf buf) {
      this.buf = buf;
    }

    public ValueReader(Buf buf, UniqueIds uniqueIds) {
      this.uniqueIds = uniqueIds;
      this.buf = buf;
    }

    private void readHeader() {
      if (headerLength != -1) {
        return;
      }
      try {
        this.headerLength = Bytes.getInt(buf.readBytes(4));
        int numValues = Bytes.getInt(buf.readBytes(4));
        for (int i = 0; i < numValues; i++) {
          int id = buf.readInt();
          int pos = buf.readInt();
          String name = uniqueIds.getSchemaName(id);
          valuePositions.put(name, pos);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public ValueReader copy(byte[] bytes) {
      return new ValueReader(this.buf.copy(bytes), uniqueIds);
    }

    public boolean hasRead(String value) {
      boolean hasRead = readValues.contains(value);
      if (!hasRead) {
        readValues.add(value);
        return false;
      }
      return true;
    }

    public boolean position(String value) {
      readHeader();
      Integer position = valuePositions.get(value);
      if (position == null) {
        return false;
      }
      try {
        buf.setPosition(position + headerLength);
        return true;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public boolean readBoolean() {
      try {
        return buf.readBoolean();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public byte readByte() {
      try {
        return buf.readByte();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public byte[] readBytes(int size) {
      try {
        return buf.readBytes(size);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public short readShort() {
      try {
        return buf.readShort();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public int readInt() {
      try {
        return buf.readInt();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public long readLong() {
      try {
        return buf.readLong();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public float readFloat() {
      try {
        return buf.readFloat();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public double readDouble() {
      try {
        return buf.readDouble();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public char readChar() {
      try {
        return buf.readChar();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public String readString() {
      try {
        return buf.readString();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }


    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls) {
      try {
        return (T) buf.readObject(cls);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class ValueWriter {

    protected Buf values;
    protected UniqueIds uniqueIds;

    private Buf header;
    private int position = 0;
    private int numValues = 0;

    protected ValueWriter(Buf values) {
      this.values = values;
    }

    public ValueWriter(Buf values, UniqueIds uniqueIds) {
      this.values = values;
      this.uniqueIds = uniqueIds;
    }

    public ValueWriter(Buf values, Buf header, UniqueIds uniqueIds) {
      this.values = values;
      this.header = header;
      this.uniqueIds = uniqueIds;
    }

    public ValueWriter copy() {
      return new ValueWriter(values.copy(), header.copy(), uniqueIds);
    }

    public void start(String name) throws IOException {
      position = values.getPosition();
    }

    public void end(String name) throws IOException {
      if (position == values.getPosition()) {
        // no value was written
        return;
      }
      Integer id = uniqueIds.getSchemaId(name);
      header.writeInt(id);
      header.writeInt(position);
      numValues++;
    }

    public byte[] getBytes() {
      byte[] valuesBytes = values.getByteArray();
      byte[] headerBytes = header.getByteArray();
      byte[] result = new byte[4 + 4 + headerBytes.length + valuesBytes.length];
      Bytes.setInt(result, result.length - valuesBytes.length);
      Bytes.setInt(result, numValues, 4);
      System.arraycopy(headerBytes, 0, result, 8, headerBytes.length);
      System.arraycopy(valuesBytes, 0, result, 8 + headerBytes.length, valuesBytes.length);
      return result;
    }

    public void writeBoolean(boolean value) throws IOException {
      values.writeBoolean(value);
    }

    public void writeChar(char value) throws IOException {
      values.writeChar(value);
    }

    public void writeByte(byte value) throws IOException {
      values.writeByte(value);
    }

    public void writeBytes(byte[] value) throws IOException {
      values.writeBytes(value);
    }

    public void writeShort(short value) throws IOException {
      values.writeShort(value);
    }

    public void writeInt(int value) throws IOException {
      values.writeInt(value);
    }

    public void writeLong(long value) throws IOException {
      values.writeLong(value);
    }

    public void writeFloat(float value) throws IOException {
      values.writeFloat(value);
    }

    public void writeDouble(double value) throws IOException {
      values.writeDouble(value);
    }

    public void writeString(String value) throws IOException {
      values.writeString(value);
    }

    public void writeObject(Object value) throws IOException {
      values.writeObject(value);
    }
  }
}
