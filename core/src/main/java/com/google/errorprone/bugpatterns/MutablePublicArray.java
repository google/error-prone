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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isArrayType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
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
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class MutablePublicArray extends BugChecker implements VariableTreeMatcher {

  private static final Matcher<VariableTree> NON_EMPTY_PUBLIC_STATIC_FINAL_ARRAY_DECLARATION =
      allOf(
          hasModifier(Modifier.PUBLIC),
          hasModifier(Modifier.STATIC),
          hasModifier(Modifier.FINAL),
          new NonEmptyArrayMatcher());

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (NON_EMPTY_PUBLIC_STATIC_FINAL_ARRAY_DECLARATION.matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  private static class NonEmptyArrayMatcher implements Matcher<VariableTree> {
    @Override
    public boolean matches(VariableTree arrayExpression, VisitorState state) {
      if (isArrayType().matches(arrayExpression, state)) {
        JCNewArray newArray = (JCNewArray) arrayExpression.getInitializer();
        if (!newArray.getDimensions().isEmpty()) {
          return newArray.getDimensions().stream()
              .allMatch(
                  jcExpression -> {
                    JCLiteral literal = (JCLiteral) jcExpression;
                    return literal.getKind() == Kind.INT_LITERAL
                        && (Integer) literal.getValue() > 0;
                  });
        } else {
          // For in line array initializer.
          return newArray.getInitializers() != null && !newArray.getInitializers().isEmpty();
        }
      }
      return false;
    }
  }
}
