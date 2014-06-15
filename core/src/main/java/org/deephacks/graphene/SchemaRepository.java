package org.deephacks.graphene;

import org.deephacks.graphene.Schema.KeySchema;
import org.deephacks.graphene.internal.serialization.BufAllocator;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemaRepository {
  public static final String SCHEMA_PATH = "META-INF/graphene/schema";
  private Map<Class<?>, Schema> schemas = new HashMap<>();
  private BufAllocator bufAllocator;
  private UniqueIds uniqueIds;

  public SchemaRepository(BufAllocator bufAllocator, UniqueIds uniqueIds) {
    this.bufAllocator = bufAllocator;
    this.uniqueIds = uniqueIds;
    for (Class<?> cls : readEntityClasses()) {
      try {
        Method method = cls.getDeclaredMethod("getKeySchema");
        method.setAccessible(true);
        Class<?> entityInterface = getEntityInterface(cls);
        Schema<?> schema = new Schema<>(cls, (KeySchema) method.invoke(null), bufAllocator, uniqueIds);
        schemas.put(entityInterface, schema);
        schemas.put(cls, schema);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private Class<?> getEntityInterface(Class<?> cls) {
    for (Class<?> iface : cls.getInterfaces()) {
      if (cls.getSimpleName().contains(iface.getSimpleName())) {
        return iface;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T> Schema<T> getSchema(Class<T> cls) {
    Schema<T> schema = schemas.get(cls);
    if (schema == null) {
      throw new IllegalArgumentException("No schema found for class " + cls.getName());
    }
    return schema;
  }

  private List<Class<?>> readEntityClasses() {
    try {
      URL resource = Thread.currentThread().getContextClassLoader().getResource(SCHEMA_PATH);
      if (resource == null) {
        throw new IllegalStateException("No " + SCHEMA_PATH + " found.");
      }
      List<Class<?>> classes = new ArrayList<>();
      for (String line : Files.readAllLines(Paths.get(resource.toURI()))) {
        if (line != null && !line.trim().isEmpty()) {
          try {
            classes.add(Class.forName(line.trim()));
          } catch (ClassNotFoundException e) {
            // ignore
          }
        }
      }
      return classes;
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
