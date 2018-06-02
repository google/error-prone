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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.errorprone.BaseErrorProneJavaCompiler;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.bugpatterns.apidiff.ApiDiff.ClassMemberKey;
import com.google.errorprone.bugpatterns.apidiff.CompilationBuilderHelpers.CompilationBuilder;
import com.google.errorprone.bugpatterns.apidiff.CompilationBuilderHelpers.CompilationResult;
import com.google.errorprone.bugpatterns.apidiff.CompilationBuilderHelpers.SourceBuilder;
import com.google.errorprone.scanner.ErrorProneScanner;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ApiDiffChecker}Test. */
@RunWith(JUnit4.class)
public class ApiDiffCheckerTest {

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();
  private final JavacFileManager fileManager =
      new JavacFileManager(new Context(), false, StandardCharsets.UTF_8);

  /** An {@link ApiDiffChecker} for testing. */
  @BugPattern(name = "SampleChecker", severity = SeverityLevel.ERROR, summary = "")
  private static class SampleApiDiffChecker extends ApiDiffChecker {
    SampleApiDiffChecker(ApiDiff apiDiff) {
      super(apiDiff);
    }
  }

  @Test
  public void newDerivedMethod() throws Exception {
    ApiDiff diff =
        ApiDiff.fromMembers(
            Collections.emptySet(),
            ImmutableSetMultimap.of("lib/Derived", ClassMemberKey.create("g", "()V")));

    Path originalJar =
        new CompilationBuilder(JavacTool.create(), tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "Base.java", //
                        "package lib;",
                        "public class Base {",
                        "  public void f() {}",
                        "}")
                    .addSourceLines(
                        "Derived.java", //
                        "package lib;",
                        "public class Derived extends Base {",
                        "}")
                    .build())
            .compileOutputToJarOrDie();

