// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SearchingScanner extends TreePathScanner<Void, VisitorState> {
  protected void reportMatch(Tree match, VisitorState state) {
    state.getMatchListener().onMatch(match);
  }
}
