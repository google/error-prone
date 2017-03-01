/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;

import com.google.common.collect.Iterables;
import com.google.errorprone.RefactoringCollection.RefactoringResult;
import com.google.errorprone.scanner.ErrorProneScannerTransformer;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JavacMessages;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.Processor;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

/** An Error Prone compiler that matches the interface of {@link com.sun.tools.javac.Main}. */
public class BaseErrorProneCompiler {

  private final DiagnosticListener<? super JavaFileObject> diagnosticListener;
  private final PrintWriter errOutput;
  private final String compilerName;
  private final ScannerSupplier scannerSupplier;

  private BaseErrorProneCompiler(
      String compilerName,
      PrintWriter errOutput,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      ScannerSupplier scannerSupplier) {
    this.errOutput = errOutput;
    this.compilerName = compilerName;
    this.diagnosticListener = diagnosticListener;
    this.scannerSupplier = checkNotNull(scannerSupplier, "scannerSupplier must not be null");
  }

  /** Returns a {@link BaseErrorProneCompiler} builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** A {@link BaseErrorProneCompiler} builder. */
  public static class Builder {
    private DiagnosticListener<? super JavaFileObject> diagnosticListener = null;
    private PrintWriter errOutput =
        new PrintWriter(
            new BufferedWriter(new OutputStreamWriter(System.err, Charset.defaultCharset())), true);
    private String compilerName = "javac (with error-prone)";
    private ScannerSupplier scannerSupplier;

    public BaseErrorProneCompiler build() {
      return new BaseErrorProneCompiler(
          compilerName, errOutput, diagnosticListener, scannerSupplier);
    }

    public Builder named(String compilerName) {
      this.compilerName = compilerName;
      return this;
    }

    public Builder redirectOutputTo(PrintWriter errOutput) {
      this.errOutput = errOutput;
      return this;
    }

    public Builder listenToDiagnostics(DiagnosticListener<? super JavaFileObject> listener) {
      this.diagnosticListener = listener;
      return this;
    }

    public Builder report(ScannerSupplier scannerSupplier) {
      this.scannerSupplier = scannerSupplier;
      return this;
    }
  }

  public Result run(String[] args) {
    return run(args, new Context());
  }

  /**
   * Default to compiling with the same -source and -target as the host's javac.
   *
   * <p>This prevents, e.g., targeting Java 8 by default when using error-prone on JDK7.
   */
  private static Iterable<String> defaultToLatestSupportedLanguageLevel(Iterable<String> args) {

    String overrideLanguageLevel;
    switch (JAVA_SPECIFICATION_VERSION.value()) {
      case "1.7":
        overrideLanguageLevel = "7";
        break;
      case "1.8":
        overrideLanguageLevel = "8";
        break;
      default:
        return args;
    }

    return Iterables.concat(
        Arrays.asList(
            // suppress xlint 'options' warnings to avoid diagnostics like:
            // 'bootstrap class path not set in conjunction with -source 1.7'
            "-Xlint:-options", "-source", overrideLanguageLevel, "-target", overrideLanguageLevel),
        args);
  }

  /**
   * Sets javac's {@code -XDcompilePolicy} flag to ensure that all classes in a file are attributed
   * before any of them are lowered. Error Prone depends on this behavior when analyzing files that
   * contain multiple top-level classes.
   *
   * @throws InvalidCommandLineOptionException if the {@code -XDcompilePolicy} flag is passed in the
   *     existing arguments with an unsupported value
   */
  private static Iterable<String> setCompilePolicyToByFile(Iterable<String> args)
      throws InvalidCommandLineOptionException {
    for (String arg : args) {
      if (arg.startsWith("-XDcompilePolicy")) {
        String value = arg.substring(arg.indexOf('=') + 1);
        switch (value) {
          case "byfile":
          case "simple":
            break;
          default:
            throw new InvalidCommandLineOptionException(
                String.format("-XDcompilePolicy=%s is not supported by Error Prone", value));
        }
        // don't do anything if a valid policy is already set
        return args;
      }
    }
    return Iterables.concat(args, Arrays.asList("-XDcompilePolicy=byfile"));
  }

