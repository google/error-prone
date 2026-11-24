/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.comp.CompileStates.CompileState;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.Options;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import org.jspecify.annotations.Nullable;

/** An Error Prone compiler that implements {@link javax.tools.JavaCompiler}. */
public class BaseErrorProneJavaCompiler implements JavaCompiler {

  private final JavaCompiler javacTool;
  private final ScannerSupplier scannerSupplier;

  public BaseErrorProneJavaCompiler(ScannerSupplier scannerSupplier) {
    this(JavacTool.create(), scannerSupplier);
  }

  BaseErrorProneJavaCompiler(JavaCompiler javacTool, ScannerSupplier scannerSupplier) {
    this.javacTool = javacTool;
    this.scannerSupplier = scannerSupplier;
  }

  @Override
  public CompilationTask getTask(
      Writer out,
      JavaFileManager fileManager,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      Iterable<String> options,
      Iterable<String> classes,
      Iterable<? extends JavaFileObject> compilationUnits) {
    ErrorProneOptions errorProneOptions = ErrorProneOptions.processArgs(options);
    ImmutableList<String> javacOpts =
        applyRequiredJavacOptions(errorProneOptions.getRemainingArgs());
    JavacTask task =
        (JavacTask)
            javacTool.getTask(
                out, fileManager, diagnosticListener, javacOpts, classes, compilationUnits);
    addTaskListener(task, scannerSupplier, errorProneOptions);
    return task;
  }

  static void addTaskListener(
      JavacTask javacTask, ScannerSupplier scannerSupplier, ErrorProneOptions errorProneOptions) {
    Context context = ((BasicJavacTask) javacTask).getContext();
    setupMessageBundle(context);
    checkRequiredJavacOptions(Options.instance(context));
    RefactoringCollection[] refactoringCollection = {null};
    javacTask.addTaskListener(
        ErrorProneAnalyzer.createAnalyzer(
            scannerSupplier, errorProneOptions, context, refactoringCollection));
    if (refactoringCollection[0] != null) {
      javacTask.addTaskListener(
          new ErrorProneAnalyzer.RefactoringTask(context, refactoringCollection[0]));
    }
  }

  @Override
  public StandardJavaFileManager getStandardFileManager(
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      Locale locale,
      Charset charset) {
    return javacTool.getStandardFileManager(diagnosticListener, locale, charset);
  }

  @Override
  public int isSupportedOption(String option) {
    int numberOfArgs = javacTool.isSupportedOption(option);
    if (numberOfArgs != -1) {
      return numberOfArgs;
    }
    return ErrorProneOptions.isSupportedOption(option);
  }

  @Override
  public int run(InputStream in, OutputStream out, OutputStream err, String... arguments) {
    return javacTool.run(in, out, err, arguments);
  }

  @Override
  public Set<SourceVersion> getSourceVersions() {
    Set<SourceVersion> filtered = EnumSet.noneOf(SourceVersion.class);
    for (SourceVersion version : javacTool.getSourceVersions()) {
      if (version.compareTo(SourceVersion.RELEASE_6) >= 0) {
        filtered.add(version);
      }
    }
    return filtered;
  }

  /**
   * Returns {@code args} with Error Prone's required javac defaults ({@code
   * -XDcompilePolicy=simple}, {@code --should-stop=ifError=FLOW}, and {@code
   * -XDaddTypeAnnotationsToSymbol=true}) prepended.
   */
  public static ImmutableList<String> applyRequiredJavacOptions(ImmutableList<String> args) {
    // Prepend defaults rather than appending so trailing dangling options (e.g., `-cp`) cannot
    // consume them, and so javac's last-wins semantics let valid explicit overrides (e.g.,
    // `-XDcompilePolicy=byfile` or `--should-stop=ifError=GENERATE`) take effect while
    // checkRequiredJavacOptions rejects unsupported values (e.g., `bytodo`, `INIT`, `false`).
    return ImmutableList.<String>builder()
        .add(
            // Attribute all classes in a file before lowering any of them; Error Prone depends on
            // this when analyzing files that contain multiple top-level classes.
            "-XDcompilePolicy=simple",
            // Continue through flow analysis even after earlier errors so checks still see
            // flow-computed facts like effective finality (see error-prone#4595).
            "--should-stop=ifError=FLOW",
            // Attach type annotations to symbols on JDK 21 (enabled by default on JDK 22+).
            "-XDaddTypeAnnotationsToSymbol=true")
        .addAll(defaultToLatestSupportedLanguageLevel(args))
        .build();
  }

