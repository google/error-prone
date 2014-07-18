/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.CompilationTestHelper.sources;
import static com.google.errorprone.DiagnosticTestHelper.diagnosticMessage;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ExpressionStatementTreeMatcher;
import com.google.errorprone.bugpatterns.EmptyStatement;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.ExpressionStatementTree;
import com.sun.tools.javac.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Integration tests for {@link ErrorProneCompiler}.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class ErrorProneCompilerIntegrationTest {

  private DiagnosticTestHelper diagnosticHelper;
  private PrintWriter printWriter;
  private ByteArrayOutputStream outputStream;
  private ErrorProneTestCompiler.Builder compilerBuilder;
  ErrorProneTestCompiler compiler;

  @Before
  public void setUp() {
    diagnosticHelper = new DiagnosticTestHelper();
    outputStream = new ByteArrayOutputStream();
    printWriter = new PrintWriter(new OutputStreamWriter(outputStream));
    compilerBuilder = new ErrorProneTestCompiler.Builder()
        .named("test")
        .redirectOutputTo(printWriter)
        .listenToDiagnostics(diagnosticHelper.collector);
    compiler = compilerBuilder.build();
  }

  @Test
  public void fileWithError() throws Exception {
    int exitCode = compiler.compile(sources(getClass(),
        "bugpatterns/EmptyIfStatementPositiveCases.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(1));

    Matcher<Iterable<Diagnostic<JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[EmptyIf]")));
    assertTrue("Error should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void fileWithWarning() throws Exception {
    compilerBuilder.report(ErrorProneScanner.forMatcher(EmptyStatement.class));
    compiler = compilerBuilder.build();
    int exitCode = compiler.compile(sources(getClass(),
        "bugpatterns/EmptyStatementPositiveCases.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(0));

    Matcher<Iterable<Diagnostic<JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[EmptyStatement]")));
    assertTrue("Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void fileWithMultipleTopLevelClasses() throws Exception {
    int exitCode = compiler.compile(
        sources(getClass(), "MultipleTopLevelClassesWithNoErrors.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(0));
  }

  @Test
  public void fileWithMultipleTopLevelClassesExtends() throws Exception {
    int exitCode = compiler.compile(
        sources(getClass(), "MultipleTopLevelClassesWithNoErrors.java",
            "ExtendedMultipleTopLevelClassesWithNoErrors.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(0));
  }

  /**
   * Regression test for a bug in which multiple top-level classes may cause
   * NullPointerExceptions in the matchers.
   */
  @Test
  public void fileWithMultipleTopLevelClassesExtendsWithError()
      throws Exception {
    int exitCode = compiler.compile(
        sources(getClass(), "MultipleTopLevelClassesWithErrors.java",
            "ExtendedMultipleTopLevelClassesWithErrors.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(1));

    Matcher<Iterable<Diagnostic<JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[SelfAssignment]")));
    assertTrue("Warning should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
    assertEquals(3, diagnosticHelper.getDiagnostics().size());
  }

  @Test
  public void unhandledExceptionsAreReportedWithoutBugParadeLink() throws Exception {
    @BugPattern(name = "", explanation = "", summary = "",
        maturity = EXPERIMENTAL, severity = ERROR, category = ONE_OFF)
    class Throwing extends BugChecker implements ExpressionStatementTreeMatcher {
      @Override
      public Description matchExpressionStatement(ExpressionStatementTree tree, VisitorState state)
      {
        throw new IllegalStateException("test123");
      }
    }
    compilerBuilder.report(new ErrorProneScanner(new Throwing()));
    compiler = compilerBuilder.build();
    int exitCode = compiler.compile(
        sources(getClass(), "MultipleTopLevelClassesWithErrors.java",
            "ExtendedMultipleTopLevelClassesWithErrors.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(1));
    Matcher<Iterable<Diagnostic<JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(CoreMatchers.<String>allOf(
            containsString("IllegalStateException: test123"),
            containsString("unhandled exception was thrown by the Error Prone"))));
    assertTrue("Error should be reported. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  /**
   * Regression test for Issue 188, error-prone doesn't work with annotation processors.
   */
  @Test
  public void annotationProcessingWorks() throws Exception {
    int exitCode = compiler.compile(
        sources(getClass(), "UsesAnnotationProcessor.java"),
        List.of(new NullAnnotationProcessor()));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(0));
  }

  /**
   * Test that if javac does dataflow on a class twice error-prone only analyses it once.
   */
  @Test
  public void reportReadyForAnalysisOnce() throws Exception {
    int exitCode = compiler.compile(
        sources(getClass(),
            "FlowConstants.java",
            "FlowSub.java",
            // This order is important: the superclass needs to occur after the subclass in the
            // sources so it goes through flow twice (once so it can be used when the subclass
            // is desugared, once normally).
            "FlowSuper.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(0));
  }
}
