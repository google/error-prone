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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CheckedExceptionNotThrown}. */
@RunWith(JUnit4.class)
public final class CheckedExceptionNotThrownTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(new CheckedExceptionNotThrown(), getClass());

  @Test
  public void noExceptionThrown_entireThrowsBlockRemoved() {
    helper
        .addInputLines(
            "Test.java", //
            "public final class Test {",
            "  /**",
            "   * Frobnicate",
            "   *",
            "   * @throws Exception foo",
            "   */",
            "  void test() throws Exception {}",
            "}")
        .addOutputLines(
            "Test.java", //
            "public final class Test {",
            "  /**",
            "   * Frobnicate",
            "   *",
            "   */",
            "  void test() {}",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void exceptionActuallyThrown_noChange() {
    helper
        .addInputLines(
            "Test.java", //
            "public final class Test {",
            "  void test() throws Exception {",
            "    Thread.sleep(1);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void overridable_noChange() {
    helper
        .addInputLines(
            "Test.java", //
            "public class Test {",
            "  void test() throws Exception {",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void thrownViaGenericChecked() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Optional;",
            "public final class Test {",
            "  int test(Optional<Integer> x) throws Exception {",
            "    return x.orElseThrow(() -> new Exception());",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void thrownViaGenericUnchecked() {
    helper
        .addInputLines(
            "Test.java", //
            "import java.util.Optional;",
            "public final class Test {",
            "  int test(Optional<Integer> x) throws Exception {",
            "    return x.orElseThrow(() -> new IllegalStateException());",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "import java.util.Optional;",
            "public final class Test {",
            "  int test(Optional<Integer> x) {",
            "    return x.orElseThrow(() -> new IllegalStateException());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void oneCheckedOneUnchecked() {
    helper
        .addInputLines(
            "Test.java", //
            "public final class Test {",
            "  void test() throws IllegalStateException, Exception {}",
            "}")
        .addOutputLines(
            "Test.java", //
            "public final class Test {",
            "  void test() throws IllegalStateException {}",
            "}")
        .doTest();
  }

  @Test
  public void ignoredOnTestMethods() {
    helper
        .addInputLines(
            "Test.java", //
            "public final class Test {",
            "  @org.junit.Test",
            "  void test() throws IllegalStateException, Exception {}",
            "}")
        .expectUnchanged()
        .setArgs("-XepCompilingTestOnlyCode")
        .doTest();
  }
}
