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

import com.google.errorprone.checkers.RefactoringMatcher.Refactor;
import com.google.errorprone.fixes.AppliedFix;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;

import java.io.IOException;
import java.util.Map;

import javax.tools.JavaFileObject;

/**
 * Making our errors appear to the user and break their build.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class JavacErrorReporter implements Reporter {
  private final Log log;
  private final Map<JCTree, Integer> endPositions;
  private final JavaFileObject sourceFile;

  // The suffix for properties in src/main/resources/com/google/errorprone/errors.properties
  private static final String MESSAGE_BUNDLE_KEY = "error.prone";

  public JavacErrorReporter(Log log, Map<JCTree, Integer> endPositions, JavaFileObject sourceFile) {
    this.log = log;
    this.endPositions = endPositions;
    this.sourceFile = sourceFile;
  }

  @Override
  public void report(Refactor refactor) {
    JavaFileObject originalSource;
    // Swap the log's source and the current file's source; then be sure to swap them back later.
    originalSource = log.useSource(sourceFile);
    try {
      CharSequence content = sourceFile.getCharContent(true);
      if (refactor.suggestedFix == null || endPositions == null) {
        log.error((DiagnosticPosition) refactor.node, MESSAGE_BUNDLE_KEY, refactor.message);
      } else {
        AppliedFix fix = AppliedFix.fromSource(content, endPositions).apply(refactor.suggestedFix);
        if (fix.isRemoveLine()) {
          log.error((DiagnosticPosition) refactor.node, MESSAGE_BUNDLE_KEY, refactor.message
              + "; did you mean to remove this line?");
        } else {
          log.error((DiagnosticPosition) refactor.node, MESSAGE_BUNDLE_KEY, refactor.message
              + "; did you mean '" + fix.getNewCodeSnippet() + "'?");
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (originalSource != null) {
        log.useSource(originalSource);
      }
    }
  }
}
