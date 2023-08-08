/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link DereferenceWithNullBranch}Test */
@RunWith(JUnit4.class)
public class DereferenceWithNullBranchTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(DereferenceWithNullBranch.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Foo.java",
            "import java.util.Optional;",
            "class Foo {",
            "  void foo(Optional<Integer> o) {",
            "    // BUG: Diagnostic contains: ",
            "    o.orElse(null).intValue();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveTernary() {
    helper
        .addSourceLines(
            "Foo.java",
            "import java.util.Optional;",
            "class Foo {",
            "  int foo(String s) {",
            "    // BUG: Diagnostic contains: ",
            "    return (s == null) ? s.length() : 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNoNullBranch() {
    helper
        .addSourceLines(
            "Foo.java",
            "import java.util.Optional;",
            "class Foo {",
            "  void foo() {",
            "    Optional.of(7).get().intValue();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeVoid() {
    helper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo() {",
            "    Class<?> c;",
            "    c = Void.class;",
            "    c = void.class;",
            "    c = Void.TYPE;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noCrashOnQualifiedClass() {
    helper
        .addSourceLines(
            "Foo.java", //
            "class Foo {",
            "  class Bar {}",
            "  Foo.Bar bar;",
            "}")
        .doTest();
  }

  @Test
  public void noCrashOnQualifiedInterface() {
    helper
        .addSourceLines(
            "Foo.java",
            "import java.util.Map;",
            "interface Foo {",
            "  void foo(Map.Entry<?, ?> o);",
            "}")
        .doTest();
  }

  @Test
  public void noCrashOnModule() {
    helper
        .addSourceLines(
            "module-info.java", //
            "module foo.bar {",
            "  requires java.logging;",
            "}")
        .doTest();
  }
}
