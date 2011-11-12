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

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;

import javax.lang.model.util.Types;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class VisitorState {
  private final Types types;
  private final Symtab symtab;
  private final TreeMaker treeMaker;
  private final JCCompilationUnit compilationUnit;
  private final TreePath path;

  public VisitorState(Types types, Symtab symtab, TreeMaker treeMaker) {
    this(types, symtab, treeMaker, null, null);
  }

  private VisitorState(Types types, Symtab symtab, TreeMaker treeMaker,
      JCCompilationUnit compilationUnit, TreePath path) {
    this.types = types;
    this.symtab = symtab;
    this.treeMaker = treeMaker;
    this.compilationUnit = compilationUnit;
    this.path = path;
  }

  public VisitorState withPath(TreePath path) {
    return new VisitorState(getTypes(), getSymtab(), treeMaker, getCompilationUnit(), path);
  }

  public VisitorState forCompilationUnit(JCCompilationUnit compilationUnit) {
    return new VisitorState(types, symtab, treeMaker, compilationUnit, path);
  }

  public TreePath getPath() {
    return path;
  }

  public TreeMaker getTreeMaker() {
    return treeMaker;
  }

  public Types getTypes() {
    return types;
  }

  public Symtab getSymtab() {
    return symtab;
  }

  public JCCompilationUnit getCompilationUnit() {
    return compilationUnit;
  }
}
