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

/** Tests for {@link StaticAssignmentInConstructor}. */
@RunWith(JUnit4.class)
public final class StaticAssignmentInConstructorTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(StaticAssignmentInConstructor.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static int foo;",
            "  public Test(int foo) {",
            "    // BUG: Diagnostic contains:",
            "    this.foo = foo;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dubiousStorageOfLatestInstance_exempted() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static Test latest;",
            "  public Test() {",
            "    latest = this;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void instanceField_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int foo;",
            "  public Test(int foo) {",
            "    this.foo = foo;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignedWithinLambda_noMatch() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static int foo;",
            "  public Test(int a) {",
            "    java.util.Arrays.asList().stream().map(x -> { foo = 1; return a; }).count();",
            "  }",
            "}")
        .doTest();
  }
}
