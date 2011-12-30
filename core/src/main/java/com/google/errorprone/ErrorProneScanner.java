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

import com.google.errorprone.checkers.EmptyIfChecker;
import com.google.errorprone.checkers.FallThroughSuppressionChecker;
import com.google.errorprone.checkers.OrderingFromChecker;
import com.google.errorprone.checkers.PreconditionsCheckNotNullChecker;
import com.google.errorprone.checkers.PreconditionsCheckNotNullPrimitive1stArgChecker;
import com.google.errorprone.checkers.PreconditionsExpensiveStringChecker;
import com.google.errorprone.checkers.RefactoringMatcher;
import com.google.errorprone.checkers.dead_exception.DeadExceptionChecker;
import com.google.errorprone.checkers.objects_equal_self_comparison.ObjectsEqualSelfComparisonChecker;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePathScanner;

import java.util.Arrays;

/**
 * Scans the parsed AST, looking for violations of any of the configured checks.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorProneScanner extends TreePathScanner<Void, RefactoringVisitorState> {

  private final Iterable<? extends RefactoringMatcher<MethodInvocationTree>>
      methodInvocationCheckers = Arrays.asList(
          new ObjectsEqualSelfComparisonChecker(),
          new OrderingFromChecker(),
          new PreconditionsCheckNotNullChecker(),
          new PreconditionsExpensiveStringChecker(),
          new PreconditionsCheckNotNullPrimitive1stArgChecker());
  
  private final Iterable<? extends RefactoringMatcher<NewClassTree>>
      newClassCheckers = Arrays.asList(
          new DeadExceptionChecker());

  private final Iterable<? extends RefactoringMatcher<AnnotationTree>>
      annotationCheckers = Arrays.asList(
          new FallThroughSuppressionChecker());

  private final Iterable<? extends RefactoringMatcher<EmptyStatementTree>>
      emptyStatementCheckers = Arrays.asList(
          new EmptyIfChecker());
  
  @Override
  public Void visitMethodInvocation(
      MethodInvocationTree methodInvocationTree, RefactoringVisitorState state) {
    for (RefactoringMatcher<MethodInvocationTree> checker : methodInvocationCheckers) {
      RefactoringVisitorState newState = state.withPath(getCurrentPath());
      if (checker.matches(methodInvocationTree, newState)) {
         state.getReporter().report(checker.refactor(methodInvocationTree, newState));
      }
    }
    super.visitMethodInvocation(methodInvocationTree, state);
    return null;
  }

  @Override
  public Void visitNewClass(NewClassTree newClassTree, RefactoringVisitorState visitorState) {
    for (RefactoringMatcher<NewClassTree> newClassChecker : newClassCheckers) {
      RefactoringVisitorState state = visitorState.withPath(getCurrentPath());
      if (newClassChecker.matches(newClassTree, state)) {
         state.getReporter().report(newClassChecker.refactor(newClassTree, state));
      }
    }
    super.visitNewClass(newClassTree, visitorState);
    return null;
  }

  @Override
  public Void visitAnnotation(AnnotationTree annotationTree, RefactoringVisitorState visitorState) {
    for (RefactoringMatcher<AnnotationTree> annotationChecker : annotationCheckers) {
      RefactoringVisitorState state = visitorState.withPath(getCurrentPath());
      if (annotationChecker.matches(annotationTree, state)) {
         state.getReporter().report(annotationChecker.refactor(annotationTree, state));
      }
    }
    super.visitAnnotation(annotationTree, visitorState);
    return null;
  }
  
  @Override
  public Void visitEmptyStatement(EmptyStatementTree emptyStatementTree,
      RefactoringVisitorState visitorState) {
    for (RefactoringMatcher<EmptyStatementTree> emptyStatementChecker : emptyStatementCheckers) {
      RefactoringVisitorState state = visitorState.withPath(getCurrentPath());
      if (emptyStatementChecker.matches(emptyStatementTree, state)) {
        state.getReporter().report(emptyStatementChecker.refactor(emptyStatementTree, state));
      }
    }
    super.visitEmptyStatement(emptyStatementTree, visitorState);
    return null;
  }
}
