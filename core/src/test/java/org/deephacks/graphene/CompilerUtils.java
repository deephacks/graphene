package org.deephacks.graphene;

import com.google.common.collect.Sets;
import junit.framework.Test;
import org.deephacks.graphene.internal.processor.SourceAnnotationProcessor;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompilerUtils {
  private static File root = computeMavenProjectRoot(CompilerUtils.class);
  private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
  private static final String sourceBaseDir;
  private static final File generatedSources = new File(root, "target/generated-test-sources/annotations");
  private static final File testClasses = new File(root, "target/test-classes");
  private static final File classes = new File(root, "target/classes");
  static {
    try {
      sourceBaseDir = new File(root, "src/test/java").getCanonicalPath();
      testClasses.mkdirs();
      classes.mkdirs();
      generatedSources.mkdirs();
    }
    catch ( IOException e ) {
      throw new RuntimeException( e );
    }
  }

  public static File computeMavenProjectRoot(Class<?> anyTestClass) {
    final String clsUri = anyTestClass.getName().replace('.', '/') + ".class";
    final URL url = anyTestClass.getClassLoader().getResource(clsUri);
    final String clsPath = url.getPath();
    // located in ./target/test-classes or ./eclipse-out/target
    final File target_test_classes = new File(clsPath.substring(0,
            clsPath.length() - clsUri.length()));
    // lookup parent's parent
    return target_test_classes.getParentFile().getParentFile();
  }

  public static void compile(Class<?>... classes) {
    compile(classes, true);
  }

  public static void compileNoClean(Class<?>[] classes) {
    compile(classes, false);
  }

  private static void compile(Class<?>[] classes, boolean clean) {
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    File[] sourceFiles = getSourceFiles(classes);
    boolean success = compile(new SourceAnnotationProcessor(), diagnostics, clean, sourceFiles);
    StringBuilder sb = new StringBuilder();

    diagnostics.getDiagnostics().stream()
            .filter(d -> d.getKind() == Kind.ERROR).forEach(d -> sb.append(d.toString()));
    if (!success) {
      throw new IllegalArgumentException(sb.toString());
    }
  }

  public static File[] getSourceFiles(Class<?>... classes) {
    List<File> files = new ArrayList<>();
    for (Class<?> cls : classes) {
      String sourceFileName = File.separator + cls.getName().replace( ".", File.separator ) + ".java";
      files.add(new File(sourceBaseDir + sourceFileName));
    }
    return files.toArray(new File[files.size()]);
  }

  public static boolean compile(Processor p1, DiagnosticCollector<JavaFileObject> diagnostics, boolean clean, File... sourceFiles) {
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

    Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(sourceFiles);

    List<String> options = new ArrayList<>();

    try {
      if (clean) {
        cleanGeneratedClasses();
      }
      fileManager.setLocation(StandardLocation.CLASS_PATH,
              Arrays.asList(classes, testClasses, getJarPath(Test.class), getJarPath(Sets.class)));
      fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, Arrays.asList(generatedSources));
      fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(testClasses));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, options, compilationUnits);
    task.setProcessors( Arrays.asList( p1 ) );

    return task.call();
  }
  private static File getJarPath(Class<?> cls) {
    return new File(cls.getProtectionDomain().getCodeSource().getLocation().getPath());
  }

  public static void cleanGeneratedClasses() throws IOException {
    generatedSources.mkdirs();
    Files.walk(Paths.get(generatedSources.getAbsolutePath()))
            .filter(f -> f.toFile().getName().endsWith("java"))
            .forEach(f -> f.toFile().delete());
  }

}