  /**
   * Validates that {@code options} satisfy Error Prone's required javac options ({@code
   * -XDcompilePolicy}, {@code --should-stop=ifError}, and {@code -XDaddTypeAnnotationsToSymbol}).
   */
  public static void checkRequiredJavacOptions(Options options) {
    checkCompilePolicy(options.get("compilePolicy"));
    checkShouldStopIfErrorPolicy(options.get("should-stop.ifError"));
    checkAddTypeAnnotationsToSymbol(options.get("addTypeAnnotationsToSymbol"));
  }

  /**
   * Default to compiling with the same -source and -target as the host's javac.
   *
   * <p>This prevents, e.g., targeting Java 8 by default when using error-prone on JDK7.
   */
  private static ImmutableList<String> defaultToLatestSupportedLanguageLevel(
      ImmutableList<String> args) {

    String overrideLanguageLevel;
    switch (JAVA_SPECIFICATION_VERSION.value()) {
      case "1.7" -> overrideLanguageLevel = "7";
      case "1.8" -> overrideLanguageLevel = "8";
      default -> {
        return args;
      }
    }

    return ImmutableList.<String>builder()
        .add(
            // suppress xlint 'options' warnings to avoid diagnostics like:
            // 'bootstrap class path not set in conjunction with -source 1.7'
            "-Xlint:-options", "-source", overrideLanguageLevel, "-target", overrideLanguageLevel)
        .addAll(args)
        .build();
  }

  /**
   * Throws InvalidCommandLineOptionException if the {@code -XDcompilePolicy} flag is set to an
   * unsupported value
   */
  static void checkCompilePolicy(@Nullable String compilePolicy) {
    if (compilePolicy == null) {
      throw new InvalidCommandLineOptionException(
          "The default compilation policy (by-todo) is not supported by Error Prone,"
              + " pass -XDcompilePolicy=simple instead");
    }
    switch (compilePolicy) {
      case "byfile", "simple" -> {}
      default ->
          throw new InvalidCommandLineOptionException(
              String.format(
                  "-XDcompilePolicy=%s is not supported by Error Prone,"
                      + " pass -XDcompilePolicy=simple instead",
                  compilePolicy));
    }
  }

  private static void checkShouldStopIfErrorPolicy(String value) {
    if (value == null) {
      throw new InvalidCommandLineOptionException(
          "The default --should-stop=ifError policy (INIT) is not supported by Error Prone,"
              + " pass --should-stop=ifError=FLOW instead");
    }
    CompileState state = CompileState.valueOf(value);
    if (CompileState.FLOW.isAfter(state)) {
      throw new InvalidCommandLineOptionException(
          String.format(
              "--should-stop=ifError=%s is not supported by Error Prone, pass"
                  + " --should-stop=ifError=FLOW instead",
              value));
    }
  }

  private static void checkAddTypeAnnotationsToSymbol(String value) {
    if (value == null) {
      if (Runtime.version().feature() <= 21) {
        throw new InvalidCommandLineOptionException(
            "-XDaddTypeAnnotationsToSymbol=true is required by Error Prone on JDK 21");
      }
      return;
    }
    if (!Boolean.parseBoolean(value)) {
      throw new InvalidCommandLineOptionException(
          "-XDaddTypeAnnotationsToSymbol must be set to true, was: " + value);
    }
  }

  /** Registers our message bundle. */
  public static void setupMessageBundle(Context context) {
    ResourceBundle bundle = ResourceBundle.getBundle("com.google.errorprone.errors");
    JavacMessages.instance(context).add(l -> bundle);
  }
}
