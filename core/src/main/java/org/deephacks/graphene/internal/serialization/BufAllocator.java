package org.deephacks.graphene.internal.serialization;

public interface BufAllocator {

  public Buf allocateInput(byte[] in);

  public Buf allocateOutput();

  public Buf allocateOutput(int size);
}
