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

import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;

import java.util.Queue;

/**
 * Look for matches, but only report the locations of the matches.
 * Useful for preliminary exploration of a potential problem, before
 * you've written any error messaging or suggested fixes.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SearchingJavaCompiler extends JavaCompiler {

  private final ErrorProneAnalyzer errorProneAnalyzer;
  private SearchResultsPrinter resultsPrinter;

  public SearchingJavaCompiler(Context context) {
    super(context);
    assert context.get(Scanner.class) != null;

    errorProneAnalyzer = new ErrorProneAnalyzer(log, context, resultsPrinter);
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
    final Scanner scanner = context.get(Scanner.class);
    context.put(compilerKey, new Factory<JavaCompiler>() {
      //@Override for OpenJDK 7 only
      public JavaCompiler make(Context ctx) {
        // Ensure that future processing rounds continue to use the same Scanner.
        ctx.put(Scanner.class, scanner);
        return new SearchingJavaCompiler(ctx);
      }
      //@Override for OpenJDK 6 only
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
    errorProneAnalyzer.reportReadyForAnalysis(env, errorCount() > 0);
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
