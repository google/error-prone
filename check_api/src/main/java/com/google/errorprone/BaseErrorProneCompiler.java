/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.MaskedClassLoader.MaskedFileManager;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.main.CommandLine;
import com.sun.tools.javac.main.Main.Result;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Processor;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * An Error Prone compiler that matches the interface of {@link com.sun.tools.javac.Main}.
 *
 * @deprecated prefer {@link BaseErrorProneJavaCompiler}, which implements the standard {@code
 *     javax.tools.JavacCompiler} interface.
 */
@Deprecated
public class BaseErrorProneCompiler {

  private final DiagnosticListener<? super JavaFileObject> diagnosticListener;
  private final PrintWriter errOutput;
  private final ScannerSupplier scannerSupplier;

  private BaseErrorProneCompiler(
      PrintWriter errOutput,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      ScannerSupplier scannerSupplier) {
    this.errOutput = errOutput;
    this.diagnosticListener = diagnosticListener;
    this.scannerSupplier = checkNotNull(scannerSupplier);
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
    private ScannerSupplier scannerSupplier;

    public BaseErrorProneCompiler build() {
      return new BaseErrorProneCompiler(errOutput, diagnosticListener, scannerSupplier);
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

  public Result run(String[] argv) {
    try {
      argv = CommandLine.parse(argv);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    List<String> javacOpts = new ArrayList<>();
    List<String> sources = new ArrayList<>();
    for (String arg : argv) {
      // TODO(cushon): is there a better way to categorize javacopts?
      if (!arg.startsWith("-") && arg.endsWith(".java")) {
        sources.add(arg);
      } else {
        javacOpts.add(arg);
      }
    }
    StandardJavaFileManager fileManager = new MaskedFileManager();
    return run(
        javacOpts.toArray(new String[0]),
        fileManager,
        ImmutableList.copyOf(fileManager.getJavaFileObjectsFromStrings(sources)),
        /* processors= */ null);
  }

  public Result run(String[] argv, List<JavaFileObject> javaFileObjects) {
    return run(argv, null, javaFileObjects, ImmutableList.<Processor>of());
  }

  public Result run(
      String[] args,
      JavaFileManager fileManager,
      List<JavaFileObject> javaFileObjects,
      Iterable<? extends Processor> processors) {
    JavaCompiler compiler = new BaseErrorProneJavaCompiler(scannerSupplier);
    try {
      CompilationTask task =
          compiler.getTask(
              errOutput,
              fileManager,
              diagnosticListener,
              ImmutableList.copyOf(args),
              null /*classes*/,
              javaFileObjects);
      if (processors != null) {
        task.setProcessors(processors);
      }
      return task.call() ? Result.OK : Result.ERROR;
    } catch (InvalidCommandLineOptionException e) {
      errOutput.print(e);
      errOutput.flush();
      return Result.CMDERR;
    }
  }
}
