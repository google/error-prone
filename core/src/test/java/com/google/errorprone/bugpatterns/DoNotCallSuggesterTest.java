/*
 * Copyright 2020 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests the {@link DoNotCallSuggester}. */
@RunWith(JUnit4.class)
public class DoNotCallSuggesterTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(DoNotCallSuggester.class, getClass());

  @Test
  public void finalClass_publicFinalMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  // BUG: Diagnostic contains: Always throws java.lang.RuntimeException",
            "  public final void foo() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_withInlineComments_type1() {
    testHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  // BUG: Diagnostic contains: Always throws java.lang.RuntimeException",
            "  public final void foo() {",
            "    // inline comments get stripped and don't matter",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_withInlineComments_type2() {
    testHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  // BUG: Diagnostic contains: Always throws java.lang.RuntimeException",
            "  public final void foo() {",
            "    /* inline comments get stripped and don't matter */",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicNonFinalMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  // BUG: Diagnostic contains: Always throws java.lang.RuntimeException",
            "  public void foo() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonFinalClass_publicFinalMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: Always throws java.lang.RuntimeException",
            "  public final void foo() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonFinalClass_publicNonFinalMethod() {
    // no suggestion since the method is overrideable
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void foo() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_throwsAVariable() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.io.IOException;",
            "final class Test {",
            "  private IOException ioe = new IOException();",
            "  // BUG: Diagnostic contains: Always throws java.io.IOException",
            "  public final void foo() throws IOException {",
            "    throw ioe;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_throwsAnotherMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.io.IOException;",
            "final class Test {",
            "  // BUG: Diagnostic contains: Always throws java.io.IOException",
            "  public final void foo() throws IOException {",
            "    throw up();",
            "  }",
            "  private IOException up() {",
            "    return new IOException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_withoutImplementingParentInterface() {
    testHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  // BUG: Diagnostic contains: Always throws java.lang.RuntimeException",
            "  public final String get() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_overridenMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Supplier;",
            "final class Test implements Supplier<String> {",
            "  @Override",
            "  public final String get() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_effectivelyOverridenMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Supplier;",
            "final class Test implements Supplier<String> {",
            "  public final String get() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_methodStartsWithProvide() {
    testHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  public final String provideString() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_methodStartsWithProduce() {
    testHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  public final String produceString() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_methodStartsWithThrows() {
    testHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  public final void throwsRuntimeException() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicFinalMethod_extendsAbstractModule() {
    testHelper
        .addSourceLines(
            "AbstractModule.java",
            "package com.google.inject;",
            "public abstract class AbstractModule {",
            "}")
        .addSourceLines(
            "Test.java",
            "final class Test extends com.google.inject.AbstractModule {",
            "  public final String extractString() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalClass_publicMethod_methodReturnsException() {
    testHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  public RuntimeException foo() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void insideAnonymousClass() {
    testHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  public final void foo() {",
            "    Object obj = new Object() {",
            "      public void foo() {",
            "        throw new RuntimeException();",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void abstractClass() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "abstract class Test {",
            "  abstract void test();",
            "}")
        .doTest();
  }

  @Test
  public void annotatedMethod() {
    testHelper
        .addSourceLines(
            "StarlarkMethod.java",
            "package net.starlark.java.annot;",
            "public @interface StarlarkMethod {",
            "}")
        .addSourceLines(
            "Test.java",
            "import net.starlark.java.annot.StarlarkMethod;",
            "final class Test {",
            "  @StarlarkMethod",
            "  public static void foo() {",
            "    throw new RuntimeException();",
            "  }",
            "}")
        .doTest();
  }
}
