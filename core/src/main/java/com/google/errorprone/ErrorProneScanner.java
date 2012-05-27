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
import com.google.errorprone.refactors.collectionIncompatibleType.CollectionIncompatibleType;
import com.google.errorprone.refactors.covariant_equals.CovariantEquals;
import com.google.errorprone.refactors.dead_exception.DeadException;
import com.google.errorprone.refactors.empty_if_statement.EmptyIfStatement;
import com.google.errorprone.refactors.emptystatement.EmptyStatement;
import com.google.errorprone.refactors.fallthroughsuppression.FallThroughSuppression;
import com.google.errorprone.refactors.objectsequalselfcomparison.ObjectsEqualSelfComparison;
import com.google.errorprone.refactors.orderingfrom.OrderingFrom;
import com.google.errorprone.refactors.preconditionschecknotnull.PreconditionsCheckNotNull;
import com.google.errorprone.refactors.preconditionschecknotnullprimitive1starg.PreconditionsCheckNotNullPrimitive1stArg;
import com.google.errorprone.refactors.preconditionsexpensivestring.PreconditionsExpensiveString;
import com.google.errorprone.refactors.selfassignment.SelfAssignment;
import com.sun.source.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.errorprone.BugPattern.MaturityLevel;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;

/**
 * Scans the parsed AST, looking for violations of any of the configured checks.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorProneScanner extends Scanner {

  @SuppressWarnings("unchecked")
  private final static Iterable<Class<? extends RefactoringMatcher<MethodInvocationTree>>>
      allMethodInvocationMatchers = Arrays.asList(
          ObjectsEqualSelfComparison.class,
          OrderingFrom.class,
          PreconditionsCheckNotNull.class,
          PreconditionsExpensiveString.class,
          PreconditionsCheckNotNullPrimitive1stArg.class,
          CollectionIncompatibleType.class,
          ObjectsEqualSelfComparison.class
  );

  @SuppressWarnings("unchecked")
  private final static Iterable<Class<? extends RefactoringMatcher<NewClassTree>>>
      allNewClassMatchers = Arrays.<Class<? extends RefactoringMatcher<NewClassTree>>>asList(
          DeadException.class);

  @SuppressWarnings("unchecked")
  private final static Iterable<Class<? extends RefactoringMatcher<AnnotationTree>>>
      allAnnotationMatchers = Arrays.<Class<? extends RefactoringMatcher<AnnotationTree>>>asList(
          FallThroughSuppression.class);

  @SuppressWarnings("unchecked")
  private final static Iterable<Class<? extends RefactoringMatcher<EmptyStatementTree>>>
      allEmptyStatementMatchers = Arrays.asList(
          EmptyIfStatement.class,
          EmptyStatement.class);

  @SuppressWarnings("unchecked")
  private final static Iterable<Class<? extends RefactoringMatcher<AssignmentTree>>>
      allAssignmentMatchers = Arrays.<Class<? extends RefactoringMatcher<AssignmentTree>>>asList(
          SelfAssignment.class);

  @SuppressWarnings("unchecked")
  private final static Iterable<Class<? extends RefactoringMatcher<MethodTree>>>
      allMethodMatchers = Arrays.<Class<? extends RefactoringMatcher<MethodTree>>>asList(
          CovariantEquals.class);

  public static ErrorProneScanner defaultChecks() {
    try {
      return new ErrorProneScanner(
          createChecks(ON_BY_DEFAULT, allMethodInvocationMatchers),
          createChecks(ON_BY_DEFAULT, allNewClassMatchers),
          createChecks(ON_BY_DEFAULT, allAnnotationMatchers),
          createChecks(ON_BY_DEFAULT, allEmptyStatementMatchers),
          createChecks(ON_BY_DEFAULT, allAssignmentMatchers),
          createChecks(ON_BY_DEFAULT, allMethodMatchers));
    } catch (Exception e) {
      throw new RuntimeException("Could not reflectively create error prone matchers", e);
    }
  }

  private static <T extends Tree> Iterable<RefactoringMatcher<T>> createChecks(
      MaturityLevel maturityLevel, Iterable<Class<? extends RefactoringMatcher<T>>> matchers)
      throws IllegalAccessException, InstantiationException {
    List<RefactoringMatcher<T>> result = new ArrayList<RefactoringMatcher<T>>();
    for (Class<? extends RefactoringMatcher<T>> matcher : matchers) {
      if (matcher.getAnnotation(BugPattern.class).maturity() == maturityLevel) {
        result.add(matcher.newInstance());
      }
    }
    return result;
  }

  private final Iterable<RefactoringMatcher<MethodInvocationTree>> methodInvocationTreeMatchers;
  private final Iterable<RefactoringMatcher<NewClassTree>> newClassTreeMatchers;
  private final Iterable<RefactoringMatcher<AnnotationTree>> annotationTreeMatchers;
  private final Iterable<RefactoringMatcher<EmptyStatementTree>> emptyStatementTreeMatchers;
  private final Iterable<RefactoringMatcher<AssignmentTree>> assignmentTreeMatchers;
  private final Iterable<RefactoringMatcher<MethodTree>> methodTreeMatchers;

  public ErrorProneScanner(Iterable<RefactoringMatcher<MethodInvocationTree>> methodInvocationTreeMatchers,
                           Iterable<RefactoringMatcher<NewClassTree>> newClassTreeMatchers,
                           Iterable<RefactoringMatcher<AnnotationTree>> annotationTreeMatchers,
                           Iterable<RefactoringMatcher<EmptyStatementTree>> emptyStatementTreeMatchers,
                           Iterable<RefactoringMatcher<AssignmentTree>> assignmentTreeMatchers,
                           Iterable<RefactoringMatcher<MethodTree>> methodTreeMatchers) {
    this.methodInvocationTreeMatchers = methodInvocationTreeMatchers;
    this.newClassTreeMatchers = newClassTreeMatchers;
    this.annotationTreeMatchers = annotationTreeMatchers;
    this.emptyStatementTreeMatchers = emptyStatementTreeMatchers;
    this.assignmentTreeMatchers = assignmentTreeMatchers;
    this.methodTreeMatchers = methodTreeMatchers;
  }

  @Override
  public Void visitMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    for (RefactoringMatcher<MethodInvocationTree> matcher : methodInvocationTreeMatchers) {
      evaluateMatch(methodInvocationTree, state, matcher);
    }
    return super.visitMethodInvocation(methodInvocationTree, state);
  }

  @Override
  public Void visitNewClass(NewClassTree newClassTree, VisitorState visitorState) {
    for (RefactoringMatcher<NewClassTree> matcher : newClassTreeMatchers) {
      evaluateMatch(newClassTree, visitorState, matcher);
    }
    return super.visitNewClass(newClassTree, visitorState);
  }

  @Override
  public Void visitAnnotation(AnnotationTree annotationTree, VisitorState visitorState) {
    for (RefactoringMatcher<AnnotationTree> matcher : annotationTreeMatchers) {
      evaluateMatch(annotationTree, visitorState, matcher);
    }
    return super.visitAnnotation(annotationTree, visitorState);
  }
  
  @Override
  public Void visitEmptyStatement(EmptyStatementTree emptyStatementTree, VisitorState visitorState) {
    for (RefactoringMatcher<EmptyStatementTree> matcher : emptyStatementTreeMatchers) {
      evaluateMatch(emptyStatementTree, visitorState, matcher);
    }
    return super.visitEmptyStatement(emptyStatementTree, visitorState);
  }
  
  @Override
  public Void visitAssignment(AssignmentTree assignmentTree, VisitorState visitorState) {
    for (RefactoringMatcher<AssignmentTree> matcher : assignmentTreeMatchers) {
      evaluateMatch(assignmentTree, visitorState, matcher);
    }
    return super.visitAssignment(assignmentTree, visitorState);
  }

  @Override
  public Void visitMethod(MethodTree node, VisitorState visitorState) {
    for (RefactoringMatcher<MethodTree> matcher : methodTreeMatchers) {
      evaluateMatch(node, visitorState, matcher);
    }
    return super.visitMethod(node, visitorState);
  }
}
