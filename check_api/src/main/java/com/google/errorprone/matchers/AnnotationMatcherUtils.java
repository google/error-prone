/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

/**
 * Utilities for matching annotations.
 *
 * @author mwacker@google.com (Mike Wacker)
 */
public class AnnotationMatcherUtils {

  /**
   * Gets the value for an argument, or null if the argument does not exist.
   *
   * @param annotationTree the AST node for the annotation
   * @param name the name of the argument whose value to get
   * @return the value of the argument, or null if the argument does not exist
   */
  public static ExpressionTree getArgument(AnnotationTree annotationTree, String name) {
    for (ExpressionTree argumentTree : annotationTree.getArguments()) {
      if (argumentTree.getKind() != Tree.Kind.ASSIGNMENT) {
        continue;
      }
      AssignmentTree assignmentTree = (AssignmentTree) argumentTree;
      if (!assignmentTree.getVariable().toString().equals(name)) {
        continue;
      }
      ExpressionTree expressionTree = assignmentTree.getExpression();
      return expressionTree;
    }
    return null;
  }

  // Static class.
  private AnnotationMatcherUtils() {}
}
