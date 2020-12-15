/*
 * Copyright 2015 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cushon@google.com (Liam Miller-Cushon) */
@RunWith(JUnit4.class)
public class VarCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(VarChecker.class, getClass());

  // fields are ignored
  @Test
  public void nonFinalField() {
    compilationHelper
        .addSourceLines("Test.java", "class Test {", "  public int x = 42;", "}")
        .doTest();
  }

  @Test
  public void finalField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            // TODO(b/21633565): force line break
            "class Test {",
            "  public final int x = 42;",
            "}")
        .doTest();
  }

  @Test
  public void positiveParam() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: public void x(@Var int y) {",
            "  public void x(int y) {",
            "    y++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeParam() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            // TODO(b/21633565): force line break
            "class Test {",
            "  public void x(int y) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveLocal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void x() {",
            "    // BUG: Diagnostic contains: @Var int y = 0;",
            "    int y = 0;",
            "    y++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeLocal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            // TODO(b/21633565): force line break
            "class Test {",
            "  public void x() {",
            "    int y = 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalLocal7() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            // TODO(b/21633565): force line break
            "class Test {",
            "  public void x() {",
            "    final int y = 0;",
            "  }",
            "}")
        .setArgs(Arrays.asList("-source", "7", "-target", "7"))
        .doTest();
  }

  @Test
  public void finalLocal8() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void x() {",
            "    // BUG: Diagnostic contains: /*START*/ int y = 0;",
            "    /*START*/ final int y = 0;",
            "  }",
            "}")
        .setArgs(Arrays.asList("-source", "8", "-target", "8"))
        .doTest();
  }

  @Test
  public void forLoop() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  {",
            "    for (int i = 0; i < 10; i++) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void enhancedFor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f (Iterable<String> xs) {",
            "    for (String x : xs) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unnecessaryFinalNativeMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: native void f(int y);",
            "  native void f(final int y);",
            "}")
        .doTest();
  }

  @Test
  public void nativeMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            // TODO(b/21633565): force line break
            "class Test {",
            "  native void f(int y);",
            "}")
        .doTest();
  }

  @Test
  public void abstractMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            // TODO(b/21633565): force line break
            "abstract class Test {",
            "  abstract void f(int y);",
            "}")
        .doTest();
  }

  @Test
  public void interfaceMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            // TODO(b/21633565): force line break
            "interface Test {",
            "  void f(int y);",
            "}")
        .doTest();
  }

  @Test
  public void varField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Var;",
            "class Test {",
            "  @Var public int x = 42;",
            "}")
        .doTest();
  }

  @Test
  public void varParam() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Var;",
            "class Test {",
            "  public void x(@Var int y) {",
            "    y++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varLocal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Var;",
            "class Test {",
            "  public void x() {",
            "    @Var int y = 0;",
            "    y++;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varCatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void x() {",
            "    try {",
            "    // BUG: Diagnostic contains: missing @Var",
            "    } catch (Exception e) {",
            "      e = null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalCatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void x() {",
            "    try {",
            "    // BUG: Diagnostic contains: Unnecessary 'final' modifier.",
            "    } catch (final Exception e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalTWR() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.InputStream;",
            "class Test {",
            "  public void x() {",
            "    // BUG: Diagnostic contains: Unnecessary 'final' modifier.",
            "    try (final InputStream is = null) {",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonFinalTWR() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.InputStream;",
            "class Test {",
            "  public void x() {",
            "    try (InputStream is = null) {",
            "    } catch (Exception e) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void receiverParameter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void f(Test this, int x) {",
            "    this.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressedByGeneratedAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.processing.Generated;",
            "@Generated(\"generator\") class Test {",
            "  public void x() {",
            "    final int y = 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressedBySuppressWarningsAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "@SuppressWarnings(\"Var\") class Test {",
            "  public void x() {",
            "    final int y = 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void notSuppressedByUnrelatedSuppressWarningsAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "@SuppressWarnings(\"Foo\") class Test {",
            "  public void x() {",
            "    // BUG: Diagnostic contains: Unnecessary 'final' modifier.",
            "    final int y = 0;",
            "  }",
            "}")
        .doTest();
  }
}
