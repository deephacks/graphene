package org.deephacks.graphene.internal.serialization;

import org.deephacks.graphene.Schema.KeySchema;
import org.deephacks.graphene.Schema.KeySchema.KeyPart;
import org.deephacks.graphene.internal.serialization.ValueSerialization.ValueReader;
import org.deephacks.graphene.internal.serialization.ValueSerialization.ValueWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class KeySerialization {

  public static class KeyReader extends ValueReader {
    private KeySchema schema;

    public KeyReader(Buf buf, KeySchema schema) {
      super(buf);
      this.schema = schema;
    }

    @Override
    public boolean position(String value) {
      KeyPart part = schema.getKeyPart(value);
      if (part == null) {
        return false;
      }
      try {
        buf.setPosition(part.getBytesPosition());
        return true;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public String readStringBytes(int size) {
      try {
        byte[] bytes = buf.readBytes(size);
        int lastNullByteFromEnd = getLastNullByteFromEnd(bytes);
        return new String(bytes, 0, size - (size - lastNullByteFromEnd), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public int getLastNullByteFromEnd(byte[] bytes) {
      int pos = bytes.length - 1;
      for (int i = pos; i >= 0; i--) {
        if (bytes[i] == 0) {
          pos = i;
        } else {
          return pos;
        }
      }
      return pos;
    }
  }

  public static class KeyWriter extends ValueWriter {
    private KeySchema schema;
    private KeyPart keyPartPosition;

    public KeyWriter(Buf values, KeySchema schema) {
      super(values);
      this.schema = schema;
    }

    @Override
    public byte[] getBytes() {
      return values.getByteArray();
    }

    @Override
    public void start(String name) throws IOException {
      keyPartPosition = schema.getKeyPart(name);
      int position = keyPartPosition.getBytesPosition();
      values.setPosition(position);
    }

    @Override
    public void end(String name) throws IOException {
    }

    @Override
    public void writeBytes(byte[] value) throws IOException {
      if (value.length > keyPartPosition.getSize()) {
        throw new IllegalArgumentException("Key " + keyPartPosition.getName() + " is to big " + value.length);
      }
      values.writeBytes(value);
    }

    public void writeStringBytes(String value, int size) throws IOException {
      byte[] bytes = new byte[size];
      byte[] stringBytes = value.getBytes(StandardCharsets.UTF_8);
      if (stringBytes.length > size) {
        throw new IllegalArgumentException("String is bigger than max size " + size + " " + value);
      }
      System.arraycopy(stringBytes, 0, bytes, 0, stringBytes.length);
      values.writeBytes(bytes);
    }
  }
}
