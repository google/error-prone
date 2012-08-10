/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to run an error-prone analysis as a phase in the javac compiler.
 */
public class ErrorProneAnalyzer {

  private final Log log;
  private final Context context;
  private final Scanner errorProneScanner;

  /**
   * Records how many classes error-prone has encountered in each file.
   */
  private final Map<CompilationUnitTree, Integer> classDefsEncountered;

  public ErrorProneAnalyzer(Log log, Context context) {
    this.log = log;
    this.context = context;
    this.classDefsEncountered = new HashMap<CompilationUnitTree, Integer>();
    this.errorProneScanner = context.get(Scanner.class);
    if (this.errorProneScanner == null) {
      throw new IllegalStateException(
          "No error-prone scanner registered in context. Is annotation processing enabled? " +
          "Please report bug to error-prone: " +
          "http://code.google.com/p/error-prone/issues/entry");
    }
  }

  /**
   * Reports that a class (represented by the env) is ready for error-prone to analyze. The
   * analysis will only occur when all classes in a compilation unit (a file) have been seen,
   * since unseen ones may not have been attributed yet.
   */
  public void reportReadyForAnalysis(Env<AttrContext> env) {
    DescriptionListener logReporter = new JavacErrorDescriptionListener(log,
        env.toplevel.endPositions,
        env.enclClass.sym.sourcefile != null
            ? env.enclClass.sym.sourcefile
            : env.toplevel.sourcefile);
    VisitorState visitorState = new VisitorState(context, logReporter);

    /* Each env corresponds to a top-level class but not necessarily a single file. We want to scan
     * a file all at once so that we see the file-level nodes like imports and package declarations.
     *
     * For the common case where a file contains only one class, we immediately scan the file.
     * For files that contain more than one class, we keep track of those files and the number of
     * enclosed class definitions we've seen. When we've seen all class definitions for that file,
     * we scan the whole file.
     */
    if (env.toplevel.getTypeDecls().size() == 1) {
      errorProneScanner.scan(env.toplevel, visitorState);
    } else {
      Integer seenCount = classDefsEncountered.get(env.toplevel);
      if (seenCount == null) {
        seenCount = 1;
      } else {
        seenCount++;
      }

      if (seenCount == env.toplevel.getTypeDecls().size()) {
        errorProneScanner.scan(env.toplevel, visitorState);
        classDefsEncountered.remove(env.toplevel);
      } else {
        classDefsEncountered.put(env.toplevel, seenCount);
      }
    }
  }

}
