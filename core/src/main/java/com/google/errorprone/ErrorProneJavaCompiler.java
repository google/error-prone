/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.google.errorprone.scanner.ErrorProneScannerSuppliers;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.Context;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * An error-prone compiler that implements {@link javax.tools.JavaCompiler}.
 *
 * @author nik
 */
public class ErrorProneJavaCompiler implements JavaCompiler {

  private final JavaCompiler javacTool;
  private final ScannerSupplier scannerSupplier;

  public ErrorProneJavaCompiler() {
    this(JavacTool.create());
  }

  // package-private for testing
  ErrorProneJavaCompiler(JavaCompiler javacTool) {
    this(javacTool, ErrorProneScannerSuppliers.matureChecks());
  }

  public ErrorProneJavaCompiler(ScannerSupplier scannerSupplier) {
    this(JavacTool.create(), scannerSupplier);
  }

  private ErrorProneJavaCompiler(JavaCompiler javacTool, ScannerSupplier scannerSupplier) {
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
    List<String> remainingOptions = Arrays.asList(errorProneOptions.getRemainingArgs());
    CompilationTask task = javacTool.getTask(
        out, fileManager, diagnosticListener, remainingOptions, classes, compilationUnits);
    Context context = ((JavacTaskImpl) task).getContext();
    Scanner scanner;
    try {
      scanner = scannerSupplier.applyOverrides(errorProneOptions.getSeverityMap()).get();
    } catch (InvalidCommandLineOptionException e) {
      throw new RuntimeException(e);
    }
    ErrorProneJavacJavaCompiler.preRegister(context, scanner);
    return task;
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
}
