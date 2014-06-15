package org.deephacks.graphene.internal.processor;


import org.deephacks.graphene.Schema.KeySchema;
import org.deephacks.graphene.Schema.KeySchema.KeyPart;
import org.deephacks.graphene.internal.EntityInterface;
import org.deephacks.graphene.internal.KeyInterface;
import org.deephacks.graphene.internal.processor.GrapheneField.ArrayField;
import org.deephacks.graphene.internal.processor.GrapheneField.KeyField;
import org.deephacks.graphene.internal.processor.GrapheneType.EmbeddedType;
import org.deephacks.graphene.internal.processor.GrapheneType.EntityType;
import org.deephacks.graphene.internal.processor.GrapheneType.KeyType;
import org.deephacks.graphene.internal.serialization.KeySerialization.KeyReader;
import org.deephacks.graphene.internal.serialization.KeySerialization.KeyWriter;
import org.deephacks.graphene.internal.serialization.ValueSerialization.ValueReader;
import org.deephacks.graphene.internal.serialization.ValueSerialization.ValueWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

class GrapheneTypeGenerator extends SourceGenerator {

  public GrapheneTypeGenerator(GrapheneType type) {
    super(type);
  }

  @Override
  public String writeSource() throws IOException {
    writer.emitPackage(type.getPackageName());
    if (type.hasArrayField()) {
      writer.emitImports(Arrays.class.getName());
    }
    writer.emitImports(IOException.class.getName());
    writer.emitImports(ValueWriter.class.getCanonicalName());
    writer.emitImports(ValueReader.class.getCanonicalName());
    writer.emitImports(KeyReader.class.getCanonicalName());
    writer.emitImports(KeyWriter.class.getCanonicalName());

    writer.emitEmptyLine();
    if (type instanceof EntityType) {
      writer.beginType(type.getGeneratedGrapheneType(), "class", PUBLIC, null, type.getClassName(), EntityInterface.class.getName());
    } else if (type instanceof KeyType) {
      writer.beginType(type.getGeneratedGrapheneType(), "class", PUBLIC, null, type.getClassName(), KeyInterface.class.getName());
    } else {
      writer.beginType(type.getGeneratedGrapheneType(), "class", PUBLIC, null, type.getClassName());
    }

    writer.emitField(KeySchema.class.getCanonicalName(), "keySchema", PRIVATE_STATIC);

    // fields for builder
    for (GrapheneField field : type.getAllFields()) {
      if (field.isOptional()) {
        writer.emitField("java.util.Optional<" + field.getFullTypeString() + ">", field.getName(), PRIVATE);
      } else {
        writer.emitField(field.getFullTypeString(), field.getName(), PRIVATE);
      }

    }

    // fields for deserializer
    if (type instanceof EntityType || type instanceof KeyType) {
      writer.emitField("KeyReader", "keyReader", PRIVATE);
    }
    if (type instanceof EntityType || type instanceof EmbeddedType) {
      writer.emitField("ValueReader", "valueReader", PRIVATE);
    }

    writer.emitEmptyLine();

    // constructor for builder
    writer.beginConstructor(PACKAGE_PRIVATE, type.getAllFieldsAsStrings(), Collections.emptyList());
    for (GrapheneField field : type.getAllFields()) {
      if (!field.isPrimitive()) {
        if (field.hasDefaultValue()) {
          writer.emitStatement("this." + field.getName() + " = java.util.Optional.ofNullable(" + field.getName() + ").orElse(" + type.getClassName() + ".super." + field.getGetMethod() + "())");
        } else {
          writer.emitStatement("this." + field.getName() + " = " + field.getName());
        }
        writer.beginControlFlow("if (this." + field.getName() + " == null)");
        writer.emitStatement("throw new IllegalArgumentException(\"" + field.getName() + " is null\")");
        writer.endControlFlow();
      }
    }
    writer.endConstructor();
    writer.emitEmptyLine();

    // constructor for deserializer
    if (type instanceof EntityType) {
      writer.beginConstructor(PACKAGE_PRIVATE, "KeyReader", "keyReader", "ValueReader", "valueReader");
      writer.emitStatement("this.keyReader = keyReader");
      writer.emitStatement("this.valueReader = valueReader");
    } else if (type instanceof KeyType) {
      writer.beginConstructor(PACKAGE_PRIVATE, "KeyReader", "keyReader");
      writer.emitStatement("this.keyReader = keyReader");
    } else if (type instanceof EmbeddedType) {
      writer.beginConstructor(PACKAGE_PRIVATE, "ValueReader", "valueReader");
      writer.emitStatement("this.valueReader = valueReader");
    }
    writer.endConstructor();
    writer.emitEmptyLine();

    writeGetters();

    // implement isEmbedded interface
    if (type instanceof EmbeddedType) {
      writer.beginMethod("boolean", "isEmbedded", PUBLIC);
      writer.emitStatement("return true");
      writer.endMethod();
    } else {
      writer.beginMethod("boolean", "isEmbedded", PUBLIC);
      writer.emitStatement("return false");
      writer.endMethod();
    }

    // write serializer methods
    if (type instanceof EntityType) {
      writeSerializeMethod();
      writeSerializeKeyMethod();
      writeSerializeValueMethod();
    } else if (type instanceof EmbeddedType) {
      writeSerializeValueMethod();
    } else if (type instanceof KeyType) {
      writeSerializeKeyMethod();
    }

    writer.emitEmptyLine();

    if (type instanceof EntityType || type instanceof KeyType) {
      writeKeySchema();
      writer.emitEmptyLine();
    }

    writeEquals();
    writer.emitEmptyLine();

    writeHashCode();
    writer.emitEmptyLine();

    writeToString();
    writer.emitEmptyLine();

    writer.endType();
    writer.close();
    return out.toString();
  }

