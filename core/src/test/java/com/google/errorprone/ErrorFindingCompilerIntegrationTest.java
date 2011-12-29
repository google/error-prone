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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorFindingCompilerIntegrationTest {

  private DiagnosticCollector<JavaFileObject> diagnostics;
  private PrintWriter printWriter;
  private ByteArrayOutputStream outputStream;

  @Before
  public void setUp() {
    diagnostics = new DiagnosticCollector<JavaFileObject>();
    outputStream = new ByteArrayOutputStream();
    printWriter = new PrintWriter(new OutputStreamWriter(outputStream));
  }

  @Test
  public void testShouldFailToCompileSourceFileWithError() throws Exception {
    ErrorFindingCompiler compiler = new ErrorFindingCompiler.Builder()
        .named("test")
        .redirectOutputTo(printWriter)
        .listenToDiagnostics(diagnostics)
        .build();
    String[] sources = sources(
        "com/google/errorprone/checkers/empty_if_statement/PositiveCases.java");
    // TODO(eaftan): Running test with the annotation processor compiler enabled causes
    // the wrong copy of JavaCompiler to be used.  We should probably switch Maven to 
    // having an explicit docgen phase that calls the annotation processor with proc:only,
    // then not have the annotation processor on the compile classpath.
    String[] args = new String[sources.length+1];
    System.arraycopy(sources, 0, args, 0, sources.length);
    args[sources.length] = new String("-proc:none");
    int exitCode = compiler.compile(args);
    outputStream.flush();
    assertThat(outputStream.toString(), exitCode, is(1));

    Matcher<Iterable<? super Diagnostic<JavaFileObject>>> matcher = hasItem(allOf(
        diagnosticLineAndColumn(41L, 5L),
        diagnosticMessage(containsString("empty statement after if"))));
    assertThat("Warning should be found. Diagnostics: " + diagnostics.getDiagnostics(),
        diagnostics.getDiagnostics(), matcher);
  }

  private String[] sources(String... files) throws URISyntaxException {
    String[] result = new String[files.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = new File(getClass().getResource("/" + files[i]).toURI()).getAbsolutePath();
    }
    return result;
  }

  private TypeSafeDiagnosingMatcher<Diagnostic<JavaFileObject>> diagnosticLineAndColumn(
      final long line, final long column) {
    return new TypeSafeDiagnosingMatcher<Diagnostic<JavaFileObject>>() {
      @Override
      protected boolean matchesSafely(
          Diagnostic<JavaFileObject> item, Description mismatchDescription) {
        mismatchDescription
            .appendText("line:column")
            .appendValue(item.getLineNumber())
            .appendText(":")
            .appendValue(item.getColumnNumber());
        return item.getLineNumber() == line && item.getColumnNumber() == column;
      }

      @Override
      public void describeTo(Description description) {
        description
            .appendText("a diagnostic on line:column ")
            .appendValue(line)
            .appendText(":")
            .appendValue(column);
      }
    };
  }

  private TypeSafeDiagnosingMatcher<Diagnostic<JavaFileObject>> diagnosticMessage(
      final Matcher<String> matcher) {
    return new TypeSafeDiagnosingMatcher<Diagnostic<JavaFileObject>>() {
      @Override
      protected boolean matchesSafely(
          Diagnostic<JavaFileObject> item, Description mismatchDescription) {
        return matcher.matches(item.getMessage(Locale.getDefault()));
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a diagnostic with message ").appendDescriptionOf(matcher);
      }
    };
  }
}
