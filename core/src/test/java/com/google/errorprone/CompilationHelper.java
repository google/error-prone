// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone;

import static com.google.errorprone.DiagnosticTestHelper.hasDiagnosticOnAllMatchingLines;
import static java.util.regex.Pattern.compile;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

/**
 * Utility class for tests that need to build using error-prone.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class CompilationHelper {

  private DiagnosticTestHelper diagnosticHelper;
  private ErrorProneCompiler compiler;

  public CompilationHelper(DiagnosticTestHelper diagnosticHelper, ErrorProneCompiler compiler) {
    this.diagnosticHelper = diagnosticHelper;
    this.compiler = compiler;
  }

  public void assertCompileSucceeds(File source) {
    assertThat(compiler.compile(new String[]{"-Xjcov", source.getAbsolutePath()}), is(0));
  }

  public void assertCompileFails(File source, String... expectedMessage) throws IOException {
    assertThat(compiler.compile(new String[]{"-Xjcov", source.getAbsolutePath()}), is(1));
    assertThat(diagnosticHelper.getDiagnostics(),
        hasDiagnosticOnAllMatchingLines(source, compile(".*//BUG\\s*$"), expectedMessage));
  }

}
