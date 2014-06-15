package org.deephacks.graphene.internal.processor;

import org.deephacks.graphene.Key;
import org.deephacks.graphene.internal.processor.GrapheneField.ArrayField;
import org.deephacks.graphene.internal.processor.GrapheneField.EmbeddedField;
import org.deephacks.graphene.internal.processor.GrapheneField.EnumSetField;
import org.deephacks.graphene.internal.processor.GrapheneField.ImmutableListField;
import org.deephacks.graphene.internal.processor.GrapheneField.ImmutableMapField;
import org.deephacks.graphene.internal.processor.GrapheneField.ImmutableSetField;
import org.deephacks.graphene.internal.processor.GrapheneField.KeyField;
import org.deephacks.graphene.internal.processor.GrapheneField.ListField;
import org.deephacks.graphene.internal.processor.GrapheneField.MapField;
import org.deephacks.graphene.internal.processor.GrapheneField.ReferenceField;
import org.deephacks.graphene.internal.processor.GrapheneField.SetField;
import org.deephacks.graphene.internal.processor.GrapheneField.SingleField;
import org.deephacks.graphene.internal.processor.GrapheneField.ValueField;
import org.deephacks.graphene.internal.processor.SourceAnnotationProcessor.CompileException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

abstract class GrapheneType {
  static final List<String> KEYWORDS = Arrays.asList(
          "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
          "default", "do", "double", "else", "extends", "final", "finally", "float", "for", "goto", "if",
          "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package",
          "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
          "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
          "true", "false", "null");

  private ProcessingEnvironment processingEnv;
  private String packageName;
  private String className;
  private LinkedHashMap<String, GrapheneField> fields = new LinkedHashMap<>();
  private LinkedHashMap<String, KeyField> keys = new LinkedHashMap<>();
  private boolean hasToString = false;
  private boolean hasHashCode = false;
  private boolean hasEquals = false;
  private boolean hasPostConstruct = false;
  private String builderMethodsPrefix = "";
  private CompileException compileException = new CompileException();

  public GrapheneType(ProcessingEnvironment processingEnv, TypeElement type, String builderMethodsPrefix) throws CompileException {
    Types typeUtils = processingEnv.getTypeUtils();
    this.processingEnv = processingEnv;
    this.className = TypeUtil.classNameOf(type);
    this.packageName = TypeUtil.packageNameOf(type);
    this.builderMethodsPrefix = builderMethodsPrefix;
    /*
    For some reason type.getModifiers() always seem to be Modifier.STATIC even though type is not?!
    if (type.getNestingKind() == NestingKind.MEMBER && !type.getModifiers().contains(Modifier.STATIC)) {
      compileException.add("Nested interfaces must be static", type);
    }
    */

    List<ExecutableElement> methods = new ArrayList<>();
    findMethods(type, methods);
    for (ExecutableElement m : methods) {
      if (isToString(m)) {
        hasToString = true;
      } else if (isHashCode(m)) {
        hasHashCode = true;
      } else if (isEquals(m)) {
        hasEquals = true;
      } else if (isPostConstruct(m)) {
        hasPostConstruct = true;
      }
    }
    methods = findGetters(methods);
    for (ExecutableElement m : methods) {
      TypeElement returnType = (TypeElement) typeUtils.asElement(m.getReturnType());
      TypeMirror returnTypeMirror = m.getReturnType();
      boolean isSimpleKey = false;
      Integer keyPosition = null;
      Integer keySize = null;
      if (returnType != null || returnTypeMirror != null) {
        isSimpleKey = m.getAnnotation(Key.class) != null;
        if (isSimpleKey) {
          keyPosition = m.getAnnotation(Key.class).position();
          keySize = m.getAnnotation(Key.class).size();
        }
      }

      TypeInfo typeInfo = new TypeInfo(returnTypeMirror, returnType, typeUtils);
      String fieldName = m.getSimpleName().toString();
      if (KEYWORDS.contains(fieldName)) {
        compileException.add("Field names with Java keyword are not allowed.", m);
      }
      GrapheneField field;
      if (typeInfo.isEmbedded()) {
        field = new EmbeddedField(fieldName, typeInfo);
      } else if (typeInfo.isReference()) {
        field = new ReferenceField(fieldName, typeInfo);
      } else if (typeInfo.isKey() || isSimpleKey) {
        if (keys.size() > 1 && !(this instanceof KeyType) && isSimpleKey) {
          throw new IllegalArgumentException("Only one key field acceptable.");
        }
        if (!typeInfo.isKey() && (keySize == null || keySize == 0)) {
          keySize = TypeUtil.getSize(typeInfo.getFullTypeString());
        }
        ValueField f = new ValueField(fieldName, m.isDefault(), typeInfo, keySize);
        KeyField key = new KeyField(fieldName, typeInfo, f, keyPosition, keySize);
        if (!fieldsContains(key)) {
          keys.put(key.getName(), key);
        }
        continue;
      } else {
        field = new ValueField(fieldName, m.isDefault(), typeInfo, null);
      }
      GrapheneField f;
      if (typeInfo.isArray()) {
        f = new ArrayField(field);
      } else if (typeInfo.isList()) {
        f = new ListField(field);
      } else if (typeInfo.isImmutableList()) {
        f = new ImmutableListField(field);
      } else if (typeInfo.isSet()) {
        f = new SetField(field);
      } else if (typeInfo.isImmutableSet()) {
        f = new ImmutableSetField(field);
      } else if (typeInfo.isEnumSet()) {
        f = new EnumSetField(field);
      } else if (typeInfo.isMap()) {
        f = new MapField(field);
      } else if (typeInfo.isImmutableMap()) {
        f = new ImmutableMapField(field);
      } else {
        f = new SingleField(field);
      }
      if (!getAllFields().contains(f)) {
        fields.put(f.getName(), f);
      }
    }
    reportCompileException();
  }

