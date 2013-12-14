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


import com.google.common.base.Optional;
import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.internal.EntityClassWrapper.EntityFieldWrapper;
import org.deephacks.graphene.internal.UnsafeUtils.UnsafeEntityClassWrapper;
import org.deephacks.graphene.internal.UnsafeUtils.UnsafeEntityObjectWrapper;
import org.deephacks.graphene.internal.ValueSerialization.ValueReader;
import org.deephacks.graphene.internal.ValueSerialization.ValueWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public interface Serializer {

    public RowKey deserializeRowKey(byte[] key);

    public byte[] serializeRowKey(RowKey key);

    public Object deserializeEntity(byte[][] data);

    public byte[][] serializeEntity(Object entity);

    public static class UnsafeSerializer implements Serializer {
        private static final Logger logger = LoggerFactory.getLogger(Serializer.class);
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
            RowKey key = deserializeRowKey(data[0]);
            ValueReader reader = new ValueReader(data[1]);
            int[][] header = reader.getHeader();
            UnsafeEntityObjectWrapper wrapper = new UnsafeEntityObjectWrapper(key);
            for (int[] id : header) {
                String fieldName = ids.getSchemaName(id[0]);
                if (wrapper.isReference(fieldName)) {
                    Object value = reader.getValue(id[0], header);
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof Collection) {
                        ArrayList<Object> references = new ArrayList<>();
                        for (Object instance : (Collection) value) {
                            EntityFieldWrapper field = wrapper.getReference(fieldName);
                            Optional<?> optional = repository.get(instance, field.getType());
                            if (optional.isPresent()) {
                                references.add(optional.get());
                            }
                        }
                        wrapper.set(wrapper.getReference(fieldName), references);
                    } else {
                        EntityFieldWrapper field = wrapper.getReference(fieldName);
                        Optional<?> optional = repository.get(value, field.getType());
                        if (optional.isPresent()) {
                            wrapper.set(wrapper.getReference(fieldName), optional.get());
                        }
                    }
                } else if (wrapper.isField(fieldName)){
                    Object value = reader.getValue(id[0], header);
                    wrapper.set(wrapper.getField(fieldName), value);
                } else if (wrapper.isEmbedded(fieldName)) {
                    Object value = reader.getValue(id[0], header);
                    Class<?> type = wrapper.getEmbedded(fieldName).getType();
                    byte[] schemaKey = RowKey.getMinId(type).getKey();
                    if ( byte[].class.isAssignableFrom(value.getClass())) {
                        Object entity = deserializeEntity(new byte[][] { schemaKey, (byte[]) value} );
                        wrapper.set(wrapper.getEmbedded(fieldName), entity);
                    } else if (byte[][].class.isAssignableFrom(value.getClass())) {
                        ArrayList<Object> entities = new ArrayList<>();
                        for (byte[] bytes : (byte[][]) value) {
                            Object entity = deserializeEntity(new byte[][] { schemaKey, bytes} );
                            entities.add(entity);
                        }
                        wrapper.set(wrapper.getEmbedded(fieldName), entities);
                    } else {
                        throw new UnsupportedOperationException("Did not recognize embedded type " + value.getClass());
                    }
                } else {
                    logger.warn("Did not recognize field " + fieldName);
                }
            }
            return wrapper.getObject();
        }

        @Override
        public byte[][] serializeEntity(Object entity) {
            UnsafeEntityObjectWrapper wrapper = new UnsafeEntityObjectWrapper(entity);
            UnsafeEntityClassWrapper classWrapper = UnsafeEntityClassWrapper.get(entity.getClass());
            // embedded entities may not have key
            byte[] key = new byte[0];
            if (wrapper.getRowKey() != null) {
                key = serializeRowKey(wrapper.getRowKey());
            }
            ValueWriter writer = new ValueWriter();

            // basic fields
            for (EntityFieldWrapper field : classWrapper.getFields().values()) {
                int id = ids.getSchemaId(field.getName());
                Object value = wrapper.getValue(field);
                if (value == null) {
                    continue;
                }
                if (value instanceof Collection) {
                    if (field.isEnum()){
                        writer.putValues(id, toStrings((Collection) value), String.class);
                    } else if (field.isBasicType()) {
                        writer.putValues(id, (Collection) value, field.getType());
                    } else {
                        writer.putValues(id, toStrings((Collection) value), String.class);
                    }
                } else {
                    if (field.getType().isEnum()) {
                        writer.putValue(id, value.toString());
                    } else if (field.isBasicType()){
                        writer.putValue(id, value);
                    } else {
                        writer.putValue(id, value.toString());
                    }
                }
            }

            // reference fields
            for (EntityFieldWrapper field : classWrapper.getReferences().values()) {
                int id = ids.getSchemaId(field.getName());
                Object value = wrapper.getValue(field);
                if (value == null) {
                    continue;
                }
                if (value instanceof Collection) {
                    if (field.getType().isEnum()){
                        writer.putValues(id, toStrings((Collection) value), String.class);
                    } else if (field.isReference()){
                        writer.putValues(id, (Collection) value, String.class);
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

            // embedded fields
            for (EntityFieldWrapper field : classWrapper.getEmbedded().values()) {
                int id = ids.getSchemaId(field.getName());
                Object value = wrapper.getValue(field);
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
