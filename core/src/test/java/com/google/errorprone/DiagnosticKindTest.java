/*
 * Copyright 2015 The Error Prone Authors.
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
import static com.google.errorprone.FileObjects.forSourceLines;

import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.main.Main.Result;
import java.util.Arrays;
import javax.tools.Diagnostic;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests that {@link BugPattern.SeverityLevel}s map to appropriate {@link
 * javax.tools.Diagnostic.Kind}s and are displayed in some reasonable way on the command line.
 */
@RunWith(JUnit4.class)
public class DiagnosticKindTest {

  /**
   * The mock code we are going to analyze in our tests. Only needs a return for the matcher to
   * match on.
   */
  private static final String[] TEST_CODE = {
    "class Test {", "  void doIt() {", "    return;", " }", "}"
  };

  private DiagnosticTestHelper diagnosticHelper;
  private ErrorProneTestCompiler.Builder compilerBuilder;

  @Before
  public void setUp() {
    diagnosticHelper = new DiagnosticTestHelper();
    compilerBuilder =
        new ErrorProneTestCompiler.Builder().listenToDiagnostics(diagnosticHelper.collector);
  }

  @BugPattern(
      name = "ErrorChecker",
      summary = "This is an error!",
      explanation = "Don't do this!",
      severity = SeverityLevel.ERROR)
  public static class ErrorChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @Test
  public void testError() {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(ErrorChecker.class));
    ErrorProneTestCompiler compiler = compilerBuilder.build();
    Result result = compiler.compile(Arrays.asList(forSourceLines("Test.java", TEST_CODE)));

    assertThat(diagnosticHelper.getDiagnostics()).hasSize(1);
    assertThat(diagnosticHelper.getDiagnostics().get(0).getKind()).isEqualTo(Diagnostic.Kind.ERROR);
    assertThat(diagnosticHelper.getDiagnostics().get(0).toString()).contains("error:");
    assertThat(result).isEqualTo(Result.ERROR);
  }

  @BugPattern(
      name = "WarningChecker",
      summary = "This is a warning!",
      explanation = "Please don't do this!",
      severity = SeverityLevel.WARNING)
  public static class WarningChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @Test
  public void testWarning() {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(WarningChecker.class));
    ErrorProneTestCompiler compiler = compilerBuilder.build();
    Result result = compiler.compile(Arrays.asList(forSourceLines("Test.java", TEST_CODE)));

    assertThat(diagnosticHelper.getDiagnostics()).hasSize(1);
    assertThat(diagnosticHelper.getDiagnostics().get(0).getKind())
        .isEqualTo(Diagnostic.Kind.WARNING);
    assertThat(diagnosticHelper.getDiagnostics().get(0).toString()).contains("warning:");
    assertThat(result).isEqualTo(Result.OK);
  }

  @BugPattern(
      name = "SuggestionChecker",
      summary = "This is a suggestion!",
      explanation = "Don't do this. Or do it. I'm a suggestion, not a cop.",
      severity = SeverityLevel.SUGGESTION)
  public static class SuggestionChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @Test
  public void testSuggestion() {
    compilerBuilder.report(ScannerSupplier.fromBugCheckerClasses(SuggestionChecker.class));
    ErrorProneTestCompiler compiler = compilerBuilder.build();
    Result result = compiler.compile(Arrays.asList(forSourceLines("Test.java", TEST_CODE)));

    assertThat(diagnosticHelper.getDiagnostics()).hasSize(1);
    assertThat(diagnosticHelper.getDiagnostics().get(0).getKind()).isEqualTo(Diagnostic.Kind.NOTE);
    assertThat(diagnosticHelper.getDiagnostics().get(0).toString()).contains("Note:");
    assertThat(result).isEqualTo(Result.OK);
  }
}
