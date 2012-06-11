/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import javax.annotation.processing.Processor;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;

import static com.google.errorprone.ErrorProneScanner.EnabledPredicate.DEFAULT_CHECKS;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorProneCompiler extends Main {

  /**
   * Entry point for compiling Java code with error-prone enabled.
   * All default checks are run, and the compile fails if they find a bug.
   * @param args the same args which could be passed to javac on the command line
   */
  public static void main(String[] args) {
    System.exit(new ErrorProneCompiler.Builder().build().compile(args));
  }

  private final DiagnosticListener<? super JavaFileObject> diagnosticListener;
  private final TreePathScanner<Void, ? extends VisitorState> errorProneScanner;
  private final Class<? extends JavaCompiler> compilerClass;

  private ErrorProneCompiler(String s, PrintWriter printWriter,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      TreePathScanner<Void, ? extends VisitorState> errorProneScanner,
      Class<? extends JavaCompiler> compilerClass) {
    super(s, printWriter);
    this.diagnosticListener = diagnosticListener;
    this.errorProneScanner = errorProneScanner;
    this.compilerClass = compilerClass;
  }

  public static class Builder {
    DiagnosticListener<? super JavaFileObject> diagnosticListener = null;
    PrintWriter out = new PrintWriter(System.err, true);
    String compilerName = "javac (with error-prone)";
    TreePathScanner<Void, ? extends VisitorState> scanner = new ErrorProneScanner(DEFAULT_CHECKS);
    Class<? extends JavaCompiler> compilerClass = ErrorReportingJavaCompiler.class;

    public ErrorProneCompiler build() {
      return new ErrorProneCompiler(compilerName, out, diagnosticListener, scanner, compilerClass);
    }

    public Builder named(String compilerName) {
      this.compilerName = compilerName;
      return this;
    }

    public Builder redirectOutputTo(PrintWriter out) {
      this.out = out;
      return this;
    }

    public Builder listenToDiagnostics(DiagnosticListener<? super JavaFileObject> listener) {
      this.diagnosticListener = listener;
      return this;
    }

    public Builder search(Scanner scanner) {
      this.compilerClass = SearchingJavaCompiler.class;
      this.scanner = scanner;
      return this;
    }

    public Builder report(Scanner scanner) {
      this.compilerClass = ErrorReportingJavaCompiler.class;
      this.scanner = scanner;
      return this;
    }
  }

  /**
   * Hook into the compile method, to register our components with the compilation's context.
   */
  @Override
  public int compile(String[] strings, Context context, List<JavaFileObject> javaFileObjects,
      Iterable<? extends Processor> iterable) {
    if (diagnosticListener != null) {
      context.put(DiagnosticListener.class, diagnosticListener);
    }
    TreePathScanner<Void, ? extends VisitorState> configuredScanner =
        context.get(TreePathScanner.class);
    if (configuredScanner == null) {
      configuredScanner = this.errorProneScanner;
      context.put(TreePathScanner.class, configuredScanner);
    }

    // Register our message bundle reflectively, so we can compile against both JDK6 and JDK7.
    // OpenJDK6: com.sun.tools.javac.util.Messages.instance(context).add("com.google.errorprone.errors");
    // OpenJDK7: com.sun.tools.javac.util.JavacMessages.instance(context).add("com.google.errorprone.errors");
    if (System.getProperty("java.vm.vendor").equals("Apple Inc.")) {
      throw new UnsupportedOperationException("error-prone doesn't work on Apple JDK's yet.\n" +
          "Vote: http://code.google.com/p/error-prone/issues/detail?id=16");
    }
    try {
      Class<?> messagesClass;
      try {
        messagesClass = getClass().getClassLoader().loadClass("com.sun.tools.javac.util.Messages");
      } catch (ClassNotFoundException e) {
        messagesClass = getClass().getClassLoader().loadClass("com.sun.tools.javac.util.JavacMessages");
      }
      Object instance = messagesClass.getMethod("instance", Context.class).invoke(null, context);
      messagesClass.getMethod("add", String.class).invoke(instance, "com.google.errorprone.errors");
    } catch (Exception e) {
      throw new RuntimeException(String.format(
          "Unable to register message bundle. java.vm.vendor=[%s],  java.version=[%s]",
          System.getProperty("java.vm.vendor"), System.getProperty("java.version")),
          e);
    }

    try {
      compilerClass.getMethod("preRegister", Context.class).invoke(null, context);
    } catch (Exception e) {
      throw new RuntimeException("The JavaCompiler used must have the preRegister static method. "
          + "We are very sorry.", e);
    }
    return super.compile(strings, context, javaFileObjects, iterable);
  }
}
