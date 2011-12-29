// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone;

import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.PropertyResourceBundle;
import java.util.Queue;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorFindingCompiler extends Main {

  private final DiagnosticListener<? super JavaFileObject> diagnosticListener;
  private final ErrorCollectingTreeScanner errorProneScanner;

  /**
   * Entry point for compiling Java code with error-prone enabled.
   * @param args the same args which could be passed to javac on the command line
   */
  public static void main(String[] args) {
    System.exit(new ErrorFindingCompiler.Builder().build().compile(args));
  }

  private ErrorFindingCompiler(String s, PrintWriter printWriter,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      ErrorCollectingTreeScanner errorProneScanner) {
    super(s, printWriter);
    this.diagnosticListener = diagnosticListener;
    this.errorProneScanner = errorProneScanner;
  }

  public static class Builder {
    DiagnosticListener<? super JavaFileObject> diagnosticListener = null;
    PrintWriter out = new PrintWriter(System.err, true);
    String compilerName = "javac (with error-prone)";
    ErrorCollectingTreeScanner scanner = new ErrorProneScanner();

    public ErrorFindingCompiler build() {
      return new ErrorFindingCompiler(compilerName, out, diagnosticListener, scanner);
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

    public Builder usingScanner(ErrorCollectingTreeScanner scanner) {
      this.scanner = scanner;
      return this;
    }
  }

  @Override
  protected void javaCompilerInit(Context context) {
    super.javaCompilerInit(context);
    if (diagnosticListener != null) {
      context.put(DiagnosticListener.class, diagnosticListener);
    }
    ErrorCollectingTreeScanner configuredScanner = context.get(ErrorCollectingTreeScanner.class);
    if (configuredScanner == null) {
      configuredScanner = this.errorProneScanner;
      context.put(ErrorCollectingTreeScanner.class, configuredScanner);
    }
    setupMessageBundle(context);
    ErrorCheckingJavaCompiler.preRegister(context);
  }

  private static class ErrorCheckingJavaCompiler extends JavaCompiler {

    public ErrorCheckingJavaCompiler(Context context) {
      super(context);
    }

    /**
     * Adds an initialization hook to the Context, such that each subsequent
     * request for a JavaCompiler (i.e., a lookup for 'compilerKey' of our
     * superclass, JavaCompiler) will actually construct and return our version.
     * It's necessary since many new JavaCompilers may
     * be requested for later stages of the compilation (annotation processing),
     * within the same Context. And it's the preferred way for extending behavior
     * within javac, per the documentation in {@link Context}.
     */
    public static void preRegister(final Context context) {
      context.put(compilerKey, new Context.Factory<JavaCompiler>() {
        @Override
        public JavaCompiler make() {
          return new ErrorCheckingJavaCompiler(context);
        }
      });
    }

    @Override
    protected void flow(Env<AttrContext> attrContextEnv, Queue<Env<AttrContext>> envs) {
      super.flow(attrContextEnv, envs);
      postFlow(attrContextEnv);
    }

    /**
     * Run Error Prone analysis after performing dataflow checks.
     */
    public void postFlow(Env<AttrContext> env) {
      JavacErrorReporter logReporter = new JavacErrorReporter(log,
          env.toplevel.endPositions,
          env.enclClass.sym.sourcefile != null
              ? env.enclClass.sym.sourcefile
              : env.toplevel.sourcefile);
      VisitorState visitorState = new VisitorState(context, logReporter);
      ErrorCollectingTreeScanner scanner = context.get(ErrorCollectingTreeScanner.class);
      scanner.scan(env.toplevel, visitorState);
    }
  }

  public static void setupMessageBundle(Context context) {
    try {
      String bundlePath = "/com/google/errorprone/errors.properties";
      InputStream bundleResource = ErrorFindingCompiler.class.getResourceAsStream(bundlePath);
      if (bundleResource == null) {
        throw new IllegalStateException("Resource bundle not found at " + bundlePath);
      }
      Messages.instance(context).add(new PropertyResourceBundle(bundleResource));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
