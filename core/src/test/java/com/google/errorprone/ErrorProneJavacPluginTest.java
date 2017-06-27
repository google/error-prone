/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;

import com.google.common.base.Joiner;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
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
                ImmutableList.of("-Xplugin:ErrorProne"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(source));
    assertThat(task.call()).isFalse();
    Diagnostic<? extends JavaFileObject> diagnostic =
        diagnosticCollector
            .getDiagnostics()
            .stream()
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
                        + " -XepPatchChecks:MissingOverride -XepPatchLocation:IN_PLACE"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(fileA, fileB));
    assertThat(task.call())
        .named(Joiner.on('\n').join(diagnosticCollector.getDiagnostics()))
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
    Files.write(patchFile, ImmutableList.of("--- C.java", "--- D.java"), UTF_8);
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
                        + patchDir.toString()),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(fileA, fileB));
    assertThat(task.call())
        .named(Joiner.on('\n').join(diagnosticCollector.getDiagnostics()))
        .isTrue();
    assertThat(
            Files.readAllLines(patchFile, UTF_8)
                .stream()
                .filter(l -> l.startsWith("--- "))
                .map(l -> Paths.get(l.substring("--- ".length())).getFileName().toString())
                .collect(toImmutableList()))
        .containsExactly("A.java", "B.java");
  }
}
