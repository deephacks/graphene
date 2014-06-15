package org.deephacks.graphene.internal.processor;

import org.deephacks.graphene.Embedded;
import org.deephacks.graphene.Entity;
import org.deephacks.graphene.Key;
import org.deephacks.graphene.internal.processor.GrapheneType.EmbeddedType;
import org.deephacks.graphene.internal.processor.GrapheneType.EntityType;
import org.deephacks.graphene.internal.processor.GrapheneType.KeyType;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@SupportedOptions({ "debug", "verify" })
public class SourceAnnotationProcessor extends AbstractProcessor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new HashSet<>(Arrays.asList(Entity.class.getName(), Embedded.class.getName(), Key.class.getName()));
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  private void abortWithError(String msg, Element e) throws CompileException {
    reportError(msg, e);
    throw new CompileException();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    MetainfAnnotationProcessor metainfAnnotationProcessor = new MetainfAnnotationProcessor(processingEnv, roundEnv);
    metainfAnnotationProcessor.processAnnotations();
    metainfAnnotationProcessor.generateFiles();
    boolean claimed = false;
    for (TypeElement type : annotations) {
      Class<? extends Annotation> annotation = asAnnotation(type);
      if (Entity.class.isAssignableFrom(annotation)) {
        claimed = true;
        process(roundEnv, annotation);
      } else if (Embedded.class.isAssignableFrom(annotation)) {
        claimed = true;
        process(roundEnv, annotation);
      } else if (Key.class.isAssignableFrom(annotation)) {
        claimed = true;
        process(roundEnv, annotation);
      }
    }
    return claimed;
  }

  private void process(RoundEnvironment roundEnv, Class<? extends Annotation> annotation) {
    Collection<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
    Collection<? extends TypeElement> types = ElementFilter.typesIn(annotatedElements);
    for (TypeElement type : types) {
      try {
        if (type.getAnnotation(Entity.class) != null) {
          processType(type, annotation, type.getAnnotation(Entity.class).builderPrefix());
        } else if (type.getAnnotation(Embedded.class) != null) {
          processType(type, annotation, type.getAnnotation(Embedded.class).builderPrefix());
        } else if (type.getAnnotation(Key.class) != null) {
          processType(type, annotation, type.getAnnotation(Key.class).builderPrefix());
        }
      } catch (CompileException e) {
        reportError(e);
      } catch (IOException | RuntimeException e) {
        e.printStackTrace();
        reportError("@" + annotation.getName() + "  processor threw an exception: " + e, type);
      }
    }
  }

  private void processType(TypeElement type, Class<? extends Annotation> annotation, String builderMethodPrefix) throws CompileException, IOException {
    if (type.getKind() != ElementKind.INTERFACE) {
      abortWithError("@" + annotation.getName() + " can only be applied to interfaces", type);
    }
    if (ancestorIs(type)) {
      abortWithError("One @" + annotation.getName() + " class may not extend another", type);
    }
    GrapheneType grapheneType;
    if (type.getAnnotation(Entity.class) != null) {
      grapheneType = new EntityType(processingEnv, type, builderMethodPrefix);
    } else if (type.getAnnotation(Embedded.class) != null) {
      grapheneType = new EmbeddedType(processingEnv, type, builderMethodPrefix);
    } else if (type.getAnnotation(Key.class) != null) {
      grapheneType = new KeyType(processingEnv, type, builderMethodPrefix);
    } else {
      throw new IllegalStateException("Type not recognized " + type);
    }

    GrapheneTypeGenerator typeGenerator = new GrapheneTypeGenerator(grapheneType);
    String sourceText = typeGenerator.writeSource();
    writeSourceFile(typeGenerator.getClassName(), sourceText, type);

    BuilderGenerator builderGenerator = new BuilderGenerator(grapheneType);
    sourceText = builderGenerator.writeSource();
    writeSourceFile(builderGenerator.getClassName(), sourceText, type);
  }

  @SuppressWarnings("unchecked")
  private Class<? extends Annotation> asAnnotation(TypeElement typeElement) {
    try {
      return (Class<? extends Annotation>) Class.forName(typeElement.getQualifiedName().toString());
    } catch (ClassNotFoundException e) {
      reportError("GrapheneType element was not found.", typeElement);
      return null;
    }
  }

  private boolean ancestorIs(TypeElement type) {
    while (true) {
      TypeMirror parentMirror = type.getSuperclass();
      if (parentMirror.getKind() == TypeKind.NONE) {
        return false;
      }
      Types typeUtils = processingEnv.getTypeUtils();
      TypeElement parentElement = (TypeElement) typeUtils.asElement(parentMirror);
      if (parentElement.getAnnotation(Entity.class) != null) {
        return true;
      }
      type = parentElement;
    }
  }

  private void writeSourceFile(String className, String text, TypeElement originatingType) {
    try {
      JavaFileObject sourceFile =
              processingEnv.getFiler().createSourceFile(className, originatingType);
      try (Writer writer = sourceFile.openWriter()) {
        writer.write(text);
      }
    } catch (IOException e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
              "Could not write generated class " + className + ": " + e);
    }
  }

  @SuppressWarnings("serial")
  public static class CompileException extends Exception {
    Map<String, Element> errors = new HashMap<>();
    public CompileException() { }

    public void add(String msg, Element element) { errors.put(msg, element); }

    public Map<String,Element> getErrors() {
      return errors;
    }
  }

  public void reportError(CompileException ex) {
    for (Entry<String, Element> entry : ex.getErrors().entrySet()) {
      reportError(entry.getKey(), entry.getValue());
    }
  }

  public void reportError(String msg, Element element) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element);
  }

}