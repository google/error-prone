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

import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class VisitorState {
  public List<ImportTree> imports = new ArrayList<ImportTree>();
  public JCCompilationUnit compilationUnit;
  public final Types types;
  public final Symtab symtab;
  private TreePath path;

  public VisitorState(Types types, Symtab symtab) {
    this.types = types;
    this.symtab = symtab;
  }

  public VisitorState(Types types, Symtab symtab, JCCompilationUnit compilationUnit, TreePath path) {
    this(types, symtab);
    this.compilationUnit = compilationUnit;
    this.path = path;
  }

  public VisitorState withPath(TreePath path) {
    return new VisitorState(types, symtab, compilationUnit, path);
  }

  public TreePath getPath() {
    return path;
  }
}
