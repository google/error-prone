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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.flogger.FloggerHelpers.inferFormatSpecifier;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.ArrayList;
import java.util.List;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Prefer string formatting using printf placeholders (e.g. %s) instead of string"
            + " concatenation",
    severity = WARNING)
public class FloggerStringConcatenation extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      MethodMatchers.instanceMethod()
          .onDescendantOf("com.google.common.flogger.LoggingApi")
          .named("log")
          .withParameters("java.lang.String");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree argument = getOnlyElement(tree.getArguments());
    if (!(argument instanceof BinaryTree)) {
      return NO_MATCH;
    }
    if (constValue(argument, String.class) != null) {
      return NO_MATCH;
    }
    List<Tree> pieces = new ArrayList<>();
    argument.accept(
        new SimpleTreeVisitor<Void, Void>() {
          @Override
          public Void visitBinary(BinaryTree tree, Void unused) {
            if (tree.getKind().equals(Kind.PLUS)
                && isSameType(getType(tree), state.getSymtab().stringType, state)) {
              // + yielding a String is concatenation, and should use placeholders for each part.
              tree.getLeftOperand().accept(this, null);
              return tree.getRightOperand().accept(this, null);
            } else {
              // Otherwise it's not concatenation, and should be its own placeholder
              return defaultAction(tree, null);
            }
          }

          @Override
          public Void visitParenthesized(ParenthesizedTree node, Void unused) {
            return node.getExpression().accept(this, null);
          }

          @Override
          protected Void defaultAction(Tree tree, Void unused) {
            pieces.add(tree);
            return null;
          }
        },
        null);
    StringBuilder formatString = new StringBuilder();
    List<Tree> formatArguments = new ArrayList<>();
    for (Tree piece : pieces) {
      if (piece.getKind().equals(Kind.STRING_LITERAL)) {
        formatString.append((String) ((LiteralTree) piece).getValue());
      } else {
        formatString.append('%').append(inferFormatSpecifier(piece, state));
        formatArguments.add(piece);
      }
    }
    return describeMatch(
        tree,
        SuggestedFix.replace(
            argument,
            state.getConstantExpression(formatString)
                + ", "
                + formatArguments.stream().map(state::getSourceForNode).collect(joining(", "))));
  }
}
