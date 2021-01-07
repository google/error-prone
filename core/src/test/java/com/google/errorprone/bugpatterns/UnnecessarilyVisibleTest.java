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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link UnnecessarilyVisible}. */
@RunWith(JUnit4.class)
public final class UnnecessarilyVisibleTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessarilyVisible.class, getClass());

  @Test
  public void publicConstructor() {
    helper
        .addInputLines(
            "Test.java",
            "import javax.inject.Inject;",
            "class Test {",
            "  @Inject",
            "  public Test() {}",
            "}")
        .addOutputLines(
            "Test.java",
            "import javax.inject.Inject;",
            "class Test {",
            "  @Inject",
            "  Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void protectedConstructor() {
    helper
        .addInputLines(
            "Test.java",
            "import javax.inject.Inject;",
            "class Test {",
            "  @Inject",
            "  protected Test() {}",
            "}")
        .addOutputLines(
            "Test.java",
            "import javax.inject.Inject;",
            "class Test {",
            "  @Inject",
            "  Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void providesMethod() {
    helper
        .addInputLines(
            "Test.java",
            "import com.google.inject.Provides;",
            "class Test {",
            "  @Provides",
            "  public int foo() {",
            "    return 1;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.inject.Provides;",
            "class Test {",
            "  @Provides",
            "  int foo() {",
            "    return 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unannotated_noFinding() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  public int foo() {",
            "    return 1;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void visibleForTesting_noFinding() {
    helper
        .addInputLines(
            "Test.java", //
            "import com.google.common.annotations.VisibleForTesting;",
            "import javax.inject.Inject;",
            "class Test {",
            "  @Inject",
            "  @VisibleForTesting",
            "  public Test() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void overridesPublicMethod_noFinding() {
    helper
        .addInputLines(
            "A.java", //
            "class A {",
            "  public void foo() {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "Test.java", //
            "import com.google.inject.Provides;",
            "class Test extends A {",
            "  @Provides",
            "  public void foo() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
