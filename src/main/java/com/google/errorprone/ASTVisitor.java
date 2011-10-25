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

import com.google.errorprone.matchers.DeadExceptionChecker;
import com.google.errorprone.matchers.ErrorChecker;
import com.google.errorprone.matchers.ErrorChecker.AstError;
import com.google.errorprone.matchers.PreconditionsCheckNotNullChecker;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Visitor, following the visitor pattern, which may visit each node in the parsed AST.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ASTVisitor extends TreePathScanner<List<AstError>, VisitorState> {

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

  private final Iterable<? extends ErrorChecker<MethodInvocationTree>>
      methodInvocationCheckers = Arrays.asList(new PreconditionsCheckNotNullChecker());

  @Override
  public List<AstError> visitCompilationUnit(CompilationUnitTree compilationUnitTree, VisitorState visitorState) {
    visitorState.compilationUnit = (JCCompilationUnit)compilationUnitTree;
    List<AstError> errors = super.visitCompilationUnit(compilationUnitTree, visitorState);
    return errors != null ? errors : Collections.<AstError>emptyList();
  }

  @Override
  public List<AstError> visitMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    List<AstError> result = new ArrayList<AstError>();
    for (ErrorChecker<MethodInvocationTree> checker : methodInvocationCheckers) {
      AstError error = checker.check(methodInvocationTree, state);
      if (error != null) {
        result.add(error);
      }
    }
    super.visitMethodInvocation(methodInvocationTree, state);
    return result;
  }

  @Override
  public List<AstError> visitImport(ImportTree importTree, VisitorState state) {
    state.imports.add(importTree);
    super.visitImport(importTree, state);
    return null;
  }

  @Override
  public List<AstError> visitNewClass(NewClassTree newClassTree, VisitorState visitorState) {
    List<AstError> result = new ArrayList<AstError>();
    AstError error = new DeadExceptionChecker()
        .check(newClassTree, visitorState.withPath(getCurrentPath()));
    if (error != null) {
      result.add(error);
    }
    super.visitNewClass(newClassTree, visitorState);
    return result;
  }
}
