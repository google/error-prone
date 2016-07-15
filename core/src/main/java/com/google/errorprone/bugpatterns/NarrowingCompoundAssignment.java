/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.base.Optional;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompoundAssignmentTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.OperatorPrecedence;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.type.TypeKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "NarrowingCompoundAssignment",
  summary = "Compound assignments to bytes, shorts, chars, and floats hide dangerous casts",
  category = JDK,
  severity = WARNING
)
public class NarrowingCompoundAssignment extends BugChecker
    implements CompoundAssignmentTreeMatcher {

  private enum NarrowingCastKind {
    DEFICIENT("Compound assignments to bytes, shorts, chars, and floats hide dangerous casts"),
    FLOAT_TO_INTEGRAL(
        "Compound assignments from floating point to integral types hide dangerous casts");

    private final String message;

    private NarrowingCastKind(String message) {
      this.message = message;
    }

    String message() {
      return message;
    }
  }

  /** The set of types susceptible to the implicit narrowing bug, and their string names. */
  private static final Set<TypeKind> DEFICIENT_TYPES =
      Collections.unmodifiableSet(
          EnumSet.of(TypeKind.BYTE, TypeKind.SHORT, TypeKind.CHAR, TypeKind.FLOAT));

  private static final Set<TypeKind> INTEGRAL_TYPES =
      Collections.unmodifiableSet(
          EnumSet.of(TypeKind.BYTE, TypeKind.SHORT, TypeKind.CHAR, TypeKind.INT, TypeKind.LONG));

  private static final Set<TypeKind> FLOAT_TYPES =
      Collections.unmodifiableSet(EnumSet.of(TypeKind.FLOAT, TypeKind.DOUBLE));

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
    NarrowingCastKind castKind =
        identifyBadCast(
            getType(tree.getVariable()), getType(tree.getExpression()), state.getTypes());
    if (castKind == null) {
      return Description.NO_MATCH;
    }
    Optional<Fix> fix = rewriteCompoundAssignment(tree, state);
    if (!fix.isPresent()) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree).addFix(fix.get()).setMessage(castKind.message()).build();
  }

  /** Classifies bad casts. */
  private static NarrowingCastKind identifyBadCast(Type lhs, Type rhs, Types types) {
    if (types.isConvertible(rhs, lhs)) {
      // Exemption if the rhs is convertable to the lhs.
      // This allows, e.g.: <byte> &= <byte> since the narrowing conversion can never be
      // detected.
      return null;
    }
    if (DEFICIENT_TYPES.contains(lhs.getKind())) {
      return NarrowingCastKind.DEFICIENT;
    }
    if (FLOAT_TYPES.contains(rhs.getKind()) && INTEGRAL_TYPES.contains(lhs.getKind())) {
      return NarrowingCastKind.FLOAT_TO_INTEGRAL;
    }
    return null;
  }

  /** Desugars a compound assignment, making the cast explicit. */
  private static Optional<Fix> rewriteCompoundAssignment(
      CompoundAssignmentTree tree, VisitorState state) {
    CharSequence var = state.getSourceForNode((JCTree) tree.getVariable());
    CharSequence expr = state.getSourceForNode((JCTree) tree.getExpression());
    if (var == null || expr == null) {
      return Optional.absent();
    }
    switch (tree.getKind()) {
      case RIGHT_SHIFT_ASSIGNMENT:
        // narrowing the result of a signed right shift does not lose information
        return Optional.absent();
      default:
        break;
    }
    Kind regularAssignmentKind = regularAssignmentFromCompound(tree.getKind());
    String op = assignmentToString(regularAssignmentKind);

    // Add parens to the rhs if necessary to preserve the current precedence
    // e.g. 's -= 1 - 2' -> 's = s - (1 - 2)'
    if (tree.getExpression() instanceof JCBinary) {
      Kind rhsKind = ((JCBinary) tree.getExpression()).getKind();
      if (!OperatorPrecedence.from(rhsKind)
          .isHigher(OperatorPrecedence.from(regularAssignmentKind))) {
        expr = String.format("(%s)", expr);
      }
    }

    // e.g. 's *= 42' -> 's = (short) (s * 42)'
    String castType = getType(tree.getVariable()).toString();
    String replacement = String.format("%s = (%s) (%s %s %s)", var, castType, var, op, expr);
    return Optional.of(SuggestedFix.replace(tree, replacement));
  }
}
