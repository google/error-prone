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

import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.ErrorProneScanner.EnabledPredicate.DEFAULT_CHECKS;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;

import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.ReturnTree;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class CommandLineFlagDisableTest {

  @BugPattern(name = "DisableableChecker", altNames = "foo",
      summary = "Disableable checker that flags all return statements as errors",
      explanation = "Disableable checker that flags all return statements as errors",
      disableable = true, category = ONE_OFF, severity = ERROR, maturity = MATURE)
  private static class DisableableChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree, null);
    }
  }

  @BugPattern(name = "NondisableableChecker",
      summary = "NondisableableChecker checker that flags all return statements as errors",
      explanation = "NondisableableChecker checker that flags all return statements as errors",
      category = ONE_OFF, severity = ERROR, maturity = MATURE)
  private static class NondisableableChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree, null);
    }
  }

  private ErrorProneCompiler compiler;
  private DiagnosticTestHelper diagnosticHelper;
  private PrintStream oldStdout;
  private PrintStream oldStderr;
  private ByteArrayOutputStream testStdout = new ByteArrayOutputStream();
  private ByteArrayOutputStream testStderr = new ByteArrayOutputStream();

  @Before
  public void setUp() {
    // Redirect stdout and stderr so we can test usage and error messages.
    oldStdout = System.out;
    oldStderr = System.err;
    System.setOut(new PrintStream(testStdout));
    System.setErr(new PrintStream(testStderr));

    diagnosticHelper = new DiagnosticTestHelper();
  }

  @After
  public void tearDown() {
    System.setOut(oldStdout);
    System.setErr(oldStderr);
  }


  @Test
  public void flagWorks() throws Exception {
    compiler = new ErrorProneCompiler.Builder()
        .listenToDiagnostics(diagnosticHelper.collector)
        .report(new ErrorProneScanner(new DisableableChecker()))
        .build();

    File source = new File(
        this.getClass().getResource("CommandLineFlagTestFile.java").toURI());
    int exitCode = compiler.compile(new String[]{source.getAbsolutePath()});
    assertThat(exitCode, is(1));
    exitCode = compiler.compile(new String[]{
        "-Xepdisable:DisableableChecker", source.getAbsolutePath()});
    assertThat(exitCode, is(0));
  }

  @Test
  public void cantDisableNondisableableCheck() throws Exception {
    compiler = new ErrorProneCompiler.Builder()
        .listenToDiagnostics(diagnosticHelper.collector)
        .report(new ErrorProneScanner(new NondisableableChecker()))
        .build();

    File source = new File(
        this.getClass().getResource("CommandLineFlagTestFile.java").toURI());
    // This should exit with code 2, EXIT_CMDERR (not visible outside the com.sun.tools.javac.main
    // package).
    int exitCode = compiler.compile(
        new String[]{"-Xepdisable:NondisableableChecker", source.getAbsolutePath()});
    assertThat(exitCode, is(2));
    assertThat(testStderr.toString(),
        containsString("error-prone check NondisableableChecker may not be disabled"));
  }

  @Test
  public void noEffectWhenDisableNonexistentCheck() throws Exception {
    compiler = new ErrorProneCompiler.Builder()
        .listenToDiagnostics(diagnosticHelper.collector)
        .report(new ErrorProneScanner(new DisableableChecker()))
        .build();

    File source = new File(
        this.getClass().getResource("CommandLineFlagTestFile.java").toURI());
    int exitCode = compiler.compile(new String[]{"-Xepdisable:BogusChecker",
        source.getAbsolutePath()});
    assertThat(exitCode, is(1));
    assertThat(testStderr.toString(), isEmptyString());
  }

  @Test
  public void cantDisableByAltname() throws Exception {
    compiler = new ErrorProneCompiler.Builder()
        .listenToDiagnostics(diagnosticHelper.collector)
        .report(new ErrorProneScanner(new DisableableChecker()))
        .build();

    File source = new File(
        this.getClass().getResource("CommandLineFlagTestFile.java").toURI());
    int exitCode = compiler.compile(new String[]{"-Xepdisable:foo",
        source.getAbsolutePath()});
    assertThat(exitCode, is(1));
  }
}


