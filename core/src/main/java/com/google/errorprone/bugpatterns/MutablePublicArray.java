/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isArrayType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import javax.lang.model.element.Modifier;

/** Check for public static final declaration of Arrays. */
@BugPattern(
    name = "MutablePublicArray",
    summary =
        "Non-empty arrays are mutable, so this `public static final` array is not a constant"
            + " and can be modified by clients of this class.  Prefer an ImmutableList, or provide"
            + " an accessor method that returns a defensive copy.",
    severity = WARNING)
public class MutablePublicArray extends BugChecker implements VariableTreeMatcher {

  private static final Matcher<VariableTree> MATCHER =
      allOf(
          hasModifier(Modifier.PUBLIC),
          hasModifier(Modifier.STATIC),
          hasModifier(Modifier.FINAL),
          MutablePublicArray::nonEmptyArrayMatcher);

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    if (hasAnnotation(tree, "org.junit.experimental.theories.DataPoints", state)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  private static boolean nonEmptyArrayMatcher(VariableTree arrayExpression, VisitorState state) {
    if (!isArrayType().matches(arrayExpression, state)) {
      return false;
    }
    JCNewArray newArray = (JCNewArray) arrayExpression.getInitializer();
    if (newArray == null) {
      return false;
    }
    if (!newArray.getDimensions().isEmpty()) {
      return newArray.getDimensions().stream()
          .allMatch(
              jcExpression -> {
                JCLiteral literal = (JCLiteral) jcExpression;
                return literal.getKind() == Kind.INT_LITERAL && (Integer) literal.getValue() > 0;
              });
    }
    // For in line array initializer.
    return newArray.getInitializers() != null && !newArray.getInitializers().isEmpty();
  }
}
