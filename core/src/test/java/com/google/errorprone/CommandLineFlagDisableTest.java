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

import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.main.Main.Result;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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

  private ErrorProneTestCompiler compiler;
  private PrintWriter printWriter;
  private ByteArrayOutputStream outputStream;
  private DiagnosticTestHelper diagnosticHelper;
  private ErrorProneTestCompiler.Builder compilerBuilder;
  
  @Before
  public void setUp() {
    diagnosticHelper = new DiagnosticTestHelper();
    outputStream = new ByteArrayOutputStream();
    printWriter = new PrintWriter(new OutputStreamWriter(outputStream), true);
    compilerBuilder = new ErrorProneTestCompiler.Builder()
        .listenToDiagnostics(diagnosticHelper.collector)
        .redirectOutputTo(printWriter);
  }

  @Test
  public void flagWorks() throws Exception {
    compiler = compilerBuilder
        .report(new ErrorProneScanner(new DisableableChecker()))
        .build();

    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");
    Result exitCode = compiler.compile(sources);
    assertThat(exitCode).is(Result.ERROR);
    exitCode = compiler.compile(new String[]{"-Xepdisable:DisableableChecker"}, sources);
    assertThat(exitCode).is(Result.OK);
  }

  @Test
  public void cantDisableNondisableableCheck() throws Exception {
    compiler = compilerBuilder
        .report(new ErrorProneScanner(new NondisableableChecker()))
        .build();

    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");
    Result exitCode = compiler.compile(
        new String[]{"-Xepdisable:NondisableableChecker"}, sources);
    assertThat(exitCode).is(Result.CMDERR);
    assertThat(outputStream.toString()).contains(
        "error-prone check NondisableableChecker may not be disabled");
  }

  @Test
  public void noEffectWhenDisableNonexistentCheck() throws Exception {
    compiler =  compilerBuilder
        .report(new ErrorProneScanner(new DisableableChecker()))
        .build();

    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");
    Result exitCode = compiler.compile(new String[]{"-Xepdisable:BogusChecker"}, sources);
    assertThat(exitCode).is(Result.ERROR);
    assertThat(outputStream.toString()).is("");
  }

  @Test
  public void cantDisableByAltname() throws Exception {
    compiler =  new ErrorProneTestCompiler.Builder()
        .listenToDiagnostics(diagnosticHelper.collector)
        .report(new ErrorProneScanner(new DisableableChecker()))
        .build();

    List<JavaFileObject> sources =
        compiler.fileManager().sources(getClass(), "CommandLineFlagTestFile.java");
    Result exitCode = compiler.compile(new String[]{"-Xepdisable:foo"}, sources);
    assertThat(exitCode).is(Result.ERROR);
  }
}


