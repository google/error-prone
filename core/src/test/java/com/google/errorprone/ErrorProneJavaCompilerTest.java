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

import static com.google.errorprone.DiagnosticTestHelper.diagnosticMessage;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.DepAnnTest;
import com.google.errorprone.scanner.ScannerSupplier;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@RunWith(JUnit4.class)
public class ErrorProneJavaCompilerTest {

  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void testIsSupportedOption() {
    JavaCompiler mockCompiler = mock(JavaCompiler.class);
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler(mockCompiler);

    // javac options should be passed through
    compiler.isSupportedOption("-source");
    verify(mockCompiler).isSupportedOption("-source");

    // error-prone options should be handled
    assertThat(compiler.isSupportedOption("-Xepdisable:"), is(0));
  }

  interface JavaFileObjectDiagnosticListener extends DiagnosticListener<JavaFileObject> {}

  @Test
  public void testGetStandardJavaFileManager() {
    JavaCompiler mockCompiler = mock(JavaCompiler.class);
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler(mockCompiler);

    JavaFileObjectDiagnosticListener listener = mock(JavaFileObjectDiagnosticListener.class);
    Locale locale = Locale.CANADA;

    compiler.getStandardFileManager(listener, locale, null);
    verify(mockCompiler).getStandardFileManager(listener, locale, null);
  }

  @Test
  public void testRun() {
    JavaCompiler mockCompiler = mock(JavaCompiler.class);
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler(mockCompiler);

    InputStream in = mock(InputStream.class);
    OutputStream out = mock(OutputStream.class);
    OutputStream err = mock(OutputStream.class);
    String[] arguments = {"-source", "8", "-target", "8"};

    compiler.run(in, out, err, arguments);
    verify(mockCompiler).run(in, out, err, arguments);
  }

  @Test
  public void testSourceVersion() {
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler();
    assertTrue(compiler.getSourceVersions().contains(SourceVersion.latest()));
    assertFalse(compiler.getSourceVersions().contains(SourceVersion.RELEASE_5));
  }

  @Test
  public void fileWithErrorIntegrationTest() throws Exception {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream), true);

    ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

    JavaCompiler.CompilationTask task = new ErrorProneJavaCompiler().getTask(
        printWriter, fileManager, diagnosticHelper.collector, null, null,
        fileManager.sources(DepAnnTest.class, "DepAnnPositiveCases.java"));

    boolean succeeded = task.call();
    assertFalse(outputStream.toString(), succeeded);
    Matcher<Iterable<Diagnostic<JavaFileObject>>> matcher = Matchers.hasItem(
        diagnosticMessage(containsString("[DepAnn]")));
    assertTrue("Error should be found. " + diagnosticHelper.describe(),
        matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void testWithDisabledCheck() throws Exception {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream), true);

    ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

    JavaCompiler.CompilationTask task = new ErrorProneJavaCompiler().getTask(
        printWriter,
        fileManager,
        diagnosticHelper.collector,
        Arrays.asList("-Xepdisable:DepAnn", "-d", tempDir.getRoot().getAbsolutePath()),
        null,
        fileManager.sources(DepAnnTest.class, "DepAnnPositiveCases.java"));

    boolean succeeded = task.call();
    assertTrue(outputStream.toString(), succeeded);
  }

  @Test
  public void testWithCustomCheckPositive() throws Exception {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream), true);

    ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

    JavaCompiler compiler =
        new ErrorProneJavaCompiler(ScannerSupplier.fromBugCheckerClasses(BadShiftAmount.class));
    JavaCompiler.CompilationTask task =  compiler.getTask(
        printWriter,
        null,
        diagnosticHelper.collector,
        Arrays.asList("-d", tempDir.getRoot().getAbsolutePath()),
        null,
        fileManager.sources(BadShiftAmount.class, "BadShiftAmountPositiveCases.java"));

    boolean succeeded = task.call();
    assertFalse(outputStream.toString(), succeeded);
  }

  @Test
  public void testWithCustomCheckNegative() throws Exception {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream), true);

    ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

    JavaCompiler compiler =
        new ErrorProneJavaCompiler(ScannerSupplier.fromBugCheckerClasses(BadShiftAmount.class));
    JavaCompiler.CompilationTask task =  compiler.getTask(
        printWriter,
        null,
        diagnosticHelper.collector,
        Arrays.asList("-d", tempDir.getRoot().getAbsolutePath()),
        null,
        fileManager.sources(DepAnnTest.class, "DepAnnPositiveCases.java"));

    boolean succeeded = task.call();
    assertTrue(outputStream.toString(), succeeded);
  }
}

