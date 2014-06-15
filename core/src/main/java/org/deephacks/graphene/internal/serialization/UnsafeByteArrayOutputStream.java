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
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Byte array output stream that uses Unsafe methods to serialize/deserialize
 * much faster
 */
public class UnsafeByteArrayOutputStream extends OutputStream  {
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

  /** Bytes used in a boolean */
  public static final int SIZE_OF_BOOLEAN = 1;
  /** Bytes used in a byte */
  public static final int SIZE_OF_BYTE = 1;
  /** Bytes used in a char */
  public static final int SIZE_OF_CHAR = 2;
  /** Bytes used in a short */
  public static final int SIZE_OF_SHORT = 2;
  /** Bytes used in a medium */
  public static final int SIZE_OF_MEDIUM = 3;
  /** Bytes used in an int */
  public static final int SIZE_OF_INT = 4;
  /** Bytes used in a float */
  public static final int SIZE_OF_FLOAT = 4;
  /** Bytes used in a long */
  public static final int SIZE_OF_LONG = 8;
  /** Bytes used in a double */
  public static final int SIZE_OF_DOUBLE = 8;
  /** Default number of bytes */
  private static final int DEFAULT_BYTES = 32;
  /** Access to the unsafe class */
  private static final sun.misc.Unsafe UNSAFE;

  /** Offset of a byte array */
  private static final long BYTE_ARRAY_OFFSET  =
      UNSAFE.arrayBaseOffset(byte[].class);
  /** Offset of a long array */
  private static final long LONG_ARRAY_OFFSET =
      UNSAFE.arrayBaseOffset(long[].class);
  /** Offset of a double array */
  private static final long DOUBLE_ARRAY_OFFSET =
      UNSAFE.arrayBaseOffset(double[].class);

  /** Byte buffer */
  private byte[] buf;
  /** Position in the buffer */
  private int pos = 0;

  /**
   * Constructor
   */
  public UnsafeByteArrayOutputStream() {
    this(DEFAULT_BYTES);
  }

  /**
   * Constructor
   *
   * @param size Initial size of the underlying byte array
   */
  public UnsafeByteArrayOutputStream(int size) {
    buf = new byte[size];
  }

  /**
   * Constructor to take in a buffer
   *
   * @param buf Buffer to start with, or if null, create own buffer
   */
  public UnsafeByteArrayOutputStream(byte[] buf) {
    if (buf == null) {
      this.buf = new byte[DEFAULT_BYTES];
    } else {
      this.buf = buf;
    }
  }

  /**
   * Constructor to take in a buffer with a given setPosition into that buffer
   *
   * @param buf Buffer to start with
   * @param pos Position to write at the buffer
   */
  public UnsafeByteArrayOutputStream(byte[] buf, int pos) {
    this(buf);
    this.pos = pos;
  }

  public UnsafeByteArrayOutputStream copy() {
    return new UnsafeByteArrayOutputStream(buf.length);
  }

  public UnsafeByteArrayOutputStream copy(byte[] bytes) {
    return new UnsafeByteArrayOutputStream(bytes);
  }

  /**
   * Ensure that this buffer has enough remaining space to add the size.
   * Creates and copies to a new buffer if necessary
   *
   * @param size Size to add
   */
  private void ensureSize(int size) {
    if (pos + size > buf.length) {
      byte[] newBuf = new byte[(buf.length + size) << 1];
      System.arraycopy(buf, 0, newBuf, 0, pos);
      buf = newBuf;
    }
  }

  public byte[] getByteArray() {
    return buf;
  }

  public byte[] toByteArray() {
    return Arrays.copyOf(buf, pos);

  }

  public void reset() {
    pos = 0;
  }

  public int getPos() {
    return pos;
  }

  @Override
  public void write(int b) throws IOException {
    ensureSize(SIZE_OF_BYTE);
    buf[pos] = (byte) b;
    pos += SIZE_OF_BYTE;
  }

  @Override
  public void write(byte[] b) throws IOException {
    ensureSize(b.length);
    System.arraycopy(b, 0, buf, pos, b.length);
    pos += b.length;
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    ensureSize(len);
    System.arraycopy(b, off, buf, pos, len);
    pos += len;
  }

  public void writeBoolean(boolean v) throws IOException {
    ensureSize(SIZE_OF_BOOLEAN);
    UNSAFE.putBoolean(buf, BYTE_ARRAY_OFFSET + pos, v);
    pos += SIZE_OF_BOOLEAN;
  }

  public void writeByte(int v) throws IOException {
    ensureSize(SIZE_OF_BYTE);
    UNSAFE.putByte(buf, BYTE_ARRAY_OFFSET + pos, (byte) v);
    pos += SIZE_OF_BYTE;
  }

  public void writeShort(int v) throws IOException {
    ensureSize(SIZE_OF_SHORT);
    UNSAFE.putShort(buf, BYTE_ARRAY_OFFSET + pos, (short) v);
    pos += SIZE_OF_SHORT;
  }

  public void writeChar(int v) throws IOException {
    ensureSize(SIZE_OF_CHAR);
    UNSAFE.putChar(buf, BYTE_ARRAY_OFFSET + pos, (char) v);
    pos += SIZE_OF_CHAR;
  }

  public void writeInt(int v) throws IOException {
    ensureSize(SIZE_OF_INT);
    UNSAFE.putInt(buf, BYTE_ARRAY_OFFSET + pos, v);
    pos += SIZE_OF_INT;
  }

  public void skipBytes(int bytesToSkip) {
    if ((pos + bytesToSkip) > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, pos + bytesToSkip));
    }
    pos += bytesToSkip;
  }

  public void writeInt(int pos, int value) {
    if (pos + SIZE_OF_INT > this.pos) {
      throw new IndexOutOfBoundsException(
          "writeInt: Tried to write int to setPosition " + pos +
              " but current length is " + this.pos);
    }
    UNSAFE.putInt(buf, BYTE_ARRAY_OFFSET + pos, value);
  }

  public void writeLong(long v) throws IOException {
    ensureSize(SIZE_OF_LONG);
    UNSAFE.putLong(buf, BYTE_ARRAY_OFFSET + pos, v);
    pos += SIZE_OF_LONG;
  }

  public void writeFloat(float v) throws IOException {
    ensureSize(SIZE_OF_FLOAT);
    UNSAFE.putFloat(buf, BYTE_ARRAY_OFFSET + pos, v);
    pos += SIZE_OF_FLOAT;
  }

  public void writeDouble(double v) throws IOException {
    ensureSize(SIZE_OF_DOUBLE);
    UNSAFE.putDouble(buf, BYTE_ARRAY_OFFSET + pos, v);
    pos += SIZE_OF_DOUBLE;
  }
}