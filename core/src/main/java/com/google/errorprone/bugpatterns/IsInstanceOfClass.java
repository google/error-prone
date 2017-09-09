/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "IsInstanceOfClass",
  summary = "The argument to Class#isInstance(Object) should not be a Class",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class IsInstanceOfClass extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> INSTANCE_OF_CLASS =
      Matchers.allOf(
          instanceMethod().onExactClass("java.lang.Class").named("isInstance"),
          argument(
              0,
              // Class is final so we could just use isSameType, but we want to
              // test for the same _erased_ type.
              Matchers.<ExpressionTree>isSubtypeOf("java.lang.Class")));

  /** Suggests removing getClass() or changing to Class.class. */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!INSTANCE_OF_CLASS.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.replace(tree, buildReplacement(tree, state)));
  }

  static String buildReplacement(MethodInvocationTree tree, VisitorState state) {

    Operand lhs = classify((JCTree) ASTHelpers.getReceiver(tree.getMethodSelect()), state);
    Operand rhs = classify((JCTree) Iterables.getOnlyElement(tree.getArguments()), state);

    // expr.getClass().isInstance(Bar.class) -> expr instanceof Bar
    if (lhs.kind() == Kind.GET_CLASS && rhs.kind() == Kind.LITERAL) {
      return String.format("%s instanceof %s", lhs.value(), rhs.value());
    }

    // expr1.getClass().isInstance(expr2.getClass()) -> expr2.getClass().isInstance(expr1)
    if (lhs.kind() == Kind.GET_CLASS && rhs.kind() == Kind.GET_CLASS) {
      return String.format("%s.getClass().isInstance(%s)", rhs.value(), lhs.value());
    }

    // Foo.class.isInstance(Bar.class) -> Bar.class == Class.class
    if (lhs.kind() == Kind.LITERAL && rhs.kind() == Kind.LITERAL) {
      return String.format("%s.class == Class.class", rhs.value()); // !!
    }

    // Foo.class.isInstance(expr.getClass()) -> expr instanceof Foo
    if (lhs.kind() == Kind.LITERAL && rhs.kind() == Kind.GET_CLASS) {
      return String.format("%s instanceof %s", rhs.value(), lhs.value());
    }

    // clazz.isInstance(expr.getClass()) -> clazz.isInstance(expr)
    if (rhs.kind() == Kind.GET_CLASS) {
      return String.format("%s.isInstance(%s)", lhs.source(), rhs.value());
    }

    // expr.getClass().isInstance(clazz) -> clazz.isInstance(expr)
    if (lhs.kind() == Kind.GET_CLASS) {
      return String.format("%s.isInstance(%s)", rhs.source(), lhs.value());
    }

    // clazz1.isInstance(clazz2) -> clazz2.isAssignableFrom(clazz1)
    // clazz.isInstance(Bar.class) -> Bar.class.isAssignableFrom(clazz)
    // Foo.class.isInstance(clazz) -> clazz.isAssignableFrom(Foo.class)
    return String.format("%s.isAssignableFrom(%s)", rhs.source(), lhs.source());
  }

  enum Kind {
    LITERAL,
    GET_CLASS,
    EXPR
  }

  @AutoValue
  abstract static class Operand {
    abstract Kind kind();

    abstract CharSequence value();

    abstract CharSequence source();

    static Operand create(Kind kind, CharSequence value, CharSequence source) {
      return new AutoValue_IsInstanceOfClass_Operand(kind, value, source);
    }
  }

  static Operand classify(JCTree tree, VisitorState state) {
    CharSequence source = state.getSourceForNode(tree);
    if (tree instanceof MethodInvocationTree) {
      // expr.getClass() -> "expr"
      MethodInvocationTree receiverInvocation = (MethodInvocationTree) tree;
      MethodSymbol sym = ASTHelpers.getSymbol(receiverInvocation);
      if (sym != null) {
        if (sym.getSimpleName().contentEquals("getClass") && sym.params().isEmpty()) {
          if (receiverInvocation.getMethodSelect() instanceof IdentifierTree) {
            // unqualified `getClass()`
            return Operand.create(Kind.EXPR, state.getSourceForNode(tree), source);
          }
          return Operand.create(
              Kind.GET_CLASS,
              state.getSourceForNode((JCTree) ASTHelpers.getReceiver(receiverInvocation)),
              source);
        }
      }
    } else if (tree instanceof MemberSelectTree) {
      // Foo.class -> "Foo"
      MemberSelectTree select = (MemberSelectTree) tree;
      if (select.getIdentifier().contentEquals("class")) {
        return Operand.create(
            Kind.LITERAL, state.getSourceForNode((JCTree) select.getExpression()), source);
      }
    }
    return Operand.create(Kind.EXPR, source, source);
  }
}
