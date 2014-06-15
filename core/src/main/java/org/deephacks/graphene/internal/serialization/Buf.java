package org.deephacks.graphene.internal.serialization;

import java.io.IOException;

public abstract class Buf implements AutoCloseable {

  public abstract Buf setPosition(int position) throws IOException;

  public abstract int getPosition() throws IOException;

  public abstract Buf writeBoolean(boolean o) throws IOException;

  public abstract Buf writeByte(byte o) throws IOException;

  public abstract Buf writeShort(short o) throws IOException;

  public abstract Buf writeChar(char c) throws IOException;

  public abstract Buf writeInt(int o) throws IOException;

  public abstract Buf writeLong(long o) throws IOException;

  public abstract Buf writeFloat(float o) throws IOException;

  public abstract Buf writeDouble(double o) throws IOException;

  public abstract Buf writeBytes(byte[] o) throws IOException;

  public abstract Buf writeString(String o) throws IOException;

  public abstract Buf writeObject(Object o) throws IOException;

  public abstract boolean readBoolean() throws IOException;

  public abstract byte readByte() throws IOException;

  public abstract short readShort() throws IOException;

  public abstract int readInt() throws IOException;

  public abstract long readLong() throws IOException;

  public abstract float readFloat() throws IOException;

  public abstract double readDouble() throws IOException;

  public abstract char readChar() throws IOException;

  public abstract String readString() throws IOException;

  public abstract byte[] readBytes(int num) throws IOException;

  public abstract Object readObject(Class<?> cls) throws IOException;

  public abstract byte[] getByteArray();

  public abstract Buf copy(byte[] bytes);

  public abstract Buf copy();
}