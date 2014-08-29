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

import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Pair;

import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaFileObject;

/**
* @author alexeagle@google.com (Alex Eagle)
*/
class SearchResultsPrinter implements MatchListener {

  private final List<Pair<Tree, JavaFileObject>> matches =
      new ArrayList<>();
  private JavaFileObject sourceFile;

  @Override
  public void onMatch(Tree tree) {
    matches.add(new Pair<Tree, JavaFileObject>(tree, sourceFile));
  }

  public void printMatches(Log log) {
    for (Pair<Tree, JavaFileObject> match : matches) {
      JavaFileObject originalSource;
      // Swap the log's source and the current file's source; then be sure to swap them back later.
      originalSource = log.useSource(match.snd);
      try {
        log.note((DiagnosticPosition)match.fst, "searchresult", "Matched.");
      } finally {
        if (originalSource != null) {
          log.useSource(originalSource);
        }
      }
    }
    log.note("searchresult.count", matches.size());
  }

  public void setCompilationUnit(JavaFileObject sourceFile) {
    this.sourceFile = sourceFile;
  }
}
