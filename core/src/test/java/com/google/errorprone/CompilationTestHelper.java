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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.testing.compile.JavaFileObjects;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * Utility class for tests that need to build using error-prone.
 * @author eaftan@google.com (Eddie Aftandilian)
 *
 * TODO(eaftan): Refactor default argument construction to make setup cleaner.
 */
public class CompilationTestHelper {

  private static final List<String> DEFAULT_ARGS = Arrays.asList(
    "-encoding", "UTF-8",
    "-Xjcov");

  private static final Joiner LINE_JOINER = Joiner.on('\n');

  public static List<JavaFileObject> sources(Class<?> clazz, String... fileNames) {
    List<JavaFileObject> result = new ArrayList<JavaFileObject>();
    for (String fileName : fileNames) {
      result.add(source(clazz, fileName));
    }
    return result;
  }

  public static JavaFileObject source(Class<?> clazz, String fileName) {
    return JavaFileObjects.forResource(clazz.getResource(fileName));
  }

  public static JavaFileObject forSourceLines(String fileName, String... lines) {
    return JavaFileObjects.forSourceString(fileName, LINE_JOINER.join(lines));
  }

  private DiagnosticTestHelper diagnosticHelper;
  private ErrorProneCompiler compiler;
  private ByteArrayOutputStream outputStream;
  private JavaFileManager fileManager;

  private CompilationTestHelper(Scanner scanner, String checkName) {
    this.diagnosticHelper = new DiagnosticTestHelper(checkName);
    this.fileManager = getFileManager(diagnosticHelper.collector, null, null);
    this.outputStream = new ByteArrayOutputStream();
    this.compiler = new ErrorProneCompiler.Builder().report(scanner)
        .redirectOutputTo(new PrintWriter(outputStream, /*autoFlush=*/true))
        .listenToDiagnostics(diagnosticHelper.collector).build();
  }

  public static CompilationTestHelper newInstance(Scanner scanner) {
    return new CompilationTestHelper(scanner, null);
  }

  public static CompilationTestHelper newInstance(Scanner scanner, String checkName) {
    return new CompilationTestHelper(scanner, checkName);
  }

  /**
   * Test an error-prone {@link BugChecker} that should be part of the default {@link
   * ErrorProneScanner}. Confirms that the check is included in the list of all checkers in
   * {@link ErrorProneScanner}.
   */
  public static CompilationTestHelper newInstance(Class<? extends BugChecker> matcherClass) {
    Scanner scanner = ErrorProneScanner.forMatcher(matcherClass);
    String checkName = matcherClass.getAnnotation(BugPattern.class).name();
    return new CompilationTestHelper(scanner, checkName);
  }

  /**
   * Test a custom error-prone {@link BugChecker} that should not be included in the default
   * {@link ErrorProneScanner}.
   */
  public static CompilationTestHelper newInstance(BugChecker checker) {
    Scanner scanner = new ErrorProneScanner(checker);
    String checkName = checker.getCanonicalName();
    return new CompilationTestHelper(scanner, checkName);
  }

  /**
   * Creates a list of arguments to pass to the compiler, including the list of source files
   * to compile.  Uses DEFAULT_ARGS as the base and appends the extraArgs passed in.
   */
  private static List<String> buildArguments(List<String> extraArgs) {
    return ImmutableList.<String>builder().addAll(DEFAULT_ARGS).addAll(extraArgs).build();
  }

  /**
   * TODO(cushon): Investigate using compile_testing in a more legitimate fashion.
   */
  public static JavaFileManager getFileManager(DiagnosticListener<? super JavaFileObject>
                                                   diagnosticListener,
                                               Locale locale,
                                               Charset charset) {
    try {
      JavacFileManager wrappedFileManager = (JavacFileManager) ToolProvider.getSystemJavaCompiler()
          .getStandardFileManager
              (diagnosticListener, locale, charset);
      Class<?> clazz = Class.forName("com.google.testing.compile.InMemoryJavaFileManager");
      Constructor<?> ctor = clazz.getDeclaredConstructor(JavaFileManager.class);
      ctor.setAccessible(true);
      return (JavaFileManager) ctor.newInstance(wrappedFileManager);
    } catch (Exception e) {
      throw new LinkageError(e.getMessage());
    }
  }

