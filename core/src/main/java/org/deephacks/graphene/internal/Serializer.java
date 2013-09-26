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


import org.deephacks.graphene.internal.EntityClassWrapper.EntityFieldWrapper;
import org.deephacks.graphene.internal.UnsafeUtils.UnsafeEntityClassWrapper;
import org.deephacks.graphene.internal.UnsafeUtils.UnsafeEntityObjectWrapper;
import org.deephacks.graphene.internal.ValueSerialization.ValueReader;
import org.deephacks.graphene.internal.ValueSerialization.ValueWriter;

import java.util.ArrayList;
import java.util.Collection;

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
            RowKey key = deserializeRowKey(data[0]);
            ValueReader reader = new ValueReader(data[1]);
            int[][] header = reader.getHeader();
            UnsafeEntityObjectWrapper wrapper = new UnsafeEntityObjectWrapper(key);
            for (int[] id : header) {
                String fieldName = ids.getSchemaName(id[0]);
                Object value = reader.getValue(id[0], header);
                wrapper.set(wrapper.getField(fieldName), value);
            }
            return wrapper.getObject();
        }

        @Override
        public byte[][] serializeEntity(Object entity) {
            UnsafeEntityObjectWrapper wrapper = new UnsafeEntityObjectWrapper(entity);
            UnsafeEntityClassWrapper classWrapper = UnsafeEntityClassWrapper.get(entity.getClass());
            byte[] key = serializeRowKey(wrapper.getRowKey());
            ValueWriter writer = new ValueWriter();
            for (EntityFieldWrapper field : classWrapper.getFields().values()) {
                int id = ids.getSchemaId(field.getName());
                Object value = wrapper.getValue(field.getName());
                if (value instanceof Collection) {
                    if (field.getType().isEnum()){
                        writer.putValues(id, toStrings((Collection) value), String.class);
                    } else {
                        writer.putValues(id, (Collection) value, field.getType());
                    }

                } else {
                    if (field.getType().isEnum()) {
                        writer.putValue(id, value.toString());
                    } else {
                        writer.putValue(id, value);
                    }

                }
            }
            return new byte[][] { key, writer.write()};
        }

        private Collection<?> toStrings(Collection values) {
            ArrayList<String> strings = new ArrayList<>();
            for (Object value : values) {
                strings.add(value.toString());
            }
            return strings;
        }
    }
}
