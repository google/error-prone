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

/** Tests for {@link UseTimeInScope}. */
@RunWith(JUnit4.class)
public final class UseTimeInScopeTest {
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(UseTimeInScope.class, getClass());

  @Test
  public void clockInScope_refactored() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.time.Clock;",
            "import java.time.Instant;",
            "public class Test {",
            "  private Clock clock;",
            "  public Instant test() {",
            "    return Clock.systemDefaultZone().instant();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.Clock;",
            "import java.time.Instant;",
            "public class Test {",
            "  private Clock clock;",
            "  public Instant test() {",
            "    return clock.instant();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noClockInScope_noFinding() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.time.Clock;",
            "public class Test {",
            "  public Clock test() {",
            "    return Clock.systemDefaultZone();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void assignmentToSameVariable_noFinding() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.time.Clock;",
            "public class Test {",
            "  private Clock clock;",
            "  public Test() {",
            "    this.clock = Clock.systemDefaultZone();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void variableInitialization_noFinding() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.time.Clock;",
            "public class Test {",
            "  private Clock clock = Clock.systemDefaultZone();",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
