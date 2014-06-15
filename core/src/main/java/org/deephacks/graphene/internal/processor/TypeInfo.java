package org.deephacks.graphene.internal.processor;

import org.deephacks.graphene.Embedded;
import org.deephacks.graphene.Entity;
import org.deephacks.graphene.Key;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class TypeInfo {
  private final TypeMirror typeMirror;
  /**
   * primitive values does not have a declared type
   */
  private DeclaredType declaredType;
  private List<TypeMirror> typeArgs = new ArrayList<>();
  private List<String> typeArgStrings = new ArrayList<>();
  private String fullTypeString;
  private String typeString;
  private String packageName;
  private String simpleClassName;
  private Types typeUtils;
  private boolean isOptional = false;

  public TypeInfo(TypeMirror typeMirror, TypeElement typeElement, Types typeUtils) {
    this.typeMirror = typeMirror;
    this.typeUtils = typeUtils;
    if (typeElement != null && "java.util.Optional".equals(typeElement.toString())) {
      isOptional = true;
    }
    List<TypeMirror> args = getTypeArgs(typeMirror);
    for (TypeMirror t : args) {
      if (isOptional) {
        typeMirror = args.get(0);
        typeElement = (TypeElement) typeUtils.asElement(typeMirror);
        List<TypeMirror> types = getTypeArgs(t);
        for (TypeMirror t2 : types) {
          typeArgs.add(t2);
          typeArgStrings.add(t2.toString());
        }
      } else {
        typeArgs.add(t);
        typeArgStrings.add(t.toString());
      }
    }
    this.fullTypeString = typeMirror.toString();
    if (!isPrimitive() && !isArray()) {
      this.typeString = rawTypeToString(declaredType, '.');
      this.packageName = TypeUtil.packageNameOf(typeElement);
      this.simpleClassName = TypeUtil.simpleClassNameOf(typeElement);
    }
  }

  public String getGeneratedGrapheneType() {
    String packageName = getPackageName();
    String className = getSimpleClassName();
    if (typeArgs.size() == 1) {
      TypeElement type = (TypeElement) typeUtils.asElement(typeArgs.get(0));
      packageName = TypeUtil.packageNameOf(type);
      className = TypeUtil.simpleClassNameOf(type);
    } else if (typeArgs.size() == 2) {
      TypeElement type = (TypeElement) typeUtils.asElement(typeArgs.get(1));
      packageName = TypeUtil.packageNameOf(type);
      className = TypeUtil.simpleClassNameOf(type);
    }

    if (isEmbedded()) {
      return packageName + ".Embedded_" + className;
    } else if (isKey()) {
      return packageName + ".Key_" + className;
    } else if (isReference()) {
      return packageName + ".Entity_" + className;
    } else {
      throw new IllegalArgumentException("Not a graphene type: " + getFullTypeString());
    }
  }

  public boolean isOptional() {
    return isOptional;
  }

  public boolean isList() {
    return !(isPrimitive() || isArray()) && (typeString.equals("java.util.List") || typeString.equals("java.util.Collection"));
  }

  public boolean isSet() {
    return !(isPrimitive() || isArray()) && (typeString.equals("java.util.Set"));
  }

  public boolean isEnumSet() {
    return !(isPrimitive() || isArray()) && (typeString.equals("java.util.EnumSet"));
  }

  public boolean isMap() {
    return !(isPrimitive() || isArray()) && typeString.equals("java.util.Map");
  }

  public boolean isImmutableList() {
    return !(isPrimitive() || isArray()) && (typeString.equals("com.google.common.collect.ImmutableList") || typeString.equals("com.google.common.collect.ImmutableCollection"));
  }

  public boolean isImmutableSet() {
    return !(isPrimitive() || isArray()) && (typeString.equals("com.google.common.collect.ImmutableSet"));
  }

    public boolean isImmutableMap() {
    return !(isPrimitive() || isArray()) && typeString.equals("com.google.common.collect.ImmutableMap");
  }

  public TypeKind getTypeKind() {
    return typeMirror.getKind();
  }

  public void addTypeArgs(TypeMirror typeArg) {
    this.typeArgs.add(typeArg);
    this.typeArgStrings.add(typeArg.toString());
  }

  public List<TypeMirror> getTypeArgs() {
    return typeArgs;
  }

  public List<String> getTypeArgStrings() {
    return typeArgStrings;
  }

  public boolean isEmbedded() {
    if (declaredType != null && declaredType.asElement().getAnnotation(Embedded.class) != null) {
      return true;
    }
    final AtomicReference<Boolean> ref = new AtomicReference<>(false);
    for (TypeMirror type : getTypeArgs()) {
      type.accept(new SimpleTypeVisitor8<Void, Void>() {
        @Override
        public Void visitDeclared(DeclaredType declaredType, Void v) {
          ref.set(declaredType.asElement().getAnnotation(Embedded.class) != null);
          return null;
        }
      }, null);
    }
    return ref.get();
  }

  public boolean isEmbeddedTypeArg(int i) {
    final AtomicReference<Boolean> ref = new AtomicReference<>(false);
    TypeMirror type = getTypeArgs().get(i);
    type.accept(new SimpleTypeVisitor8<Void, Void>() {
      @Override
      public Void visitDeclared(DeclaredType declaredType, Void v) {
        ref.set(declaredType.asElement().getAnnotation(Embedded.class) != null);
        return null;
      }
    }, null);
    return ref.get();
  }

  public boolean isKey() {
    return declaredType != null && declaredType.asElement().getAnnotation(Key.class) != null;
  }

  public boolean isReference() {
    if (declaredType != null && declaredType.asElement().getAnnotation(Entity.class) != null) {
      return true;
    }
    final AtomicReference<Boolean> ref = new AtomicReference<>(false);
    for (TypeMirror type : getTypeArgs()) {
      type.accept(new SimpleTypeVisitor8<Void, Void>() {
        @Override
        public Void visitDeclared(DeclaredType declaredType, Void v) {
          ref.set(declaredType.asElement().getAnnotation(Entity.class) != null);
          return null;
        }
      }, null);
    }
    return ref.get();
  }

  public String getFullTypeString() {
    return fullTypeString;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getSimpleClassName() {
    return simpleClassName;
  }


  public boolean isPrimitive() {
    return typeMirror.getKind().isPrimitive();
  }

  public boolean isArray() {
    return typeMirror.getKind() == TypeKind.ARRAY;
  }

  private List<TypeMirror> getTypeArgs(TypeMirror typeMirror) {
    if (typeMirror.getKind().isPrimitive()) {
      return new ArrayList<>();
    }
    List<TypeMirror> typeArgs = new ArrayList<>();
    typeMirror.accept(new SimpleTypeVisitor8<Void, Void>() {
      @Override
      public Void visitDeclared(DeclaredType declaredType, Void v) {
        TypeInfo.this.declaredType = declaredType;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        for (int i = 0; i < typeArguments.size(); i++) {
          typeArgs.add(typeArguments.get(i));
        }
        return null;
      }

      @Override
      public Void visitArray(ArrayType arrayType, Void v) {
        TypeMirror type = arrayType.getComponentType();
        if (type instanceof PrimitiveType) {
          typeArgs.add(type); // Don't box, since this is an array.
        }
        return null;
      }

      @Override
      public Void visitError(ErrorType errorType, Void v) {
        // Error type found, a type may not yet have been generated, but we need the type
        // so we can generate the correct code in anticipation of the type being available
        // to the compiler.

        // Paramterized types which don't exist are returned as an error type whose name is "<any>"
        if ("<any>".equals(errorType.toString())) {
          throw new IllegalStateException("GrapheneType reported as <any> is likely a not-yet generated parameterized type.");
        }
        // TODO(cgruber): Figure out a strategy for non-FQCN cases.
        addTypeArgs(errorType);
        return null;
      }

      @Override
      protected Void defaultAction(TypeMirror typeMirror, Void v) {
        throw new UnsupportedOperationException(
                "Unexpected TypeKind " + typeMirror.getKind() + " for " + typeMirror);
      }
    }, null);
    return typeArgs;
  }

  private PackageElement getPackage(Element type) {
    while (type.getKind() != ElementKind.PACKAGE) {
      type = type.getEnclosingElement();
    }
    return (PackageElement) type;
  }

  private String rawTypeToString(TypeMirror type, char innerClassSeparator) {
    if (!(type instanceof DeclaredType)) {
      throw new IllegalArgumentException("Unexpected type: " + type + " " + typeMirror);
    }
    StringBuilder result = new StringBuilder();
    DeclaredType declaredType = (DeclaredType) type;
    rawTypeToString(result, (TypeElement) declaredType.asElement(), innerClassSeparator);
    return result.toString();
  }


  private void rawTypeToString(StringBuilder result, TypeElement type,
                               char innerClassSeparator) {
    String packageName = getPackage(type).getQualifiedName().toString();
    String qualifiedName = type.getQualifiedName().toString();
    if (packageName.isEmpty()) {
      result.append(qualifiedName.replace('.', innerClassSeparator));
    } else {
      result.append(packageName);
      result.append('.');
      result.append(
              qualifiedName.substring(packageName.length() + 1).replace('.', innerClassSeparator));
    }
  }
}
