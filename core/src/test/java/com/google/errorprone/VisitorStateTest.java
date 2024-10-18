/*
 * Copyright 2019 The Error Prone Authors.
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
import static com.google.common.collect.Streams.concat;
import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static javax.tools.StandardLocation.ANNOTATION_PROCESSOR_PATH;

import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ClassTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link VisitorState}. */
@RunWith(JUnit4.class)
public class VisitorStateTest {

  @Test
  public void symbolFromString_defaultPackage() {
    assertThat(VisitorState.inferBinaryName("InDefaultPackage")).isEqualTo("InDefaultPackage");
  }

  @Test
  public void symbolFromString_nestedTypeInDefaultPackage() {
    assertThat(VisitorState.inferBinaryName("InDefaultPackage.Nested"))
        .isEqualTo("InDefaultPackage$Nested");
  }

  @Test
  public void symbolFromString_regularClass() {
    assertThat(VisitorState.inferBinaryName("test.RegularClass")).isEqualTo("test.RegularClass");
    assertThat(VisitorState.inferBinaryName("com.google.RegularClass"))
        .isEqualTo("com.google.RegularClass");
  }

  @Test
  public void symbolFromString_nestedTypeInRegularPackage() {
    assertThat(VisitorState.inferBinaryName("test.RegularClass.Nested"))
        .isEqualTo("test.RegularClass$Nested");
    assertThat(VisitorState.inferBinaryName("com.google.RegularClass.Nested"))
        .isEqualTo("com.google.RegularClass$Nested");
  }

  @Test
  public void getConstantExpression() {
    JavacTask task =
        JavacTool.create()
            .getTask(
                /* out= */ null,
                FileManagers.testFileManager(),
                /* diagnosticListener= */ null,
                /* options= */ ImmutableList.of(),
                /* classes= */ ImmutableList.of(),
                /* compilationUnits= */ ImmutableList.of());
    Context context = ((BasicJavacTask) task).getContext();
    VisitorState visitorState = VisitorState.createForUtilityPurposes(context);
    assertThat(visitorState.getConstantExpression("hello ' world")).isEqualTo("\"hello ' world\"");
    assertThat(visitorState.getConstantExpression("hello \n world"))
        .isEqualTo("\"hello \\n world\"");
    assertThat(visitorState.getConstantExpression('\'')).isEqualTo("'\\''");
    assertThat(visitorState.getConstantExpression(new StringBuilder("hello ' world")))
        .isEqualTo("\"hello ' world\"");
  }

  // The following is taken from ErrorProneJavacPluginTest. There may be an easier way.
  // It's possible that it's overkill for what we need here.

  /** A bugpattern for testing. */
  @BugPattern(summary = "", severity = ERROR)
  public static class CheckThatTriesToMemoizeBasedOnTreePath extends BugChecker
      implements ClassTreeMatcher {
    // The following is bogus: The value is cached per compilation, not per compilation unit!
    // We'll test that the plugin throws an exception about that problem.
    private final Supplier<Optional<ClassTree>> firstClassTreeInCompilationUnit =
        VisitorState.memoize(
            s ->
                s.getPath().getCompilationUnit().getTypeDecls().stream()
                    .filter(t -> t.getKind().asInterface().equals(ClassTree.class))
                    .map(ClassTree.class::cast)
                    .findFirst());

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      return buildDescription(tree)
          .setMessage(
              "Found "
                  + tree.getSimpleName()
                  + " in file whose main class is "
                  + firstClassTreeInCompilationUnit.get(state).map(ClassTree::getSimpleName))
          .build();
    }
  }

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void memoizeCannotAccessTreePath() throws IOException {
    File libJar = tempFolder.newFile("lib.jar");
    try (var fis = new FileOutputStream(libJar);
        var jos = new JarOutputStream(fis)) {
      jos.putNextEntry(new JarEntry("META-INF/services/" + BugChecker.class.getName()));
      jos.write(CheckThatTriesToMemoizeBasedOnTreePath.class.getName().getBytes(UTF_8));
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
        ANNOTATION_PROCESSOR_PATH,
        concat(
                Stream.of(libJar),
                Splitter.on(File.pathSeparatorChar)
                    .splitToStream(StandardSystemProperty.JAVA_CLASS_PATH.value())
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
                    "-Xplugin:ErrorProne -XepDisableAllChecks"
                        + " -Xep:CheckThatTriesToMemoizeBasedOnTreePath:ERROR",
                    "-XDcompilePolicy=byfile"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(source));
    assertThat(task.call()).isFalse();
    Diagnostic<? extends JavaFileObject> diagnostic =
        diagnosticCollector.getDiagnostics().stream()
            .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
            .collect(onlyElement());
    assertThat(diagnostic.getMessage(ENGLISH)).contains("UnsupportedOperationException");
  }
}
