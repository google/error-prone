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
    ImmutableList<String> javacOpts = errorProneOptions.getRemainingArgs();
    javacOpts = defaultToLatestSupportedLanguageLevel(javacOpts);
    javacOpts = setCompilePolicyToByFile(javacOpts);
    javacOpts = setShouldStopIfErrorPolicyToFlow(javacOpts);
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
    checkCompilePolicy(Options.instance(context).get("compilePolicy"));
    checkShouldStopIfErrorPolicy(Options.instance(context).get("should-stop.ifError"));
    setupMessageBundle(context);
    RefactoringCollection[] refactoringCollection = {null};
    javacTask.addTaskListener(
        ErrorProneAnalyzer.createAnalyzer(
            scannerSupplier, errorProneOptions, context, refactoringCollection));
    if (refactoringCollection[0] != null) {
      javacTask.addTaskListener(
          new ErrorProneAnalyzer.RefactoringTask(context, refactoringCollection[0]));
    }

    if (Options.instance(context).isSet("-verbose")) {
      javacTask.addTaskListener(
          new TimingReporter(ErrorProneTimings.instance(context), Log.instance(context)));
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

  /**
   * Sets javac's {@code -XDcompilePolicy} flag to ensure that all classes in a file are attributed
   * before any of them are lowered. Error Prone depends on this behavior when analyzing files that
   * contain multiple top-level classes.
   */
  private static ImmutableList<String> setCompilePolicyToByFile(ImmutableList<String> args) {
    for (String arg : args) {
      if (arg.startsWith("-XDcompilePolicy")) {
        String value = arg.substring(arg.indexOf('=') + 1);
        checkCompilePolicy(value);
        return args; // don't do anything if a valid policy is already set
      }
    }
    return ImmutableList.<String>builder().addAll(args).add("-XDcompilePolicy=simple").build();
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

  private static ImmutableList<String> setShouldStopIfErrorPolicyToFlow(
      ImmutableList<String> args) {
    for (String arg : args) {
      if (arg.startsWith("--should-stop=ifError") || arg.startsWith("-XDshould-stop.ifError")) {
        String value = arg.substring(arg.lastIndexOf('=') + 1);
        checkShouldStopIfErrorPolicy(value);
        return args; // don't do anything if a valid policy is already set
      }
    }
    return ImmutableList.<String>builder().addAll(args).add("--should-stop=ifError=FLOW").build();
  }

  /** Registers our message bundle. */
  public static void setupMessageBundle(Context context) {
    ResourceBundle bundle = ResourceBundle.getBundle("com.google.errorprone.errors");
    JavacMessages.instance(context).add(l -> bundle);
  }
}
