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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.typeCast;
import static com.sun.source.tree.Tree.Kind.INT_LITERAL;
import static com.sun.source.tree.Tree.Kind.LONG_LITERAL;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.TypeCastTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "NegativeCharLiteral",
    summary = "Casting a negative signed literal to an (unsigned) char might be misleading.",
    severity = WARNING)
public class NegativeCharLiteral extends BugChecker implements TypeCastTreeMatcher {

  private static final Matcher<TypeCastTree> isIntegralLiteralCastToChar =
      typeCast(isSameType("char"), anyOf(kindIs(LONG_LITERAL), kindIs(INT_LITERAL)));

  @Override
  public Description matchTypeCast(TypeCastTree tree, VisitorState state) {
    if (!isIntegralLiteralCastToChar.matches(tree, state)) {
      return NO_MATCH;
    }

    long literalValue = ((Number) ((LiteralTree) tree.getExpression()).getValue()).longValue();
    if (literalValue >= 0) {
      return NO_MATCH;
    }

    char castResult = (char) literalValue;
    String replacement;
    if (castResult == Character.MAX_VALUE) {
      replacement = "Character.MAX_VALUE";
    } else {
      replacement = String.format("Character.MAX_VALUE - %s", Character.MAX_VALUE - castResult);
    }
    return describeMatch(tree, SuggestedFix.builder().replace(tree, replacement).build());
  }
}
