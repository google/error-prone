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

import static com.google.errorprone.DiagnosticTestHelper.diagnosticMessage;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorReportingJavaCompilerIntegrationTest extends IntegrationTest {

  private DiagnosticTestHelper diagnosticHelper;
  private PrintWriter printWriter;
  private ByteArrayOutputStream outputStream;
  ErrorProneCompiler compiler;

  @Before
  public void setUp() {
    diagnosticHelper = new DiagnosticTestHelper();
    outputStream = new ByteArrayOutputStream();
    printWriter = new PrintWriter(new OutputStreamWriter(outputStream));
    compiler = new ErrorProneCompiler.Builder()
        .named("test")
        .redirectOutputTo(printWriter)
        .listenToDiagnostics(diagnosticHelper.collector)
        .build();
  }

  @Test
  public void fileWithError() throws Exception {
    int exitCode = compiler.compile(sources(
        "com/google/errorprone/bugpatterns/EmptyIfStatementPositiveCases.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(1));

    Matcher<Iterable<? super Diagnostic<JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[EmptyIf]")));
    assertThat("Warning should be found. " + diagnosticHelper.describe(),
        diagnosticHelper.getDiagnostics(), matcher);
  }

  @Test
  public void fileWithMultipleTopLevelClasses() throws Exception {
    int exitCode = compiler.compile(
        sources("com/google/errorprone/MultipleTopLevelClassesWithNoErrors.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(0));
  }

  @Test
  public void fileWithMultipleTopLevelClassesExtends() throws Exception {
    int exitCode = compiler.compile(
        sources("com/google/errorprone/MultipleTopLevelClassesWithNoErrors.java",
            "com/google/errorprone/ExtendedMultipleTopLevelClassesWithNoErrors.java"));
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
        sources("com/google/errorprone/MultipleTopLevelClassesWithErrors.java",
            "com/google/errorprone/ExtendedMultipleTopLevelClassesWithErrors.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(1));

    Matcher<Iterable<? super Diagnostic<JavaFileObject>>> matcher = hasItem(
        diagnosticMessage(containsString("[SelfAssignment]")));
    assertThat("Warning should be found. " + diagnosticHelper.describe(),
        diagnosticHelper.getDiagnostics(), matcher);
  }
}
