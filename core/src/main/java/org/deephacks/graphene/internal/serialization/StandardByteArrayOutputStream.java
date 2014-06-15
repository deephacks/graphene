package org.deephacks.graphene.internal.serialization;

import java.io.ByteArrayOutputStream;

public class StandardByteArrayOutputStream extends ByteArrayOutputStream {

  public StandardByteArrayOutputStream(int size) {
    super(size);
  }

  public StandardByteArrayOutputStream(byte[] bytes) {
    buf = bytes;
  }

  public StandardByteArrayOutputStream copy() {
    return new StandardByteArrayOutputStream(buf.length);
  }

  public StandardByteArrayOutputStream copy(byte[] bytes) {
    return new StandardByteArrayOutputStream(bytes);
  }

  public int getPosition() {
    return count;
  }
}
