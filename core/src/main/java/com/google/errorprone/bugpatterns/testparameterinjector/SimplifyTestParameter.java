/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.testparameterinjector;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.getExplicitTestParameterValues;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.implicitTestParameterAnnotation;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.isBooleanType;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.isEnumType;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.isExhaustiveBooleanValues;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.enumValues;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.Set;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Explicitly specifying all values on a boolean or enum @TestParameter is unnecessary",
    severity = WARNING)
public final class SimplifyTestParameter extends BugChecker implements AnnotationTreeMatcher {

  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    ImmutableList<String> values = getExplicitTestParameterValues(tree, state);
    if (values.isEmpty()) {
      return NO_MATCH;
    }
    VariableTree variableTree = state.findEnclosing(VariableTree.class);
    if (variableTree == null) {
      return NO_MATCH;
    }
    Type type = getType(variableTree);
    if (isBooleanType(type, state) && isExhaustiveBooleanValues(values)) {
      return describe(tree, state);
    }
    if (isEnumType(type)) {
      Set<String> enumValues = enumValues(type.tsym);
      if (values.size() == enumValues.size() && ImmutableSet.copyOf(values).equals(enumValues)) {
        return describe(tree, state);
      }
    }
    return NO_MATCH;
  }

  private Description describe(AnnotationTree tree, VisitorState state) {
    return describeMatch(
        tree, SuggestedFix.replace(tree, implicitTestParameterAnnotation(tree, state)));
  }
}
