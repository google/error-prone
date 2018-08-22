/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.DiagnosticTestHelper.LookForCheckNameInDiagnostic;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.main.Main.Result;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/** Helps test Error Prone bug checkers and compilations. */
public class CompilationTestHelper {
  private static final ImmutableList<String> DEFAULT_ARGS =
      ImmutableList.of(
          "-encoding",
          "UTF-8",
          // print stack traces for completion failures
          "-XDdev",
          "-parameters",
          "-XDcompilePolicy=simple");

  private final DiagnosticTestHelper diagnosticHelper;
  private final BaseErrorProneJavaCompiler compiler;
  private final ByteArrayOutputStream outputStream;
  private final ErrorProneInMemoryFileManager fileManager;
  private final List<JavaFileObject> sources = new ArrayList<>();
  private List<String> args = ImmutableList.of();
  private boolean expectNoDiagnostics = false;
  private Optional<Result> expectedResult = Optional.absent();
  private boolean checkWellFormed = true;
  private LookForCheckNameInDiagnostic lookForCheckNameInDiagnostic =
      LookForCheckNameInDiagnostic.YES;

  private CompilationTestHelper(ScannerSupplier scannerSupplier, String checkName, Class<?> clazz) {
    this.fileManager = new ErrorProneInMemoryFileManager(clazz);
    try {
      fileManager.setLocation(StandardLocation.SOURCE_PATH, Collections.emptyList());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    this.diagnosticHelper = new DiagnosticTestHelper(checkName);
    this.outputStream = new ByteArrayOutputStream();
    this.compiler = new BaseErrorProneJavaCompiler(JavacTool.create(), scannerSupplier);
  }

  /**
   * Returns a new {@link CompilationTestHelper}.
   *
   * @param scannerSupplier the {@link ScannerSupplier} to test
   * @param clazz the class to use to locate file resources
   */
  public static CompilationTestHelper newInstance(ScannerSupplier scannerSupplier, Class<?> clazz) {
    return new CompilationTestHelper(scannerSupplier, null, clazz);
  }

  /**
   * Returns a new {@link CompilationTestHelper}.
   *
   * @param checker the {@link BugChecker} to test
   * @param clazz the class to use to locate file resources
   */
  public static CompilationTestHelper newInstance(
      Class<? extends BugChecker> checker, Class<?> clazz) {
    ScannerSupplier scannerSupplier = ScannerSupplier.fromBugCheckerClasses(checker);
    String checkName = checker.getAnnotation(BugPattern.class).name();
    return new CompilationTestHelper(scannerSupplier, checkName, clazz);
  }

  /**
   * Pass -proc:none unless annotation processing is explicitly enabled, to avoid picking up
   * annotation processors via service loading.
   */
  // TODO(cushon): test compilations should be isolated so they can't pick things up from the
  // ambient classpath.
  static List<String> disableImplicitProcessing(List<String> args) {
    if (args.indexOf("-processor") != -1 || args.indexOf("-processorpath") != -1) {
      return args;
    }
    return ImmutableList.<String>builder().addAll(args).add("-proc:none").build();
  }

  /**
   * Creates a list of arguments to pass to the compiler, including the list of source files to
   * compile. Uses DEFAULT_ARGS as the base and appends the extraArgs passed in.
   */
  private static List<String> buildArguments(List<String> extraArgs) {
    return ImmutableList.<String>builder()
        .addAll(DEFAULT_ARGS)
        .addAll(disableImplicitProcessing(extraArgs))
        .build();
  }

  /**
   * Adds a source file to the test compilation, from the string content of the file.
   *
   * <p>The diagnostics expected from compiling the file are inferred from the file contents. For
   * each line of the test file that contains the bug marker pattern "// BUG: Diagnostic contains:
   * foo", we expect to see a diagnostic on that line containing "foo". For each line of the test
   * file that does <i>not</i> contain the bug marker pattern, we expect no diagnostic to be
   * generated. You can also use "// BUG: Diagnostic matches: X" in tandem with {@code
   * expectErrorMessage("X", "foo")} to allow you to programatically construct the error message.
   *
   * @param path a path for the source file
   * @param lines the content of the source file
   */
  // TODO(eaftan): We could eliminate this path parameter and just infer the path from the
  // package and class name
  public CompilationTestHelper addSourceLines(String path, String... lines) {
    this.sources.add(fileManager.forSourceLines(path, lines));
    return this;
  }

  /**
   * Adds a source file to the test compilation, from an existing resource file.
   *
   * <p>See {@link #addSourceLines} for how expected diagnostics should be specified.
   *
   * @param path the path to the source file
   */
  public CompilationTestHelper addSourceFile(String path) {
    this.sources.add(fileManager.forResource(path));
    return this;
  }

  /**
   * Sets custom command-line arguments for the compilation. These will be appended to the default
   * compilation arguments.
   */
  public CompilationTestHelper setArgs(List<String> args) {
    this.args = args;
    return this;
  }

  /**
   * Tells the compilation helper to expect that no diagnostics will be generated, even if the
   * source file contains bug markers. Useful for testing that a check is actually disabled when the
   * proper command-line argument is passed.
   */
  public CompilationTestHelper expectNoDiagnostics() {
    this.expectNoDiagnostics = true;
    return this;
  }

  /**
   * By default, the compilation helper will not run Error Prone on compilations that fail with
   * javac errors. This behaviour can be disabled to test the interaction between Error Prone checks
   * and javac diagnostics.
   */
  public CompilationTestHelper ignoreJavacErrors() {
    this.checkWellFormed = false;
    return this;
  }

  /**
   * By default, the compilation helper will only inspect diagnostics generated by the check being
   * tested. This behaviour can be disabled to test the interaction between Error Prone checks and
   * javac diagnostics.
   */
  public CompilationTestHelper matchAllDiagnostics() {
    this.lookForCheckNameInDiagnostic = LookForCheckNameInDiagnostic.NO;
    return this;
  }

  /**
   * Tells the compilation helper to expect a specific result from the compilation, e.g. success or
   * failure.
   */
  public CompilationTestHelper expectResult(Result result) {
    expectedResult = Optional.of(result);
    return this;
  }

  /**
   * Expects an error message matching {@code matcher} at the line below a comment matching the key.
   * For example, given the source
   *
   * <pre>
   *   // BUG: Diagnostic matches: X
   *   a = b + c;
   * </pre>
   *
   * ... you can use {@code expectErrorMessage("X", Predicates.containsPattern("Can't add b to
   * c"));}
   *
   * <p>Error message keys that don't match any diagnostics will cause test to fail.
   */
  public CompilationTestHelper expectErrorMessage(String key, Predicate<? super String> matcher) {
    diagnosticHelper.expectErrorMessage(key, matcher);
    return this;
  }

  /** Performs a compilation and checks that the diagnostics and result match the expectations. */
  // TODO(eaftan): any way to ensure that this is actually called?
  public void doTest() {
    Preconditions.checkState(!sources.isEmpty(), "No source files to compile");
    List<String> allArgs = buildArguments(args);
    Result result = compile(sources, allArgs.toArray(new String[allArgs.size()]));
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticHelper.getDiagnostics()) {
      if (diagnostic.getCode().contains("error.prone.crash")) {
        fail(diagnostic.getMessage(Locale.ENGLISH));
      }
    }
    if (expectNoDiagnostics) {
      List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticHelper.getDiagnostics();
      assertWithMessage(
              String.format(
                  "Expected no diagnostics produced, but found %d: %s",
                  diagnostics.size(), diagnostics))
          .that(diagnostics.size())
          .isEqualTo(0);
      assertWithMessage(
              String.format(
                  "Expected compilation result to be "
                      + expectedResult.or(Result.OK)
                      + ", but was %s. No diagnostics were emitted."
                      + " OutputStream from Compiler follows.\n\n%s",
                  result,
                  outputStream.toString()))
          .that(result)
          .isEqualTo(expectedResult.or(Result.OK));
    } else {
      for (JavaFileObject source : sources) {
        try {
          diagnosticHelper.assertHasDiagnosticOnAllMatchingLines(
              source, lookForCheckNameInDiagnostic);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      assertTrue(
          "Unused error keys: " + diagnosticHelper.getUnusedLookupKeys(),
          diagnosticHelper.getUnusedLookupKeys().isEmpty());
    }

    if (expectedResult.isPresent()) {
      assertWithMessage(
              String.format(
                  "Expected compilation result %s, but was %s\n%s\n%s",
                  expectedResult.get(),
                  result,
                  Joiner.on('\n').join(diagnosticHelper.getDiagnostics()),
                  outputStream.toString()))
          .that(result)
          .isEqualTo(expectedResult.get());
    }
  }

  private Result compile(Iterable<JavaFileObject> sources, String[] args) {
    if (checkWellFormed) {
      checkWellFormed(sources, args);
    }
    createAndInstallTempFolderForOutput(fileManager);
    return compiler
            .getTask(
                new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(outputStream, UTF_8)),
                    /*autoFlush=*/ true),
                fileManager,
                diagnosticHelper.collector,
                /* options= */ ImmutableList.copyOf(args),
                /* classes= */ ImmutableList.of(),
                sources)
            .call()
        ? Result.OK
        : Result.ERROR;
  }

