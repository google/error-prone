/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ErrorProneJavacPlugin}Test */
@RunWith(JUnit4.class)
public class ErrorProneJavacPluginTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void hello() throws IOException {
    FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    Path source = fileSystem.getPath("Test.java");
    Files.write(
        source,
        ImmutableList.of(
            "package test;",
            "import java.util.HashSet;",
            "import java.util.Set;",
            "class Test {",
            "  public static void main(String[] args) {",
            "    Set<Short> s = new HashSet<>();",
            "    for (short i = 0; i < 100; i++) {",
            "      s.add(i);",
            "      s.remove(i - 1);",
            "    }",
            "    System.out.println(s.size());",
            "  }",
            "}"),
        UTF_8);
    JavacFileManager fileManager = new JavacFileManager(new Context(), false, UTF_8);
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    JavacTask task =
        JavacTool.create()
            .getTask(
                null,
                fileManager,
                diagnosticCollector,
                ImmutableList.of("-Xplugin:ErrorProne", "-XDcompilePolicy=byfile"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(source));
    assertThat(task.call()).isFalse();
    Diagnostic<? extends JavaFileObject> diagnostic =
        diagnosticCollector.getDiagnostics().stream()
            .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
            .collect(onlyElement());
    assertThat(diagnostic.getMessage(ENGLISH)).contains("[CollectionIncompatibleType]");
  }