  /**
   * Asserts that the compilation succeeds (exits with code 0).
   *
   * @param sources The list of {@code File}s to compile
   * @param args Extra command-line arguments to pass to the compiler
   */
  public void assertCompileSucceeds(List<JavaFileObject> sources, List<String> args) {
    List<String> allArgs = buildArguments(args);
    int exitCode = compile(asJavacList(sources), allArgs.toArray(new String[0]));
    assertThat(diagnosticHelper.getDiagnostics().toString(), exitCode, is(0));
    // TODO(eaftan): complain if there are any diagnostics
  }

  public void assertCompileSucceeds(List<JavaFileObject> sources) {
    assertCompileSucceeds(sources, ImmutableList.<String>of());
  }

  /**
   * Convenience method for the common case of one source file and no extra args.
   */
  public void assertCompileSucceeds(JavaFileObject source) {
    assertCompileSucceeds(ImmutableList.of(source));
  }

  /**
   * Assert that the compile succeeds, and that for each line of the test file that contains
   * the pattern //BUG("foo"), the diagnostic at that line contains "foo".
   */
  public void assertCompileSucceedsWithMessages(List<JavaFileObject> sources) throws IOException {
    assertCompileSucceeds(sources);
    for (JavaFileObject source : sources) {
      diagnosticHelper.assertHasDiagnosticOnAllMatchingLines(source);
    }
  }

  /**
   * Convenience method for the common case of one source file and no extra args.
   */
  public void assertCompileSucceedsWithMessages(JavaFileObject source) throws IOException {
    assertCompileSucceedsWithMessages(ImmutableList.of(source));
  }

  /**
   * Assert that the compile fails, and that for each line of the test file that contains
   * the pattern //BUG("foo"), the diagnostic at that line contains "foo".
   *
   * @param sources The list of {@code File}s to compile
   */
  public void assertCompileFailsWithMessages(List<JavaFileObject> sources) throws IOException {
    assertCompileFailsWithMessages(sources, Collections.<String>emptyList());
  }

  /**
   * Assert that the compile fails, and that for each line of the test file that contains
   * the pattern //BUG("foo"), the diagnostic at that line contains "foo".
   * @param sources The list of {@code File}s to compile
   * @param args The list of args to pass to the compilation
   */
  public void assertCompileFailsWithMessages(List<JavaFileObject> sources, List<String> args)
      throws IOException {
    List<String> allArgs = buildArguments(args);
    int exitCode = compile(asJavacList(sources), allArgs.toArray(new String[0]));
    assertThat("Compiler returned an unexpected error code", exitCode, is(1));
    for (JavaFileObject source : sources) {
      diagnosticHelper.assertHasDiagnosticOnAllMatchingLines(source);
    }
  }

  /**
   * Convenience method for the common case of one source file and no extra args.
   */
  public void assertCompileFailsWithMessages(JavaFileObject source) throws IOException {
    assertCompileFailsWithMessages(ImmutableList.of(source));
  }

  int compile(Iterable<JavaFileObject> sources, String[] args) {
    checkWellFormed(sources, args);
    Context context = new Context();
    context.put(JavaFileManager.class, fileManager);
    return compiler.compile(args, context, asJavacList(sources), null);
  }

  private void checkWellFormed(Iterable<JavaFileObject> sources, String[] args) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    OutputStream outputStream = new ByteArrayOutputStream();
    CompilationTask task = compiler.getTask(
        new PrintWriter(outputStream, /*autoFlush=*/true),
        fileManager,
        null,
        Arrays.asList(ErrorProneOptions.processArgs(args).getRemainingArgs()),
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