  private void writeKeySchema() throws IOException {
    writer.beginMethod(KeySchema.class.getCanonicalName(), "keySchema", PUBLIC_STATIC);
    if (type.getKeys().size() == 1 && type.getKeys().get(0).isKeyClass()) {
      KeyField key = type.getKeys().get(0);
      writer.emitStatement("return " + key.getGeneratedGrapheneType() + ".keySchema()");
      writer.endMethod();
      return;
    }
    writer.beginControlFlow("if (keySchema == null)");
    writer.emitStatement(List.class.getCanonicalName() + "<" + KeyPart.class.getCanonicalName() + "> parts = new " + ArrayList.class.getCanonicalName() + "<>()");
    writer.emitStatement("int bytesPosition = 0");
    ListIterator<KeyField> it = type.getKeys().listIterator();
    while (it.hasNext()) {
      KeyField field = it.next();
      int size = TypeUtil.getSize(field.getFullTypeString());
      writer.emitStatement("parts.add(new " + KeyPart.class.getCanonicalName() + "(\"" + field.getName() + "\", "
              + field.getFullTypeString() + ".class, " + field.getSize().orElse(size) + " , bytesPosition))");
      if (it.hasNext()) {
        writer.emitStatement("bytesPosition += " + field.getSize().orElse(size));
      }
    }
    writer.emitStatement("keySchema = new " + KeySchema.class.getCanonicalName() + "(parts)");
    writer.endControlFlow();
    writer.emitStatement("return keySchema");
    writer.endMethod();
  }

  private void writeSerializeMethod() throws IOException {
    List<String> params = Arrays.asList(KeyWriter.class.getSimpleName(), "keyWriter",
            ValueWriter.class.getSimpleName(), "valueWriter", "int", "schemaId");
    List<String> throwz = Arrays.asList(IOException.class.getName());
    writer.beginMethod("byte[][]", "serialize", SourceGenerator.PUBLIC, params, throwz);
    writer.emitStatement("return new byte[][]{ serializeKey(keyWriter, schemaId), serializeValue(valueWriter) }");
    writer.endMethod();
    writer.emitEmptyLine();
  }


