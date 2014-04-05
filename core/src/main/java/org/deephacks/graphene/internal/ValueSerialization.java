/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deephacks.graphene.internal;

import org.deephacks.graphene.internal.BytesUtils.DataType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

/**
 * General utility for writing a set of values into compact binary form.
 * <p>
 * Each byte array begin with a header followed by actual data. The header
 * store metadata that id and position of actual values stored in the
 * byte array. Each value begin with a type byte followed by the actual
 * value. Length metadata is also stored in the case of list values.
 * <p>
 * Headers/metadata and values are written using variable integers which store
 * smaller numbers with smaller number of bytes - hence more compact.
 */
public class ValueSerialization {

  /**
   * Used for writing values to binary form.
   */
  public static class ValueWriter {
    private HashMap<Integer, byte[]> properties = new HashMap<>();

    /**
     * Put a collection type property.
     *
     * @param id         id of the property.
     * @param collection values
     * @param cls        type of values in the collection.
     */
    public void putValues(int id, Collection<?> collection, DataType type) {
      try {
        if (collection == null) {
          return;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int size = collection.size();
        switch (type) {
          case BYTE:
            bytes.write(DataType.BYTE_LIST.getId());
            bytes.write(VarInt32.write(size));
            for (Object o : collection) {
              bytes.write((Byte) o);
            }
            properties.put(id, bytes.toByteArray());
            break;
          case BYTE_ARRAY:
            bytes.write(DataType.BYTE_ARRAY_LIST.getId());
            bytes.write(VarInt32.write(size));

            for (Object o : collection) {
              bytes.write(VarInt32.write(((byte[]) o).length));
              bytes.write((byte[]) o);
            }
            properties.put(id, bytes.toByteArray());
            break;
          case SHORT:
            bytes.write(DataType.SHORT_LIST.getId());
            bytes.write(VarInt32.write(size));
            for (Object o : collection) {
              bytes.write(VarInt32.write((Short) o));
            }
            properties.put(id, bytes.toByteArray());
            break;
          case INTEGER:
            bytes.write(DataType.INTEGER_LIST.getId());
            bytes.write(VarInt32.write(size));
            for (Object o : collection) {
              bytes.write(VarInt32.write((Integer) o));
            }
            properties.put(id, bytes.toByteArray());
            break;
          case LONG:
            bytes.write(DataType.LONG_LIST.getId());
            bytes.write(VarInt32.write(size));
            for (Object o : collection) {
              bytes.write(Bytes.fromLong((Long) o));
            }
            properties.put(id, bytes.toByteArray());
            break;
          case FLOAT:
            bytes.write(DataType.FLOAT_LIST.getId());
            bytes.write(VarInt32.write(size));
            for (Object o : collection) {
              bytes.write(Bytes.fromFloat((Float) o));
            }
            properties.put(id, bytes.toByteArray());
            break;
          case DOUBLE:
            bytes.write(DataType.DOUBLE_LIST.getId());
            bytes.write(VarInt32.write(size));
            for (Object o : collection) {
              bytes.write(Bytes.fromDouble((Double) o));
            }
            properties.put(id, bytes.toByteArray());
            break;
          case BOOLEAN:
            bytes.write(DataType.BOOLEAN_LIST.getId());
            bytes.write(VarInt32.write(size));
            for (Object o : collection) {
              bytes.write(BytesUtils.toByte((Boolean) o));
            }
            properties.put(id, bytes.toByteArray());
            break;
          case CHAR:
            bytes.write(DataType.CHAR_LIST.getId());
            bytes.write(VarInt32.write(size));
            for (Object o : collection) {
              bytes.write(VarInt32.write((char) o));
            }
            properties.put(id, bytes.toByteArray());
            break;
          case STRING:
            bytes.write(DataType.STRING_LIST.getId());
            String[] strings = collection.toArray(new String[collection.size()]);
            byte[] byteString = BytesUtils.toBytes(strings);
            bytes.write(byteString);
            properties.put(id, bytes.toByteArray());
            break;
          default:
            throw new UnsupportedOperationException("Did not recognize " + type);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Put a single valued property.
     *
     * @param id    id of the property.
     * @param value value of the property
     */
    public void putValue(int id, Object value, DataType type) {
      try {
        if (value == null) {
          return;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        switch (type) {
          case BYTE:
            bytes.write(DataType.BYTE.getId());
            bytes.write((Byte) value);
            properties.put(id, bytes.toByteArray());
            break;
          case BYTE_ARRAY:
            bytes.write(DataType.BYTE_ARRAY.getId());
            byte[] bytesValue = (byte[]) value;
            bytes.write(VarInt32.write(bytesValue.length));
            bytes.write(bytesValue);
            properties.put(id, bytes.toByteArray());
            break;
          case SHORT:
            bytes.write(DataType.SHORT.getId());
            bytes.write(VarInt32.write((Short) value));
            properties.put(id, bytes.toByteArray());
            break;
          case INTEGER:
            bytes.write(DataType.INTEGER.getId());
            bytes.write(VarInt32.write((Integer) value));
            properties.put(id, bytes.toByteArray());
            break;
          case LONG:
            bytes.write(DataType.LONG.getId());
            bytes.write(Bytes.fromLong((Long) value));
            properties.put(id, bytes.toByteArray());
            break;
          case FLOAT:
            bytes.write(DataType.FLOAT.getId());
            bytes.write(Bytes.fromFloat((Float) value));
            properties.put(id, bytes.toByteArray());
            break;
          case DOUBLE:
            bytes.write(DataType.DOUBLE.getId());
            bytes.write(Bytes.fromDouble((Double) value));
            properties.put(id, bytes.toByteArray());
            break;
          case BOOLEAN:
            bytes.write(DataType.BOOLEAN.getId());
            bytes.write(BytesUtils.toByte((Boolean) value));
            properties.put(id, bytes.toByteArray());
            break;
          case CHAR:
            bytes.write(DataType.CHAR.getId());
            bytes.write(VarInt32.write((char) value));
            properties.put(id, bytes.toByteArray());
            break;
          case STRING:
            bytes.write(DataType.STRING.getId());
            byte[] b = ((String) value).getBytes();
            bytes.write(VarInt32.write(b.length));
            bytes.write(b);
            properties.put(id, bytes.toByteArray());
            break;
          default:
            throw new UnsupportedOperationException("Type not recognized " + type);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * @return writes the values into binary form.
     */
    public byte[] write() {
      try {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        // number of properties
        header.write(VarInt32.write(properties.size()));
        int idx = 0;
        for (int id : properties.keySet()) {
          byte[] property = properties.get(id);
          header.write(VarInt32.write(id));
          header.write(VarInt32.write(idx));
          idx += property.length;
        }

        ByteArrayOutputStream values = new ByteArrayOutputStream();
        for (Integer id : properties.keySet()) {
          byte[] property = properties.get(id);
          values.write(property);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] headerLength = header.toByteArray();
        // first header length
        bytes.write(Bytes.fromInt(headerLength.length + 4));
        // then the header
        bytes.write(header.toByteArray());
        // last the values
        bytes.write(values.toByteArray());
        return bytes.toByteArray();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Used for reading a binary representation of values.
   */
  public static class ValueReader {
    /**
     * raw data of the bean
     */
    private byte[] data;

    /**
     * Construct values that was written by a ValueWriter.
     *
     * @param data raw values data.
     */
    public ValueReader(byte[] data) {
      this.data = data;
    }

    /**
     * @return the ids
     */
    public int[][] getHeader() {
      // header length
      int headerLength = Bytes.getInt(data, 0);
      int cursor = 4;
      // first the number of properties
      int numProperties = VarInt32.read(data, cursor);
      int[][] header = new int[numProperties][];
      // move index forward to starting position
      cursor += VarInt32.size(numProperties);
      // each element contain two VarInt32, the first is the id
      // and the second is the index where the actual value is located
      for (int i = 0; i < numProperties; i++) {
        int propertyId = VarInt32.read(data, cursor);
        cursor += VarInt32.size(propertyId);
        // move cursor forward past the propertyIdx position (to the next
        // propertyId) since we are not interested propertyIdx right now
        int propertyIdx = VarInt32.read(data, cursor);
        header[i] = new int[]{propertyId, propertyIdx + headerLength};
        cursor += VarInt32.size(propertyIdx);
      }
      return header;
    }

    public Object getValue(int id, int[][] header, DataType type) {
      int idx = getValueIndex(id, header);
      if (idx < 0) {
        return null;
      }
      idx = idx + 1;
      switch (type) {
        case BYTE:
          return data[idx];
        case BYTE_ARRAY:
          return BytesUtils.toBytes(data, idx);
        case SHORT:
          return (short) VarInt32.read(data, idx);
        case INTEGER:
          return VarInt32.read(data, idx);
        case LONG:
          return Bytes.getLong(data, idx);
        case FLOAT:
          return BytesUtils.getFloat(data, idx);
        case DOUBLE:
          return BytesUtils.getDouble(data, idx);
        case CHAR:
          return (char) VarInt32.read(data, idx);
        case BOOLEAN:
          return data[idx] != 0;
        case STRING:
          return BytesUtils.getString(data, idx);
        default:
          throw new UnsupportedOperationException("Could not recognize " + type);
      }
    }


    public Object getValues(int id, int[][] header, DataType type) {
      int idx = getValueIndex(id, header);
      if (idx < 0) {
        return null;
      }
      idx = idx + 1;
      switch (type) {
        case BYTE:
          return BytesUtils.toByteList(data, idx);
        case BYTE_ARRAY:
          return BytesUtils.toBytesList(data, idx);
        case SHORT:
          return BytesUtils.toShortList(data, idx);
        case CHAR:
          return BytesUtils.toCharList(data, idx);
        case INTEGER:
          return BytesUtils.toIntList(data, idx);
        case LONG:
          return BytesUtils.toLongList(data, idx);
        case FLOAT:
          return BytesUtils.toFloatList(data, idx);
        case DOUBLE:
          return BytesUtils.toDoubleList(data, idx);
        case BOOLEAN:
          return BytesUtils.toBooleanList(data, idx);
        case STRING:
          return BytesUtils.toStringList(data, idx);
        default:
          throw new UnsupportedOperationException("Could not recognize " + type);
      }
    }

    public boolean valueExist(int id, int[][] header) {
      int idx = getValueIndex(id, header);
      return idx >= 0;
    }

    private int getValueIndex(int id, int[][] header) {
      for (int[] property : header) {
        if (property[0] == id) {
          return property[1];
        }
      }
      return -1;
    }
  }
}
