// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone;

import com.sun.source.tree.Tree;

/**
 * A listener which is told about AST nodes which match the search.
 * @author alexeagle@google.com (Alex Eagle)
 */
public interface MatchListener {
  void onMatch(Tree tree);
}
