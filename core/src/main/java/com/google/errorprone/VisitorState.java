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
public abstract class VisitorState {

  protected final Context context;
  protected final TreePath path;

  public VisitorState(Context context, TreePath path) {
    this.context = context;
    this.path = path;
  }

  public abstract VisitorState withPath(TreePath path);

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
}
