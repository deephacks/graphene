package org.deephacks.graphene.internal.processor;

import com.squareup.javawriter.JavaWriter;

import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.deephacks.graphene.internal.processor.TypeUtil.toCapitalizedBufType;

abstract class GrapheneField implements Comparable<GrapheneField> {
  public static final String KEY_READER = "keyReader";
  public static final String KEY_WRITER = "keyWriter";
  public static final String VALUE_READER = "valueReader";
  public static final String VALUE_WRITER = "valueWriter";
  private final boolean hasDefaultValue;

  protected String name;
  protected String getMethod;
  protected TypeInfo typeInfo;
  protected String readBuf = VALUE_READER;
  protected String writeBuf = VALUE_WRITER;

  protected GrapheneField(String name, TypeInfo typeInfo, boolean hasDefaultValue) {
    this.getMethod = name;
    name = name.substring(3, name.length());
    this.name = Character.toLowerCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : "");
    this.typeInfo = typeInfo;
    this.hasDefaultValue = hasDefaultValue;
  }

  protected GrapheneField(String name, TypeInfo typeInfo, boolean hasDefaultValue, String readBuf, String writeBuf) {
    this.getMethod = name;
    name = name.substring(3, name.length());
    this.name = Character.toLowerCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : "");
    this.typeInfo = typeInfo;
    this.readBuf = readBuf;
    this.writeBuf = writeBuf;
    this.hasDefaultValue = hasDefaultValue;
  }

  protected GrapheneField(GrapheneField field) {
    this.getMethod = field.getGetMethod();
    this.name = field.getName();
    this.typeInfo = field.typeInfo;
    this.readBuf = field.readBuf;
    this.writeBuf = field.writeBuf;
    this.hasDefaultValue = field.hasDefaultValue;
  }

  public void startRead(JavaWriter writer) throws IOException {
    writer.beginControlFlow("if (" + readBuf + " != null && !" + readBuf + ".hasRead(\"" + getName() + "\"))");
    if (!(this instanceof KeyField && ((KeyField) this).isKeyClass())) {
      if (isOptional()) {
        writer.beginControlFlow("if (!" + readBuf + ".position(\"" + getName() + "\"))");
        writer.emitStatement(getName() + " = java.util.Optional.empty()");
        writer.emitStatement("return " + getName());
        writer.endControlFlow();
      } else {
        writer.emitStatement(readBuf + ".position(\"" + getName() + "\")");
      }
    }
  }

  public void endRead(JavaWriter writer) throws IOException {
    writer.endControlFlow();
  }

  public abstract void read(JavaWriter writer, String... name) throws IOException;

  public abstract void write(JavaWriter writer, String... name) throws IOException;

  public void startWrite(JavaWriter writer) throws IOException {
    writer.emitStatement(writeBuf + ".start(\"" + getName() + "\")");
  }

  public void endWrite(JavaWriter writer) throws IOException {
    writer.emitStatement(writeBuf + ".end(\"" + getName() + "\")");
  }

  public boolean isOptional() {
    return typeInfo.isOptional();
  }

  public String getValueType() {
    String type;
    if (getTypeArgs().size() == 0) {
      type = getFullTypeString();
    } else if (getTypeArgStrings().size() == 1) {
      // List or Set
      type = getTypeArgStrings().get(0);
    } else {
      // Map
      type = getTypeArgStrings().get(1);
    }
    return type;
  }

  protected void setField(JavaWriter writer, String code) throws IOException {
    if (isOptional()) {
      writer.emitStatement(getName() + " = java.util.Optional.ofNullable(" + code + ")");
    } else {
      writer.emitStatement(getName() + " = " + code);
    }
  }

  public String getGetMethod() {
    return getMethod;
  }

  public String getGeneratedGrapheneType() {
    return typeInfo.getGeneratedGrapheneType();
  }

  public String getCapitalizedBufType() {
    return toCapitalizedBufType(getFullTypeString());
  }

  public String getCapitalizedBufType(String value) {
    return toCapitalizedBufType(value);
  }

  public String getFullTypeString() {
    return typeInfo.getFullTypeString();
  }

  public String getSimpleType() {
    String type = typeInfo.getFullTypeString();
    return type.substring(0, type.length() - 2);
  }

  public String getName() {
    return name;
  }

  public String getGetName() {
    if (isOptional()) {
      return name + ".get()";
    } else {
      return name;
    }
  }

  public String getNameFirstCapitalized() {
    return Character.toUpperCase(name.charAt(0)) + name.substring(1, name.length());
  }

  public List<TypeMirror> getTypeArgs() {
    return typeInfo.getTypeArgs();
  }

  public List<String> getTypeArgStrings() {
    return typeInfo.getTypeArgStrings();
  }

  public String generateEquals() {
    switch (typeInfo.getTypeKind()) {
      case BYTE:
      case SHORT:
      case CHAR:
      case INT:
      case LONG:
      case BOOLEAN:
        return "this." + getGetMethod() + "() != that." + getGetMethod() + "()";
      case FLOAT:
        return "Float.floatToIntBits(this." + getGetMethod() + "()) != Float.floatToIntBits(that." + getGetMethod() + "())";
      case DOUBLE:
        return "Double.doubleToLongBits(this." + getGetMethod() + "()) != Double.doubleToLongBits(that." + getGetMethod() + "())";
      case ARRAY:
        return "!Arrays.equals(this." + getGetMethod() + "(), that." + getGetMethod() + "())";
      default:
        if (isOptional()) {
          return "!(this." + getGetMethod() + "() == null ? that." + getGetMethod() + "() == null : this." + getGetMethod() + "().equals(that." + getGetMethod() + "()))";
        } else {
          return "!(this." + getGetMethod() + "().equals(that." + getGetMethod() + "()))";
        }
    }
  }

  public String generateHashCode() {
    switch (typeInfo.getTypeKind()) {
      case BYTE:
      case SHORT:
      case CHAR:
      case INT:
        return getGetMethod() + "()";
      case LONG:
        return "(" + getGetMethod() + "() >>> 32) ^ " + getGetMethod() + "()";
      case FLOAT:
        return "Float.floatToIntBits(" + getGetMethod() + "())";
      case DOUBLE:
        return "(Double.doubleToLongBits(" + getGetMethod() + "()) >>> 32) ^ "
                + "Double.doubleToLongBits(" + getGetMethod() + "())";
      case BOOLEAN:
        return getGetMethod() + "() ? 1231 : 1237";
      case ARRAY:
        return "Arrays.hashCode(" + getGetMethod() + "())";
      default:
        if (isOptional()) {
          return "(" + getGetMethod() + "() == null) ? 0 : " + getGetMethod() + "().hashCode()";
        } else {
          return getGetMethod() + "().hashCode()";
        }
    }
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GrapheneField that = (GrapheneField) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

  @Override
  public int compareTo(GrapheneField o) {
    return this.getGetMethod().compareTo(o.getGetMethod());
  }

  public boolean isPrimitive() {
    return typeInfo.isPrimitive();
  }

  public boolean hasDefaultValue() {
    return hasDefaultValue;
  }

  public static class ValueField extends GrapheneField {
    private Optional<Integer> size;

    public ValueField(String name, boolean hasDefaultValue, TypeInfo typeInfo, Integer size) {
      super(name, typeInfo, hasDefaultValue, VALUE_READER, VALUE_WRITER);
      this.size = Optional.ofNullable(size);
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      String stmt = name[0] + " = " + readBuf + ".read" + getCapitalizedBufType() + "()";
      // object types are read by providing the actual class to Buf
      if (getCapitalizedBufType().equalsIgnoreCase("object")) {
        String type = getValueType();
        stmt = name[0] + " = " + readBuf + ".read" + getCapitalizedBufType() + "(" + type + ".class)";
      } else if (getCapitalizedBufType().equalsIgnoreCase("string") && size.isPresent()) {
        stmt = name[0] + " = " + readBuf + ".readStringBytes(" + size.get() + ")";
      }
      writer.emitStatement(stmt);
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      String stmt = writeBuf + ".write" + getCapitalizedBufType() + "(" + name[0] + ")";
      if (getCapitalizedBufType().equalsIgnoreCase("string") && size.isPresent()) {
        stmt = writeBuf + ".writeStringBytes(" + name[0] + ", " + size.get() + ")";
      }
      writer.emitStatement(stmt);
    }

  }

  public static class EmbeddedField extends GrapheneField {

    public EmbeddedField(String name, TypeInfo typeInfo) {
      super(name, typeInfo, false);
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      String var = getName() + "Bytes";
      writer.emitStatement("int size = " + readBuf + ".readInt()");
      writer.emitStatement("byte[] " + var + " = " + readBuf + ".readBytes(size)");
      writer.emitStatement(name[0] + " = new " + getGeneratedGrapheneType() + "(" + readBuf + ".copy(" + var + "))");
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      String var = getName() + "Bytes";
      writer.emitStatement("byte[] " + var + " = " +
              "((" + getGeneratedGrapheneType() + ") " + name[0] + ")" +
              ".serializeValue(" + writeBuf + ".copy())");
      writer.emitStatement(writeBuf + ".writeInt(" + var + ".length)");
      writer.emitStatement(writeBuf + ".writeBytes(" + var + ")");
    }
  }


  public static class ReferenceField extends GrapheneField {

    public ReferenceField(String name, TypeInfo typeInfo) {
      super(name, typeInfo, false);
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      // FIXME
      writer.emitStatement(name[0] + " = null");
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {

    }
  }

  public static class KeyField extends GrapheneField {
    private ValueField field;
    private Optional<Integer> position;
    private Optional<Integer> size;

    public KeyField(String name, TypeInfo typeInfo, ValueField field, Integer position, Integer size) {
      super(name, typeInfo, false, KEY_READER, KEY_WRITER);
      if (typeInfo.isOptional()) {
        throw new IllegalArgumentException("Keys cant be optional.");
      }
      this.field = field;
      this.field.writeBuf = KEY_WRITER;
      this.field.readBuf = KEY_READER;
      this.position = Optional.ofNullable(position);
      this.size = Optional.ofNullable(size);
      if (typeInfo.isList() || typeInfo.isSet() || typeInfo.isMap()) {
        throw new IllegalArgumentException("Collection keys are not supported.");
      }
      if (typeInfo.isArray() && !typeInfo.getFullTypeString().startsWith("byte")) {
        throw new IllegalArgumentException("Only byte array keys are supported.");
      }
    }

    public KeyField(String name, TypeInfo typeInfo, ValueField field) {
      this(name, typeInfo, field, null, null);
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      if (isKeyClass()) {
        writer.emitStatement(getName() + " = new " + field.getGeneratedGrapheneType() + "(" + readBuf + ")");
      } else if (typeInfo.isArray() && typeInfo.getFullTypeString().startsWith("byte")) {
        writer.emitStatement(getName() + " = " + readBuf + ".readBytes(" + size.get() + ")");
      } else {
        field.read(writer, getName());
      }
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      if (isKeyClass()) {
        writer.emitStatement("return ((" + field.getGeneratedGrapheneType() + ") " + getGetMethod() + "()).serializeKey(" + writeBuf + ", schemaId)");
      } else if (typeInfo.isArray() && typeInfo.getFullTypeString().startsWith("byte")) {
        writer.emitStatement(writeBuf + ".writeBytes(" + field.getName() + ")");
      } else {
        field.write(writer, getName());
      }
    }

    public Optional<Integer> getPosition() {
      return position;
    }

    public Optional<Integer> getSize() {
      return size;
    }

    public boolean isKeyClass() {
      return typeInfo != null && typeInfo.isKey();
    }
  }

  public static class SingleField extends GrapheneField {
    private GrapheneField field;

    public SingleField(GrapheneField field) {
      super(field);
      this.field = field;
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      if (isPrimitive()) {
        writer.emitStatement(getFullTypeString() + " _value");
      } else {
        writer.emitStatement(getFullTypeString() + " _value = null");
      }

      field.read(writer, "_value");
      if (isOptional()) {
        writer.emitStatement(getName() + " = java.util.Optional.ofNullable(_value)");
      } else {
        writer.emitStatement(getName() + " = _value");
      }
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      if (isOptional()) {
        writer.beginControlFlow("if (" + getName() + ".isPresent())");
        field.write(writer, getName() + ".get()");
      } else {
        field.write(writer, getName());
      }

      if (isOptional()) {
        writer.endControlFlow();
      }
    }
  }


  public static class ArrayField extends GrapheneField {
    private GrapheneField field;

    public ArrayField(GrapheneField field) {
      super(field);
      this.field = field;
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      if (getFullTypeString().startsWith("byte")) {
        writer.emitStatement("int size = " + readBuf + ".readInt()");
        writer.emitStatement(getName() + " = " + readBuf + ".readBytes(size)");
      } else {
        writer.emitStatement("int size = " + readBuf + ".readInt()");
        writer.emitStatement(getName() + " = new " + getSimpleType() + "[size]");
        writer.beginControlFlow("for (int i = 0; i < size; i++)");
        field.read(writer, getName() + "[i]");
        writer.endControlFlow();
      }
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      if (getFullTypeString().startsWith("byte")) {
        writer.emitStatement(writeBuf + ".writeInt(" + getName() + ".length)");
        writer.emitStatement(writeBuf + ".writeBytes(" + getName() + ")");
      } else {
        writer.emitStatement(writeBuf + ".writeInt(" + getName() + ".length)");
        writer.beginControlFlow("for (int i = 0; i < " + getName() + ".length; i++)");
        field.write(writer, getName() + "[i]");
        writer.endControlFlow();
      }
    }
  }

  public static class ListField extends GrapheneField {

    private GrapheneField field;

    public ListField(GrapheneField field) {
      super(field);
      this.field = field;
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      if (isOptional()) {
        writer.emitStatement(getName() + " = java.util.Optional.of(new java.util.ArrayList<>())");
      } else {
        writer.emitStatement(getName() + " = new java.util.ArrayList<>()");
      }

      writer.emitStatement("int listSize = " + readBuf + ".readInt()");
      writer.beginControlFlow("for (int i = 0; i < listSize; i++)");
      field.read(writer, getTypeArgStrings().get(0) + " val");
      writer.emitStatement(getGetName() + ".add(val)");
      writer.endControlFlow();
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      if (isOptional()) {
        writer.beginControlFlow("if (" + getName() + ".isPresent())");
      }
      writer.emitStatement(writeBuf + ".writeInt(" + getGetName() + ".size())");
      writer.beginControlFlow("for (" + getTypeArgStrings().get(0) + " e : " + getGetName() + ")");
      field.write(writer, "e");
      writer.endControlFlow();
      if (isOptional()) {
        writer.endControlFlow();
      }
    }
  }

  public static class ImmutableListField extends GrapheneField {
    private GrapheneField field;

    public ImmutableListField(GrapheneField field) {
      super(field);
      this.field = field;
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      if (isOptional()) {
        writer.beginControlFlow("if (" + getName() + ".isPresent())");
      }
      writer.emitStatement(writeBuf + ".writeInt(" + getGetName() + ".size())");
      writer.beginControlFlow("for (" + getTypeArgStrings().get(0) + " e : " + getGetName() + ")");
      field.write(writer, "e");
      writer.endControlFlow();
      if (isOptional()) {
        writer.endControlFlow();
      }
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      writer.emitStatement("com.google.common.collect.ImmutableList.Builder<" + getTypeArgStrings().get(0) + "> builder = com.google.common.collect.ImmutableList.builder()");
      writer.emitStatement("int listSize = " + readBuf + ".readInt()");
      writer.beginControlFlow("for (int i = 0; i < listSize; i++)");
      field.read(writer, getTypeArgStrings().get(0) + " val");
      writer.emitStatement("builder.add(val)");
      writer.endControlFlow();
      setField(writer, "builder.build()");
    }
  }


  public static class SetField extends GrapheneField {
    private GrapheneField field;

    public SetField(GrapheneField field) {
      super(field);
      this.field = field;
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      if (isOptional()) {
        writer.emitStatement(getName() + " = java.util.Optional.of(new java.util.HashSet<>())");
      } else {
        writer.emitStatement(getName() + " = new java.util.HashSet<>()");
      }

      writer.emitStatement("int setSize = " + readBuf + ".readInt()");
      writer.beginControlFlow("for (int i = 0; i < setSize; i++)");
      field.read(writer, getTypeArgStrings().get(0) + " val");
      writer.emitStatement(getGetName() + ".add(val)");
      writer.endControlFlow();
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      if (isOptional()) {
        writer.beginControlFlow("if (" + getName() + ".isPresent())");
      }
      writer.emitStatement(writeBuf + ".writeInt(" + field.getGetName() + ".size())");
      writer.beginControlFlow("for (" + field.getTypeArgStrings().get(0) + " e : " + field.getGetName() + ")");
      field.write(writer, "e");
      writer.endControlFlow();
      if (isOptional()) {
        writer.endControlFlow();
      }
    }
  }

  public static class ImmutableSetField extends GrapheneField {
    private GrapheneField field;

    public ImmutableSetField(GrapheneField field) {
      super(field);
      this.field = field;
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      writer.emitStatement("com.google.common.collect.ImmutableSet.Builder<" + getTypeArgStrings().get(0) + "> builder = com.google.common.collect.ImmutableSet.builder()");
      writer.emitStatement("int setSize = " + readBuf + ".readInt()");
      writer.beginControlFlow("for (int i = 0; i < setSize; i++)");
      field.read(writer, getTypeArgStrings().get(0) + " val");
      writer.emitStatement("builder.add(val)");
      writer.endControlFlow();
      setField(writer, "builder.build()");

    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      if (isOptional()) {
        writer.beginControlFlow("if (" + getName() + ".isPresent())");
      }
      writer.emitStatement(writeBuf + ".writeInt(" + field.getGetName() + ".size())");
      writer.beginControlFlow("for (" + field.getTypeArgStrings().get(0) + " e : " + field.getGetName() + ")");
      field.write(writer, "e");
      writer.endControlFlow();
      if (isOptional()) {
        writer.endControlFlow();
      }
    }
  }


  public static class EnumSetField extends GrapheneField {
    private GrapheneField field;

    public EnumSetField(GrapheneField field) {
      super(field);
      this.field = field;
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      writer.emitStatement(field.getName() + " = java.util.EnumSet.noneOf(" + field.getTypeArgStrings().get(0)+ ".class)");
      writer.emitStatement("int size = " + readBuf + ".readInt()");
      writer.beginControlFlow("for (int i = 0; i < size; i++)");
      field.read(writer, getTypeArgStrings().get(0) + " val");
      writer.emitStatement(field.getGetName() + ".add(val)");
      writer.endControlFlow();
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      writer.emitStatement(writeBuf + ".writeInt(" + field.getGetName() + ".size())");
      writer.beginControlFlow("for (" + field.getTypeArgStrings().get(0) + " e : " + field.getGetName() + ")");
      field.write(writer, "e");
      writer.endControlFlow();
    }
  }


  public static class MapField extends GrapheneField {

    private GrapheneField field;

    public MapField(GrapheneField field) {
      super(field);
      this.field = field;
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      String keyType = getTypeArgStrings().get(0);
      if (isOptional()) {
        writer.emitStatement(getName() + " = java.util.Optional.of(new java.util.HashMap<>())");
      } else {
        writer.emitStatement(getName() + " = new java.util.HashMap<>()");
      }
      writer.emitStatement("int mapSize = " + readBuf + ".readInt()");
      writer.beginControlFlow("for (int i = 0; i < mapSize; i++)");
      if (getCapitalizedBufType(keyType).equalsIgnoreCase("object")) {
        writer.emitStatement(keyType + " key = " + readBuf + ".read" + getCapitalizedBufType(keyType) + "(" + keyType + ".class)");
      } else {
        writer.emitStatement(keyType + " key = " + readBuf + ".read" + getCapitalizedBufType(keyType) + "()");
      }
      field.read(writer, getTypeArgStrings().get(1) + " val");
      writer.emitStatement(getGetName() + ".put(key, val)");
      writer.endControlFlow();
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      String keyType = getTypeArgStrings().get(0);
      String keyVar = getName() + "Keys";
      if (isOptional()) {
        writer.beginControlFlow("if (" + getName() + ".isPresent())");
      }
      writer.emitStatement("java.util.Set<" + keyType + "> " + keyVar + " = " + getGetName() + ".keySet()");
      writer.emitStatement(writeBuf + ".writeInt(" + keyVar + ".size())");
      writer.beginControlFlow("for (" + keyType + " k : " + keyVar + ")");
      if (typeInfo.isEmbeddedTypeArg(0)) {
        throw new IllegalArgumentException("Keys are not allowed to be embedded type: " + field.getName());
      }
      writer.emitStatement(writeBuf + ".write" + getCapitalizedBufType(keyType) + "(k)");
      writer.emitStatement(getTypeArgStrings().get(1) + " kv = " + getGetName() + ".get(k)");
      field.write(writer, "kv");
      writer.endControlFlow();
      if (isOptional()) {
        writer.endControlFlow();
      }
    }
  }

  public static class ImmutableMapField extends GrapheneField {
    private GrapheneField field;

    public ImmutableMapField(GrapheneField field) {
      super(field);
      this.field = field;
    }

    @Override
    public void read(JavaWriter writer, String... name) throws IOException {
      String keyType = getTypeArgStrings().get(0);
      writer.emitStatement("com.google.common.collect.ImmutableMap.Builder<" + getCapitalizedBufType(keyType) +
              ", " + getTypeArgStrings().get(1) + "> builder = com.google.common.collect.ImmutableMap.builder();");

      writer.emitStatement("int mapSize = " + readBuf + ".readInt()");
      writer.beginControlFlow("for (int i = 0; i < mapSize; i++)");
      if (getCapitalizedBufType(keyType).equalsIgnoreCase("object")) {
        writer.emitStatement(keyType + " key = " + readBuf + ".read" + getCapitalizedBufType(keyType) + "(" + keyType + ".class)");
      } else {
        writer.emitStatement(keyType + " key = " + readBuf + ".read" + getCapitalizedBufType(keyType) + "()");
      }
      field.read(writer, getTypeArgStrings().get(1) + " val");
      writer.emitStatement("builder.put(key, val)");
      writer.endControlFlow();
      setField(writer, "builder.build()");
    }

    @Override
    public void write(JavaWriter writer, String... name) throws IOException {
      String keyType = getTypeArgStrings().get(0);
      String keyVar = getName() + "Keys";
      if (isOptional()) {
        writer.beginControlFlow("if (" + getName() + ".isPresent())");
      }
      writer.emitStatement("java.util.Set<" + keyType + "> " + keyVar + " = " + getGetName() + ".keySet()");
      writer.emitStatement(writeBuf + ".writeInt(" + keyVar + ".size())");
      writer.beginControlFlow("for (" + keyType + " k : " + keyVar + ")");
      if (typeInfo.isEmbeddedTypeArg(0)) {
        throw new IllegalArgumentException("Keys are not allowed to be embedded type: " + field.getName());
      }
      writer.emitStatement(writeBuf + ".write" + getCapitalizedBufType(keyType) + "(k)");
      writer.emitStatement(getTypeArgStrings().get(1) + " kv = " + getGetName() + ".get(k)");
      field.write(writer, "kv");
      writer.endControlFlow();
      if (isOptional()) {
        writer.endControlFlow();
      }
    }
  }
}
