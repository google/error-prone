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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link UnnecessaryLambda}Test */
@RunWith(JUnit4.class)
public class UnnecessaryLambdaTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new UnnecessaryLambda(), getClass());

  @Test
  public void method() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private Function<String, String> f() {",
            "    return x -> {",
            "      return \"hello \" + x;",
            "    };",
            "  }",
            "  void g() {",
            "    Function<String, String> f = f();",
            "    System.err.println(f().apply(\"world\"));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private String f(String x) {",
            "    return \"hello \" + x;",
            "  }",
            "  void g() {",
            "    Function<String, String> f = this::f;",
            "    System.err.println(f(\"world\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void method_static() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private static Function<String, String> f() {",
            "    return x -> \"hello \" + x;",
            "  }",
            "  void g() {",
            "    Function<String, String> f = f();",
            "    System.err.println(f().apply(\"world\"));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private static String f(String x) {",
            "    return \"hello \" + x;",
            "  }",
            "  void g() {",
            "    Function<String, String> f = Test::f;",
            "    System.err.println(f(\"world\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void method_void() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Consumer;",
            "class Test {",
            "  private Consumer<String> f() {",
            "    return x -> System.err.println(x);",
            "  }",
            "  void g() {",
            "    Consumer<String> f = f();",
            "    f().accept(\"world\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.Consumer;",
            "class Test {",
            "  private void f(String x) {",
            "    System.err.println(x);",
            "  }",
            "  void g() {",
            "    Consumer<String> f = this::f;",
            "    f(\"world\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variable_instance() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private final Function<String, String> camelCase = x -> \"hello \" + x;",
            "  void g() {",
            "    Function<String, String> f = camelCase;",
            "    System.err.println(camelCase.apply(\"world\"));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private String camelCase(String x) {",
            "    return \"hello \" + x;",
            "  }",
            "  void g() {",
            "    Function<String, String> f = this::camelCase;",
            "    System.err.println(camelCase(\"world\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variable_static() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private static final Function<String, String> F = x -> \"hello \" + x;",
            "  void g() {",
            "    Function<String, String> l = Test.F;",
            "    System.err.println(F.apply(\"world\"));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private static String f(String x) {",
            "    return \"hello \" + x;",
            "  }",
            "  void g() {",
            "    Function<String, String> l = Test::f;",
            "    System.err.println(f(\"world\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void method_shapes() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.BiFunction;",
            "import java.util.function.Supplier;",
            "class Test {",
            "  private Supplier<String> f() {",
            "    return () -> \"hello \";",
            "  }",
            "  private BiFunction<String, String, String> g() {",
            "    return (a, b) -> a + \"hello \" + b;",
            "  }",
            "  private Runnable h() {",
            "    return () -> System.err.println();",
            "  }",
            "  void main() {",
            "    System.err.println(f().get());",
            "    System.err.println(g().apply(\"a\", \"b\"));",
            "    h().run();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.BiFunction;",
            "import java.util.function.Supplier;",
            "class Test {",
            "  private String f() {",
            "    return \"hello \";",
            "  }",
            "  private String g(String a, String b) {",
            "    return a + \"hello \" + b;",
            "  }",
            "  private void h() {",
            "    System.err.println();",
            "  }",
            "  void main() {",
            "    System.err.println(f());",
            "    System.err.println(g(\"a\", \"b\"));",
            "    h();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonFunctionalInterfaceMethod() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Predicate;",
            "class Test {",
            "  private static final Predicate<String> F = x -> \"hello \".equals(x);",
            "  void g() {",
            "    Predicate<String> l = Test.F.and(x -> true);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }
}
