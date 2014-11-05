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
import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.ScannerSupplier;

import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.main.Main.Result;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaFileObject;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class CommandLineFlagTest {

  @BugPattern(name = "DisableableChecker", altNames = "foo",
      summary = "Disableable checker that flags all return statements as errors",
      explanation = "Disableable checker that flags all return statements as errors",
      disableable = true, category = ONE_OFF, severity = ERROR, maturity = MATURE)
  private static class DisableableChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @BugPattern(name = "NondisableableChecker",
      summary = "NondisableableChecker checker that flags all return statements as errors",
      explanation = "NondisableableChecker checker that flags all return statements as errors",
      category = ONE_OFF, severity = ERROR, maturity = MATURE)
  private static class NondisableableChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @BugPattern(name = "WarningChecker",
      summary = "Checker that flags all return statements as warnings",
      explanation = "Checker that flags all return statements as warningss",
      category = ONE_OFF, severity = WARNING, maturity = MATURE)
  private static class WarningChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @BugPattern(name = "ErrorChecker",
      summary = "Checker that flags all return statements as errors",
      explanation = "Checker that flags all return statements as errors",
      category = ONE_OFF, severity = ERROR, maturity = MATURE)
  private static class ErrorChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  private ErrorProneTestCompiler.Builder builder;
  private DiagnosticTestHelper diagnosticHelper;
  private Writer output;

  @Before
  public void setUp() {
    output = new StringWriter();
    diagnosticHelper = new DiagnosticTestHelper();
    builder = new ErrorProneTestCompiler.Builder()
        .listenToDiagnostics(diagnosticHelper.collector)
        .redirectOutputTo(new PrintWriter(output, true));
  }


  /* Tests for new-style ("-Xep:") flags */

  @Test
  public void malformedFlag() throws Exception {
    ErrorProneTestCompiler compiler = builder.build();

    List<String> badArgs = Arrays.asList(
        "-Xep:Foo:WARN:jfkdlsdf", // too many parts
        "-Xep:", // no check name
        "-Xep:Foo:FJDKFJSD"); // nonexistent severity level

    Result exitCode;
    for (String badArg : badArgs) {
      exitCode = compiler.compile(new String[]{badArg},
          Collections.<JavaFileObject>emptyList());
      assertThat(exitCode).isEqualTo(Result.CMDERR);
      assertThat(output.toString()).contains("invalid flag");
    }
  }

  // We have to use one of the built-in checkers for the following two tests because there is no
  // way to specify a custom checker and have it be off by default.
  @Test
  public void canEnableWithDefaultSeverity() throws Exception {
    ErrorProneTestCompiler compiler = builder.build();
    List<JavaFileObject> sources = compiler.fileManager().sources(getClass(),
        "bugpatterns/EmptyIfStatementPositiveCases.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();

    exitCode = compiler.compile(new String[]{"-Xep:EmptyIf"}, sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);
  }

  @Test
  public void canEnableWithOverriddenSeverity() throws Exception {
    ErrorProneTestCompiler compiler = builder.build();
    List<JavaFileObject> sources = compiler.fileManager().sources(getClass(),
        "bugpatterns/EmptyIfStatementPositiveCases.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();

    diagnosticHelper.clearDiagnostics();
    exitCode = compiler.compile(new String[]{"-Xep:EmptyIf:WARN"}, sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isNotEmpty();
    assertThat(diagnosticHelper.getDiagnostics().toString()).contains("[EmptyIf]");
  }

  @Test
  public void canPromoteToError() throws Exception {
    ErrorProneTestCompiler compiler = builder
        .report(ScannerSupplier.fromBugCheckers(new WarningChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isNotEmpty();

    exitCode = compiler.compile(new String[]{"-Xep:WarningChecker:ERROR"}, sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);
  }

  @Test
  public void canDemoteToWarning() throws Exception {
    ErrorProneTestCompiler compiler = builder
        .report(ScannerSupplier.fromBugCheckers(new ErrorChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);

    diagnosticHelper.clearDiagnostics();
    exitCode = compiler.compile(new String[]{"-Xep:ErrorChecker:WARN"}, sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isNotEmpty();
  }

  @Test
  public void canDisable() throws Exception {
    ErrorProneTestCompiler compiler = builder
        .report(ScannerSupplier.fromBugCheckers(new DisableableChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);

    diagnosticHelper.clearDiagnostics();
    exitCode = compiler.compile(new String[]{"-Xep:DisableableChecker:OFF"}, sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics().size()).isEqualTo(0);
  }

  @Test
  public void cantDisableNondisableableCheck() throws Exception {
    ErrorProneTestCompiler compiler = builder
        .report(ScannerSupplier.fromBugCheckers(new NondisableableChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(new String[]{"-Xep:NondisableableChecker:OFF"}, sources);
    assertThat(exitCode).isEqualTo(Result.CMDERR);
    assertThat(output.toString()).contains("NondisableableChecker may not be disabled");
  }

  @Test
  public void cantOverrideNonexistentCheck() throws Exception {
    ErrorProneTestCompiler compiler =  builder.build();
    List<String> badOptions = Arrays.asList(
        "-Xep:BogusChecker:ERROR",
        "-Xep:BogusChecker:WARN",
        "-Xep:BogusChecker:OFF",
        "-Xep:BogusChecker");

    for (String badOption : badOptions) {
      Result exitCode = compiler.compile(new String[]{badOption},
          Collections.<JavaFileObject>emptyList());
      assertThat(exitCode).isEqualTo(Result.CMDERR);
      assertThat(output.toString()).contains("BogusChecker is not a valid checker name");
    }
  }

  @Test
  public void cantOverrideByAltname() throws Exception {
    ErrorProneTestCompiler compiler =  builder
        .report(ScannerSupplier.fromBugCheckers(new DisableableChecker()))
        .build();

    List<String> altNameOptions = Arrays.asList(
        "-Xep:foo:ERROR",
        "-Xep:foo:WARN",
        "-Xep:foo:OFF",
        "-Xep:foo");

    for (String altNameOption : altNameOptions) {
      Result exitCode = compiler.compile(new String[]{altNameOption},
          Collections.<JavaFileObject>emptyList());
      assertThat(exitCode).isEqualTo(Result.CMDERR);
      assertThat(output.toString()).contains("foo is not a valid checker name");
    }
  }


  /* Tests for old-style ("-Xepdisable:") flags */

  @Test
  public void canDisableWithOldStyleFlag() throws Exception {
    ErrorProneTestCompiler compiler = builder
        .report(ScannerSupplier.fromBugCheckers(new DisableableChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);

    diagnosticHelper.clearDiagnostics();
    exitCode = compiler.compile(new String[]{"-Xepdisable:DisableableChecker"}, sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
  }

  @Test
  public void cantDisableNondisableableCheckWithOldStyleFlag() throws Exception {
    ErrorProneTestCompiler compiler = builder
        .report(ScannerSupplier.fromBugCheckers(new NondisableableChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(new String[]{"-Xepdisable:NondisableableChecker"}, sources);
    assertThat(exitCode).isEqualTo(Result.CMDERR);
    assertThat(output.toString()).contains("NondisableableChecker may not be disabled");
  }

  @Test
  public void cantDisableNonexistentCheckWithOldStyleFlag() throws Exception {
    ErrorProneTestCompiler compiler =  builder
        .report(ScannerSupplier.fromBugCheckers(new DisableableChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(new String[]{"-Xepdisable:BogusChecker"}, sources);
    assertThat(exitCode).isEqualTo(Result.CMDERR);
    assertThat(output.toString()).contains("BogusChecker is not a valid checker name");
  }

  @Test
  public void cantDisableByAltnameWithOldStyleFlag() throws Exception {
    ErrorProneTestCompiler compiler =  builder
        .report(ScannerSupplier.fromBugCheckers(new DisableableChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(new String[]{"-Xepdisable:foo"}, sources);
    assertThat(exitCode).isEqualTo(Result.CMDERR);
    assertThat(output.toString()).contains("foo is not a valid checker name");
  }
}


