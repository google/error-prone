/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static org.junit.Assert.assertTrue;

import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.ReturnTree;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link CompilationTestHelper}Test  */
@RunWith(JUnit4.class)
public class CompilationTestHelperTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(new ReturnTreeChecker());
  }

  @Test
  public void fileWithSyntaxErrorShouldFail() throws Exception {
    boolean failed = true;
    try {
      compilationHelper.assertCompileFailsWithMessages(
          compilationHelper.fileManager().forSourceLines("Test.java",
              "class Test {",
              "  void m() {",
              "    // BUG: Diagnostic contains:",
              "    return}", // there's a syntax error on this line, but it shouldn't register as
                             // an error-prone diagnostic
              "}"
              ));
      failed = false;
    } catch (Throwable unused) {
      // Expect test to fail:
      assertTrue(unused.getMessage().contains(
          "Test program failed to compile with non error-prone error"));
    }
    assertTrue(failed);
  }

  @BugPattern(name = "ReturnTreeChecker",
      summary = "Method may return normally.",
      explanation = "Consider mutating some global state instead.",
      category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
  private static class ReturnTreeChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }
}
