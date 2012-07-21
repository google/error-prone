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

  public CompilationHelper(Scanner scanner) {
    diagnosticHelper = new DiagnosticTestHelper();
    compiler = new ErrorProneCompiler.Builder()
        .report(scanner)
        .listenToDiagnostics(diagnosticHelper.collector)
        .build();
  }

  public void assertCompileSucceeds(File source) {
    assertThat(compiler.compile(new String[]{"-Xjcov", source.getAbsolutePath()}), is(0));
  }

  /**
   * Assert that the compilation fails, and for each line that contains the //BUG pattern, the
   * expectedMessages should match (in order).  The number of expectedMessages should match the
   * number of lines in the file that have been tagged with //BUG.
   */
  public void assertCompileFailsDiffMessages(File source, String... expectedMessages) throws IOException {
    assertThat(compiler.compile(new String[]{"-Xjcov", source.getAbsolutePath()}), is(1));
    assertThat(diagnosticHelper.getDiagnostics(),
        hasDiagnosticOnAllMatchingLines(source, compile(".*//BUG\\s*$"), expectedMessages));
  }

  /**
   * Assert that the compilation fails, and every line that contains the //BUG pattern produces
   * an error message that matches the one given.
   */
  public void assertCompileFailsSameMessage(File source, String expectedMessage) throws IOException {
    assertThat(compiler.compile(new String[]{"-Xjcov", source.getAbsolutePath()}), is(1));
    assertThat(diagnosticHelper.getDiagnostics(),
        hasDiagnosticOnAllMatchingLines(source, compile(".*//BUG\\s*$"), expectedMessage));
  }

}
