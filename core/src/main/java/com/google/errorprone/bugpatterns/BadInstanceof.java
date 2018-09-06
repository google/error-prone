/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.SIMPLIFICATION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.isNonNull;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.InstanceOfTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * Matches instanceof checks where the expression is a subtype of the checked type.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "BadInstanceof",
    summary = "instanceof used in a way that is equivalent to a null check.",
    severity = WARNING,
    tags = SIMPLIFICATION,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class BadInstanceof extends BugChecker implements InstanceOfTreeMatcher {

  private static final String NON_NULL =
      "`%s` is a non-null instance of %s which is a subtype of %s, so this check is always true.";

  private static final String NULLABLE =
      "`%s` is an instance of %s which is a subtype of %s, so this is equivalent to a null check.";

  @Override
  public Description matchInstanceOf(InstanceOfTree tree, VisitorState state) {
    if (!isSubtype(getType(tree.getExpression()), getType(tree.getType()), state)) {
      return NO_MATCH;
    }
    String subType =
        SuggestedFixes.prettyType(state, /* fix= */ null, getType(tree.getExpression()));
    String expression = state.getSourceForNode(tree.getExpression());
    String superType = state.getSourceForNode(tree.getType());
    if (isNonNull().matches(tree.getExpression(), state)) {
      return buildDescription(tree)
          .setMessage(String.format(NON_NULL, expression, subType, superType))
          .build();
    }
    return buildDescription(tree)
        .setMessage(String.format(NULLABLE, expression, subType, superType))
        .addFix(getFix(tree, state))
        .build();
  }

  private static SuggestedFix getFix(InstanceOfTree tree, VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    Tree grandParent = state.getPath().getParentPath().getParentPath().getLeaf();
    if (parent.getKind() == Kind.PARENTHESIZED
        && grandParent.getKind() == Kind.LOGICAL_COMPLEMENT) {
      return SuggestedFix.replace(
          grandParent, state.getSourceForNode(tree.getExpression()) + " == null");
    }
    return SuggestedFix.replace(tree, state.getSourceForNode(tree.getExpression()) + " != null");
  }
}
