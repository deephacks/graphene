package org.deephacks.graphene.internal.serialization;

public class StandardBufAllocator  implements BufAllocator {

  @Override
  public Buf allocateInput(byte[] in) {
    return new StandardByteArrayBuf(new StandardByteArrayInputStream(in));
  }

  @Override
  public Buf allocateOutput() {
    return new StandardByteArrayBuf(new StandardByteArrayOutputStream(256));
  }

  @Override
  public Buf allocateOutput(int size) {
    return new StandardByteArrayBuf(new StandardByteArrayOutputStream(size));
  }
}
