/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.covariant_equals;

import static com.google.errorprone.DiagnosticTestHelper.diagnosticMessage;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

import com.google.errorprone.DiagnosticTestHelper;
import com.google.errorprone.ErrorProneCompiler;

import org.hamcrest.Matcher;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CovariantEqualsTest {
  @Test public void testPositiveCase() throws Exception {
    ErrorProneCompiler compiler;
    DiagnosticTestHelper diagnosticHelper;
    Matcher<Iterable<? super Diagnostic<JavaFileObject>>> overrideMatcher = hasItem(
        diagnosticMessage(containsString("Did you mean '@Override")));
    for (String inputFile : Arrays.asList("PositiveCase1.java", "PositiveCase2.java",
        "PositiveCase3.java")) {
      diagnosticHelper = new DiagnosticTestHelper();
      compiler = new ErrorProneCompiler.Builder()
          .report(new CovariantEquals.Scanner())
          .listenToDiagnostics(diagnosticHelper.collector)
          .build();
      File source = new File(this.getClass().getResource(inputFile).toURI());
      assertThat(compiler.compile(new String[]{"-Xjcov", source.getAbsolutePath()}), is(1));
      assertThat("In diagnostics: " + diagnosticHelper.getDiagnostics(),
          diagnosticHelper.getDiagnostics(), overrideMatcher);
    }

    diagnosticHelper = new DiagnosticTestHelper();
    compiler = new ErrorProneCompiler.Builder()
        .report(new CovariantEquals.Scanner())
        .listenToDiagnostics(diagnosticHelper.collector)
        .build();
    File source = new File(this.getClass().getResource("PositiveCase4.java").toURI());
    assertThat(compiler.compile(new String[]{"-Xjcov", source.getAbsolutePath()}), is(1));
    Matcher<Iterable<? super Diagnostic<JavaFileObject>>> removeMatcher = hasItem(
            diagnosticMessage(containsString("Did you mean to remove this line")));
    assertThat("In diagnostics: " + diagnosticHelper.getDiagnostics(),
        diagnosticHelper.getDiagnostics(), removeMatcher);
  }

  @Test public void testNegativeCase() throws Exception {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ErrorProneCompiler compiler = new ErrorProneCompiler.Builder()
        .report(new CovariantEquals.Scanner())
        .listenToDiagnostics(diagnosticHelper.collector)
        .build();
    File source = new File(this.getClass().getResource("NegativeCases.java").toURI());
    assertThat(compiler.compile(new String[]{source.getAbsolutePath()}), is(0));
  }
}
