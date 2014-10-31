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

package com.google.errorprone;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.ScannerSupplier;

import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.util.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;

/**
 * Utility class for tests that need to build using error-prone.
 * @author eaftan@google.com (Eddie Aftandilian)
 *
 * TODO(user): Refactor default argument construction to make setup cleaner.
 */
public class CompilationTestHelper {

  private static final List<String> DEFAULT_ARGS = ImmutableList.of(
      "-encoding", "UTF-8", "-Xjcov");

  private final DiagnosticTestHelper diagnosticHelper;
  private final ErrorProneCompiler compiler;
  private final ByteArrayOutputStream outputStream;
  private final ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

  private CompilationTestHelper(ScannerSupplier scannerSupplier, String checkName) {
    this.diagnosticHelper = new DiagnosticTestHelper(checkName);
    this.outputStream = new ByteArrayOutputStream();
    this.compiler = new ErrorProneCompiler.Builder()
        .report(scannerSupplier)
        .redirectOutputTo(new PrintWriter(outputStream, /*autoFlush=*/true))
        .listenToDiagnostics(diagnosticHelper.collector)
        .build();
  }

  public static CompilationTestHelper newInstance(ScannerSupplier scannerSupplier) {
    return new CompilationTestHelper(scannerSupplier, null);
  }

  public static CompilationTestHelper newInstance(ScannerSupplier scannerSupplier,
      String checkName) {
    return new CompilationTestHelper(scannerSupplier, checkName);
  }

  /**
   * Test an error-prone {@link BugChecker}.
   */
  public static CompilationTestHelper newInstance(BugChecker checker) {
    ScannerSupplier scannerSupplier = ScannerSupplier.fromBugCheckers(checker);
    String checkName = checker.canonicalName();
    return new CompilationTestHelper(scannerSupplier, checkName);
  }

  /**
   * Pass -proc:none unless annotation processing is explicitly enabled, to avoid picking up
   * annotation processors via service loading.
   */
  // TODO(user): test compilations should be isolated so they can't pick things up from the
  // ambient classpath.
  static List<String> disableImplicitProcessing(List<String> args) {
    if (args.indexOf("-processor") != -1
        || args.indexOf("-processorpath") != -1) {
      return args;
    }
    return ImmutableList.<String>builder().addAll(args).add("-proc:none").build();
  }

  /**
   * Creates a list of arguments to pass to the compiler, including the list of source files
   * to compile.  Uses DEFAULT_ARGS as the base and appends the extraArgs passed in.
   */
  private static List<String> buildArguments(List<String> extraArgs) {
    return ImmutableList.<String>builder()
        .addAll(DEFAULT_ARGS)
        .addAll(disableImplicitProcessing(extraArgs))
        .build();
  }

  /**
   * Returns the file manager associated with the test compilation.
   */
  public ErrorProneInMemoryFileManager fileManager() {
    return fileManager;
  }

  /**
   * Asserts that the compilation succeeds and no diagnostics were produced.
   *
   * @param sources The list of files to compile
   * @param args Extra command-line arguments to pass to the compiler
   */
  public void assertCompileSucceeds(List<JavaFileObject> sources, List<String> args) {
    List<String> allArgs = buildArguments(args);
    Result exitCode = compile(asJavacList(sources), allArgs.toArray(new String[0]));
    List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticHelper.getDiagnostics();
    assertThat("Compilation failed: " + diagnostics.toString(), exitCode, is(Result.OK));
    assertThat("Compilation succeeded but gave warnings: " + diagnostics.toString(),
        diagnostics.size(), is(0));
  }

  /**
   * Asserts that the compilation succeeds and no diagnostics were produced.
   *
   * @param sources The list of files to compile
   */
  public void assertCompileSucceeds(List<JavaFileObject> sources) {
    assertCompileSucceeds(sources, ImmutableList.<String>of());
  }

  /**
   * Asserts that the compilation succeeds and no diagnostics were produced. Convenience method
   * for the common case of one source file and no extra args.
   */
  public void assertCompileSucceeds(JavaFileObject source) {
    assertCompileSucceeds(ImmutableList.of(source));
  }

