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

import com.google.errorprone.checkers.ErrorChecker.AstError;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for scanning a compilation unit's AST, and producing error information.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorCollectingTreeScanner extends TreePathScanner<List<AstError>, VisitorState> {

  @Override
  public List<AstError> reduce(List<AstError> r1, List<AstError> r2) {
    List<AstError> concat = new ArrayList<AstError>();
    if (r1 != null) {
      concat.addAll(r1);
    }
    if (r2 != null) {
      concat.addAll(r2);
    }
    return concat;
  }

  @Override
  public List<AstError> visitCompilationUnit(
      CompilationUnitTree compilationUnitTree, VisitorState visitorState) {
    visitorState.compilationUnit = (JCCompilationUnit)compilationUnitTree;
    List<AstError> errors = super.visitCompilationUnit(compilationUnitTree, visitorState);
    return errors != null ? errors : Collections.<AstError>emptyList();
  }
}
