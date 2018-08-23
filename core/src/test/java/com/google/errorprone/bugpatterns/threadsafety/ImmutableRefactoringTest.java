/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ImmutableRefactoring}Test */
@RunWith(JUnit4.class)
public class ImmutableRefactoringTest {

  private final BugCheckerRefactoringTestHelper compilationHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ImmutableRefactoring(), getClass());

  @Test
  public void positive() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "import javax.annotation.concurrent.Immutable;",
            "@Immutable class Test {",
            "  final int a = 42;",
            "  final String b = null;",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  final int a = 42;",
            "  final String b = null;",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addInputLines(
            "Test.java",
            "import javax.annotation.concurrent.Immutable;",
            "@Immutable class Test {",
            "  int a = 42;",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
