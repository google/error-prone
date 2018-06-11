/*
 * Copyright 2018 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.apidiff;

import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.file.JavacFileManager;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/** Compilation test helpers for ApiDiff tools. */
public class CompilationBuilderHelpers {

  /** A collection of sources to compile. */
  public static class SourceBuilder {
    final File tempFolder;
    final List<Path> sources = new ArrayList<>();

    public SourceBuilder(File tempFolder) {
      this.tempFolder = tempFolder;
    }

    public SourceBuilder addSourceLines(String name, String... lines) throws IOException {
      Path filePath = Paths.get(tempFolder.getAbsolutePath(), name);
      sources.add(filePath);
      Files.write(filePath, Arrays.asList(lines), UTF_8, StandardOpenOption.CREATE);
      return this;
    }

    public List<Path> build() {
      return ImmutableList.copyOf(sources);
    }
  }

  /** A wrapper around {@link JavaCompiler}. */
  public static class CompilationBuilder {

    final JavaCompiler compiler;
    final File tempFolder;
    final JavacFileManager fileManager;

    private Collection<Path> sources = Collections.emptyList();
    private Collection<Path> classpath = Collections.emptyList();
    private Iterable<String> javacopts = Collections.emptyList();

    public CompilationBuilder(
        JavaCompiler compiler, File tempFolder, JavacFileManager fileManager) {
      this.compiler = compiler;
      this.tempFolder = tempFolder;
      this.fileManager = fileManager;
    }

    public CompilationBuilder setSources(Collection<Path> sources) {
      this.sources = sources;
      return this;
    }

    public CompilationBuilder setClasspath(Collection<Path> classpath) {
      this.classpath = classpath;
      return this;
    }

    CompilationBuilder setJavacopts(Iterable<String> javacopts) {
      this.javacopts = javacopts;
      return this;
    }

    CompilationResult compile() throws IOException {
      Path output = tempFolder.toPath();
      DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
      StringWriter errorOutput = new StringWriter();

      fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, classpath);
      fileManager.setLocationFromPaths(StandardLocation.SOURCE_PATH, Collections.<Path>emptyList());
      fileManager.setLocationFromPaths(
          StandardLocation.ANNOTATION_PROCESSOR_PATH, Collections.<Path>emptyList());

      fileManager.setLocationFromPaths(
          StandardLocation.CLASS_OUTPUT, Collections.singleton(output));

      boolean ok =
          compiler
              .getTask(
                  new PrintWriter(errorOutput, true),
                  fileManager,
                  diagnosticCollector,
                  javacopts,
                  /*classes=*/ Collections.<String>emptyList(),
                  fileManager.getJavaFileObjectsFromPaths(sources))
              .call();

      return CompilationResult.create(
          ok, diagnosticCollector.getDiagnostics(), errorOutput.toString(), output);
    }

    CompilationResult compileOrDie() throws IOException {
      CompilationResult result = compile();
      assertWithMessage("Compilation failed unexpectedly")
          .that(result.ok())
          .named(Joiner.on('\n').join(result.diagnostics()))
          .isTrue();
      return result;
    }

    public Path compileOutputToJarOrDie() throws IOException {
      final Path outputDir = compileOrDie().classOutput();
      final Path outputJar = outputDir.resolve("output.jar");
      try (OutputStream os = Files.newOutputStream(outputJar);
          final JarOutputStream jos = new JarOutputStream(os)) {
        Files.walkFileTree(
            outputDir,
            new SimpleFileVisitor<Path>() {
              @Override
              public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                  throws IOException {
                if (path.toString().endsWith(".class")) {
                  // Manually join so that we use / instead of the platform default.
                  jos.putNextEntry(new JarEntry(Joiner.on('/').join(outputDir.relativize(path))));
                  Files.copy(path, jos);
                }
                return FileVisitResult.CONTINUE;
              }
            });
      }
      return outputJar;
    }
  }

  /** The output from a compilation. */
  @AutoValue
  abstract static class CompilationResult {
    abstract boolean ok();

    abstract ImmutableList<Diagnostic<? extends JavaFileObject>> diagnostics();

    abstract String errorOutput();

    abstract Path classOutput();

    static CompilationResult create(
        boolean ok,
        Iterable<Diagnostic<? extends JavaFileObject>> diagnostics,
        String errorOutput,
        Path classOutput) {
      return new AutoValue_CompilationBuilderHelpers_CompilationResult(
          ok, ImmutableList.copyOf(diagnostics), errorOutput, classOutput);
    }
  }
}
