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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link UnnecessaryAnonymousClass}Test */
@RunWith(JUnit4.class)
public class UnnecessaryAnonymousClassTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new UnnecessaryAnonymousClass(), getClass());

  @Test
  public void variable_instance() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private final Function<String, String> camelCase = new Function<String, String>() {",
            "    public String apply(String x) {",
            "      return \"hello \" + x;",
            "    }",
            "  };",
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
        // Make sure the method body is still reformatted correctly.
        .doTest(TEXT_MATCH);
  }

  @Test
  public void variable_static() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private static final Function<String, String> F = new Function<String, String>() {",
            "    public String apply(String x) {",
            "      return \"hello \" + x;",
            "    }",
            "  };",
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
  public void abstractClass() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  static abstract class Impl implements Function<String, String> {",
            "    public String apply(String input) {",
            "      return input;",
            "    }",
            "    public abstract void f(String input);",
            "  }",
            "  private final Function<String, String> camelCase = new Impl() {",
            "    public void f(String input) {}",
            "  };",
            "  void g() {",
            "    Function<String, String> f = camelCase;",
            "    System.err.println(camelCase.apply(\"world\"));",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void recursive() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private static final Function<Object, Object> STRINGIFY =",
            "      new Function<Object, Object>() {",
            "        @Override",
            "        public Object apply(Object input) {",
            "          return transform(STRINGIFY);",
            "        }",
            "      };",
            "  public static Object transform(Function<Object, Object> f) {",
            "    return f.apply(\"a\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  private static Object stringify(Object input) {",
            "    return transform(Test::stringify);",
            "  }",
            "  public static Object transform(Function<Object, Object> f) {",
            "    return f.apply(\"a\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void invokingDefaultMethod() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  interface Foo {",
            "    int foo(int a);",
            "    default void bar() {",
            "      foo(1);",
            "    }",
            "  }",
            "  private static final Foo FOO = new Foo() {",
            "    @Override public int foo(int a) {",
            "      return 2 * a;",
            "    }",
            "  };",
            "  public static void test() {",
            "    FOO.bar();",
            "    useFoo(FOO);",
            "  }",
            "  public static void useFoo(Foo foo) {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void mockitoSpy() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.function.Function;",
            "import org.mockito.Spy;",
            "class Test {",
            "  interface Foo {",
            "    int foo(int a);",
            "  }",
            "  @Spy",
            "  private static final Foo FOO = new Foo() {",
            "    @Override",
            "    public int foo(int a) {",
            "      return 2 * a;",
            "    }",
            "  };",
            "  public static void test() {",
            "    FOO.foo(2);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
