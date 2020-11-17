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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link OptionalMapUnusedValue} bugpattern. */
@RunWith(JUnit4.class)
public final class OptionalMapUnusedValueTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(OptionalMapUnusedValue.class, getClass());

  @Test
  public void positive_methodReference() {
    helper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  private Integer foo(Integer v) {return v;}",
            "",
            "  public void bar(Optional<Integer> optional) {",
            "    optional.map(this::foo);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  private Integer foo(Integer v) {return v;}",
            "",
            "  public void bar(Optional<Integer> optional) {",
            "    optional.ifPresent(this::foo);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_statementLambda() {
    helper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  private Integer foo(Integer v) {return v;}",
            "",
            "  public void bar(Optional<Integer> optional) {",
            "    optional.map(v -> foo(v));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  private Integer foo(Integer v) {return v;}",
            "",
            "  public void bar(Optional<Integer> optional) {",
            "    optional.ifPresent(v -> foo(v));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_resultReturned() {
    helper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  private Integer foo(Integer v) {return v;}",
            "",
            "  public Optional<Integer> bar(Optional<Integer> optional) {",
            "    return optional.map(this::foo);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negative_resultAssigned() {
    helper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  private Integer foo(Integer v) {return v;}",
            "",
            "  public void bar(Optional<Integer> optional) {",
            "    Optional<Integer> result = optional.map(this::foo);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negative_resultMethodCall() {
    helper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  private Integer foo(Integer v) {return v;}",
            "",
            "  public void bar(Optional<Integer> optional) {",
            "    optional.map(this::foo).orElse(42);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negative_nonStatementLambda() {
    helper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  public void bar(Optional<Integer> optional) {",
            "    optional.map(v -> v + 1);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negative_voidIncompatibleLambdaBlock() {
    helper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  public void bar(Optional<Integer> optional) {",
            "    optional.map(v -> {return 2;});",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
