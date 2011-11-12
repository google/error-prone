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

import com.google.errorprone.checkers.DeadExceptionChecker;
import com.google.errorprone.checkers.ErrorChecker;
import com.google.errorprone.checkers.ErrorChecker.AstError;
import com.google.errorprone.checkers.FallThroughSuppressionChecker;
import com.google.errorprone.checkers.PreconditionsCheckNotNullChecker;
import com.google.errorprone.checkers.PreconditionsCheckNotNullPrimitive1stArgChecker;
import com.google.errorprone.checkers.PreconditionsExpensiveStringChecker;

import com.sun.source.tree.AnnotationTree;
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

  private final Iterable<? extends ErrorChecker<MethodInvocationTree>>
      methodInvocationCheckers = Arrays.asList(
          new PreconditionsCheckNotNullChecker(),
          new PreconditionsExpensiveStringChecker(),
          new PreconditionsCheckNotNullPrimitive1stArgChecker());
  
  private final Iterable<? extends ErrorChecker<NewClassTree>>
      newClassCheckers = Arrays.asList(new DeadExceptionChecker());

  private final Iterable<? extends ErrorChecker<AnnotationTree>>
      annotationCheckers = Arrays.asList(
          new FallThroughSuppressionChecker());

  @Override
  public List<AstError> visitMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    List<AstError> result = new ArrayList<AstError>();
    for (ErrorChecker<MethodInvocationTree> checker : methodInvocationCheckers) {
      AstError error = checker.check(methodInvocationTree, state.withPath(getCurrentPath()));
      if (error != null) {
        result.add(error);
      }
    }
    super.visitMethodInvocation(methodInvocationTree, state);
    return result;
  }

  @Override
  public List<AstError> visitNewClass(NewClassTree newClassTree, VisitorState visitorState) {
    List<AstError> result = new ArrayList<AstError>();
    for (ErrorChecker<NewClassTree> newClassChecker : newClassCheckers) {
      AstError error = newClassChecker
          .check(newClassTree, visitorState.withPath(getCurrentPath()));
      if (error != null) {
        result.add(error);
      }
    }
    super.visitNewClass(newClassTree, visitorState);
    return result;
  }

  @Override
  public List<AstError> visitAnnotation(AnnotationTree annotationTree, VisitorState visitorState) {
    List<AstError> result = new ArrayList<AstError>();
    for (ErrorChecker<AnnotationTree> annotationChecker : annotationCheckers) {
      AstError error = annotationChecker
          .check(annotationTree, visitorState.withPath(getCurrentPath()));
      if (error != null) {
        result.add(error);
      }
    }
    super.visitAnnotation(annotationTree, visitorState);
    return result;
  }
}
