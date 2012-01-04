// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone;

import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.PropertyResourceBundle;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorProneCompiler extends Main {

  /**
   * Entry point for compiling Java code with error-prone enabled.
   * All default refactors are run, and the compile fails if they find a bug.
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
    TreePathScanner<Void, ? extends VisitorState> scanner = new ErrorProneScanner();
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

    public Builder search(SearchingScanner scanner) {
      this.compilerClass = SearchingJavaCompiler.class;
      this.scanner = scanner;
      return this;
    }

    public Builder refactor(RefactoringScanner scanner) {
      this.compilerClass = ErrorReportingJavaCompiler.class;
      this.scanner = scanner;
      return this;
    }
  }

  @Override
  protected void javaCompilerInit(final Context context) {
    super.javaCompilerInit(context);
    if (diagnosticListener != null) {
      context.put(DiagnosticListener.class, diagnosticListener);
    }
    TreePathScanner<Void, ? extends VisitorState> configuredScanner =
        context.get(TreePathScanner.class);
    if (configuredScanner == null) {
      configuredScanner = this.errorProneScanner;
      context.put(TreePathScanner.class, configuredScanner);
    }
    setupMessageBundle(context);
    try {
      compilerClass.getMethod("preRegister", Context.class).invoke(null, context);
    } catch (Exception e) {
      throw new RuntimeException("The JavaCompiler used must have the preRegister static method. "
          + "We are very sorry.", e);
    }
  }

  public static void setupMessageBundle(Context context) {
    try {
      String bundlePath = "/com/google/errorprone/errors.properties";
      InputStream bundleResource = ErrorProneCompiler.class.getResourceAsStream(bundlePath);
      if (bundleResource == null) {
        throw new IllegalStateException("Resource bundle not found at " + bundlePath);
      }
      Messages.instance(context).add(new PropertyResourceBundle(bundleResource));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
