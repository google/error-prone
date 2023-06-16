/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.hasDefinitelyNullBranch;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.varsProvenNullByParentTernary;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static javax.lang.model.element.ElementKind.PACKAGE;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.Name;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Dereference of an expression with a null branch", severity = ERROR)
public final class DereferenceWithNullBranch extends BugChecker implements MemberSelectTreeMatcher {
  private static final Supplier<Name> CLASS_KEYWORD = memoize(state -> state.getName("class"));

  @Override
  public Description matchMemberSelect(MemberSelectTree select, VisitorState state) {
    if (!memberSelectExpressionIsATrueExpression(select, state)) {
      return NO_MATCH;
    }

    if (!hasDefinitelyNullBranch(
        select.getExpression(),
        /*
         * TODO(cpovirk): Precompute sets of definitelyNullVars instead passing an empty set, and
         * include and varsProvenNullByParentIf alongside varsProvenNullByParentTernary.
         */
        ImmutableSet.of(),
        varsProvenNullByParentTernary(state.getPath()),
        state)) {
      return NO_MATCH;
    }

    return describeMatch(select);
  }

  private static boolean memberSelectExpressionIsATrueExpression(
      MemberSelectTree select, VisitorState state) {
    // We use the same logic here as we do in
    // https://github.com/jspecify/jspecify-reference-checker/blob/06e85b1eb79ecbb9aa6f5713bc759fb5cf402975/src/main/java/com/google/jspecify/nullness/NullSpecVisitor.java#L195-L206
    // (Here, we might not need the isInterface() check, but we keep it for consistency.)
    // I could also imagine checking for `getKind() != MODULE`, but it's been working without.

    if (select.getIdentifier().equals(CLASS_KEYWORD.get(state))) {
      return false;
    }

    Symbol symbol = getSymbol(select.getExpression());
    if (symbol == null) {
      return true;
    }
    return !symbol.getKind().isClass()
        && !symbol.getKind().isInterface()
        && symbol.getKind() != PACKAGE;
  }
}
