// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class VisitorState {

  private final RefactorListener refactorListener;
  private final MatchListener matchListener;
  private final Context context;
  private final TreePath path;

  public VisitorState(Context context, TreePath path,
      RefactorListener refactorListener, MatchListener matchListener) {
    this.context = context;
    this.path = path;
    this.refactorListener = refactorListener;
    this.matchListener = matchListener;
  }

  public VisitorState withPath(TreePath path) {
    return new VisitorState(context, path, refactorListener, matchListener);
  }

  public TreePath getPath() {
    return path;
  }

  public TreeMaker getTreeMaker() {
    return TreeMaker.instance(context);
  }

  public Types getTypes() {
    return Types.instance(context);
  }

  public Symtab getSymtab() {
    return Symtab.instance(context);
  }

  public RefactorListener getRefactorListener() {
    return refactorListener;
  }

  public MatchListener getMatchListener() {
    return matchListener;
  }
}