  @Test
  public void applyFixes() throws IOException {
    // TODO(b/63064865): Test is broken on Windows.  Disable for now.
    Assume.assumeFalse(StandardSystemProperty.OS_NAME.value().startsWith("Windows"));

    Path tmp = temporaryFolder.newFolder().toPath();
    Path fileA = tmp.resolve("A.java");
    Path fileB = tmp.resolve("B.java");
    Files.write(
        fileA,
        ImmutableList.of(
            "class A implements Runnable {", //
            "  public void run() {}",
            "}"),
        UTF_8);
    Files.write(
        fileB,
        ImmutableList.of(
            "class B implements Runnable {", //
            "  public void run() {}",
            "}"),
        UTF_8);
    JavacFileManager fileManager = new JavacFileManager(new Context(), false, UTF_8);
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    JavacTask task =
        JavacTool.create()
            .getTask(
                null,
                fileManager,
                diagnosticCollector,
                ImmutableList.of(
                    "-Xplugin:ErrorProne"
                        + " -XepPatchChecks:MissingOverride -XepPatchLocation:IN_PLACE",
                    "-XDcompilePolicy=byfile"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(fileA, fileB));
    assertWithMessage(Joiner.on('\n').join(diagnosticCollector.getDiagnostics()))
        .that(task.call())
        .isTrue();
    assertThat(Files.readAllLines(fileA, UTF_8))
        .containsExactly(
            "class A implements Runnable {", //
            "  @Override public void run() {}",
            "}")
        .inOrder();
    assertThat(Files.readAllLines(fileB, UTF_8))
        .containsExactly(
            "class B implements Runnable {", //
            "  @Override public void run() {}",
            "}")
        .inOrder();
  }

  @Test
  public void applyToPatchFile() throws IOException {
    // TODO(b/63064865): Test is broken on Windows.  Disable for now.
    Assume.assumeFalse(StandardSystemProperty.OS_NAME.value().startsWith("Windows"));

    Path tmp = temporaryFolder.newFolder().toPath();
    Path patchDir = temporaryFolder.newFolder().toPath();
    Files.createDirectories(patchDir);
    Path patchFile = patchDir.resolve("error-prone.patch");
    // verify that any existing content in the patch file is deleted
    // Ignored - this behavior is not desirable with the addition of HubSpotPatchUtils
    // Files.write(patchFile, ImmutableList.of("--- C.java", "--- D.java"), UTF_8);
    Path fileA = tmp.resolve("A.java");
    Path fileB = tmp.resolve("B.java");
    Files.write(
        fileA,
        ImmutableList.of(
            "class A implements Runnable {", //
            "  public void run() {}",
            "}"),
        UTF_8);
    Files.write(
        fileB,
        ImmutableList.of(
            "class B implements Runnable {", //
            "  public void run() {}",
            "}"),
        UTF_8);
    JavacFileManager fileManager = new JavacFileManager(new Context(), false, UTF_8);
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    JavacTask task =
        JavacTool.create()
            .getTask(
                null,
                fileManager,
                diagnosticCollector,
                ImmutableList.of(
                    "-Xplugin:ErrorProne"
                        + " -XepPatchChecks:MissingOverride -XepPatchLocation:"
                        + patchDir,
                    "-XDcompilePolicy=byfile"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(fileA, fileB));
    assertWithMessage(Joiner.on('\n').join(diagnosticCollector.getDiagnostics()))
        .that(task.call())
        .isTrue();
    assertThat(
            Files.readAllLines(patchFile, UTF_8).stream()
                .filter(l -> l.startsWith("--- "))
                .map(l -> Paths.get(l.substring("--- ".length())).getFileName().toString())
                .collect(toImmutableList()))
        .containsExactly("A.java", "B.java");
  }

  @Test
  public void noPolicyGiven() throws IOException {
    FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    Path source = fileSystem.getPath("Test.java");
    Files.write(source, "class Test {}".getBytes(UTF_8));
    JavacFileManager fileManager = new JavacFileManager(new Context(), false, UTF_8);
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    StringWriter sw = new StringWriter();
    JavacTask task =
        JavacTool.create()
            .getTask(
                new PrintWriter(sw, true),
                fileManager,
                diagnosticCollector,
                ImmutableList.of("-Xplugin:ErrorProne"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(source));
    RuntimeException expected = assertThrows(RuntimeException.class, () -> task.call());
    assertThat(expected)
        .hasMessageThat()
        .contains("The default compilation policy (by-todo) is not supported");
  }

  @Test
  public void explicitBadPolicyGiven() throws IOException {
    FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    Path source = fileSystem.getPath("Test.java");
    Files.write(source, "class Test {}".getBytes(UTF_8));
    JavacFileManager fileManager = new JavacFileManager(new Context(), false, UTF_8);
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    StringWriter sw = new StringWriter();
    JavacTask task =
        JavacTool.create()
            .getTask(
                new PrintWriter(sw, true),
                fileManager,
                diagnosticCollector,
                ImmutableList.of("-XDcompilePolicy=bytodo", "-Xplugin:ErrorProne"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(source));
    RuntimeException expected = assertThrows(RuntimeException.class, () -> task.call());
    assertThat(expected).hasMessageThat().contains("-XDcompilePolicy=bytodo is not supported");
  }

  @Test
  public void stopOnErrorPolicy() throws IOException {
    FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    Path one = fileSystem.getPath("One.java");
    Path two = fileSystem.getPath("Two.java");
    Files.write(
        one,
        ImmutableList.of(
            "package test;",
            "import java.util.HashSet;",
            "import java.util.Set;",
            "class One {",
            "  public static void main(String[] args) {",
            "    Set<Short> s = new HashSet<>();",
            "    for (short i = 0; i < 100; i++) {",
            "      s.add(i);",
            "      s.remove(i - 1);",
            "    }",
            "    System.out.println(s.size());",
            "  }",
            "}"),
        UTF_8);
    Files.write(
        two,
        ImmutableList.of(
            "package test;",
            "class Two {",
            "  public static void main(String[] args) {",
            "    new Exception();",
            "  }",
            "}"),
        UTF_8);
    JavacFileManager fileManager = new JavacFileManager(new Context(), false, UTF_8);
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    JavacTask task =
        JavacTool.create()
            .getTask(
                null,
                fileManager,
                diagnosticCollector,
                ImmutableList.of(
                    "-Xplugin:ErrorProne",
                    "-XDcompilePolicy=byfile",
                    "-XDshould-stop.ifError=LOWER"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(one, two));
    assertThat(task.call()).isFalse();
    ImmutableList<String> diagnostics =
        diagnosticCollector.getDiagnostics().stream()
            .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
            .map(d -> d.getMessage(ENGLISH))
            .collect(toImmutableList());
    assertThat(diagnostics).hasSize(2);
    assertThat(diagnostics.get(0)).contains("[CollectionIncompatibleType]");
    assertThat(diagnostics.get(1)).contains("[DeadException]");
  }

  /** A bugpattern for testing. */
  @BugPattern(name = "TestCompilesWithFix", summary = "", severity = SeverityLevel.ERROR)
  public static class TestCompilesWithFix extends BugChecker implements ReturnTreeMatcher {

    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      // add a no-op fix to exercise compilesWithFix
      SuggestedFix fix = SuggestedFix.postfixWith(tree, "//");
      return SuggestedFixes.compilesWithFix(fix, state)
          ? describeMatch(tree, fix)
          : Description.NO_MATCH;
    }
  }

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void compilesWithFix() throws IOException {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      jos.putNextEntry(new JarEntry("META-INF/services/" + BugChecker.class.getName()));
      jos.write(TestCompilesWithFix.class.getName().getBytes(UTF_8));
    }

    FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    Path source = fileSystem.getPath("Test.java");
    Files.write(
        source,
        ImmutableList.of(
            "import java.util.HashSet;",
            "import java.util.Set;",
            "class Test {",
            "  void f() {",
            "    return;",
            "  }",
            "}"),
        UTF_8);
    JavacFileManager fileManager = new JavacFileManager(new Context(), false, UTF_8);
    fileManager.setLocation(
        StandardLocation.ANNOTATION_PROCESSOR_PATH,
        Streams.concat(
                Stream.of(libJar),
                Streams.stream(
                        Splitter.on(File.pathSeparatorChar)
                            .split(StandardSystemProperty.JAVA_CLASS_PATH.value()))
                    .map(File::new))
            .collect(toImmutableList()));
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    JavacTask task =
        JavacTool.create()
            .getTask(
                null,
                fileManager,
                diagnosticCollector,
                ImmutableList.of(
                    "-Xplugin:ErrorProne -XepDisableAllChecks -Xep:TestCompilesWithFix:ERROR",
                    "-XDcompilePolicy=byfile"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(source));
    assertThat(task.call()).isFalse();
    Diagnostic<? extends JavaFileObject> diagnostic =
        diagnosticCollector.getDiagnostics().stream()
            .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
            .collect(onlyElement());
    assertThat(diagnostic.getMessage(ENGLISH)).contains("[TestCompilesWithFix]");
  }
}
