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

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static org.junit.Assert.fail;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.main.Main.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CompilationTestHelper}. */
@RunWith(JUnit4.class)
public class CompilationTestHelperTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ReturnTreeChecker.class, getClass());
  }

  @Test
  public void fileWithNoBugMarkersAndNoErrorsShouldPass() {
    compilationHelper.addSourceLines("Test.java", "public class Test {}").doTest();
  }

  @Test
  public void fileWithNoBugMarkersAndErrorFails() {
    try {
      compilationHelper
          .addSourceLines(
              "Test.java",
              "public class Test {",
              "  public boolean doIt() {",
              "    return true;",
              "  }",
              "}")
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage()).contains("Saw unexpected error on line 3");
    }
  }

  @Test
  public void fileWithBugMarkerAndNoErrorsFails() {
    try {
      compilationHelper
          .addSourceLines(
              "Test.java",
              "public class Test {",
              "  // BUG: Diagnostic contains:",
              "  public void doIt() {}",
              "}")
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage()).contains("Did not see an error on line 3");
    }
  }

  @Test
  public void fileWithBugMatcherAndNoErrorsFails() {
    try {
      compilationHelper
          .addSourceLines(
              "Test.java",
              "public class Test {",
              "  // BUG: Diagnostic matches: X",
              "  public void doIt() {}",
              "}")
          .expectErrorMessage("X", Predicates.containsPattern(""))
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage()).contains("Did not see an error on line 3");
    }
  }

  @Test
  public void fileWithBugMarkerAndMatchingErrorSucceeds() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public boolean doIt() {",
            "    // BUG: Diagnostic contains: Method may return normally",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fileWithBugMatcherAndMatchingErrorSucceeds() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public boolean doIt() {",
            "    // BUG: Diagnostic matches: X",
            "    return true;",
            "  }",
            "}")
        .expectErrorMessage("X", Predicates.containsPattern("Method may return normally"))
        .doTest();
  }

  @Test
  public void fileWithBugMarkerAndErrorOnWrongLineFails() {
    try {
      compilationHelper
          .addSourceLines(
              "Test.java",
              "public class Test {",
              "  // BUG: Diagnostic contains:",
              "  public boolean doIt() {",
              "    return true;",
              "  }",
              "}")
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage()).contains("Did not see an error on line 3");
    }
  }

  @Test
  public void fileWithBugMatcherAndErrorOnWrongLineFails() {
    try {
      compilationHelper
          .addSourceLines(
              "Test.java",
              "public class Test {",
              "  // BUG: Diagnostic matches: X",
              "  public boolean doIt() {",
              "    return true;",
              "  }",
              "}")
          .expectErrorMessage("X", Predicates.containsPattern(""))
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage()).contains("Did not see an error on line 3");
    }
  }

  @Test
  public void fileWithMultipleBugMarkersAndMatchingErrorsSucceeds() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public boolean doIt() {",
            "    // BUG: Diagnostic contains: Method may return normally",
            "    return true;",
            "  }",
            "  public String doItAgain() {",
            "    // BUG: Diagnostic contains: Method may return normally",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fileWithMultipleSameBugMatchersAndMatchingErrorsSucceeds() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public boolean doIt() {",
            "    // BUG: Diagnostic matches: X",
            "    return true;",
            "  }",
            "  public String doItAgain() {",
            "    // BUG: Diagnostic matches: X",
            "    return null;",
            "  }",
            "}")
        .expectErrorMessage("X", Predicates.containsPattern("Method may return normally"))
        .doTest();
  }

  @Test
  public void fileWithMultipleDifferentBugMatchersAndMatchingErrorsSucceeds() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public boolean doIt() {",
            "    // BUG: Diagnostic matches: X",
            "    return true;",
            "  }",
            "  public String doItAgain() {",
            "    // BUG: Diagnostic matches: Y",
            "    return null;",
            "  }",
            "}")
        .expectErrorMessage("X", Predicates.containsPattern("Method may return normally"))
        .expectErrorMessage("Y", Predicates.containsPattern("Method may return normally"))
        .doTest();
  }

  @Test
  public void fileWithSyntaxErrorFails() throws Exception {
    try {
      compilationHelper
          .addSourceLines(
              "Test.java",
              "class Test {",
              "  void m() {",
              "    // BUG: Diagnostic contains:",
              "    return}", // there's a syntax error on this line, but it shouldn't register as
              // an error-prone diagnostic
              "}")
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage())
          .contains("Test program failed to compile with non Error Prone error");
    }
  }

  @Test
  public void expectedResultMatchesActualResultSucceeds() {
    compilationHelper
        .expectResult(Result.OK)
        .addSourceLines("Test.java", "public class Test {}")
        .doTest();
  }

  @Test
  public void expectedResultDiffersFromActualResultFails() {
    try {
      compilationHelper
          .expectResult(Result.ERROR)
          .addSourceLines("Test.java", "public class Test {}")
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage()).contains("Expected compilation result ERROR, but was OK");
    }
  }

  @Test
  public void expectNoDiagnoticsAndNoDiagnosticsProducedSucceeds() {
    compilationHelper
        .expectNoDiagnostics()
        .addSourceLines("Test.java", "// BUG: Diagnostic contains:", "public class Test {}")
        .doTest();
  }

  @Test
  public void expectNoDiagnoticsAndNoDiagnosticsProducedSucceedsWithMatches() {
    compilationHelper
        .expectNoDiagnostics()
        .addSourceLines("Test.java", "// BUG: Diagnostic matches: X", "public class Test {}")
        .expectErrorMessage("X", Predicates.containsPattern(""))
        .doTest();
  }

  @Test
  public void expectNoDiagnoticsButDiagnosticsProducedFails() {
    try {
      compilationHelper
          .expectNoDiagnostics()
          .addSourceLines(
              "Test.java",
              "public class Test {",
              "  public boolean doIt() {",
              "    // BUG: Diagnostic contains:",
              "    return true;",
              "  }",
              "}")
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage()).contains("Expected no diagnostics produced, but found 1");
    }
  }

  @Test
  public void expectNoDiagnoticsButDiagnosticsProducedFailsWithMatches() {
    try {
      compilationHelper
          .expectNoDiagnostics()
          .addSourceLines(
              "Test.java",
              "public class Test {",
              "  public boolean doIt() {",
              "    // BUG: Diagnostic matches: X",
              "    return true;",
              "  }",
              "}")
          .expectErrorMessage("X", Predicates.containsPattern(""))
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage()).contains("Expected no diagnostics produced, but found 1");
    }
  }

  @Test
  public void failureWithErrorAndNoDiagnosticFails() {
    try {
      compilationHelper
          .expectNoDiagnostics()
          .addSourceLines("Test.java", "public class Test {}")
          .setArgs(ImmutableList.of("-Xep:ReturnTreeChecker:Squirrels")) // Bad flag crashes.
          .ignoreJavacErrors()
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage())
          .contains(
              "Expected compilation result to be OK, but was CMDERR. No diagnostics were"
                  + " emitted.");
      assertThat(expected.getMessage()).contains("InvalidCommandLineOptionException");
    }
  }

  @Test
  public void commandLineArgToDisableCheckWorks() {
    compilationHelper
        .setArgs(ImmutableList.of("-Xep:ReturnTreeChecker:OFF"))
        .expectNoDiagnostics()
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  public boolean doIt() {",
            "    // BUG: Diagnostic contains:",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void missingExpectErrorFails() {
    try {
      compilationHelper
          .addSourceLines("Test.java", " // BUG: Diagnostic matches: X", "public class Test {}")
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage()).contains("No expected error message with key [X]");
    }
  }

  @BugPattern(
    name = "ReturnTreeChecker",
    summary = "Method may return normally.",
    explanation = "Consider mutating some global state instead.",
    category = JDK,
    severity = ERROR
  )
  public static class ReturnTreeChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @Test
  public void unexpectedDiagnosticOnFirstLine() {
    try {
      CompilationTestHelper.newInstance(PackageTreeChecker.class, getClass())
          .addSourceLines("test/Test.java", "package test;", "public class Test {}")
          .doTest();
      fail();
    } catch (AssertionError expected) {
      assertThat(expected.getMessage()).contains("Package declaration found");
    }
  }

  @BugPattern(
    name = "PackageTreeChecker",
    summary = "Package declaration found",
    explanation = "Prefer to use the default package for everything.",
    category = JDK,
    severity = ERROR
  )
  public static class PackageTreeChecker extends BugChecker implements CompilationUnitTreeMatcher {
    @Override
    public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
      if (tree.getPackage() != null) {
        return describeMatch(tree.getPackage());
      }
      return Description.NO_MATCH;
    }
  }
}