  /**
   * Assert that the compile succeeds, and that for each line of the test file that contains
   * the pattern "// BUG: Diagnostic contains: foo", the diagnostic at that line contains "foo".
   */
  public void assertCompileSucceedsWithMessages(List<JavaFileObject> sources) throws IOException {
    assertCompileSucceedsIgnoringWarnings(sources);
    for (JavaFileObject source : sources) {
      diagnosticHelper.assertHasDiagnosticOnAllMatchingLines(source);
    }
  }

  /**
   * Asserts that the compile succeeds, and that for each line of the test file that contains
   * the pattern "// BUG: Diagnostic contains: foo", the diagnostic at that line contains "foo".
   * Convenience method for the common case of one source file and no extra args.
   */
  public void assertCompileSucceedsWithMessages(JavaFileObject source) throws IOException {
    assertCompileSucceedsWithMessages(ImmutableList.of(source));
  }

  /**
   * Asserts that the compilation succeeds regardless of whether any diagnostics were produced.
   */
  private void assertCompileSucceedsIgnoringWarnings(List<JavaFileObject> sources) {
    List<String> allArgs = buildArguments(Collections.<String>emptyList());
    Result exitCode = compile(asJavacList(sources), allArgs.toArray(new String[0]));
    assertThat(diagnosticHelper.getDiagnostics().toString(), exitCode, is(Result.OK));
  }

  /**
   * Asserts that the compile fails, and that for each line of the test file that contains
   * the pattern "// BUG: Diagnostic contains: foo", the diagnostic at that line contains "foo".
   *
   * @param sources The list of files to compile
   */
  public void assertCompileFailsWithMessages(List<JavaFileObject> sources) throws IOException {
    assertCompileFailsWithMessages(sources, Collections.<String>emptyList());
  }

  /**
   * Asserts that the compile fails, and that for each line of the test file that contains
   * the pattern "// BUG: Diagnostic contains: foo", the diagnostic at that line contains "foo".
   *
   * @param sources The list of files to compile
   * @param args The list of args to pass to the compilation
   */
  public void assertCompileFailsWithMessages(List<JavaFileObject> sources, List<String> args)
      throws IOException {
    List<String> allArgs = buildArguments(args);
    Result exitCode = compile(asJavacList(sources), allArgs.toArray(new String[0]));
    assertThat("Compiler returned an unexpected error code", exitCode, is(Result.ERROR));
    for (JavaFileObject source : sources) {
      diagnosticHelper.assertHasDiagnosticOnAllMatchingLines(source);
    }
  }

  /**
   * Asserts that the compile fails, and that for each line of the test file that contains
   * the pattern "// BUG: Diagnostic contains: foo", the diagnostic at that line contains "foo".
   * Convenience method for the common case of one source file and no extra args.
   */
  public void assertCompileFailsWithMessages(JavaFileObject source) throws IOException {
    assertCompileFailsWithMessages(ImmutableList.of(source));
  }

  Result compile(Iterable<JavaFileObject> sources, String[] args) {
    checkWellFormed(sources, args);
    Context context = new Context();
    return compiler.compile(args, context, fileManager, asJavacList(sources), null);
  }

  private void checkWellFormed(Iterable<JavaFileObject> sources, String[] args) {
    JavaCompiler compiler = JavacTool.create();
    OutputStream outputStream = new ByteArrayOutputStream();
    CompilationTask task = compiler.getTask(
        new PrintWriter(outputStream, /*autoFlush=*/true),
        fileManager,
        null,
        buildArguments(Arrays.asList(ErrorProneOptions.processArgs(args).getRemainingArgs())),
        null,
        sources);
    boolean result = task.call();
    assertTrue(String.format("Test program failed to compile with non error-prone error: %s",
        outputStream.toString()), result);
  }

  public static <T> com.sun.tools.javac.util.List<T> asJavacList(Iterable<? extends T> xs) {
    com.sun.tools.javac.util.List<T> result = com.sun.tools.javac.util.List.nil();
    for (T x : xs) {
      result = result.append(x);
    }
    return result;
  }

}
