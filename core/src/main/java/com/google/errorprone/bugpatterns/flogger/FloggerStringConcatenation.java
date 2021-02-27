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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getType;
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
    name = "FloggerStringConcatenation",
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
            checkState(tree.getKind().equals(Kind.PLUS));
            tree.getLeftOperand().accept(this, null);
            tree.getRightOperand().accept(this, null);
            return null;
          }

          @Override
          public Void visitParenthesized(ParenthesizedTree node, Void unused) {
            node.getExpression().accept(this, null);
            return null;
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
        formatString.append(formatSpecifier(piece));
        formatArguments.add(piece);
      }
    }
    return describeMatch(
        tree,
        SuggestedFix.replace(
            argument,
            state.getConstantExpression(formatString.toString())
                + ", "
                + formatArguments.stream().map(state::getSourceForNode).collect(joining(", "))));
  }

  private static String formatSpecifier(Tree piece) {
    switch (getType(piece).getKind()) {
      case INT:
      case LONG:
        return "%d";
      case FLOAT:
      case DOUBLE:
        return "%g";
      case BOOLEAN:
        // %b is identical to %s in Flogger, but not in String.format, so it might be risky
        // to train people to prefer it. (In format() a Boolean "null" becomes "false",)
        return "%s";
      default:
        return "%s";
    }
  }
}
