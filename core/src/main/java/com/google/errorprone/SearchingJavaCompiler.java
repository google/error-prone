// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone;

import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;

import java.util.Queue;

/**
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SearchingJavaCompiler extends JavaCompiler {

  private SearchResultsPrinter resultsPrinter;

  public SearchingJavaCompiler(Context context) {
    super(context);
    resultsPrinter = new SearchResultsPrinter();
  }

  /**
   * Adds an initialization hook to the Context, such that each subsequent
   * request for a JavaCompiler (i.e., a lookup for 'compilerKey' of our
   * superclass, JavaCompiler) will actually construct and return our version.
   * It's necessary since many new JavaCompilers may
   * be requested for later stages of the compilation (annotation processing),
   * within the same Context. And it's the preferred way for extending behavior
   * within javac, per the documentation in {@link com.sun.tools.javac.util.Context}.
   */
  public static void preRegister(final Context context) {
    context.put(compilerKey, new Factory<JavaCompiler>() {
      @Override
      public JavaCompiler make() {
        return new SearchingJavaCompiler(context);
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
    resultsPrinter.setCompilationUnit(env.toplevel.sourcefile);
    VisitorState visitorState = new VisitorState(context, resultsPrinter);
    Scanner scanner = (Scanner) context.get(TreePathScanner.class);
    scanner.scan(env.toplevel, visitorState);
  }

  private boolean printed = false;
  @Override
  public void close(boolean disposeNames) {
    if (!printed) {
      resultsPrinter.printMatches(log);
      printed = true;
    }
    super.close(disposeNames);
  }

}
