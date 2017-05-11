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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.Suppressibility.UNSUPPRESSIBLE;

import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.EmptyIfStatement;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.main.Main.Result;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.tools.JavaFileObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author eaftan@google.com (Eddie Aftandilian) */
@RunWith(JUnit4.class)
public class CommandLineFlagTest {

  @BugPattern(
    name = "DisableableChecker",
    altNames = "foo",
    summary = "Disableable checker that flags all return statements as errors",
    explanation = "Disableable checker that flags all return statements as errors",
    category = ONE_OFF,
    severity = ERROR
  )
  public static class DisableableChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @BugPattern(
    name = "NondisableableChecker",
    summary = "NondisableableChecker checker that flags all return statements as errors",
    explanation = "NondisableableChecker checker that flags all return statements as errors",
    suppressibility = UNSUPPRESSIBLE,
    category = ONE_OFF,
    severity = ERROR
  )
  public static class NondisableableChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @BugPattern(
    name = "WarningChecker",
    summary = "Checker that flags all return statements as warnings",
    explanation = "Checker that flags all return statements as warningss",
    category = ONE_OFF,
    severity = WARNING
  )
  public static class WarningChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @BugPattern(
    name = "ErrorChecker",
    summary = "Checker that flags all return statements as errors",
    explanation = "Checker that flags all return statements as errors",
    category = ONE_OFF,
    severity = ERROR
  )
  public static class ErrorChecker extends BugChecker implements ReturnTreeMatcher {
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
    builder =
        new ErrorProneTestCompiler.Builder()
            .report(BuiltInCheckerSuppliers.defaultChecks())
            .listenToDiagnostics(diagnosticHelper.collector)
            .redirectOutputTo(new PrintWriter(output, true));
  }

  /* Tests for new-style ("-Xep:") flags */

  @Test
  public void malformedFlag() throws Exception {
    ErrorProneTestCompiler compiler = builder.build();

    List<String> badArgs =
        Arrays.asList(
            "-Xep:Foo:WARN:jfkdlsdf", // too many parts
            "-Xep:", // no check name
            "-Xep:Foo:FJDKFJSD"); // nonexistent severity level

    Result exitCode;
    for (String badArg : badArgs) {
      exitCode = compiler.compile(new String[] {badArg}, Collections.<JavaFileObject>emptyList());
      assertThat(exitCode).isEqualTo(Result.CMDERR);
      assertThat(output.toString()).contains("invalid flag");
    }
  }

  // We have to use one of the built-in checkers for the following two tests because there is no
  // way to specify a custom checker and have it be off by default.
  @Test
  public void canEnableWithDefaultSeverity() throws Exception {
    ErrorProneTestCompiler compiler = builder.build();
    List<JavaFileObject> sources =
        compiler
            .fileManager()
            .forResources(EmptyIfStatement.class, "testdata/EmptyIfStatementPositiveCases.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();

    exitCode = compiler.compile(new String[] {"-Xep:EmptyIf"}, sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);
  }

  @Test
  public void canEnableWithOverriddenSeverity() throws Exception {
    ErrorProneTestCompiler compiler = builder.build();
    List<JavaFileObject> sources =
        compiler
            .fileManager()
            .forResources(EmptyIfStatement.class, "testdata/EmptyIfStatementPositiveCases.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();

    diagnosticHelper.clearDiagnostics();
    exitCode = compiler.compile(new String[] {"-Xep:EmptyIf:WARN"}, sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isNotEmpty();
    assertThat(diagnosticHelper.getDiagnostics().toString()).contains("[EmptyIf]");
  }

  @Test
  public void canPromoteToError() throws Exception {
    ErrorProneTestCompiler compiler =
        builder.report(ScannerSupplier.fromBugCheckerClasses(WarningChecker.class)).build();
    List<JavaFileObject> sources =
        compiler.fileManager().forResources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isNotEmpty();

    exitCode = compiler.compile(new String[] {"-Xep:WarningChecker:ERROR"}, sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);
  }

  @Test
  public void canDemoteToWarning() throws Exception {
    ErrorProneTestCompiler compiler =
        builder.report(ScannerSupplier.fromBugCheckerClasses(ErrorChecker.class)).build();
    List<JavaFileObject> sources =
        compiler.fileManager().forResources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);

    diagnosticHelper.clearDiagnostics();
    exitCode = compiler.compile(new String[] {"-Xep:ErrorChecker:WARN"}, sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isNotEmpty();
  }

  @Test
  public void canDisable() throws Exception {
    ErrorProneTestCompiler compiler =
        builder.report(ScannerSupplier.fromBugCheckerClasses(DisableableChecker.class)).build();
    List<JavaFileObject> sources =
        compiler.fileManager().forResources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);

    diagnosticHelper.clearDiagnostics();
    exitCode = compiler.compile(new String[] {"-Xep:DisableableChecker:OFF"}, sources);
    assertThat(exitCode).isEqualTo(Result.OK);
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
  }

  @Test
  public void cantDisableNondisableableCheck() throws Exception {
    ErrorProneTestCompiler compiler =
        builder.report(ScannerSupplier.fromBugCheckerClasses(NondisableableChecker.class)).build();
    List<JavaFileObject> sources =
        compiler.fileManager().forResources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(new String[] {"-Xep:NondisableableChecker:OFF"}, sources);
    assertThat(output.toString()).contains("NondisableableChecker may not be disabled");
    assertThat(exitCode).isEqualTo(Result.CMDERR);
  }

  @Test
  public void cantOverrideNonexistentCheck() throws Exception {
    ErrorProneTestCompiler compiler = builder.build();
    List<JavaFileObject> sources =
        compiler.fileManager().forResources(getClass(), "CommandLineFlagTestFile.java");
    List<String> badOptions =
        Arrays.asList(
            "-Xep:BogusChecker:ERROR",
            "-Xep:BogusChecker:WARN",
            "-Xep:BogusChecker:OFF",
            "-Xep:BogusChecker");

    for (String badOption : badOptions) {
      Result exitCode = compiler.compile(new String[] {badOption}, sources);
      assertThat(exitCode).isEqualTo(Result.CMDERR);
      assertThat(output.toString()).contains("BogusChecker is not a valid checker name");
    }
  }

  @Test
  public void cantOverrideByAltname() throws Exception {
    ErrorProneTestCompiler compiler =
        builder.report(ScannerSupplier.fromBugCheckerClasses(DisableableChecker.class)).build();
    List<JavaFileObject> sources =
        compiler.fileManager().forResources(getClass(), "CommandLineFlagTestFile.java");

    Result exitCode = compiler.compile(new String[] {"-Xep:foo:OFF"}, sources);
    assertThat(exitCode).isEqualTo(Result.CMDERR);
    assertThat(output.toString()).contains("foo is not a valid checker name");
  }

  @Test
  public void ignoreUnknownChecksFlagAllowsOverridingUnknownCheck() throws Exception {
    ErrorProneTestCompiler compiler = builder.build();
    List<JavaFileObject> sources =
        compiler.fileManager().forResources(getClass(), "CommandLineFlagTestFile.java");
    List<String> badOptions =
        Arrays.asList(
            "-Xep:BogusChecker:ERROR",
            "-Xep:BogusChecker:WARN",
            "-Xep:BogusChecker:OFF",
            "-Xep:BogusChecker");

    for (String badOption : badOptions) {
      Result exitCode =
          compiler.compile(new String[] {"-XepIgnoreUnknownCheckNames", badOption}, sources);
      assertThat(exitCode).isEqualTo(Result.OK);
    }
  }
}
