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

import com.google.errorprone.refactors.RefactoringMatcher;
import com.google.errorprone.refactors.deadexception.DeadException;
import com.google.errorprone.refactors.emptyifstatement.EmptyIfStatement;
import com.google.errorprone.refactors.fallthroughsuppression.FallThroughSuppression;
import com.google.errorprone.refactors.objectsequalselfcomparison.ObjectsEqualSelfComparison;
import com.google.errorprone.refactors.orderingfrom.OrderingFrom;
import com.google.errorprone.refactors.preconditionschecknotnull.PreconditionsCheckNotNull;
import com.google.errorprone.refactors.preconditionschecknotnullprimitive1starg.PreconditionsCheckNotNullPrimitive1stArg;
import com.google.errorprone.refactors.preconditionsexpensivestring.PreconditionsExpensiveString;
import com.google.errorprone.refactors.selfassignment.SelfAssignment;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;

import java.util.Arrays;

/**
 * Scans the parsed AST, looking for violations of any of the configured checks.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorProneScanner extends Scanner {

  private final Iterable<? extends RefactoringMatcher<MethodInvocationTree>>
      methodInvocationMatchers = Arrays.asList(
          new ObjectsEqualSelfComparison(),
          new OrderingFrom(),
          new PreconditionsCheckNotNull(),
          new PreconditionsExpensiveString(),
          new PreconditionsCheckNotNullPrimitive1stArg());
  
  private final Iterable<? extends RefactoringMatcher<NewClassTree>>
      newClassMatchers = Arrays.asList(
          new DeadException());

  private final Iterable<? extends RefactoringMatcher<AnnotationTree>>
      annotationMatchers = Arrays.asList(
          new FallThroughSuppression());

  private final Iterable<? extends RefactoringMatcher<EmptyStatementTree>>
      emptyStatementMatchers = Arrays.asList(
          new EmptyIfStatement());
  
  private final Iterable<? extends RefactoringMatcher<AssignmentTree>>
      assignmentMatchers = Arrays.asList(
          new SelfAssignment());
  
  @Override
  public Void visitMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    for (RefactoringMatcher<MethodInvocationTree> matcher : methodInvocationMatchers) {
      VisitorState newState = state.withPath(getCurrentPath());
      if (!isSuppressed(matcher.getName()) && matcher.matches(methodInvocationTree, newState)) {
         reportMatch(matcher, methodInvocationTree, newState);
      }
    }
    super.visitMethodInvocation(methodInvocationTree, state);
    return null;
  }

  @Override
  public Void visitNewClass(NewClassTree newClassTree, VisitorState visitorState) {
    for (RefactoringMatcher<NewClassTree> matcher : newClassMatchers) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      if (!isSuppressed(matcher.getName()) && matcher.matches(newClassTree, state)) {
         reportMatch(matcher, newClassTree, state);
      }
    }
    super.visitNewClass(newClassTree, visitorState);
    return null;
  }

  @Override
  public Void visitAnnotation(AnnotationTree annotationTree, VisitorState visitorState) {
    for (RefactoringMatcher<AnnotationTree> matcher : annotationMatchers) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      if (!isSuppressed(matcher.getName()) && matcher.matches(annotationTree, state)) {
         reportMatch(matcher, annotationTree, state);
      }
    }
    super.visitAnnotation(annotationTree, visitorState);
    return null;
  }
  
  @Override
  public Void visitEmptyStatement(EmptyStatementTree emptyStatementTree,
      VisitorState visitorState) {
    for (RefactoringMatcher<EmptyStatementTree> matcher : emptyStatementMatchers) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      if (!isSuppressed(matcher.getName()) && matcher.matches(emptyStatementTree, state)) {
        reportMatch(matcher, emptyStatementTree, state);
      }
    }
    super.visitEmptyStatement(emptyStatementTree, visitorState);
    return null;
  }
  
  @Override
  public Void visitAssignment(AssignmentTree assignmentTree,
      VisitorState visitorState) {
    for (RefactoringMatcher<AssignmentTree> matcher : assignmentMatchers) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      if (!isSuppressed(matcher.getName()) && matcher.matches(assignmentTree, state)) {
        reportMatch(matcher, assignmentTree, state);
      }
    }
    super.visitAssignment(assignmentTree, visitorState);
    return null;
  }
}
