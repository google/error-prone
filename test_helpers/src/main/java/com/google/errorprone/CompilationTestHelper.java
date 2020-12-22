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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.errorprone.FileObjects.forResource;
import static com.google.errorprone.FileObjects.forSourceLines;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static org.junit.Assert.fail;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.errorprone.DiagnosticTestHelper.LookForCheckNameInDiagnostic;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.ScannerSupplier;
import com.google.errorprone.util.RuntimeVersion;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.main.Main.Result;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.annotation.Nullable;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/** Helps test Error Prone bug checkers and compilations. */
@CheckReturnValue
public class CompilationTestHelper {
  private static final ImmutableList<String> DEFAULT_ARGS =
      ImmutableList.of(
          "-encoding",
          "UTF-8",
          // print stack traces for completion failures
          "-XDdev",
          "-parameters",
          "-XDcompilePolicy=simple",
          // Don't limit errors/warnings for tests to the default of 100
          "-Xmaxerrs",
          "500",
          "-Xmaxwarns",
          "500");

  private final DiagnosticTestHelper diagnosticHelper;
  private final BaseErrorProneJavaCompiler compiler;
  private final ByteArrayOutputStream outputStream;
  private final Class<?> clazz;
  private final List<JavaFileObject> sources = new ArrayList<>();
  private ImmutableList<String> extraArgs = ImmutableList.of();
  @Nullable private ImmutableList<Class<?>> overrideClasspath;
  private boolean expectNoDiagnostics = false;
  private Optional<Result> expectedResult = Optional.empty();
  private LookForCheckNameInDiagnostic lookForCheckNameInDiagnostic =
      LookForCheckNameInDiagnostic.YES;

  private boolean run = false;

  private CompilationTestHelper(ScannerSupplier scannerSupplier, String checkName, Class<?> clazz) {
    this.clazz = clazz;
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
    if (args.contains("-processor") || args.contains("-processorpath")) {
      return args;
    }
    return ImmutableList.<String>builder().addAll(args).add("-proc:none").build();
  }

  /**
   * Creates a list of arguments to pass to the compiler. Uses DEFAULT_ARGS as the base and appends
   * the overridden classpath, if provided, and any extraArgs that were provided.
   */
  private static List<String> buildArguments(
      @Nullable List<Class<?>> overrideClasspath, List<String> extraArgs) {
    ImmutableList.Builder<String> result = ImmutableList.<String>builder().addAll(DEFAULT_ARGS);
    getOverrideClasspath(overrideClasspath)
        .ifPresent((Path jar) -> result.add("-cp").add(jar.toString()));
    return result.addAll(disableImplicitProcessing(extraArgs)).build();
  }

  private static Optional<Path> getOverrideClasspath(@Nullable List<Class<?>> overrideClasspath) {
    if (overrideClasspath == null) {
      return Optional.empty();
    }
    try {
      Path tempJarFile = Files.createTempFile(/* prefix = */ null, /* suffix = */ ".jar");
      try (OutputStream os = Files.newOutputStream(tempJarFile);
          JarOutputStream jos = new JarOutputStream(os)) {
        for (Class<?> clazz : overrideClasspath) {
          String entryPath = clazz.getName().replace('.', '/') + ".class";
          jos.putNextEntry(new JarEntry(entryPath));
          try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
            ByteStreams.copy(is, jos);
          }
        }
      }
      return Optional.of(tempJarFile);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Adds a source file to the test compilation, from the string content of the file.
   *
   * <p>The diagnostics expected from compiling the file are inferred from the file contents. For
   * each line of the test file that contains the bug marker pattern "// BUG: Diagnostic contains:
   * foo", we expect to see a diagnostic on that line containing "foo". For each line of the test
   * file that does <i>not</i> contain the bug marker pattern, we expect no diagnostic to be
   * generated. You can also use "// BUG: Diagnostic matches: X" in tandem with {@code
   * expectErrorMessage("X", "foo")} to allow you to programmatically construct the error message.
   *
   * @param path a path for the source file
   * @param lines the content of the source file
   */
  // TODO(eaftan): We could eliminate this path parameter and just infer the path from the
  // package and class name
  public CompilationTestHelper addSourceLines(String path, String... lines) {
    this.sources.add(forSourceLines(path, lines));
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
    this.sources.add(forResource(clazz, path));
    return this;
  }

  /**
   * Sets the classpath for the test compilation, overriding the default of using the runtime
   * classpath of the test execution. This is useful to verify correct behavior when the classpath
   * is incomplete.
   *
   * @param classes the class(es) to use as the classpath
   */
  public CompilationTestHelper withClasspath(Class<?>... classes) {
    this.overrideClasspath = ImmutableList.copyOf(classes);
    return this;
  }

  public CompilationTestHelper addModules(String... modules) {
    if (!RuntimeVersion.isAtLeast9()) {
      return this;
    }
    return setArgs(
        stream(modules)
            .map(m -> String.format("--add-exports=%s=ALL-UNNAMED", m))
            .collect(toImmutableList()));
  }

  /**
   * Sets custom command-line arguments for the compilation. These will be appended to the default
   * compilation arguments.
   */
  public CompilationTestHelper setArgs(List<String> args) {
    checkState(
        extraArgs.isEmpty(),
        "Extra args already set: old value: %s, new value: %s",
        extraArgs,
        args);
    this.extraArgs = ImmutableList.copyOf(args);
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
  public void doTest() {
    checkState(!sources.isEmpty(), "No source files to compile");
    checkState(!run, "doTest should only be called once");
    this.run = true;
    Result result = compile();
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
                      + expectedResult.orElse(Result.OK)
                      + ", but was %s. No diagnostics were emitted."
                      + " OutputStream from Compiler follows.\n\n%s",
                  result,
                  outputStream))
          .that(result)
          .isEqualTo(expectedResult.orElse(Result.OK));
    } else {
      for (JavaFileObject source : sources) {
        try {
          diagnosticHelper.assertHasDiagnosticOnAllMatchingLines(
              source, lookForCheckNameInDiagnostic);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      assertWithMessage("Unused error keys: " + diagnosticHelper.getUnusedLookupKeys())
          .that(diagnosticHelper.getUnusedLookupKeys().isEmpty())
          .isTrue();
    }

    expectedResult.ifPresent(
        expected ->
            assertWithMessage(
                    String.format(
                        "Expected compilation result %s, but was %s\n%s\n%s",
                        expected,
                        result,
                        Joiner.on('\n').join(diagnosticHelper.getDiagnostics()),
                        outputStream))
                .that(result)
                .isEqualTo(expected));
  }

  private Result compile() {
    List<String> processedArgs = buildArguments(overrideClasspath, extraArgs);
    return compiler
            .getTask(
                new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(outputStream, UTF_8)),
                    /*autoFlush=*/ true),
                FileManagers.testFileManager(),
                diagnosticHelper.collector,
                /* options= */ ImmutableList.copyOf(processedArgs),
                /* classes= */ ImmutableList.of(),
                sources)
            .call()
        ? Result.OK
        : Result.ERROR;
  }
}