  private static void createAndInstallTempFolderForOutput(
      ErrorProneInMemoryFileManager fileManager) {
    Path tempDirectory;
    try {
      tempDirectory =
          Files.createTempDirectory(
              fileManager.fileSystem().getRootDirectories().iterator().next(), "");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    Arrays.stream(StandardLocation.values())
        .filter(StandardLocation::isOutputLocation)
        .forEach(
            outputLocation -> {
              try {
                fileManager.setLocationFromPaths(outputLocation, ImmutableList.of(tempDirectory));
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
  }

  private void checkWellFormed(Iterable<JavaFileObject> sources, String[] args) {
    createAndInstallTempFolderForOutput(fileManager);
    JavaCompiler compiler = JavacTool.create();
    OutputStream outputStream = new ByteArrayOutputStream();
    String[] remainingArgs = null;
    try {
      remainingArgs = ErrorProneOptions.processArgs(args).getRemainingArgs();
    } catch (InvalidCommandLineOptionException e) {
      fail("Exception during argument processing: " + e);
    }
    CompilationTask task =
        compiler.getTask(
            new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(outputStream, UTF_8)),
                /*autoFlush=*/ true),
            fileManager,
            null,
            buildArguments(Arrays.asList(remainingArgs)),
            null,
            sources);
    boolean result = task.call();
    assertWithMessage(
            String.format(
                "Test program failed to compile with non Error Prone error: %s",
                outputStream.toString()))
        .that(result)
        .isTrue();
  }
}
