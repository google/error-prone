/*
 * Copyright 2020 The Error Prone Authors.
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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit test for {@link UnnecessaryMethodReference}. */
@RunWith(JUnit4.class)
public final class UnnecessaryMethodReferenceTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnnecessaryMethodReference.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessaryMethodReference.class, getClass());

  @Test
  public void positiveCase() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, Function<Integer, String> fn) {",
            "    // BUG: Diagnostic contains: (fn)",
            "    return xs.map(fn::apply);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveCase_refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, Function<Integer, String> fn) {",
            "    return xs.map(fn::apply);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, Function<Integer, String> fn) {",
            "    return xs.map(fn);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveWithExtraInheritance() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, A fn) {",
            "    // BUG: Diagnostic contains: (fn)",
            "    return xs.map(fn::apply);",
            "  }",
            "  abstract static class A implements Function<Integer, String> {",
            "    @Override",
            "    public String apply(Integer i) {",
            "      return i.toString();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeWithExtraInheritance() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  Stream<String> map(Stream<Integer> xs, A fn) {",
            "    return xs.map(fn::frobnicate);",
            "  }",
            "  abstract static class A implements Function<Integer, String> {",
            "    abstract String frobnicate(Integer i);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void withNonAbstractMethodOnInterface() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "import java.util.stream.Stream;",
            "abstract class Test {",
            "  void test(A a) {",
            "    // BUG: Diagnostic contains:",
            "    foo(a::foo);",
            "    foo(a::bar);",
            "  }",
            "  abstract void foo(A a);",
            "  interface A {",
            "    String foo(Integer i);",
            "    default String bar(Integer i) {",
            "      return null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
