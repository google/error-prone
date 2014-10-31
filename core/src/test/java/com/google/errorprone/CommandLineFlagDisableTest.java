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
import java.util.List;

import javax.tools.JavaFileObject;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class CommandLineFlagDisableTest {

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

  @Test
  public void flagWorks() throws Exception {
    ErrorProneTestCompiler compiler = builder
        .report(ScannerSupplier.fromBugCheckers(new DisableableChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");
    
    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);
    assertThat(diagnosticHelper.getDiagnostics().size()).isGreaterThan(0);
    
    diagnosticHelper.clearDiagnostics();
    exitCode = compiler.compile(new String[]{"-Xepdisable:DisableableChecker"}, sources);
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
    
    Result exitCode = compiler.compile(new String[]{"-Xepdisable:NondisableableChecker"}, sources);
    assertThat(exitCode).isEqualTo(Result.CMDERR);
    assertThat(output.toString()).contains(
        "error-prone check NondisableableChecker may not be disabled");
  }

  @Test
  public void noEffectWhenDisableNonexistentCheck() throws Exception {
    ErrorProneTestCompiler compiler =  builder
        .report(ScannerSupplier.fromBugCheckers(new DisableableChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");
    
    Result exitCode = compiler.compile(new String[]{"-Xepdisable:BogusChecker"}, sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);
    assertThat(output.toString()).isEqualTo("");
  }

  @Test
  public void cantDisableByAltname() throws Exception {
    ErrorProneTestCompiler compiler =  builder
        .report(ScannerSupplier.fromBugCheckers(new DisableableChecker()))
        .build();
    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");
    
    Result exitCode = compiler.compile(new String[]{"-Xepdisable:foo"}, sources);
    assertThat(exitCode).isEqualTo(Result.ERROR);
    assertThat(output.toString()).isEqualTo("");
  }
}


