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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.tools.javac.code.Type;
import java.util.Comparator;
import org.jspecify.annotations.Nullable;

/** Identifies Comparators that are unnecessarily boxing the comparison key. */
@BugPattern(
    summary = "Comparator.comparing unnecessarily boxes numerical primitives",
    severity = WARNING)
public final class BoxingComparator extends BugChecker implements MethodInvocationTreeMatcher {
  private static final String COMPARATOR = Comparator.class.getCanonicalName();
  private static final Matcher<ExpressionTree> COMPARING =
      staticMethod().onClass(COMPARATOR).named("comparing");
  private static final Matcher<ExpressionTree> THEN_COMPARING =
      instanceMethod()
          .onDescendantOf(COMPARATOR)
          .named("thenComparing")
          .withParameters("java.util.function.Function");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    boolean isComparing = COMPARING.matches(tree, state);
    boolean isThenComparing = THEN_COMPARING.matches(tree, state);
    if (!isComparing && !isThenComparing) {
      return Description.NO_MATCH;
    }

    if (!tree.getTypeArguments().isEmpty()) {
      return Description.NO_MATCH;
    }

    if (tree.getArguments().size() != 1) {
      return Description.NO_MATCH;
    }

    Type resultType = getResultType(getOnlyElement(tree.getArguments()));
    if (resultType == null) {
      return Description.NO_MATCH;
    }

    String suffix = getSuffix(resultType);
    if (suffix == null) {
      return Description.NO_MATCH;
    }

    String newMethodName = ASTHelpers.getSymbol(tree).getSimpleName() + suffix;
    SuggestedFix fix = SuggestedFixes.renameMethodInvocation(tree, newMethodName, state);
    if (isComparing && tree.getMethodSelect() instanceof IdentifierTree) {
      fix = fix.toBuilder().addStaticImport(COMPARATOR + "." + newMethodName).build();
    }
    return describeMatch(tree, fix);
  }

  private static @Nullable Type getResultType(ExpressionTree keyExtractor) {
    return switch (ASTHelpers.stripParentheses(keyExtractor)) {
      case MemberReferenceTree memberRef -> ASTHelpers.getSymbol(memberRef).getReturnType();
      case LambdaExpressionTree lambda ->
          switch (lambda.getBody()) {
            case ExpressionTree expr -> ASTHelpers.getType(expr);
            case BlockTree block
                when block.getStatements().size() == 1
                    && getOnlyElement(block.getStatements()) instanceof ReturnTree ret ->
                ASTHelpers.getType(ret.getExpression());
            default -> null;
          };
      default -> null;
    };
  }

  private static @Nullable String getSuffix(Type type) {
    // Note that byte, short, and char are widened to int, and float is widened to double,
    // matching the available JDK primitive comparators (e.g., comparingInt, comparingDouble).
    return switch (type.getKind()) {
      case INT, BYTE, SHORT, CHAR -> "Int";
      case LONG -> "Long";
      case DOUBLE, FLOAT -> "Double";
      default -> null;
    };
  }
}
