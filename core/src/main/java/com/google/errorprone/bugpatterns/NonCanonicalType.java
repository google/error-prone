/*
 * Copyright 2019 The Error Prone Authors.
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
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.bugpatterns.StaticImports.StaticImportInfo;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;

/** Flags types being referred to by their non-canonical name. */
@BugPattern(
    name = "NonCanonicalType",
    summary = "This type is referred to by a non-canonical name, which may be misleading.",
    severity = WARNING)
public final class NonCanonicalType extends BugChecker implements MemberSelectTreeMatcher {
  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    // Only match on the outermost member select.
    if (state.getPath().getParentPath().getLeaf() instanceof MemberSelectTree) {
      return NO_MATCH;
    }
    StaticImportInfo importInfo = StaticImports.tryCreate(tree, state);
    if (importInfo == null || !importInfo.members().isEmpty()) {
      return NO_MATCH;
    }
    // Skip generated code. There are some noisy cases in AutoValue.
    if (importInfo.canonicalName().contains("$")) {
      return NO_MATCH;
    }
    String nonCanonicalName = getNonCanonicalName(tree);
    if (importInfo.canonicalName().equals(nonCanonicalName)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    SuggestedFix fix =
        fixBuilder
            .replace(tree, qualifyType(state, fixBuilder, importInfo.canonicalName()))
            .build();
    return describeMatch(tree, fix);
  }

  /**
   * Find the non-canonical name which is being used to refer to this type. We can't just use {@code
   * getSymbol}, given that points to the same symbol as the canonical name.
   */
  private static String getNonCanonicalName(ExpressionTree tree) {
    switch (tree.getKind()) {
      case IDENTIFIER:
        return getSymbol(tree).getQualifiedName().toString();
      case MEMBER_SELECT:
        MemberSelectTree memberSelectTree = (MemberSelectTree) tree;
        Symbol expressionSymbol = getSymbol(memberSelectTree.getExpression());
        if (!(expressionSymbol instanceof TypeSymbol)) {
          return getSymbol(tree).getQualifiedName().toString();
        }
        return getNonCanonicalName(memberSelectTree.getExpression())
            + "."
            + memberSelectTree.getIdentifier();
      default:
        throw new AssertionError();
    }
  }
}
