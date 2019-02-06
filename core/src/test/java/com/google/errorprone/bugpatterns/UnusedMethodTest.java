/*
 * Copyright 2018 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnusedMethod}. */
@RunWith(JUnit4.class)
public final class UnusedMethodTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnusedMethod.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new UnusedMethod(), getClass());



  @Test
  public void unusedNative() {
    helper
        .addSourceLines(
            "UnusedNative.java",
            "package unusedvars;",
            "public class UnusedNative {",
            "  private native void aNativeMethod();",
            "}")
        .doTest();
  }


  @Test
  public void unuseds() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "import java.io.IOException;",
            "import java.io.ObjectStreamException;",
            "import java.util.List;",
            "import javax.inject.Inject;",
            "public class Unuseds {",
            "  // BUG: Diagnostic contains:",
            "  private void notUsedMethod() {}",
            "  // BUG: Diagnostic contains:",
            "  private static void staticNotUsedMethod() {}",
            "  @SuppressWarnings({\"deprecation\", \"unused\"})",
            "  class UsesSuppressWarning {",
            "    private int f1;",
            "    private void test1() {",
            "      int local;",
            "    }",
            "    @SuppressWarnings(value = \"unused\")",
            "    private void test2() {",
            "      int local;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void exemptedMethods() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "import java.io.IOException;",
            "import java.io.ObjectStreamException;",
            "public class Unuseds {",
            "  private void readObject(java.io.ObjectInputStream in) throws IOException {",
            "    in.hashCode();",
            "  }",
            "  private void writeObject(java.io.ObjectOutputStream out) throws IOException {",
            "    out.writeInt(123);",
            "  }",
            "  private Object readResolve() {",
            "    return null;",
            "  }",
            "  private void readObjectNoData() throws ObjectStreamException {}",
            "}")
        .doTest();
  }

  @Test
  public void exemptedByName() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "class ExemptedByName {",
            "  private void unused1(int a, int unusedParam) {",
            "    int unusedLocal = a;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressions() {
    helper
        .addSourceLines(
            "Unuseds.java",
            "package unusedvars;",
            "class Suppressed {",
            "  @SuppressWarnings({\"deprecation\", \"unused\"})",
            "  class UsesSuppressWarning {",
            "    private void test1() {",
            "      int local;",
            "    }",
            "    @SuppressWarnings(value = \"unused\")",
            "    private void test2() {",
            "      int local;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void removal_javadocsAndNonJavadocs() {
    refactoringHelper
        .addInputLines(
            "UnusedWithComment.java",
            "package unusedvars;",
            "public class UnusedWithComment {",
            "  /**",
            "   * Method comment",
            "   */private void test1() {",
            "  }",
            "  /** Method comment */",
            "  private void test2() {",
            "  }",
            "  // Non javadoc comment",
            "  private void test3() {",
            "  }",
            "}")
        .addOutputLines(
            "UnusedWithComment.java", //
            "package unusedvars;",
            "public class UnusedWithComment {",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void usedInLambda() {
    helper
        .addSourceLines(
            "UsedInLambda.java",
            "package unusedvars;",
            "import java.util.Arrays;",
            "import java.util.List;",
            "import java.util.function.Function;",
            "import java.util.stream.Collectors;",
            "/** Method parameters used in lambdas and anonymous classes */",
            "public class UsedInLambda {",
            "  private Function<Integer, Integer> usedInLambda() {",
            "    return x -> 1;",
            "  }",
            "  private String print(Object o) {",
            "    return o.toString();",
            "  }",
            "  public List<String> print(List<Object> os) {",
            "    return os.stream().map(this::print).collect(Collectors.toList());",
            "  }",
            "  public static void main(String[] args) {",
            "    System.err.println(new UsedInLambda().usedInLambda());",
            "    System.err.println(new UsedInLambda().print(Arrays.asList(1, 2, 3)));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void onlyForMethodReference() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Predicate;",
            "class Test {",
            "  private static boolean foo(int a) {",
            "    return true;",
            "  }",
            "  Predicate<Integer> pred = Test::foo;",
            "}")
        .doTest();
  }
}
