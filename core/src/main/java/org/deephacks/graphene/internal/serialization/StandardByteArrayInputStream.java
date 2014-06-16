package org.deephacks.graphene.internal.serialization;

import java.io.ByteArrayInputStream;

public class StandardByteArrayInputStream extends ByteArrayInputStream {

  public StandardByteArrayInputStream(byte[] buf) {
    super(buf);
  }

  public StandardByteArrayInputStream(byte buf[], int offset, int length) {
    super(buf, offset, length);
  }

  public void position(int position) {
    this.pos = position;
  }

  public int getPosition() {
    return pos;
  }

  public StandardByteArrayInputStream copy() {
    return new StandardByteArrayInputStream(buf, 0, buf.length);
  }

  public StandardByteArrayInputStream copy(byte[] bytes) {
    return new StandardByteArrayInputStream(bytes);
  }
}