  private boolean fieldsContains(GrapheneField field) {
    if (fields.keySet().contains(field.getName())) {
      return true;
    }
    if (keys.keySet().contains(field.getName())) {
      return true;
    }
    return false;
  }


  public String getBuilderMethodsPrefix() {
    return builderMethodsPrefix;
  }

  public List<GrapheneField> getFields() {
    return new ArrayList<>(fields.values());
  }

  public List<KeyField> getKeys() {
    return new ArrayList<>(keys.values());
  }

  public List<GrapheneField> getAllFields() {
    return concat(getKeys().stream(), getFields().stream()).collect(toList());
  }

  public boolean hasArrayField() {
    for (GrapheneField property : fields.values()) {
      if (property instanceof ArrayField) {
        return true;
      }
    }
    return false;
  }

  public List<String> getAllFieldsAsStrings() {
    List<String> propertyStrings = new ArrayList<>();
    for (GrapheneField field : getAllFields()) {
      if (field.isOptional()) {
        propertyStrings.add("java.util.Optional<" + field.getFullTypeString() + ">");
      } else {
        propertyStrings.add(field.getFullTypeString());
      }

      propertyStrings.add(field.getName());
    }
    return propertyStrings;
  }

  public abstract String getGeneratedGrapheneType();

  public String getClassName() {
    return className;
  }

  public String getSimpleClassName() {
    if (className.contains(".")) {
      return className.substring(className.lastIndexOf('.') + 1);
    } else {
      return className;
    }
  }

  public String getPackageName() {
    return packageName;
  }

  public boolean hasToString() {
    return hasToString;
  }

  public boolean hasHashCode() {
    return hasHashCode;
  }

  public boolean hasEquals() {
    return hasEquals;
  }

  public boolean hasPostConstruct() {
    return hasPostConstruct;
  }


  private void findMethods(TypeElement type, List<ExecutableElement> methods) {
    Types typeUtils = processingEnv.getTypeUtils();
    for (TypeMirror superInterface : type.getInterfaces()) {
      findMethods((TypeElement) typeUtils.asElement(superInterface), methods);
    }
    List<ExecutableElement> theseMethods = ElementFilter.methodsIn(type.getEnclosedElements());
    for (ExecutableElement method : theseMethods) {
        methods.add(method);
    }
  }

  private List<ExecutableElement> findGetters(List<ExecutableElement> methods) {
    List<ExecutableElement> toImplement = new ArrayList<>();
    for (ExecutableElement method : methods) {
      if (method.getSimpleName().toString().startsWith("get") && !isToStringOrEqualsOrHashCode(method)) {
        if (method.getParameters().isEmpty() && method.getReturnType().getKind() != TypeKind.VOID) {
          if (isReferenceArrayType(method.getReturnType())) {
            compileException.add("Class can only have primitive array fields", method);
          }
          toImplement.add(method);
        }
      }
    }
    return toImplement;
  }

  private static boolean isReferenceArrayType(TypeMirror type) {
    return type.getKind() == TypeKind.ARRAY
            && !((ArrayType) type).getComponentType().getKind().isPrimitive();
  }

  private boolean isToStringOrEqualsOrHashCode(ExecutableElement method) {
    return isToString(method) || isEquals(method) || isHashCode(method);
  }

  private boolean isToString(ExecutableElement method) {
    String name = method.getSimpleName().toString();
    boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
    return (isStatic && name.equals("toString") && method.getParameters().size() == 1
            && method.getParameters().get(0).asType().toString().equals(className));
  }

  private boolean isHashCode(ExecutableElement method) {
    String name = method.getSimpleName().toString();
    boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
    return (isStatic && name.equals("hashCode") && method.getParameters().size() == 1
            && method.getParameters().get(0).asType().toString().equals(className));
  }

  private boolean isEquals(ExecutableElement method) {
    String name = method.getSimpleName().toString();
    boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
    return (isStatic && name.equals("equals") && method.getParameters().size() == 2
            && method.getParameters().get(0).asType().toString().equals(className)
            && method.getParameters().get(1).asType().toString().equals(className));
  }

  private boolean isPostConstruct(ExecutableElement method) {
    String name = method.getSimpleName().toString();
    boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
    return (isStatic && name.equals("postConstruct") && method.getParameters().size() == 1
            && method.getParameters().get(0).asType().toString().equals(className));
  }

  public void reportCompileException() throws CompileException {
    if (!compileException.errors.isEmpty()) {
      throw compileException;
    }
  }

  public static class EntityType extends GrapheneType {

    public EntityType(ProcessingEnvironment processingEnv, TypeElement type, String builderMethodsPrefix) throws CompileException {
      super(processingEnv, type, builderMethodsPrefix);
    }

    @Override
    public String getGeneratedGrapheneType() {
      return getPackageName() + ".Entity_" + getSimpleClassName();
    }
  }

  public static class EmbeddedType extends GrapheneType {

    public EmbeddedType(ProcessingEnvironment processingEnv, TypeElement type, String builderMethodsPrefix) throws CompileException {
      super(processingEnv, type, builderMethodsPrefix);
    }

    @Override
    public String getGeneratedGrapheneType() {
      return getPackageName() + ".Embedded_" + getSimpleClassName();
    }

  }

  public static class KeyType extends GrapheneType {

    public KeyType(ProcessingEnvironment processingEnv, TypeElement type, String builderMethodsPrefix) throws CompileException {
      super(processingEnv, type, builderMethodsPrefix);
    }
    @Override
    public String getGeneratedGrapheneType() {
      return getPackageName() + ".Key_" + getSimpleClassName();
    }
  }

}

