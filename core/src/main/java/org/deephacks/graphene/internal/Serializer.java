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
import org.deephacks.graphene.internal.EntityClassWrapper.EntityMethodWrapper;
import org.deephacks.graphene.internal.ValueSerialization.ValueReader;
import org.deephacks.graphene.internal.ValueSerialization.ValueWriter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
    private static final EntityRepository repository = new EntityRepository();

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
      StateMap state = new StateMap(data);
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
          if (method.isEnum()) {
            writer.putValues(id, toStrings((Collection) value), String.class);
          } else if (method.isBasicType()) {
            writer.putValues(id, (Collection) value, method.getType());
          } else {
            writer.putValues(id, toStrings((Collection) value), String.class);
          }
        } else {
          if (method.getType().isEnum()) {
            writer.putValue(id, value.toString());
          } else if (method.isBasicType()) {
            writer.putValue(id, value);
          } else {
            writer.putValue(id, value.toString());
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
          if (method.getType().isEnum()) {
            writer.putValues(id, toStrings((Collection) value), String.class);
          } else if (method.isReference()) {
            writer.putValues(id, (Collection) value, String.class);
          } else {
            writer.putValues(id, (Collection) value, method.getType());
          }
        } else {
          if (method.getType().isEnum()) {
            writer.putValue(id, value.toString());
          } else {
            writer.putValue(id, value);
          }
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
          writer.putValues(id, values, byte[].class);
        } else {
          byte[][] embedded = serializeEntity(value);
          writer.putValue(id, embedded[1]);
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

    public StateMap(byte[][] state) {
      this.state = state;
      this.rowkey = new RowKey(state[0]);
      this.wrapper =  EntityClassWrapper.get(rowkey.getCls());
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
      if (wrapper.getId().getName().equals(key)) {
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
          Object value = reader.getValue(id[0], header);
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
          return reader.getValue(id[0], header);
        } else if (wrapper.isEmbedded(methodName)) {
          throw new IllegalArgumentException("not impl");
          /*
          Object value = reader.getValue(id[0], header);
          Class<?> type = wrapper.getEmbedded(methodName).getType();
          byte[] schemaKey = RowKey.getMinId(type).getKey();
          if (byte[].class.isAssignableFrom(value.getClass())) {
            Object entity = deserializeEntity(new byte[][]{schemaKey, (byte[]) value});
            wrapper.set(wrapper.getEmbedded(methodName), entity);
          } else if (byte[][].class.isAssignableFrom(value.getClass())) {
            ArrayList<Object> entities = new ArrayList<>();
            for (byte[] bytes : (byte[][]) value) {
              Object entity = deserializeEntity(new byte[][]{schemaKey, bytes});
              entities.add(entity);
            }
            wrapper.set(wrapper.getEmbedded(methodName), entities);
          } else {
            throw new UnsupportedOperationException("Did not recognize embedded type " + value.getClass());
          }
          */
        }
      }
      return null;
    }

    @Override
    public boolean containsKey(Object key) {
      // entity must always have a key!
      if (wrapper.getId().getName().equals(key)) {
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
}
