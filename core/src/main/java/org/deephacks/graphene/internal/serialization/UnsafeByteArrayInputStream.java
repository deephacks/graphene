/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.graphene.internal.serialization;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Byte array output stream that uses Unsafe methods to serialize/deserialize
 * much faster
 */
public class UnsafeByteArrayInputStream {
  /** Access to the unsafe class */
  private static final sun.misc.Unsafe UNSAFE;
  static {
    try {
      Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      UNSAFE = (sun.misc.Unsafe) field.get(null);
      // Checkstyle exception due to needing to check if unsafe is allowed
      // CHECKSTYLE: stop IllegalCatch
    } catch (Exception e) {
      // CHECKSTYLE: resume IllegalCatch
      throw new RuntimeException("UnsafeByteArrayOutputStream: Failed to " +
          "get unsafe", e);
    }
  }
  /** Offset of a byte array */
  private static final long BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
  /** Offset of a long array */
  private static final long LONG_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(long[].class);
  /** Offset of a double array */
  private static final long DOUBLE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(double[].class);

  /** Byte buffer */
  private final byte[] buf;
  /** Buffer length */
  private final int bufLength;
  /** Position in the buffer */
  private int pos = 0;

  /**
   * Constructor
   *
   * @param buf Buffer to read from
   */
  public UnsafeByteArrayInputStream(byte[] buf) {
    this.buf = buf;
    this.bufLength = buf.length;
  }

  /**
   * Constructor.
   *
   * @param buf Buffer to read from
   * @param offset Offsetin the buffer to start reading from
   * @param length Max length of the buffer to read
   */
  public UnsafeByteArrayInputStream(byte[] buf, int offset, int length) {
    this.buf = buf;
    this.pos = offset;
    this.bufLength = length;
  }

  public UnsafeByteArrayInputStream copy() {
    return new UnsafeByteArrayInputStream(buf, 0, bufLength);
  }

  public UnsafeByteArrayInputStream copy(byte[] bytes) {
    return new UnsafeByteArrayInputStream(bytes);
  }


  public void position(int pos) {
    this.pos = pos;
  }

  /**
   * How many bytes are still available?
   *
   * @return Number of bytes available
   */
  public int available() {
    return bufLength - pos;
  }

  /**
   * What setPosition in the stream?
   *
   * @return Position
   */
  public int getPos() {
    return pos;
  }

  /**
   * Check whether there are enough remaining bytes for an operation
   *
   * @param requiredBytes Bytes required to read
   * @throws IOException When there are not enough bytes to read
   */
  private void ensureRemaining(int requiredBytes) throws IOException {
    if (bufLength - pos < requiredBytes) {
      throw new IOException("ensureRemaining: Only " + (bufLength - pos) +
          " bytes remaining, trying to read " + requiredBytes);
    }
  }

  public void readFully(byte[] b) throws IOException {
    ensureRemaining(b.length);
    System.arraycopy(buf, pos, b, 0, b.length);
    pos += b.length;
  }

  public void readFully(byte[] b, int off, int len) throws IOException {
    ensureRemaining(len);
    System.arraycopy(buf, pos, b, off, len);
    pos += len;
  }


  public int skipBytes(int n) throws IOException {
    ensureRemaining(n);
    pos += n;
    return n;
  }

  public boolean readBoolean() throws IOException {
    ensureRemaining(UnsafeByteArrayOutputStream.SIZE_OF_BOOLEAN);
    boolean value = UNSAFE.getBoolean(buf,
        BYTE_ARRAY_OFFSET + pos);
    pos += UnsafeByteArrayOutputStream.SIZE_OF_BOOLEAN;
    return value;
  }

  public byte readByte() throws IOException {
    ensureRemaining(UnsafeByteArrayOutputStream.SIZE_OF_BYTE);
    byte value = UNSAFE.getByte(buf,
        BYTE_ARRAY_OFFSET + pos);
    pos += UnsafeByteArrayOutputStream.SIZE_OF_BYTE;
    return value;
  }

  public int readUnsignedByte() throws IOException {
    return (short) (readByte() & 0xFF);
  }


  public short readShort() throws IOException {
    ensureRemaining(UnsafeByteArrayOutputStream.SIZE_OF_SHORT);
    short value = UNSAFE.getShort(buf,
        BYTE_ARRAY_OFFSET + pos);
    pos += UnsafeByteArrayOutputStream.SIZE_OF_SHORT;
    return value;
  }

  public int readUnsignedShort() throws IOException {
    return readShort() & 0xFFFF;
  }

  public char readChar() throws IOException {
    ensureRemaining(UnsafeByteArrayOutputStream.SIZE_OF_CHAR);
    char value = UNSAFE.getChar(buf,
        BYTE_ARRAY_OFFSET + pos);
    pos += UnsafeByteArrayOutputStream.SIZE_OF_CHAR;
    return value;
  }

  public int readInt() throws IOException {
    ensureRemaining(UnsafeByteArrayOutputStream.SIZE_OF_INT);
    int value = UNSAFE.getInt(buf, BYTE_ARRAY_OFFSET + pos);
    pos += UnsafeByteArrayOutputStream.SIZE_OF_INT;
    return value;
  }

  public long readLong() throws IOException {
    ensureRemaining(UnsafeByteArrayOutputStream.SIZE_OF_LONG);
    long value = UNSAFE.getLong(buf,
        BYTE_ARRAY_OFFSET + pos);
    pos += UnsafeByteArrayOutputStream.SIZE_OF_LONG;
    return value;
  }

  public float readFloat() throws IOException {
    ensureRemaining(UnsafeByteArrayOutputStream.SIZE_OF_FLOAT);
    float value = UNSAFE.getFloat(buf,
        BYTE_ARRAY_OFFSET + pos);
    pos += UnsafeByteArrayOutputStream.SIZE_OF_FLOAT;
    return value;
  }

  public double readDouble() throws IOException {
    ensureRemaining(UnsafeByteArrayOutputStream.SIZE_OF_DOUBLE);
    double value = UNSAFE.getDouble(buf,
        BYTE_ARRAY_OFFSET + pos);
    pos += UnsafeByteArrayOutputStream.SIZE_OF_DOUBLE;
    return value;
  }

  /**
   * Get an int at an arbitrary setPosition in a byte[]
   *
   * @param buf Buffer to get the int from
   * @param pos Position in the buffer to get the int from
   * @return Int at the buffer setPosition
   */
  public static int getInt(byte[] buf, int pos) {
    return UNSAFE.getInt(buf, BYTE_ARRAY_OFFSET + pos);
  }
}