  private String[] prepareCompilation(String[] argv, Context context)
      throws InvalidCommandLineOptionException {

    Iterable<String> newArgs = defaultToLatestSupportedLanguageLevel(Arrays.asList(argv));
    newArgs = setCompilePolicyToByFile(newArgs);
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(newArgs);

    argv = epOptions.getRemainingArgs();

    if (diagnosticListener != null) {
      context.put(DiagnosticListener.class, diagnosticListener);
    }
    MaskedClassLoader.preRegisterFileManager(context);

    setupMessageBundle(context);
    ErrorProneAnalyzer analyzer;
    if (epOptions.patchingOptions().doRefactor()) {
      RefactoringCollection refactoringCollection =
          epOptions.patchingOptions().inPlace()
              ? RefactoringCollection.refactorInPlace()
              : RefactoringCollection.refactorToPatchFile(
                  epOptions.patchingOptions().baseDirectory());

      // Refaster refactorer or using builtin checks
      CodeTransformer codeTransformer =
          epOptions
              .patchingOptions()
              .customRefactorer()
              .or(
                  () -> {
                    ScannerSupplier toUse = scannerSupplier;
                    Set<String> namedCheckers = epOptions.patchingOptions().namedCheckers();
                    if (!namedCheckers.isEmpty()) {
                      toUse =
                          scannerSupplier.filter(
                              bci -> namedCheckers.contains(bci.canonicalName()));
                    }
                    return ErrorProneScannerTransformer.create(
                        toUse.applyOverrides(epOptions).get());
                  })
              .get();

      analyzer =
          ErrorProneAnalyzer.createWithCustomDescriptionListener(
              codeTransformer, epOptions, context, refactoringCollection);
      context.put(RefactoringCollection.class, refactoringCollection);
    } else {
      analyzer = ErrorProneAnalyzer.createByScanningForPlugins(scannerSupplier, epOptions, context);
    }
    MultiTaskListener.instance(context).add(analyzer);

    return argv;
  }

  private Result run(String[] argv, Context context) {
    try {
      argv = prepareCompilation(argv, context);
    } catch (InvalidCommandLineOptionException e) {
      errOutput.println(e.getMessage());
      errOutput.flush();
      return Result.CMDERR;
    }

    try {
      Result compileResult = new Main(compilerName, errOutput).compile(argv, context);
      return wrapPotentialRefactoringCall(compileResult, context.get(RefactoringCollection.class));
    } catch (InvalidCommandLineOptionException e) {
      errOutput.println(e.getMessage());
      errOutput.flush();
      return Result.CMDERR;
    }
  }

  public Result run(String[] argv, List<JavaFileObject> javaFileObjects) {
    Context context = new Context();
    return run(argv, context, null, javaFileObjects, Collections.<Processor>emptyList());
  }

  public Result run(
      String[] argv,
      Context context,
      JavaFileManager fileManager,
      List<JavaFileObject> javaFileObjects,
      Iterable<? extends Processor> processors) {

    try {
      argv = prepareCompilation(argv, context);
    } catch (InvalidCommandLineOptionException e) {
      errOutput.println(e.getMessage());
      errOutput.flush();
      return Result.CMDERR;
    }

    JavacTool tool = JavacTool.create();
    JavacTaskImpl task =
        (JavacTaskImpl)
            tool.getTask(
                errOutput, fileManager, null, Arrays.asList(argv), null, javaFileObjects, context);
    if (processors != null) {
      task.setProcessors(processors);
    }
    try {
      return wrapPotentialRefactoringCall(task.doCall(), context.get(RefactoringCollection.class));
    } catch (InvalidCommandLineOptionException e) {
      errOutput.println(e.getMessage());
      errOutput.flush();
      return Result.CMDERR;
    }
  }

  private Result wrapPotentialRefactoringCall(
      Result original, @Nullable RefactoringCollection refactoringCollection) {
    if (refactoringCollection == null) {
      return original;
    }

    // Attempt the refactor
    try {
      RefactoringResult refactoringResult = refactoringCollection.applyChanges();
      if (refactoringResult.type() == RefactoringCollection.RefactoringResultType.CHANGED) {
        errOutput.println(refactoringResult.message());
        errOutput.flush();
      }
      return original;
    } catch (Exception e) {
      errOutput.append(e.getMessage());
      errOutput.flush();
      return Result.ERROR;
    }
  }

  /** Registers our message bundle. */
  public static void setupMessageBundle(Context context) {
    ResourceBundle bundle = ResourceBundle.getBundle("com.google.errorprone.errors");
    JavacMessages.instance(context).add(l -> bundle);
  }
}
