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
import java.util.List;

import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;

/**
 * Scans the parsed AST, looking for violations of any of the enabled checks.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorProneScanner extends Scanner {

  /**
   * Selects which checks should be enabled when the compile is run.
   */
  public interface EnabledPredicate {
    boolean isEnabled(Class<? extends RefactoringMatcher<?>> check, BugPattern annotation);

    /**
     * Selects all checks which are annotated with maturity = ON_BY_DEFAULT.
     */
    public static final EnabledPredicate DEFAULT_CHECKS = new EnabledPredicate() {
      @Override public boolean isEnabled(Class<? extends RefactoringMatcher<?>> check, BugPattern annotation) {
        return annotation.maturity() == ON_BY_DEFAULT;
      }
    };
  }

  private final Iterable<RefactoringMatcher<MethodInvocationTree>> methodInvocationMatchers;
  private final Iterable<RefactoringMatcher<NewClassTree>> newClassMatchers;
  private final Iterable<RefactoringMatcher<AnnotationTree>> annotationMatchers;
  private final Iterable<RefactoringMatcher<EmptyStatementTree>> emptyStatementMatchers;
  private final Iterable<RefactoringMatcher<AssignmentTree>> assignmentMatchers;
  private final Iterable<RefactoringMatcher<MethodTree>> methodMatchers;

  @SuppressWarnings("unchecked")
  public ErrorProneScanner(EnabledPredicate enabled) {
    try {
      this.methodInvocationMatchers = createChecks(enabled,
          ObjectsEqualSelfComparison.class,
          OrderingFrom.class,
          PreconditionsCheckNotNull.class,
          PreconditionsExpensiveString.class,
          PreconditionsCheckNotNullPrimitive1stArg.class,
          CollectionIncompatibleType.class,
          ObjectsEqualSelfComparison.class
      );
      this.newClassMatchers = createChecks(enabled, DeadException.class);
      this.annotationMatchers = createChecks(enabled, FallThroughSuppression.class);
      this.emptyStatementMatchers = createChecks(enabled,
          EmptyIfStatement.class,
          EmptyStatement.class
      );
      this.assignmentMatchers = createChecks(enabled, SelfAssignment.class);
      this.methodMatchers = createChecks(enabled, CovariantEquals.class);
    } catch (Exception e) {
      throw new RuntimeException("Could not reflectively create error prone matchers", e);
    }
  }

  private static <T extends Tree> Iterable<RefactoringMatcher<T>> createChecks(
      EnabledPredicate predicate, Class<? extends RefactoringMatcher<T>>... matchers)
      throws IllegalAccessException, InstantiationException {
    List<RefactoringMatcher<T>> result = new ArrayList<RefactoringMatcher<T>>();
    for (Class<? extends RefactoringMatcher<T>> matcher : matchers) {
      if (predicate.isEnabled(matcher, matcher.getAnnotation(BugPattern.class))) {
        result.add(matcher.newInstance());
      }
    }
    return result;
  }

  @Override
  public Void visitMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    for (RefactoringMatcher<MethodInvocationTree> matcher : methodInvocationMatchers) {
      evaluateMatch(methodInvocationTree, state, matcher);
    }
    return super.visitMethodInvocation(methodInvocationTree, state);
  }

  @Override
  public Void visitNewClass(NewClassTree newClassTree, VisitorState visitorState) {
    for (RefactoringMatcher<NewClassTree> matcher : newClassMatchers) {
      evaluateMatch(newClassTree, visitorState, matcher);
    }
    return super.visitNewClass(newClassTree, visitorState);
  }

  @Override
  public Void visitAnnotation(AnnotationTree annotationTree, VisitorState visitorState) {
    for (RefactoringMatcher<AnnotationTree> matcher : annotationMatchers) {
      evaluateMatch(annotationTree, visitorState, matcher);
    }
    return super.visitAnnotation(annotationTree, visitorState);
  }
  
  @Override
  public Void visitEmptyStatement(EmptyStatementTree emptyStatementTree, VisitorState visitorState) {
    for (RefactoringMatcher<EmptyStatementTree> matcher : emptyStatementMatchers) {
      evaluateMatch(emptyStatementTree, visitorState, matcher);
    }
    return super.visitEmptyStatement(emptyStatementTree, visitorState);
  }
  
  @Override
  public Void visitAssignment(AssignmentTree assignmentTree, VisitorState visitorState) {
    for (RefactoringMatcher<AssignmentTree> matcher : assignmentMatchers) {
      evaluateMatch(assignmentTree, visitorState, matcher);
    }
    return super.visitAssignment(assignmentTree, visitorState);
  }

  @Override
  public Void visitMethod(MethodTree node, VisitorState visitorState) {
    for (RefactoringMatcher<MethodTree> matcher : methodMatchers) {
      evaluateMatch(node, visitorState, matcher);
    }
    return super.visitMethod(node, visitorState);
  }
}
