/*
 * Copyright 2022 The Error Prone Authors.
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

/** Tests for {@link BugPatternNaming}. */
@RunWith(JUnit4.class)
public final class BugPatternNamingTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(BugPatternNaming.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            "package com.google.errorprone.bugpatterns;",
            "import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;",
            "import com.google.errorprone.BugPattern;",
            "@BugPattern(name = \"Trololol\", severity = WARNING, summary = \"\")",
            "// BUG: Diagnostic contains:",
            "class Test extends BugChecker {}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "package com.google.errorprone.bugpatterns;",
            "import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;",
            "import com.google.errorprone.BugPattern;",
            "@BugPattern(name = \"Test\", severity = WARNING, summary = \"\")",
            "class Test extends BugChecker {}")
        .doTest();
  }
}
