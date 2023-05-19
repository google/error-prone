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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class InjectOnBugCheckersTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(InjectOnBugCheckers.class, getClass());

  @Test
  public void positive() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.ErrorProneFlags;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "@BugPattern(summary = \"\", severity = BugPattern.SeverityLevel.WARNING)",
            "public class Test extends BugChecker {",
            "  // BUG: Diagnostic contains: @Inject",
            "  public Test(ErrorProneFlags f) {}",
            "}")
        .doTest();
  }

  @Test
  public void notTheActualBugPattern_noFinding() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.ErrorProneFlags;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "public class Test extends BugChecker {",
            "  public Test(ErrorProneFlags f) {}",
            "}")
        .doTest();
  }

  @Test
  public void zeroArgConstructor_noFinding() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "@BugPattern(summary = \"\", severity = BugPattern.SeverityLevel.WARNING)",
            "public class Test extends BugChecker {",
            "  public Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void alreadyAnnotated_noFinding() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.BugPattern;",
            "import com.google.errorprone.ErrorProneFlags;",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import javax.inject.Inject;",
            "@BugPattern(summary = \"\", severity = BugPattern.SeverityLevel.WARNING)",
            "public class Test extends BugChecker {",
            "  @Inject",
            "  public Test(ErrorProneFlags f) {}",
            "}")
        .doTest();
  }
}
