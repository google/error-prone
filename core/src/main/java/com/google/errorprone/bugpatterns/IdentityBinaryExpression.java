/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.Optional;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "IdentityBinaryExpression",
    altNames = "SelfEquality",
    category = JDK,
    summary = "A binary expression where both operands are the same is usually incorrect.",
    severity = ERROR)
public class IdentityBinaryExpression extends BugChecker implements BinaryTreeMatcher {

  private static final Matcher<Tree> ASSERTION =
      toType(
          ExpressionTree.class,
          staticMethod().anyClass().namedAnyOf("assertTrue", "assertFalse", "assertThat"));

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (constValue(tree.getLeftOperand()) != null) {
      switch (tree.getKind()) {
        case LEFT_SHIFT: // bit field initialization, e.g. `1 << 1`, `1 << 2`, ...
        case DIVIDE: // aspect ratios, e.g. `1.0f / 1.0f`, `2.0f / 3.0f`, ...
        case MINUS: // character arithmetic, e.g. `'A' - 'A'`, `'B' - 'A'`, ...
          return NO_MATCH;
        default: // fall out
      }
    }
    String replacement;
    switch (tree.getKind()) {
      case DIVIDE:
        replacement = "1";
        break;
      case MINUS:
      case REMAINDER:
        replacement = "0";
        break;
      case GREATER_THAN_EQUAL:
      case LESS_THAN_EQUAL:
      case EQUAL_TO:
        if (ASSERTION.matches(state.getPath().getParentPath().getLeaf(), state)) {
          return NO_MATCH;
        }
        replacement = "true";
        break;
      case LESS_THAN:
      case GREATER_THAN:
      case NOT_EQUAL_TO:
      case XOR:
        if (ASSERTION.matches(state.getPath().getParentPath().getLeaf(), state)) {
          return NO_MATCH;
        }
        replacement = "false";
        break;
      case AND:
      case OR:
      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
        replacement = state.getSourceForNode(tree.getLeftOperand());
        break;
      case LEFT_SHIFT:
      case RIGHT_SHIFT:
      case UNSIGNED_RIGHT_SHIFT:
        replacement = null; // ¯\_(ツ)_/¯
        break;
      case MULTIPLY:
      case PLUS:
      default:
        return NO_MATCH;
    }
    if (!tree.getLeftOperand().toString().equals(tree.getRightOperand().toString())) {
      return NO_MATCH;
    }
    switch (tree.getKind()) {
      case EQUAL_TO:
        replacement = isNanReplacement(tree, state).orElse(replacement);
        break;
      case NOT_EQUAL_TO:
        replacement = isNanReplacement(tree, state).map(r -> "!" + r).orElse(replacement);
        break;
      default: // fall out
    }
    Description.Builder description = buildDescription(tree);
    if (replacement != null) {
      description.setMessage(
          String.format(
              "A binary expression where both operands are the same is usually incorrect;"
                  + " the value of this expression is equivalent to `%s`.",
              replacement));
    }
    return description.build();
  }

  private static Optional<String> isNanReplacement(BinaryTree tree, VisitorState state) {
    Types types = state.getTypes();
    Symtab symtab = state.getSymtab();
    Type type = getType(tree.getLeftOperand());
    if (type == null) {
      return Optional.empty();
    }
    type = types.unboxedTypeOrType(type);
    String name;
    if (isSameType(type, symtab.floatType, state)) {
      name = "Float";
    } else if (isSameType(type, symtab.doubleType, state)) {
      name = "Double";
    } else {
      return Optional.empty();
    }
    return Optional.of(
        String.format("%s.isNaN(%s)", name, state.getSourceForNode(tree.getLeftOperand())));
  }
}
