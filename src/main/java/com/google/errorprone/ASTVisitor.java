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

import com.google.errorprone.matchers.ErrorProducingMatcher.AstError;
import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.google.errorprone.matchers.ErrorProducingMatcher;
import com.google.errorprone.matchers.PreconditionsCheckNotNullMatcher;

import static java.util.Arrays.asList;

/**
 * Visitor, following the visitor pattern, which may visit each node in the parsed AST.
 * @author Alex Eagle (alexeagle@google.com)
 */
class ASTVisitor extends TreeScanner<Void, VisitorState> {

  private final ErrorReporter errorReporter;

  private final Iterable<? extends ErrorProducingMatcher<MethodInvocationTree>>
      methodInvocationMatchers = asList(new PreconditionsCheckNotNullMatcher());

  public ASTVisitor(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, VisitorState state) {
    for (ErrorProducingMatcher<MethodInvocationTree> matcher : methodInvocationMatchers) {
      AstError error = matcher.matchWithError(methodInvocationTree, state);
      if (error != null) {
        errorReporter.emitError(error);
      }
    }
    super.visitMethodInvocation(methodInvocationTree, state);
    return null;
  }

  @Override
  public Void visitImport(ImportTree importTree, VisitorState state) {
    state.imports.add(importTree);
    super.visitImport(importTree, state);
    return null;
  }
}
