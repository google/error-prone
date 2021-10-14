/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.fixByPrefixingWithNullableAnnotation;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "EqualsMissingNullable",
    summary = "Method overrides Object.equals but does not have @Nullable on its parameter",
    severity = SUGGESTION)
public class EqualsMissingNullable extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (!equalsMethodDeclaration().matches(methodTree, state)) {
      return NO_MATCH;
    }

    VariableTree parameterTree = getOnlyElement(methodTree.getParameters());
    VarSymbol parameter = getSymbol(parameterTree);
    if (NullnessAnnotations.fromAnnotationsOn(parameter).orElse(null) == Nullness.NULLABLE) {
      return NO_MATCH;
    }

    return describeMatch(parameterTree, fixByPrefixingWithNullableAnnotation(state, parameterTree));
  }
}