  private void writeSerializeKeyMethod() throws IOException {
    List<String> params = Arrays.asList(KeyWriter.class.getSimpleName(), "keyWriter", "int", "schemaId");
    List<String> throwz = Arrays.asList(IOException.class.getName());
    writer.beginMethod("byte[]", "serializeKey", SourceGenerator.PUBLIC, params, throwz);
    if (type.getKeys().size() >= 1 && !type.getKeys().get(0).isKeyClass()) {
      writer.emitStatement("keyWriter.writeInt(schemaId)");
    }
    for (KeyField key : type.getKeys()) {
      if (key.isKeyClass()) {
        key.write(writer);
        writer.endMethod();
        writer.emitEmptyLine();
        return;
      } else {

        key.startWrite(writer);
        key.write(writer);
        key.endWrite(writer);
      }
      writer.emitEmptyLine();
    }

    writer.emitStatement("return keyWriter.getBytes()");
    writer.endMethod();
    writer.emitEmptyLine();
  }


  private void writeSerializeValueMethod() throws IOException {
    List<String> params = Arrays.asList(ValueWriter.class.getSimpleName(), "valueWriter");
    List<String> throwz = Arrays.asList(IOException.class.getName());
    writer.beginMethod("byte[]", "serializeValue", SourceGenerator.PUBLIC, params, throwz);
    for (GrapheneField field : type.getFields()) {
      field.startWrite(writer);
      field.write(writer);
      field.endWrite(writer);
      writer.emitEmptyLine();
    }
    writer.emitStatement("return valueWriter.getBytes()");
    writer.endMethod();
    writer.emitEmptyLine();
  }

  private void writeGetters() throws IOException {
    for (GrapheneField field : type.getAllFields()) {
      if (field.isOptional()) {
        writer.beginMethod("java.util.Optional<" + field.getFullTypeString() + ">", field.getGetMethod(), PUBLIC);
      } else {
        writer.beginMethod(field.getFullTypeString(), field.getGetMethod(), PUBLIC);
      }
      field.startRead(writer);
      field.read(writer);
      field.endRead(writer);
      writer.emitStatement("return " + field.getName());
      writer.endMethod();
      writer.emitEmptyLine();
    }
  }

  private void writeToString() throws IOException {
    ListIterator<GrapheneField> it;
    writer.emitAnnotation("Override");
    writer.beginMethod("String", "toString", PUBLIC);

    String s = "return \"" + type.getSimpleClassName() + "{\" \n";
    it = type.getAllFields().listIterator();
    while (it.hasNext()) {
      GrapheneField field = it.next();
      if (field instanceof ArrayField) {
        s += "+ \"" + field.getName() + "=\" + Arrays.toString(" + field.getGetMethod() + "())";
      } else {
        s += "+ \"" + field.getName() + "=\" + " + field.getGetMethod() + "()";
      }
      if (it.hasNext()) {
        s += " + \",\"\n";
      }
    }
    s += " + \"}\"";
    writer.emitStatement(s);
    writer.endMethod();
  }

  private void writeHashCode() throws IOException {
    writer.emitAnnotation("Override");
    writer.beginMethod("int", "hashCode", PUBLIC);
    writer.emitStatement("int h = 1");
    writer.emitStatement("h *= 1000003");
    ListIterator<GrapheneField> it = type.getAllFields().listIterator();
    while (it.hasNext()) {
      GrapheneField field = it.next();
      writer.emitStatement("h ^= " + field.generateHashCode());
      if (it.hasNext()) {
        writer.emitStatement("h *= 1000003");
      }
    }
    writer.emitStatement("return h");
    writer.endMethod();
  }

  private void writeEquals() throws IOException {
    writer.emitAnnotation("Override");
    writer.beginMethod("boolean", "equals", PUBLIC, "Object", "o");
    writer.emitStatement("if (o == this) return true");
    writer.emitStatement("if (!(o instanceof " + type.getClassName() + ")) return true");
    writer.emitStatement(type.getClassName() + " that = (" + type.getClassName() + ") o");
    for (GrapheneField value : type.getAllFields()) {
      writer.emitStatement("if (" + value.generateEquals() + ") return false");
      // writer.emitStatement("if (!this." + value.getGetMethod() + "().equals(that." + value.getGetMethod() + "())) return false");
    }
    writer.emitStatement("return true");
    writer.endMethod();
    writer.emitEmptyLine();
  }
}
