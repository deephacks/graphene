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


import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.internal.BytesUtils.DataType;
import org.deephacks.graphene.internal.EntityClassWrapper.EntityMethodWrapper;
import org.deephacks.graphene.internal.ValueSerialization.ValueReader;
import org.deephacks.graphene.internal.ValueSerialization.ValueWriter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface Serializer {


  public RowKey deserializeRowKey(byte[] key);

  public byte[] serializeRowKey(RowKey key);

  public Object deserializeEntity(byte[][] data);

  public byte[][] serializeEntity(Object entity);

  public static class UnsafeSerializer implements Serializer {
    private static final UniqueIds ids = new UniqueIds();

    @Override
    public RowKey deserializeRowKey(byte[] key) {
      return new RowKey(key);
    }

    @Override
    public byte[] serializeRowKey(RowKey key) {
      return key.getKey();
    }

    @Override
    public Object deserializeEntity(byte[][] data) {
      StateMap state = new StateMap(data, this);
      RowKey key = new RowKey(data[0]);
      return key.create(state);
    }

    @Override
    public byte[][] serializeEntity(Object entity) {
      EntityObjectWrapper wrapper = new EntityObjectWrapper(entity);
      EntityClassWrapper classWrapper = EntityClassWrapper.get(entity.getClass());
      // embedded entities may not have key
      byte[] key = new byte[0];
      if (wrapper.getRowKey() != null) {
        key = serializeRowKey(wrapper.getRowKey());
      }
      ValueWriter writer = new ValueWriter();

      // basic fields
      for (EntityMethodWrapper method : classWrapper.getMethods().values()) {
        int id = ids.getSchemaId(method.getName());
        Object value = wrapper.getValue(method);
        if (value == null) {
          continue;
        }
        if (value instanceof Collection) {
          Collection<Object> values = (Collection<Object>) value;
          if (method.isBasicType()) {
            writer.putValues(id, values, method.getDataType());
          } else {
            values = values.stream().map(v -> convert(v, method.getDataType())).collect(Collectors.toList());
            writer.putValues(id, values, DataType.BYTE_ARRAY);
          }
        } else {
          if (method.isBasicType()) {
            writer.putValue(id, value, method.getDataType());
          } else {
            byte[] convert = convert(value, method.getDataType());
            writer.putValue(id, convert, DataType.BYTE_ARRAY);

          }
        }
      }

      // reference fields
      for (EntityMethodWrapper method : classWrapper.getReferences().values()) {
        int id = ids.getSchemaId(method.getName());
        Object value = wrapper.getValue(method);
        if (value == null) {
          continue;
        }
        if (value instanceof Collection) {
          writer.putValues(id, (Collection) value, DataType.STRING);
        } else {
          writer.putValue(id, value.toString(), DataType.STRING);
        }
      }

      // embedded fields
      for (EntityMethodWrapper method : classWrapper.getEmbedded().values()) {
        int id = ids.getSchemaId(method.getName());
        Object value = wrapper.getValue(method);
        if (value == null) {
          continue;
        }
        if (value instanceof Collection) {
          ArrayList<byte[]> values = new ArrayList<>();
          for (Object val : (Collection) value) {
            byte[][] embedded = serializeEntity(val);
            values.add(embedded[1]);
          }
          writer.putValues(id, values, DataType.BYTE_ARRAY);
        } else {
          byte[][] embedded = serializeEntity(value);
          writer.putValue(id, embedded[1], DataType.BYTE_ARRAY);
        }
      }
      return new byte[][]{key, writer.write()};
    }

    private Collection<?> toStrings(Collection<?> values) {
      return values.stream().map(Object::toString).collect(Collectors.toList());
    }
  }

  public static class StateMap extends AbstractMap<String, Object> {
    private static final UniqueIds ids = new UniqueIds();
    private static final EntityRepository repository = new EntityRepository();
    byte[][] state = new byte[0][];
    private RowKey rowkey;
    private EntityClassWrapper wrapper;
    private Serializer serializer;

    public StateMap(byte[][] state, Serializer serializer) {
      this.state = state;
      this.rowkey = new RowKey(state[0]);
      this.wrapper =  EntityClassWrapper.get(rowkey.getCls());
      this.serializer = serializer;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      return new HashSet<>();
    }

    public void setState(byte[][] state) {
      this.state = state;
    }

    @Override
    public Object get(Object key) {
      EntityClassWrapper wrapper = EntityClassWrapper.get(rowkey.getCls());
      EntityMethodWrapper idMethod = wrapper.getId();
      if (idMethod != null &&  idMethod.getName().equals(key)) {
        return rowkey.getInstance();
      }
      ValueReader reader = new ValueReader(state[1]);
      int[][] header = reader.getHeader();
      for (int[] id : header) {
        String methodName = ids.getSchemaName(id[0]);
        if (!methodName.equals(key)) {
          continue;
        }
        if (wrapper.isReference(methodName)) {
          boolean isCollection = wrapper.getReference(methodName).isCollection();
          Object value;
          if (!isCollection) {
            value = reader.getValue(id[0], header, DataType.STRING);
          } else {
            value = reader.getValues(id[0], header, DataType.STRING);
          }
          if (value == null) {
            continue;
          }
          if (value instanceof Collection) {
            ArrayList<Object> references = new ArrayList<>();
            for (Object instance : (Collection) value) {
              EntityMethodWrapper method = wrapper.getReference(methodName);
              Optional<?> optional = repository.get(instance, method.getType());
              if (optional.isPresent()) {
                references.add(optional.get());
              }
            }
            return references;
          } else {
            EntityMethodWrapper method = wrapper.getReference(methodName);
            Optional<?> optional = repository.get(value, method.getType());
            return optional.get();
          }
        } else if (wrapper.isMethod(methodName)) {
          EntityMethodWrapper method = wrapper.getMethod(methodName);
          if (method.isBasicType()) {
            if (method.isCollection()) {
              return reader.getValues(id[0], header, method.getDataType());
            } else {
              return reader.getValue(id[0], header, method.getDataType());
            }
          } else {
            if (method.isCollection()) {
              byte[][] array = (byte[][]) reader.getValues(id[0], header, DataType.BYTE_ARRAY);
              List<Object> objects = new ArrayList<>();
              for (byte[] bytes : array) {
                objects.add(convert(bytes, method.getDataType(), method.getType()));
              }
              return objects;
            } else {
              byte[] array = (byte[]) reader.getValue(id[0], header, DataType.BYTE_ARRAY);
              return convert(array, method.getDataType(), method.getType());
            }
          }
        } else if (wrapper.isEmbedded(methodName)) {
          EntityMethodWrapper method = wrapper.getEmbedded(methodName);
          byte[] schemaKey = RowKey.getMinId(method.getType()).getKey();
          if (!method.isCollection()) {
            Object value = reader.getValue(id[0], header, DataType.BYTE_ARRAY);
            return serializer.deserializeEntity(new byte[][]{schemaKey, (byte[]) value});
          } else {
            Object value = reader.getValues(id[0], header, DataType.BYTE_ARRAY);
            ArrayList<Object> entities = new ArrayList<>();
            for (byte[] bytes : (byte[][]) value) {
              Object entity = serializer.deserializeEntity(new byte[][]{schemaKey, bytes});
              entities.add(entity);
            }
            return entities;
          }
        }
      }
      return null;
    }

    @Override
    public boolean containsKey(Object key) {
      EntityMethodWrapper id = wrapper.getId();
      if (id != null && id.getName().equals(key)) {
        return true;
      }
      ValueReader reader = new ValueReader(state[1]);
      int[][] header = reader.getHeader();
      return reader.valueExist(ids.getSchemaId(key.toString()), header);
    }

    public void setValues(byte[][] values) {
      this.state = values;
    }
  }

  static Charset charset = Charset.forName("UTF-8");

  static byte[] convert(Object object, DataType type) {
    Class<?> cls = object.getClass();
    if (type == DataType.DATE) {
      long time = ((Date) object).getTime();
      return Bytes.fromLong(time);
    } else if (type == DataType.BIG_DECIMAL) {
      String string = object.toString();
      return string.getBytes(charset);
    } else if (type == DataType.BIG_INTEGER) {
      String string = object.toString();
      return string.getBytes(charset);
    } else if (type == DataType.ENUM) {
      String string = object.toString();
      return string.getBytes(charset);
    } else if (type == DataType.LOCAL_DATE_TIME) {
      return BytesUtils.writeBytes((LocalDateTime) object);
    } else if (type == DataType.ZONED_DATE_TIME) {
      return BytesUtils.writeBytes((ZonedDateTime) object);
    } else if (type == DataType.LOCAL_DATE) {
      return BytesUtils.toBytes((LocalDate) object);
    } else if (type == DataType.LOCAL_TIME) {
      return BytesUtils.toBytes((LocalTime) object);
    } else if (type == DataType.INSTANT) {
      return BytesUtils.toBytes((Instant) object);
    } else if (type == DataType.PERIOD) {
      String string = object.toString();
      return string.getBytes(charset);
    } else if (type == DataType.DURATION) {
      String string = object.toString();
      return string.getBytes(charset);
    } else {
      throw new IllegalArgumentException("Did not recognize type " + cls);
    }
  }

  static Object convert(byte[] value, DataType type, Class<?> cls) {
    if (type == DataType.DATE) {
      return new Date(Bytes.getLong(value));
    } else if (type == DataType.BIG_DECIMAL) {
      String string = new String(value);
      return new BigDecimal(string);
    } else if (type == DataType.BIG_INTEGER) {
      String string = new String(value);
      return new BigInteger(string);
    } else if (type == DataType.ENUM) {
      return Enum.valueOf((Class) cls, new String(value));
    } else if (type == DataType.LOCAL_DATE_TIME) {
      return BytesUtils.getLocalDateTime(value);
    } else if (type == DataType.ZONED_DATE_TIME) {
      return BytesUtils.getZonedDateTime(value);
    } else if (type == DataType.LOCAL_DATE) {
      return BytesUtils.getLocalDate(value, 0);
    } else if (type == DataType.LOCAL_TIME) {
      return BytesUtils.getLocalTime(value, 0);
    } else if (type == DataType.INSTANT) {
      return BytesUtils.getInstant(value);
    } else if (type == DataType.PERIOD) {
      return Period.parse(new String(value));
    } else if (type == DataType.DURATION) {
      return Duration.parse(new String(value));
    } else {
      throw new IllegalArgumentException("Did not recognize type " + cls);
    }
  }
}
