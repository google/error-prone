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

import com.google.errorprone.matchers.DeadExceptionMatcher;
import com.google.errorprone.matchers.ErrorProducingMatcher;
import com.google.errorprone.matchers.ErrorProducingMatcher.AstError;
import com.google.errorprone.matchers.PreconditionsCheckNotNullMatcher;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Visitor, following the visitor pattern, which may visit each node in the parsed AST.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ASTVisitor extends TreeScanner<List<AstError>, VisitorState> {

  private final Iterable<? extends ErrorProducingMatcher<MethodInvocationTree>>
      methodInvocationMatchers = Arrays.asList(new PreconditionsCheckNotNullMatcher());

  @Override
  public List<AstError> visitCompilationUnit(CompilationUnitTree compilationUnitTree, VisitorState visitorState) {
    visitorState.compilationUnit = (JCCompilationUnit)compilationUnitTree;
    return super.visitCompilationUnit(compilationUnitTree, visitorState);
  }

  @Override
  public List<AstError> visitMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    List<AstError> result = new ArrayList<AstError>();
    for (ErrorProducingMatcher<MethodInvocationTree> matcher : methodInvocationMatchers) {
      AstError error = matcher.matchWithError(methodInvocationTree, state);
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
    new DeadExceptionMatcher().matchWithError(newClassTree, visitorState);
    return super.visitNewClass(newClassTree, visitorState);
  }

}
