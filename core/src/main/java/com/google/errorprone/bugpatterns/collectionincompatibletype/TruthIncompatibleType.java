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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.TypeCompatibilityUtils;
import com.google.errorprone.bugpatterns.TypeCompatibilityUtils.TypeCompatibilityReport;
import com.google.errorprone.bugpatterns.collectionincompatibletype.AbstractCollectionIncompatibleTypeMatcher.MatchResult;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import javax.annotation.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "TruthIncompatibleType",
    summary = "Argument is not compatible with the subject's type.",
    severity = WARNING)
public class TruthIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {

  // TODO(cushon): expand to other assertThat methods to handle e.g. ListSubject
  private static final Matcher<ExpressionTree> START_OF_ASSERTION =
      allOf(
          anyOf(
              staticMethod().onClass("com.google.common.truth.Truth").named("assertThat"),
              instanceMethod()
                  .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
                  .named("that")),
          not(TruthIncompatibleType::isSpecialCasedSubject));

  public static final Matcher<ExpressionTree> IS_EQUAL_TO =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.Subject")
          .namedAnyOf("isEqualTo", "isNotEqualTo");

  private static final AbstractCollectionIncompatibleTypeMatcher MATCHER =
      new AbstractCollectionIncompatibleTypeMatcher() {

        @Override
        Matcher<ExpressionTree> methodMatcher() {
          return IS_EQUAL_TO;
        }

        @Nullable
        @Override
        Type extractSourceType(MethodInvocationTree tree, VisitorState state) {
          return getType(getOnlyElement(tree.getArguments()));
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
          ExpressionTree receiver = getReceiver(tree);
          if (!START_OF_ASSERTION.matches(receiver, state)) {
            return null;
          }
          Tree argument =
              getOnlyElement(((MethodInvocationTree) receiver).getArguments())
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
                      null);
          return getType(argument);
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
      return NO_MATCH;
    }
    TypeCompatibilityReport compatibilityReport =
        TypeCompatibilityUtils.compatibilityOfTypes(
            result.targetType(), result.sourceType(), state);
    if (compatibilityReport.compatible()
        || (isNumericType(result.sourceType(), state)
            && isNumericType(result.targetType(), state))) {
      return NO_MATCH;
    }
    String sourceType = Signatures.prettyType(result.sourceType());
    String targetType = Signatures.prettyType(result.targetType());
    if (sourceType.equals(targetType)) {
      sourceType = result.sourceType().toString();
      targetType = result.targetType().toString();
    }
    return buildDescription(tree).setMessage(result.message(sourceType, targetType)).build();
  }

  private static boolean isSpecialCasedSubject(ExpressionTree tree, VisitorState state) {
    if (!(tree instanceof MethodInvocationTree)) {
      return false;
    }
    MethodSymbol symbol = getSymbol((MethodInvocationTree) tree);
    VarSymbol parameter = symbol.params().get(0);
    return isNumericType(parameter.type, state);
  }

  private static boolean isNumericType(Type parameter, VisitorState state) {
    return parameter.isNumeric()
        || isSubtype(parameter, state.getTypeFromString("java.lang.Number"), state);
  }
}
