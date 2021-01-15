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

/** Unit tests for {@link UnnecessaryOptionalGet}. */
@RunWith(JUnit4.class)
public final class UnnecessaryOptionalGetTest {
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(new UnnecessaryOptionalGet(), getClass());

  @Test
  public void genericOptionalVars_sameVarGet_replacesWithLambdaArg() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op = Optional.of(\"hello\");",
            "    op.ifPresent(x -> System.out.println(op.get()));",
            "    op.map(x -> Long.parseLong(op.get()));",
            "    op.filter(x -> op.get().isEmpty());",
            "    op.flatMap(x -> Optional.of(op.get()));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op = Optional.of(\"hello\");",
            "    op.ifPresent(x -> System.out.println(x));",
            "    op.map(x -> Long.parseLong(x));",
            "    op.filter(x -> x.isEmpty());",
            "    op.flatMap(x -> Optional.of(x));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void guava_sameVarGet_replacesWithLambdaArg() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op = Optional.of(\"hello\");",
            "    op.transform(x -> Long.parseLong(op.get()));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op = Optional.of(\"hello\");",
            "    op.transform(x -> Long.parseLong(x));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void genericOptionalVars_orElseVariations_replacesWithLambdaArg() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op = Optional.of(\"hello\");",
            "    op.ifPresent(x -> System.out.println(op.orElse(\"other\")));",
            "    op.ifPresent(x -> System.out.println(op.orElseGet(() -> \"other\")));",
            "    op.ifPresent(x -> System.out.println(op.orElseThrow(RuntimeException::new)));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op = Optional.of(\"hello\");",
            "    op.ifPresent(x -> System.out.println(x));",
            "    op.ifPresent(x -> System.out.println(x));",
            "    op.ifPresent(x -> System.out.println(x));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void guava_orVariations_replacesWithLambdaArg() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op = Optional.of(\"hello\");",
            "    op.transform(x -> Long.parseLong(op.or(\"other\")));",
            "    op.transform(x -> Long.parseLong(op.or(() -> \"other\")));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.base.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op = Optional.of(\"hello\");",
            "    op.transform(x -> Long.parseLong(x));",
            "    op.transform(x -> Long.parseLong(x));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void genericOptionalVars_sameVarGet_lamdaBlocks_replacesWithLamdaArg() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op = Optional.of(\"hello\");",
            "    op.ifPresent(x -> { System.out.println(op.get()); });",
            "    op.map(x -> { return Long.parseLong(op.get()); });",
            "    op.filter(x -> { return op.get().isEmpty(); });",
            "    op.flatMap(x -> { return Optional.of(op.get()); });",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op = Optional.of(\"hello\");",
            "    op.ifPresent(x -> { System.out.println(x); });",
            "    op.map(x -> { return Long.parseLong(x); });",
            "    op.filter(x -> { return x.isEmpty(); });",
            "    op.flatMap(x -> { return Optional.of(x); });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void genericOptionalVars_differentOptionalVarGet_doesNothing() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  private void home() {",
            "    Optional<String> op1 = Optional.of(\"hello\");",
            "    Optional<String> op2 = Optional.of(\"hello\");",
            "    op1.ifPresent(x -> System.out.println(op2.get()));",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void genericOptionalVars_differentMethodGet_doesNothing() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  private void home() {",
            "    myOpFunc1().ifPresent(x -> System.out.println(myOpFunc2().get()));",
            "  }",
            "  private Optional<String> myOpFunc1() {",
            "    return Optional.of(\"hello\");",
            "  }",
            "  private Optional<String> myOpFunc2() {",
            "    return Optional.of(\"hello\");",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void genericOptionalMethods_sameMethodInvocation_replacesWithLamdaArg() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  private void home() {",
            "    myOpFunc().ifPresent(x -> System.out.println(myOpFunc().get()));",
            "  }",
            "  private Optional<String> myOpFunc() {",
            "    return Optional.of(\"hello\");",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void primitiveOptionals() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.OptionalDouble;",
            "import java.util.OptionalInt;",
            "import java.util.OptionalLong;",
            "public class Test {",
            "  private void home() {",
            "    OptionalDouble opDouble = OptionalDouble.of(1.0);",
            "    OptionalInt opInt = OptionalInt.of(1);",
            "    OptionalLong opLong = OptionalLong.of(1L);",
            "    opDouble.ifPresent(x -> System.out.println(opDouble.getAsDouble()));",
            "    opInt.ifPresent(x -> System.out.println(opInt.getAsInt()));",
            "    opLong.ifPresent(x -> System.out.println(opLong.getAsLong()));",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.OptionalDouble;",
            "import java.util.OptionalInt;",
            "import java.util.OptionalLong;",
            "public class Test {",
            "  private void home() {",
            "    OptionalDouble opDouble = OptionalDouble.of(1.0);",
            "    OptionalInt opInt = OptionalInt.of(1);",
            "    OptionalLong opLong = OptionalLong.of(1L);",
            "    opDouble.ifPresent(x -> System.out.println(x));",
            "    opInt.ifPresent(x -> System.out.println(x));",
            "    opLong.ifPresent(x -> System.out.println(x));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void differentReceivers() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.util.Optional;",
            "public class Test {",
            "  abstract static class T {",
            "    abstract Optional<String> getValue();",
            "  }",
            "  static void test(T actual, T expected) {",
            "    actual",
            "        .getValue()",
            "        .ifPresent(",
            "            actualValue -> {",
            "              String expectedValue = expected.getValue().get();",
            "              actualValue.equals(expectedValue);",
            "            });",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
