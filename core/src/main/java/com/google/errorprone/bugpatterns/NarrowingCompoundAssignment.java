/*
 * Copyright 2014 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.Signatures.prettyType;

import com.google.common.base.Optional;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompoundAssignmentTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.OperatorPrecedence;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Compound assignments may hide dangerous casts",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public class NarrowingCompoundAssignment extends BugChecker
    implements CompoundAssignmentTreeMatcher {

  static String assignmentToString(Tree.Kind kind) {
    switch (kind) {
      case MULTIPLY:
        return "*";
      case DIVIDE:
        return "/";
      case REMAINDER:
        return "%";
      case PLUS:
        return "+";
      case MINUS:
        return "-";
      case LEFT_SHIFT:
        return "<<";
      case AND:
        return "&";
      case XOR:
        return "^";
      case OR:
        return "|";
      case RIGHT_SHIFT:
        return ">>";
      case UNSIGNED_RIGHT_SHIFT:
        return ">>>";
      default:
        throw new IllegalArgumentException("Unexpected operator assignment kind: " + kind);
    }
  }

  static Kind regularAssignmentFromCompound(Kind kind) {
    switch (kind) {
      case MULTIPLY_ASSIGNMENT:
        return Kind.MULTIPLY;
      case DIVIDE_ASSIGNMENT:
        return Kind.DIVIDE;
      case REMAINDER_ASSIGNMENT:
        return Kind.REMAINDER;
      case PLUS_ASSIGNMENT:
        return Kind.PLUS;
      case MINUS_ASSIGNMENT:
        return Kind.MINUS;
      case LEFT_SHIFT_ASSIGNMENT:
        return Kind.LEFT_SHIFT;
      case AND_ASSIGNMENT:
        return Kind.AND;
      case XOR_ASSIGNMENT:
        return Kind.XOR;
      case OR_ASSIGNMENT:
        return Kind.OR;
      case RIGHT_SHIFT_ASSIGNMENT:
        return Kind.RIGHT_SHIFT;
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
        return Kind.UNSIGNED_RIGHT_SHIFT;
      default:
        throw new IllegalArgumentException("Unexpected compound assignment kind: " + kind);
    }
  }

  @Override
  public Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state) {
    String message =
        identifyBadCast(
            getType(tree.getVariable()), getType(tree.getExpression()), state.getTypes());
    if (message == null) {
      return Description.NO_MATCH;
    }
    Optional<Fix> fix = rewriteCompoundAssignment(tree, state);
    if (!fix.isPresent()) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree).addFix(fix.get()).setMessage(message).build();
  }

  /** Classifies bad casts. */
  private static @Nullable String identifyBadCast(Type lhs, Type rhs, Types types) {
    if (!lhs.isPrimitive()) {
      return null;
    }
    if (types.isConvertible(rhs, lhs)) {
      // Exemption if the rhs is convertible to the lhs.
      // This allows, e.g.: <byte> &= <byte> since the narrowing conversion can never be
      // detected.
      // This also allows, for example, char += char, which could overflow, but this is no
      // different than any other integral addition.
      return null;
    }
    return String.format(
        "Compound assignments from %s to %s hide lossy casts", prettyType(rhs), prettyType(lhs));
  }

  /** Desugars a compound assignment, making the cast explicit. */
  private static Optional<Fix> rewriteCompoundAssignment(
      CompoundAssignmentTree tree, VisitorState state) {
    CharSequence var = state.getSourceForNode(tree.getVariable());
    CharSequence expr = state.getSourceForNode(tree.getExpression());
    if (var == null || expr == null) {
      return Optional.absent();
    }
    switch (tree.getKind()) {
      case RIGHT_SHIFT_ASSIGNMENT:
        // narrowing the result of a signed right shift does not lose information
        return Optional.absent();
      case AND_ASSIGNMENT:
      case XOR_ASSIGNMENT:
      case OR_ASSIGNMENT:
        if (twiddlingConstantBitsOk(tree)) {
          return Optional.absent();
        }
        break;
      default:
        break;
    }
    Kind regularAssignmentKind = regularAssignmentFromCompound(tree.getKind());
    String op = assignmentToString(regularAssignmentKind);

    // Add parens to the rhs if necessary to preserve the current precedence
    // e.g. 's -= 1 - 2' -> 's = s - (1 - 2)'
    OperatorPrecedence rhsPrecedence =
        tree.getExpression() instanceof JCBinary
            ? OperatorPrecedence.from(tree.getExpression().getKind())
            : tree.getExpression() instanceof ConditionalExpressionTree
                ? OperatorPrecedence.TERNARY
                : null;

    if (rhsPrecedence != null) {
      if (!rhsPrecedence.isHigher(OperatorPrecedence.from(regularAssignmentKind))) {
        expr = String.format("(%s)", expr);
      }
    }

    // e.g. 's *= 42' -> 's = (short) (s * 42)'
    String castType = getType(tree.getVariable()).toString();
    String replacement = String.format("%s = (%s) (%s %s %s)", var, castType, var, op, expr);
    return Optional.of(SuggestedFix.replace(tree, replacement));
  }

  private static boolean twiddlingConstantBitsOk(CompoundAssignmentTree tree) {
    int shift;
    switch (ASTHelpers.getType(tree.getVariable()).getKind()) {
      case BYTE:
        shift = 8;
        break;
      case SHORT:
        shift = 16;
        break;
      default:
        return false;
    }
    Object constValue = ASTHelpers.constValue(tree.getExpression());
    if (!(constValue instanceof Integer || constValue instanceof Long)) {
      return false;
    }
    long constLong = ((Number) constValue).longValue();
    long shifted = constLong >> shift;
    return shifted == 0 || shifted == ~0;
  }
}
