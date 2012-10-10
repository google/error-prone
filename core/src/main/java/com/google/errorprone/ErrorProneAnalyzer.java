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
import com.sun.source.tree.Tree;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * Used to run an error-prone analysis as a phase in the javac compiler.
 */
public class ErrorProneAnalyzer {

  private final Log log;
  private final Context context;
  private final Scanner errorProneScanner;

  /**
   * Which classes error-prone has encountered.
   */
  private final Set<Tree> classesEncountered;

  /**
   * Which compilation units have been scanned.
   */
  private final Set<CompilationUnitTree> compilationUnitsScanned;

  public ErrorProneAnalyzer(Log log, Context context) {
    this.log = log;
    this.context = context;
    this.classesEncountered = new HashSet<Tree>();
    this.compilationUnitsScanned = new HashSet<CompilationUnitTree>();
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
   * analysis will only occur when all classes in a compilation unit (a file) have been seen.
   */
  public void reportReadyForAnalysis(Env<AttrContext> env) {
    if (!compilationUnitsScanned.contains(env.toplevel)) {
      // TODO(eaftan): This check for size == 1 is an optimization for the common case of 1 class
      // per file. We should benchmark to see if it actually helps.
      if (env.toplevel.getTypeDecls().size() == 1) {
        errorProneScanner.scan(env.toplevel, createVisitorState(env));
        compilationUnitsScanned.add(env.toplevel);
      } else {
        classesEncountered.add(env.tree);
        if (allClassesSeen(env)) {
          errorProneScanner.scan(env.toplevel, createVisitorState(env));
          compilationUnitsScanned.add(env.toplevel);
        }
      }
    }
  }

  /**
   * Create a VisitorState object from an environment.
   */
  private VisitorState createVisitorState(Env<AttrContext> env) {
    DescriptionListener logReporter = new JavacErrorDescriptionListener(log,
        env.toplevel.endPositions,
        env.enclClass.sym.sourcefile != null
            ? env.enclClass.sym.sourcefile
            : env.toplevel.sourcefile);
    VisitorState visitorState = new VisitorState(context, logReporter);
    return visitorState;
  }

  /**
   * Determines whether all classes in this compilation unit been seen.
   */
  private boolean allClassesSeen(Env<AttrContext> env) {
    for (Tree tree : env.toplevel.getTypeDecls()) {
      if (!classesEncountered.contains(tree)) {
        return false;
      }
    }
    return true;
  }

}
