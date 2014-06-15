package org.deephacks.graphene.internal.processor;


import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

class BuilderGenerator extends SourceGenerator {
  private String prefix;

  public BuilderGenerator(GrapheneType type) {
    super(type, type.getPackageName() + "." + type.getSimpleClassName() + "Builder");
    this.prefix = type.getBuilderMethodsPrefix();
  }

  @Override
  public String writeSource() throws IOException {
    writer.emitPackage(type.getPackageName());
    if (type.hasArrayField()) {
      writer.emitImports(Arrays.class.getName());
    }
    writer.emitEmptyLine();

    writer.beginType(className, "class", PUBLIC);

    // fields for builder
    for (GrapheneField field : type.getAllFields()) {
      if (field.isOptional()) {
        writer.emitField("java.util.Optional<" + field.getFullTypeString() + ">", field.getName(), PRIVATE, "java.util.Optional.empty()");
      } else {
        writer.emitField(field.getFullTypeString(), field.getName(), PRIVATE);
      }
    }

    writer.emitEmptyLine();

    writeWithMethod();

    writer.emitEmptyLine();

    writeBuildMethod();

    writer.endType();
    writer.close();
    return out.toString();
  }

  private void writeBuildMethod() throws IOException {
    writer.beginMethod(type.getClassName(), "build", PUBLIC);
    StringBuilder sb = new StringBuilder();
    Iterator<GrapheneField> it = type.getAllFields().iterator();
    while (it.hasNext()) {
      GrapheneField field = it.next();
      sb.append(field.getName());
      if (it.hasNext()) {
        sb.append(",");
      }
    }
    writer.emitStatement("return new " + type.getGeneratedGrapheneType() + "("+sb.toString()+")");
    writer.endMethod();
    writer.emitEmptyLine();
  }

  private void writeWithMethod() throws IOException {
    for (GrapheneField field : type.getAllFields()) {
      String methodName = "".equals(prefix.trim()) ? field.getName() : prefix + field.getNameFirstCapitalized();
      writer.beginMethod(className, methodName, PUBLIC, field.getFullTypeString(), field.getName());
      if (field.isOptional()) {
        writer.emitStatement("this." + field.getName() + " = java.util.Optional.ofNullable(" + field.getName() + ")");
      } else {
        writer.emitStatement("this." + field.getName() + " = " + field.getName());
      }

      writer.emitStatement("return this");
      writer.endMethod();
      writer.emitEmptyLine();
    }
  }
}
