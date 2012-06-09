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

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import static com.google.errorprone.DiagnosticTestHelper.diagnosticMessage;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorFindingCompilerIntegrationTest {

  private DiagnosticTestHelper diagnosticHelper;
  private PrintWriter printWriter;
  private ByteArrayOutputStream outputStream;

  @Before
  public void setUp() {
    diagnosticHelper = new DiagnosticTestHelper();
    outputStream = new ByteArrayOutputStream();
    printWriter = new PrintWriter(new OutputStreamWriter(outputStream));
  }

  @Test
  public void testShouldFailToCompileSourceFileWithError() throws Exception {
    ErrorProneCompiler compiler = new ErrorProneCompiler.Builder()
        .named("test")
        .redirectOutputTo(printWriter)
        .listenToDiagnostics(diagnosticHelper.collector)
        .build();
    int exitCode = compiler.compile(sources(
        "com/google/errorprone/bugpatterns/empty_if_statement/PositiveCases.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(1));

    Matcher<Iterable<? super Diagnostic<JavaFileObject>>> matcher = hasItem(allOf(
        diagnosticMessage(containsString("Empty statement after if"))));
    assertThat("Warning should be found. " + diagnosticHelper.describe(),
        diagnosticHelper.getDiagnostics(), matcher);
  }
  
  @Test
  public void testShouldSucceedCompileSourceFileWithMultipleTopLevelClasses() throws Exception {
    ErrorProneCompiler compiler = new ErrorProneCompiler.Builder()
        .named("test")
        .redirectOutputTo(printWriter)
        .listenToDiagnostics(diagnosticHelper.collector)
        .build();
    int exitCode = compiler.compile(sources("com/google/errorprone/Foo.java"));
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(0));

  }

  private String[] sources(String... files) throws URISyntaxException {
    String[] result = new String[files.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = new File(getClass().getResource("/" + files[i]).toURI()).getAbsolutePath();
    }
    return result;
  }
}
