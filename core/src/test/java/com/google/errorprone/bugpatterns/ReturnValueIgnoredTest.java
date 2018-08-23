/*
 * Copyright 2012 The Error Prone Authors.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author alexeagle@google.com (Alex Eagle) */
@RunWith(JUnit4.class)
public class ReturnValueIgnoredTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ReturnValueIgnored.class, getClass());
  }

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("ReturnValueIgnoredPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("ReturnValueIgnoredNegativeCases.java").doTest();
  }

  @Test
  public void function() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  void f(Function<Integer, Integer> f) {",
            "    // BUG: Diagnostic contains:",
            "    f.apply(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void consumer() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Consumer;",
            "class Test {",
            "  void f(Consumer<Integer> f) {",
            "    f.accept(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void functionVoid() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.function.Function;",
            "class Test {",
            "  void f(Function<Integer, Void> f) {",
            "    f.apply(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreInTests() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.fail;",
            "import java.util.function.Function;",
            "class Test {",
            "  void f(Function<Integer, Integer> f) {",
            "    try {",
            "      f.apply(0);",
            "      fail();",
            "    } catch (Exception expected) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stream() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains:",
            "    \"\".codePoints().count();",
            "    \"\".codePoints().forEach(i -> {});",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void issue876() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.file.Path;",
            "abstract class Test {",
            "  void test(Path p) {",
            "    // BUG: Diagnostic contains:",
            "    E e = p::toRealPath;",
            "  }",
            "  abstract <T> void a(T t);",
            "  public interface E {",
            "    void run() throws Exception;",
            "  }",
            "}")
        .doTest();
  }
}
