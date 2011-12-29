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

import com.google.errorprone.checkers.DescribingMatcher.MatchDescription;
import com.google.errorprone.checkers.EmptyIfChecker;
import com.google.errorprone.checkers.DescribingMatcher;
import com.google.errorprone.checkers.dead_exception.DeadExceptionChecker;
import com.google.errorprone.checkers.FallThroughSuppressionChecker;
import com.google.errorprone.checkers.OrderingFromChecker;
import com.google.errorprone.checkers.PreconditionsCheckNotNullChecker;
import com.google.errorprone.checkers.PreconditionsCheckNotNullPrimitive1stArgChecker;
import com.google.errorprone.checkers.PreconditionsExpensiveStringChecker;
import com.google.errorprone.checkers.objects_equal_self_comparison.ObjectsEqualSelfComparisonChecker;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scans the parsed AST, looking for violations of any of the configured checks.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorProneScanner extends ErrorCollectingTreeScanner {

  private final Iterable<? extends DescribingMatcher<MethodInvocationTree>>
      methodInvocationCheckers = Arrays.asList(
          new ObjectsEqualSelfComparisonChecker(),
          new OrderingFromChecker(),
          new PreconditionsCheckNotNullChecker(),
          new PreconditionsExpensiveStringChecker(),
          new PreconditionsCheckNotNullPrimitive1stArgChecker());
  
  private final Iterable<? extends DescribingMatcher<NewClassTree>>
      newClassCheckers = Arrays.asList(
          new DeadExceptionChecker());

  private final Iterable<? extends DescribingMatcher<AnnotationTree>>
      annotationCheckers = Arrays.asList(
          new FallThroughSuppressionChecker());

  private final Iterable<? extends DescribingMatcher<EmptyStatementTree>>
      emptyStatementCheckers = Arrays.asList(
          new EmptyIfChecker());
  
  @Override
  public List<MatchDescription> visitMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    List<MatchDescription> result = new ArrayList<MatchDescription>();
    for (DescribingMatcher<MethodInvocationTree> checker : methodInvocationCheckers) {
      VisitorState newState = state.withPath(getCurrentPath());
      if (checker.matches(methodInvocationTree, newState)) {
         result.add(checker.describe(methodInvocationTree, newState));
      }
    }
    super.visitMethodInvocation(methodInvocationTree, state);
    return result;
  }

  @Override
  public List<MatchDescription> visitNewClass(NewClassTree newClassTree, VisitorState visitorState) {
    List<MatchDescription> result = new ArrayList<MatchDescription>();
    for (DescribingMatcher<NewClassTree> newClassChecker : newClassCheckers) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      if (newClassChecker.matches(newClassTree, state)) {
         result.add(newClassChecker.describe(newClassTree, state));
      }
    }
    super.visitNewClass(newClassTree, visitorState);
    return result;
  }

  @Override
  public List<MatchDescription> visitAnnotation(AnnotationTree annotationTree, VisitorState visitorState) {
    List<MatchDescription> result = new ArrayList<MatchDescription>();
    for (DescribingMatcher<AnnotationTree> annotationChecker : annotationCheckers) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      if (annotationChecker.matches(annotationTree, state)) {
         result.add(annotationChecker.describe(annotationTree, state));
      }
    }
    super.visitAnnotation(annotationTree, visitorState);
    return result;
  }
  
  @Override
  public List<MatchDescription> visitEmptyStatement(EmptyStatementTree emptyStatementTree,
      VisitorState visitorState) {
    List<MatchDescription> result = new ArrayList<MatchDescription>();
    for (DescribingMatcher<EmptyStatementTree> emptyStatementChecker : emptyStatementCheckers) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      if (emptyStatementChecker.matches(emptyStatementTree, state)) {
        result.add(emptyStatementChecker.describe(emptyStatementTree, state));
      }
    }
    super.visitEmptyStatement(emptyStatementTree, visitorState);
    return result;
  }
}
