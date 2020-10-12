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

/** {@link UnnecessaryParentheses}Test */
@RunWith(JUnit4.class)
public class UnnecessaryParenthesesTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new UnnecessaryParentheses(), getClass());
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnnecessaryParentheses.class, getClass());

  @Test
  public void test() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(int x) {",
            "    if (true) System.err.println((x));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(int x) {",
            "    if (true) System.err.println(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void anonymousClass() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.common.base.Function;",
            "import com.google.common.collect.Iterables;",
            "import java.util.List;",
            "class Test {",
            "  Iterable<Integer> f(List<Integer> l) {",
            "    return Iterables.transform(",
            "        l,",
            "        (new Function<Integer, Integer>() {",
            "          public Integer apply(Integer a) {",
            "              return a * 2;",
            "         }",
            "        }));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Function;",
            "import com.google.common.collect.Iterables;",
            "import java.util.List;",
            "class Test {",
            "  Iterable<Integer> f(List<Integer> l) {",
            "    return Iterables.transform(",
            "        l,",
            "        new Function<Integer, Integer>() {",
            "          public Integer apply(Integer a) {",
            "              return a * 2;",
            "         }",
            "        });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void binaryTrees() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int e() {",
            "    // BUG: Diagnostic contains:",
            "    return (\"b\").hashCode();",
            "  }",
            "  int f() {",
            "    return (\"a\" + \"b\").hashCode();",
            "  }",
            "  int g() {",
            "    return (1 + 2) & 3;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void methodReference() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Predicate;",
            "class Test {",
            "  Predicate<Test> foo(Predicate<Test> a) {",
            "    // BUG: Diagnostic contains:",
            "    return foo((this::equals));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lambdaLambda() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  Function<Void, Function<Void, Void>> r = x -> (y -> y);",
            "}")
        .doTest();
  }
}
