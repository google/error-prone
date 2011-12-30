// Copyright 2011 Google Inc. All Rights Reserved.

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
      new ArrayList<Pair<Tree, JavaFileObject>>();
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
