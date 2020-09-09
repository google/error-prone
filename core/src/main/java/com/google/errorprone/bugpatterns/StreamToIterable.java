/*
 * Copyright 2020 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.FRAGILE_CODE;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyStaticImport;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.LambdaExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LambdaExpressionTree.BodyKind;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;

/** Discourage {@code stream::iterator} to create {@link Iterable}s. */
@BugPattern(
    name = "StreamToIterable",
    summary =
        "Using stream::iterator creates a one-shot Iterable, which may cause surprising failures.",
    severity = WARNING,
    tags = FRAGILE_CODE,
    documentSuppression = false)
public final class StreamToIterable extends BugChecker
    implements LambdaExpressionTreeMatcher, MemberReferenceTreeMatcher {
  private static final Matcher<ExpressionTree> STREAM_ITERATOR =
      instanceMethod().onDescendantOf("java.util.stream.Stream").named("iterator");

  @Override
  public Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state) {
    if (!isSameType(getType(tree), state.getSymtab().iterableType, state)) {
      return NO_MATCH;
    }
    if (!tree.getBodyKind().equals(BodyKind.EXPRESSION)) {
      return NO_MATCH;
    }
    ExpressionTree body = (ExpressionTree) tree.getBody();
    if (!STREAM_ITERATOR.matches(body, state)) {
      return NO_MATCH;
    }
    // Only match variables, which we can be sure aren't being re-created.
    if (!(getSymbol(getReceiver(body)) instanceof VarSymbol)) {
      return NO_MATCH;
    }
    return describeMatch(tree, body, state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    if (!STREAM_ITERATOR.matches(tree, state)) {
      return NO_MATCH;
    }
    if (!isSameType(getType(tree), state.getSymtab().iterableType, state)) {
      return NO_MATCH;
    }

    return describeMatch(tree, tree, state);
  }

  private Description describeMatch(
      ExpressionTree tree, ExpressionTree invocation, VisitorState state) {
    if (state.getPath().getParentPath().getLeaf() instanceof TypeCastTree
        && state.getPath().getParentPath().getParentPath().getLeaf()
            instanceof EnhancedForLoopTree) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix =
        SuggestedFix.builder()
            .setShortDescription(
                "Collect to an ImmutableList (caveat: this materializes the contents into memory"
                    + " at once)");
    fix.replace(
        tree,
        String.format(
            "%s.collect(%s())",
            state.getSourceForNode(getReceiver(invocation)),
            qualifyStaticImport(
                "com.google.common.collect.ImmutableList.toImmutableList", fix, state)));
    return describeMatch(tree, fix.build());
  }
}
