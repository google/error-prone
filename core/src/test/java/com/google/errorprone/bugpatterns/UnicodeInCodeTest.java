/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.common.truth.Truth.assertWithMessage;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.FileManagers;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import java.net.URI;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnicodeInCode}. */
@RunWith(JUnit4.class)
public final class UnicodeInCodeTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnicodeInCode.class, getClass());

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  final int noUnicodeHereBoss = 1;",
            "}")
        .doTest();
  }

  @Test
  public void negativeInComment() {
    helper
        .addSourceLines(
            "Test.java", //
            "/** \u03C0 */",
            "class Test {",
            "  final int noUnicodeHereBoss = 1; // roughly \u03C0",
            "}")
        .doTest();
  }

  @Test
  public void negativeInStringLiteral() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  static final String pi = \"\u03C0\";",
            "}")
        .doTest();
  }

  @Test
  public void negativeInCharLiteral() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  static final char pi = '\u03C0';",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains: Unicode character (\\u03c0)",
            "  static final double \u03C0 = 3;",
            "}")
        .doTest();
  }

  @Test
  public void positiveMultiCharacterGivesOneFinding() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains: Unicode character (\\u03c0\\u03c0)",
            "  static final double \u03C0\u03C0 = 3;",
            "}")
        .doTest();
  }

  @Test
  public void suppressibleAtClassLevel() {
    helper
        .addSourceLines(
            "Test.java", //
            "@SuppressWarnings(\"UnicodeInCode\")",
            "class Test {",
            "  static final double \u03C0 = 3;",
            "}")
        .doTest();
  }

  @Test
  public void suppressibleAtMethodLevel() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  @SuppressWarnings(\"UnicodeInCode\")",
            "  void test() {",
            "    double \u03C0 = 3;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void asciiSub() {
    JavacFileManager fileManager = FileManagers.testFileManager();
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    JavacTask task =
        JavacTool.create()
            .getTask(
                null,
                fileManager,
                diagnosticCollector,
                ImmutableList.of("-Xplugin:ErrorProne", "-XDcompilePolicy=simple"),
                ImmutableList.of(),
                ImmutableList.of(
                    new SimpleJavaFileObject(
                        URI.create("file:///Test.java"), JavaFileObject.Kind.SOURCE) {
                      @Override
                      public String getCharContent(boolean ignoreEncodingErrors) {
                        return "class Test {}" + ((char) 0x1a);
                      }
                    }));
    boolean ok = task.call();
    assertWithMessage(
            diagnosticCollector.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                .map(d -> d.getMessage(ENGLISH))
                .collect(joining("\n")))
        .that(ok)
        .isTrue();
  }
}
