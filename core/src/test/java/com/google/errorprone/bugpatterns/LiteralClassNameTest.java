/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link LiteralClassName}Test */
@RunWith(JUnit4.class)
public class LiteralClassNameTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(LiteralClassName.class, getClass());

  @Test
  public void inaccessibleClass() {
    compilationTestHelper
        .addSourceLines(
            "a/A.java", //
            "package a;",
            "class A {}")
        .addSourceLines(
            "b/B.java",
            "package b;",
            "class B {",
            "  void f() throws Exception {",
            "    Class.forName(\"a.A\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void className() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new LiteralClassName(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    Class<?> c = Class.forName(\"java.lang.String\");",
            "    c = Class.forName(\"java.lang.NoSuch\");",
            "    Class.forName(\"java.util.Optional\");",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static java.util.Objects.requireNonNull;",
            "import java.util.Optional;",
            "class Test {",
            "  void f() throws Exception {",
            "    Class<?> c = String.class;",
            "    c = Class.forName(\"java.lang.NoSuch\");",
            "    requireNonNull(Optional.class);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void arrays() throws IOException {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() throws Exception {",
            "    Class<?> c = Class.forName(\"[B\");",
            "    c = Class.forName(\"B\");",
            "  }",
            "}")
        .doTest();
  }
}
