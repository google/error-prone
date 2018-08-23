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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for InconsistentCapitalization bug checker */
@RunWith(JUnit4.class)
public class InconsistentCapitalizationTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(InconsistentCapitalization.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new InconsistentCapitalization(), getClass());

  @Test
  public void testNegativeCases() {
    compilationHelper.addSourceFile("InconsistentCapitalizationNegativeCases.java").doTest();
  }

  @Test
  public void correctsInconsistentVariableNameInMethodDefinitionToFieldCase() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  Object aa;",
            "  void method(Object aA) {",
            "    this.aa = aA;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  Object aa;",
            "  void method(Object aa) {",
            "    this.aa = aa;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameInConstructorDefinitionToFieldCase() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  Object aa;",
            "  Test(Object aA) {",
            "    this.aa = aA;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  Object aa;",
            "  Test(Object aa) {",
            "    this.aa = aa;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameInLambdaDefinitionToFieldCase() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Object ea;",
            "  Test() {",
            "    Function<Void, Object> f = (eA) -> {",
            "        this.ea = eA;",
            "        return eA;",
            "    };",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Object ea;",
            "  Test() {",
            "    Function<Void, Object> f = (ea) -> {",
            "        this.ea = ea;",
            "        return ea;",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void
      correctsInconsistentVariableNameInConstructorDefinitionWithMultipleOccurrencesToFieldCase() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  Object aa;",
            "  Test(Object aA) {",
            "    this.aa = aA;",
            "    if (aA == this.aa) {",
            "      for (Object i = aA;;) {",
            "      }",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  Object aa;",
            "  Test(Object aa) {",
            "    this.aa = aa;",
            "    if (aa == this.aa) {",
            "      for (Object i = aa;;) {",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameToFieldCaseAndQualifiesField() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  Object aa;",
            "  Test(Object aA) {",
            "    aa = aA;",
            "    if (aA == aa) {",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  Object aa;",
            "  Test(Object aa) {",
            "    this.aa = aa;",
            "    if (aa == this.aa) {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameToFieldCaseAndQualifiesNestedClassField() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Object aa;",
            "  Object ab;",
            "  class Nested {",
            "    Object aB;",
            "    Nested(Object aA) {",
            "      aa = aA;",
            "      if (aa == aA) {}",
            "      Test.this.aa = aA;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Object aa;",
            "  Object ab;",
            "  class Nested {",
            "    Object aB;",
            "    Nested(Object aa) {",
            "      Test.this.aa = aa;",
            "      if (Test.this.aa == aa) {}",
            "      Test.this.aa = aa;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameToFieldCaseAndQualifiesNestedChildClassField() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  static class A {",
            "    Object aa;",
            "    static class Nested extends A {",
            "      Nested(Object aA) {",
            "        aa = aA;",
            "        if (aa == aA) {}",
            "        super.aa = aA;",
            "      }",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  static class A {",
            "    Object aa;",
            "    static class Nested extends A {",
            "      Nested(Object aa) {",
            "        super.aa = aa;",
            "        if (super.aa == aa) {}",
            "        super.aa = aa;",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void
      correctsInconsistentVariableNameToFieldCaseInAnonymousClassAndQualifiesNestedChildClassField() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Object aa;",
            "  Function<Object, Object> f = new Function() {",
            "    public Object apply(Object aA) {",
            "      aa = aA;",
            "      return aA;",
            "    }",
            "  };",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Object aa;",
            "  Function<Object, Object> f = new Function() {",
            "    public Object apply(Object aa) {",
            "      Test.this.aa = aa;",
            "      return aa;",
            "    }",
            "  };",
            "}")
        .doTest();
  }

  // regression test for https://github.com/google/error-prone/issues/999
  @Test
  public void clash() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  Object _DocumentObjectData_QNAME;",
            "  Object _DocumentObjectdata_QNAME;",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  Object _DocumentObjectData_QNAME;",
            "  Object _DocumentObjectdata_QNAME;",
            "}")
        .doTest();
  }

  @Test
  public void i1008() {
    compilationHelper
        .addSourceLines(
            "Callback.java",
            "public class Callback {",
            "  interface WaitHandler { } // ignore",
            "  private final WaitHandler waitHandler;",
            "  // BUG: Diagnostic contains:",
            "  protected Callback(final WaitHandler waithandler) {",
            "    this.waitHandler = waithandler;",
            "  }",
            "  public static Callback doOnSuccess() {",
            "    return new Callback(null) {};",
            "  }",
            "}")
        .doTest();
  }
}
