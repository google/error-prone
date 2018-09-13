/*
 * Copyright 2017 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.type.TypeKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "FloatCast",
    summary = "Use parentheses to make the precedence explicit",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class FloatCast extends BugChecker implements TypeCastTreeMatcher {

  static final Set<TypeKind> FLOATING_POINT = EnumSet.of(TypeKind.FLOAT, TypeKind.DOUBLE);

  static final Set<TypeKind> INTEGRAL = EnumSet.of(TypeKind.LONG, TypeKind.INT);

  static final Matcher<ExpressionTree> BLACKLIST =
      staticMethod().onClass("java.lang.Math").namedAnyOf("floor", "ceil", "signum", "rint");

  static final Matcher<ExpressionTree> POW = staticMethod().onClass("java.lang.Math").named("pow");

  @Override
  public Description matchTypeCast(TypeCastTree tree, VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof BinaryTree)) {
      return NO_MATCH;
    }
    BinaryTree binop = (BinaryTree) parent;
    if (!binop.getLeftOperand().equals(tree)) {
      // the precedence is unambiguous for e.g. `i + (int) f`
      return NO_MATCH;
    }
    if (binop.getKind() != Kind.MULTIPLY) {
      // there's a bound on the imprecision for +, -, /
      return NO_MATCH;
    }
    Type castType = ASTHelpers.getType(tree.getType());
    Type operandType = ASTHelpers.getType(tree.getExpression());
    if (castType == null || operandType == null) {
      return NO_MATCH;
    }
    Symtab symtab = state.getSymtab();
    if (isSameType(ASTHelpers.getType(parent), symtab.stringType, state)) {
      // string concatenation doesn't count
      return NO_MATCH;
    }
    switch (castType.getKind()) {
      case LONG:
      case INT:
      case SHORT:
      case CHAR:
      case BYTE:
        break;
      default:
        return NO_MATCH;
    }
    switch (operandType.getKind()) {
      case FLOAT:
      case DOUBLE:
        break;
      default:
        return NO_MATCH;
    }
    if (BLACKLIST.matches(tree.getExpression(), state)) {
      return NO_MATCH;
    }
    if (POW.matches(tree.getExpression(), state)) {
      MethodInvocationTree pow = (MethodInvocationTree) tree.getExpression();
      if (pow.getArguments().stream()
          .map(ASTHelpers::getType)
          .filter(x -> x != null)
          .map(state.getTypes()::unboxedTypeOrType)
          .map(Type::getKind)
          .allMatch(INTEGRAL::contains)) {
        return NO_MATCH;
      }
    }
    // Find the outermost enclosing binop, to suggest e.g. `(long) (f * a * b)` instead of
    // `(long) (f * a) * b`.
    Tree enclosing = binop;
    TreePath path = state.getPath().getParentPath().getParentPath();
    while (path != null) {
      if (!(path.getLeaf() instanceof BinaryTree)) {
        break;
      }
      BinaryTree enclosingBinop = (BinaryTree) path.getLeaf();
      if (!enclosingBinop.getLeftOperand().equals(enclosing)) {
        break;
      }
      enclosing = enclosingBinop;
      path = path.getParentPath();
    }
    return buildDescription(tree)
        .addFix(
            SuggestedFix.builder()
                .prefixWith(tree.getExpression(), "(")
                .postfixWith(enclosing, ")")
                .build())
        .addFix(SuggestedFix.builder().prefixWith(tree, "(").postfixWith(tree, ")").build())
        .build();
  }
}
