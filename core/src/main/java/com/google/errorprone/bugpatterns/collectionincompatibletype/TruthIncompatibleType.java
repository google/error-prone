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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.TypeCompatibilityUtils;
import com.google.errorprone.bugpatterns.TypeCompatibilityUtils.TypeCompatibilityReport;
import com.google.errorprone.bugpatterns.collectionincompatibletype.AbstractCollectionIncompatibleTypeMatcher.MatchResult;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Type;
import javax.annotation.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "TruthIncompatibleType",
    summary = "Argument is not compatible with the subject's type.",
    severity = WARNING)
public class TruthIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {

  private static final AbstractCollectionIncompatibleTypeMatcher MATCHER =
      new AbstractCollectionIncompatibleTypeMatcher() {

        // TODO(cushon): expand to other assertThat methods to handle e.g. ListSubject
        private final Matcher<ExpressionTree> assertThatObject =
            MethodMatchers.staticMethod()
                .onClass("com.google.common.truth.Truth")
                .named("assertThat")
                .withParameters("java.lang.Object");

        public final Matcher<ExpressionTree> isEqualTo =
            MethodMatchers.instanceMethod()
                // TODO(cpovirk): Extend to subclasses, ignoring any with unusual behavior.
                .onExactClass("com.google.common.truth.Subject")
                .named("isEqualTo")
                .withParameters("java.lang.Object");

        @Override
        Matcher<ExpressionTree> methodMatcher() {
          return isEqualTo;
        }

        @Nullable
        @Override
        Type extractSourceType(MethodInvocationTree tree, VisitorState state) {
          return ASTHelpers.getType(getOnlyElement(tree.getArguments()));
        }

        @Nullable
        @Override
        Type extractSourceType(MemberReferenceTree tree, VisitorState state) {
          throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        ExpressionTree extractSourceTree(MethodInvocationTree tree, VisitorState state) {
          return getOnlyElement(tree.getArguments());
        }

        @Nullable
        @Override
        ExpressionTree extractSourceTree(MemberReferenceTree tree, VisitorState state) {
          throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        Type extractTargetType(MethodInvocationTree tree, VisitorState state) {
          for (ExpressionTree curr = tree;
              curr instanceof MethodInvocationTree;
              curr = ASTHelpers.getReceiver(curr)) {
            if (assertThatObject.matches(curr, state)) {
              return ASTHelpers.getType(
                  Iterables.getOnlyElement(((MethodInvocationTree) curr).getArguments())
                      .accept(
                          new SimpleTreeVisitor<Tree, Void>() {
                            @Override
                            protected Tree defaultAction(Tree node, Void unused) {
                              return node;
                            }

                            @Override
                            public Tree visitTypeCast(TypeCastTree node, Void unused) {
                              return node.getExpression().accept(this, null);
                            }

                            @Override
                            public Tree visitParenthesized(ParenthesizedTree node, Void unused) {
                              return node.getExpression().accept(this, null);
                            }
                          },
                          null));
            }
          }
          return null;
        }

        @Nullable
        @Override
        Type extractTargetType(MemberReferenceTree tree, VisitorState state) {
          throw new UnsupportedOperationException();
        }
      };

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MatchResult result = MATCHER.matches(tree, state);
    if (result == null) {
      return Description.NO_MATCH;
    }
    TypeCompatibilityReport compatibilityReport =
        TypeCompatibilityUtils.compatibilityOfTypes(
            result.targetType(), result.sourceType(), state);
    if (compatibilityReport.compatible()) {
      return Description.NO_MATCH;
    }
    String sourceType = Signatures.prettyType(result.sourceType());
    String targetType = Signatures.prettyType(result.targetType());
    if (sourceType.equals(targetType)) {
      sourceType = result.sourceType().toString();
      targetType = result.targetType().toString();
    }
    Description.Builder description = buildDescription(tree);
    description.setMessage(result.message(sourceType, targetType));
    return description.build();
  }
}
