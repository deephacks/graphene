package org.deephacks.graphene.internal.serialization;

public class UnsafeBufAllocator implements BufAllocator {

  @Override
  public Buf allocateInput(byte[] in) {
    return new UnsafeByteArrayBuf(new UnsafeByteArrayInputStream(in));
  }

  @Override
  public Buf allocateOutput() {
    return new UnsafeByteArrayBuf(new UnsafeByteArrayOutputStream(256));
  }

  @Override
  public Buf allocateOutput(int size) {
    return new UnsafeByteArrayBuf(new UnsafeByteArrayOutputStream(size));
  }
}
