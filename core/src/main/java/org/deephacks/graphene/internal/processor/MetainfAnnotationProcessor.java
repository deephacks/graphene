package org.deephacks.graphene.internal.processor;

import org.deephacks.graphene.Entity;
import org.deephacks.graphene.SchemaRepository;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class MetainfAnnotationProcessor {

  private Set<String> classes = new HashSet<>();

  private ProcessingEnvironment processingEnv;
  private RoundEnvironment roundEnv;

  public MetainfAnnotationProcessor(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv) {
    this.processingEnv = processingEnv;
    this.roundEnv = roundEnv;
  }

  public void processAnnotations() {
    Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Entity.class);
    for (Element e : elements) {
      if (e instanceof  TypeElement){
        TypeElement te = (TypeElement) e;
        String binaryName = getBinaryName(te);
        classes.add(binaryName);
      }
    }
  }

  public void generateFiles() {
    Filer filer = processingEnv.getFiler();

    String resourceFile = SchemaRepository.SCHEMA_PATH;
    try {
      SortedSet<String> allServices = new TreeSet<>();
      try {
        FileObject existingFile = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
        Set<String> oldServices = readServiceFile(existingFile.openInputStream());
        allServices.addAll(oldServices);
      } catch (IOException e) {
      }
      if (allServices.containsAll(classes)) {
        return;
      }

      allServices.addAll(classes);
      FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
      OutputStream out = fileObject.openOutputStream();
      writeServiceFile(allServices, out);
      out.close();
    } catch (IOException e) {
      throw new RuntimeException("Unable to create " + resourceFile + ", " + e);
    }
  }

  /**
   * Returns org.deephacks.Inner$Outer instead of org.deephacks.Inner.Outer.
   */
  private String getBinaryName(TypeElement element) {
    return getBinaryNameImpl(element, element.getSimpleName().toString());
  }

  private String getBinaryNameImpl(TypeElement element, String className) {
    Element enclosingElement = element.getEnclosingElement();

    if (enclosingElement instanceof PackageElement) {
      PackageElement pkg = (PackageElement) enclosingElement;
      if (pkg.isUnnamed()) {
        return className;
      }
      return pkg.getQualifiedName() + "." + className;
    }

    TypeElement typeElement = (TypeElement) enclosingElement;
    return getBinaryNameImpl(typeElement, typeElement.getSimpleName() + "$" + className);
  }

  private Set<String> readServiceFile(InputStream input) throws IOException {
    HashSet<String> serviceClasses = new HashSet<>();
    try (BufferedReader r = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
      String line;
      while ((line = r.readLine()) != null) {
        int commentStart = line.indexOf('#');
        if (commentStart >= 0) {
          line = line.substring(0, commentStart);
        }
        line = line.trim();
        if (!line.isEmpty()) {
          serviceClasses.add(line);
        }
      }
      return serviceClasses;
    }
  }

  private void writeServiceFile(Collection<String> services, OutputStream output) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"))) {
      for (String service : services) {
        writer.write(service);
        writer.newLine();
      }
      writer.flush();
    }
  }
}