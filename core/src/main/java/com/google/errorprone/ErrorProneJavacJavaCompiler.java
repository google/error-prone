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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Throwables;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.CompileStates.CompileState;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;
import com.sun.tools.javac.util.JavacMessages;

import java.util.Queue;

/**
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorProneJavacJavaCompiler extends JavaCompiler {

  private final ErrorProneAnalyzer errorProneAnalyzer;

  /**
   * Registers our message bundle.
   */
  public static void setupMessageBundle(Context context) {
    JavacMessages.instance(context).add("com.google.errorprone.errors");
  }

  private ErrorProneJavacJavaCompiler(
      Context context, CodeTransformer transformer, ErrorProneOptions errorProneOptions) {
    super(context);
    checkNotNull(transformer);

    // Setup message bundle.
    setupMessageBundle(context);

    // Create ErrorProneAnalyzer.
    errorProneAnalyzer = ErrorProneAnalyzer.create(transformer).init(context, errorProneOptions);
  }

  /**
   * Adds an initialization hook to the Context, such that each subsequent
   * request for a JavaCompiler (i.e., a lookup for 'compilerKey' of our
   * superclass, JavaCompiler) will actually construct and return our version.
   * It's necessary since many new JavaCompilers may
   * be requested for later stages of the compilation (annotation processing),
   * within the same Context.
   */
  public static void preRegister(
      Context context, 
      final CodeTransformer transformer, 
      final ErrorProneOptions errorProneOptions) {
    context.put(
        compilerKey,
        new Factory<JavaCompiler>() {
          @Override
          public JavaCompiler make(Context ctx) {
            // Ensure that future processing rounds continue to use the same Scanner.
            return new ErrorProneJavacJavaCompiler(ctx, transformer, errorProneOptions);
          }
        });
  }

  @Override
  protected void flow(Env<AttrContext> env, Queue<Env<AttrContext>> results) {
    if (compileStates.isDone(env, CompileState.FLOW)) {
      super.flow(env, results);
      return;
    }
    super.flow(env, results);
    // don't run Error Prone if there were compilation errors
    if (errorCount() > 0) {
      return;
    }
    try {
      postFlow(env);
    } catch (ErrorProneError e) {
      e.logFatalError(log);
      // let the exception propagate to javac's main, where it will cause the compilation to
      // terminate with Result.ABNORMAL
      throw e;
    } catch (Throwable e) {
      String version = ErrorProneCompiler.loadVersionFromPom().or("unknown version");
      log.error("error.prone.crash", Throwables.getStackTraceAsString(e), version);
    }
  }

  /**
   * Run Error Prone analysis after performing dataflow checks.
   */
  public void postFlow(Env<AttrContext> env) {
    errorProneAnalyzer.finished(new TaskEvent(Kind.ANALYZE, env.toplevel, env.enclClass.sym));
  }
}
