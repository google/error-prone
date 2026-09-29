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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.enumValues;
import static com.google.errorprone.util.ASTHelpers.getAnnotationMirror;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.unboxedTypeOrType;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Explicitly specifying all values on a boolean or enum @TestParameter is unnecessary",
    severity = WARNING)
public final class SimplifyTestParameter extends BugChecker implements AnnotationTreeMatcher {

  private static final Supplier<Type> TEST_PARAMETER =
      typeFromString("com.google.testing.junit.testparameterinjector.TestParameter");

  private static final ImmutableSet<String> BOOLEAN_VALUES = ImmutableSet.of("true", "false");

  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    if (tree.getArguments().isEmpty()) {
      return NO_MATCH;
    }
    if (!isSameType(getType(tree), TEST_PARAMETER.get(state), state)) {
      return NO_MATCH;
    }
    AnnotationMirror attribute = getAnnotationMirror(tree);
    if (attribute.getElementValues().size() != 1) {
      return NO_MATCH;
    }
    ImmutableList<String> values = MoreAnnotations.getStrings(attribute, "value");
    if (values.isEmpty()) {
      return NO_MATCH;
    }
    VariableTree variableTree = state.findEnclosing(VariableTree.class);
    if (variableTree == null) {
      return NO_MATCH;
    }
    Type type = getType(variableTree);
    if (isSameType(unboxedTypeOrType(type, state), state.getSymtab().booleanType, state)
        && values.size() == 2
        && values.stream()
            .map(Ascii::toLowerCase)
            .collect(toImmutableSet())
            .equals(BOOLEAN_VALUES)) {
      return describe(tree, state);
    }
    if (type.tsym != null && type.tsym.isEnum()) {
      Set<String> enumValues = enumValues(type.tsym);
      if (values.size() == enumValues.size() && ImmutableSet.copyOf(values).equals(enumValues)) {
        return describe(tree, state);
      }
    }
    return NO_MATCH;
  }

  private Description describe(AnnotationTree tree, VisitorState state) {
    return describeMatch(
        tree, SuggestedFix.replace(tree, "@" + state.getSourceForNode(tree.getAnnotationType())));
  }
}