    Path newJar =
        new CompilationBuilder(JavacTool.create(), tempFolder.newFolder(), fileManager)
            .setClasspath(Collections.singleton(originalJar))
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "Derived.java", //
                        "package lib;",
                        "public class Derived extends Base {",
                        "  public void f() {}",
                        "  public void g() {}",
                        "}")
                    .build())
            .compileOutputToJarOrDie();

    BaseErrorProneJavaCompiler errorProneCompiler =
        new BaseErrorProneJavaCompiler(
            ScannerSupplier.fromScanner(new ErrorProneScanner(new SampleApiDiffChecker(diff))));

    final CompilationResult result =
        new CompilationBuilder(errorProneCompiler, tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "Test.java",
                        "import lib.*;", //
                        "class Test {",
                        "  void f() {",
                        "    new Derived().f();",
                        "    new Derived().g();",
                        "  }",
                        "}")
                    .build())
            .setClasspath(Arrays.asList(newJar, originalJar))
            .compile();

    assertThat(getOnlyElement(result.diagnostics()).getMessage(Locale.ENGLISH))
        .contains("g() is not available in lib.Derived");
  }

  @Test
  public void addedSuperAndMethod() throws Exception {
    ApiDiff diff =
        ApiDiff.fromMembers(
            ImmutableSet.of("lib/A"),
            ImmutableSetMultimap.of("lib/B", ClassMemberKey.create("f", "()V")));

    Path originalJar =
        new CompilationBuilder(JavacTool.create(), tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "B.java", //
                        "package lib;",
                        "public class B {",
                        "}")
                    .build())
            .compileOutputToJarOrDie();

    Path newJar =
        new CompilationBuilder(JavacTool.create(), tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "A.java", //
                        "package lib;",
                        "interface A {",
                        "  default void f() {}",
                        "}")
                    .addSourceLines(
                        "B.java", //
                        "package lib;",
                        "public class B implements A {",
                        "}")
                    .build())
            .compileOutputToJarOrDie();

    BaseErrorProneJavaCompiler errorProneCompiler =
        new BaseErrorProneJavaCompiler(
            ScannerSupplier.fromScanner(new ErrorProneScanner(new SampleApiDiffChecker(diff))));

    final CompilationResult result =
        new CompilationBuilder(errorProneCompiler, tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "Test.java",
                        "import lib.*;", //
                        "class Test {",
                        "  void f(B b) {",
                        "    b.f();",
                        "  }",
                        "}")
                    .build())
            .setClasspath(Arrays.asList(newJar, originalJar))
            .compile();

    // This should be an error (the inherited A.f() is not backwards compatible), but we don't
    // detect newly added methods in newly added super types.
    assertThat(result.diagnostics()).isEmpty();
  }

  @Test
  public void movedMethodToNewSuper() throws Exception {
    ApiDiff diff = ApiDiff.fromMembers(ImmutableSet.of("lib/A"), ImmutableSetMultimap.of());

    Path originalJar =
        new CompilationBuilder(JavacTool.create(), tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "B.java", //
                        "package lib;",
                        "public class B {",
                        "  public void f() {}",
                        "}")
                    .build())
            .compileOutputToJarOrDie();

    Path newJar =
        new CompilationBuilder(JavacTool.create(), tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "A.java", //
                        "package lib;",
                        "interface A {",
                        "  default void f() {}",
                        "}")
                    .addSourceLines(
                        "B.java", //
                        "package lib;",
                        "public class B implements A {}")
                    .build())
            .compileOutputToJarOrDie();

    BaseErrorProneJavaCompiler errorProneCompiler =
        new BaseErrorProneJavaCompiler(
            ScannerSupplier.fromScanner(new ErrorProneScanner(new SampleApiDiffChecker(diff))));

    final CompilationResult result =
        new CompilationBuilder(errorProneCompiler, tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "Test.java",
                        "import lib.*;", //
                        "class Test {",
                        "  void f(B b) {",
                        "    b.f();",
                        "  }",
                        "}")
                    .build())
            .setClasspath(Arrays.asList(newJar, originalJar))
            .compile();

    assertThat(result.diagnostics()).isEmpty();
  }

  @Test
  public void movedToSuperMethodFromMiddle() throws Exception {
    ApiDiff diff =
        ApiDiff.fromMembers(
            ImmutableSet.of(), ImmutableSetMultimap.of("lib/A", ClassMemberKey.create("f", "()V")));

    Path originalJar =
        new CompilationBuilder(JavacTool.create(), tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "A.java", //
                        "package lib;",
                        "public class A {",
                        "}")
                    .addSourceLines(
                        "B.java", //
                        "package lib;",
                        "public class B extends A {",
                        "  public void f() {}",
                        "}")
                    .addSourceLines(
                        "C.java", //
                        "package lib;",
                        "public class C extends B {",
                        "}")
                    .build())
            .compileOutputToJarOrDie();

    Path newJar =
        new CompilationBuilder(JavacTool.create(), tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "A.java", //
                        "package lib;",
                        "public class A {",
                        "  public void f() {}",
                        "}")
                    .addSourceLines(
                        "B.java", //
                        "package lib;",
                        "public class B extends A {",
                        "}")
                    .addSourceLines(
                        "C.java", //
                        "package lib;",
                        "public class C extends B {",
                        "}")
                    .build())
            .compileOutputToJarOrDie();

    BaseErrorProneJavaCompiler errorProneCompiler =
        new BaseErrorProneJavaCompiler(
            ScannerSupplier.fromScanner(new ErrorProneScanner(new SampleApiDiffChecker(diff))));

    final CompilationResult result =
        new CompilationBuilder(errorProneCompiler, tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "Test.java",
                        "import lib.*;", //
                        "class Test {",
                        "  void f(C c) {",
                        "    c.f();",
                        "  }",
                        "}")
                    .build())
            .setClasspath(Arrays.asList(newJar, originalJar))
            .compile();

    // This is actually OK, but we see it as a call to the newly-added A.f, and don't consider that
    // B.f is available in the old version of the API. It's not clear how to avoid this false
    // positive.
    assertThat(result.diagnostics()).hasSize(1);
    assertThat(getOnlyElement(result.diagnostics()).getMessage(Locale.ENGLISH))
        .contains("lib.A#f() is not available in lib.C");
  }

  @Test
  public void subType() throws Exception {
    ApiDiff diff =
        ApiDiff.fromMembers(
            ImmutableSet.of(), ImmutableSetMultimap.of("lib/A", ClassMemberKey.create("f", "()V")));

    Path originalJar =
        new CompilationBuilder(JavacTool.create(), tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "A.java", //
                        "package lib;",
                        "public class A {",
                        "}")
                    .build())
            .compileOutputToJarOrDie();

    Path newJar =
        new CompilationBuilder(JavacTool.create(), tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "A.java", //
                        "package lib;",
                        "public class A {",
                        "  public void f() {}",
                        "}")
                    .build())
            .compileOutputToJarOrDie();

    BaseErrorProneJavaCompiler errorProneCompiler =
        new BaseErrorProneJavaCompiler(
            ScannerSupplier.fromScanner(new ErrorProneScanner(new SampleApiDiffChecker(diff))));

    final CompilationResult result =
        new CompilationBuilder(errorProneCompiler, tempFolder.newFolder(), fileManager)
            .setSources(
                new SourceBuilder(tempFolder.newFolder())
                    .addSourceLines(
                        "Test.java",
                        "import lib.A;", //
                        "class Test {",
                        "  void g() {",
                        "    new A() {}.f();",
                        "  }",
                        "}")
                    .build())
            .setClasspath(Arrays.asList(newJar, originalJar))
            .compile();

    assertThat(result.diagnostics()).hasSize(1);
    assertThat(getOnlyElement(result.diagnostics()).getMessage(Locale.ENGLISH))
        .contains("lib.A#f() is not available in <anonymous Test$1>");
  }
}
