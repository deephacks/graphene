package org.deephacks.graphene;

import org.deephacks.graphene.Schema.KeySchema;
import org.deephacks.graphene.internal.processor.TypeUtil;
import org.deephacks.graphene.internal.serialization.BufAllocator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemaRepository {
  public static final String SCHEMA_PATH = "META-INF/graphene/schema";
  private Map<Class<?>, Schema> interfaceSchemas = new HashMap<>();
  private Map<Class<?>, Schema> generatedSchemas = new HashMap<>();
  private BufAllocator bufAllocator;
  private UniqueIds uniqueIds;

  public SchemaRepository(BufAllocator bufAllocator, UniqueIds uniqueIds) {
    this.bufAllocator = bufAllocator;
    this.uniqueIds = uniqueIds;
    for (Class<?> cls : readEntityClasses()) {
      try {
        Class<?> generatedClass = TypeUtil.getGeneratedEntity(cls);
        KeySchema keySchema = TypeUtil.getKeySchema(cls);
        Schema<?> schema = new Schema<>(generatedClass, cls, keySchema, bufAllocator, uniqueIds);
        interfaceSchemas.put(cls, schema);
        generatedSchemas.put(generatedClass, schema);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public <T> Schema<T> getSchema(Class<T> cls) {
    Schema<T> schema;
    if (cls.getName().contains("Entity_")) {
      schema = generatedSchemas.get(cls);
    } else {
      schema = interfaceSchemas.get(cls);
    }
    if (schema == null) {
      throw new IllegalArgumentException("No schema found for class " + cls.getName());
    }
    return schema;
  }

  public List<String> list() {
    ArrayList<String> list = new ArrayList<>();
    for (Class<?> cls : interfaceSchemas.keySet()) {
      list.add(cls.getName());
    }
    return list;
  }


  private List<Class<?>> readEntityClasses() {
    try {
      URL resource = Thread.currentThread().getContextClassLoader().getResource(SCHEMA_PATH);
      if (resource == null) {
        throw new IllegalStateException("No " + SCHEMA_PATH + " found.");
      }
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
        List<Class<?>> classes = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.trim().isEmpty()) {
            try {
              classes.add(Class.forName(line.trim()));
            } catch (ClassNotFoundException e) {
              // ignore
            }
          }
        }
        return classes;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
