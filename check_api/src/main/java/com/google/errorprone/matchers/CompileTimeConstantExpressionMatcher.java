/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;

import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.ElementKind;

/**
 * A matcher for compile-time-constant expressions.
 *
 * <p>For the purposes of this matcher, a compile-time constant expression is one of the following:
 *
 * <ol>
 *   <li>Any expression for which the Java compiler can determine a constant value at compile time.
 *   <li>The expression consisting of the literal {@code null}.
 *   <li>An expression consisting of a single identifier, where the identifier is a formal method
 *       parameter that is declared {@code final} and has the {@link CompileTimeConstant}
 *       annotation.
 * </ol>
 */
public class CompileTimeConstantExpressionMatcher implements Matcher<ExpressionTree> {

  private static final Supplier<Symbol> COMPILE_TIME_CONSTANT_ANNOTATION =
      VisitorState.memoize(state -> state.getSymbolFromString(CompileTimeConstant.class.getName()));

  private final Matcher<ExpressionTree> matcher =
      Matchers.anyOf(
          // TODO(xtof): Consider utilising mdempsky's closed-over-addition matcher
          // (perhaps extended for other arithmetic operations).
          new ExpressionWithConstValueMatcher(),
          Matchers.kindIs(Tree.Kind.NULL_LITERAL),
          new FinalCompileTimeConstantIdentifierMatcher());

  @Override
  public boolean matches(ExpressionTree t, VisitorState state) {
    return matcher.matches(t, state);
  }

  private static final Matcher<ExpressionTree> IMMUTABLE_FACTORY =
      anyOf(
          staticMethod().onClass("com.google.common.collect.ImmutableList").named("of"),
          staticMethod().onClass("com.google.common.collect.ImmutableSet").named("of"));

  /**
   * A matcher for {@link ExpressionTree}s for which the java compiler can compute a constant value
   * (except a literal {@code null}).
   */
  private static final class ExpressionWithConstValueMatcher implements Matcher<ExpressionTree> {

    @Override
    public boolean matches(ExpressionTree t, VisitorState state) {
      return firstNonNull(
          t.accept(
              new SimpleTreeVisitor<Boolean, Void>() {
                @Override
                public Boolean visitConditionalExpression(
                    ConditionalExpressionTree tree, Void unused) {
                  return reduce(
                      tree.getTrueExpression().accept(this, null),
                      tree.getFalseExpression().accept(this, null));
                }

                @Override
                public Boolean visitMethodInvocation(MethodInvocationTree node, Void unused) {
                  if (!IMMUTABLE_FACTORY.matches(node, state)) {
                    return false;
                  }
                  for (ExpressionTree argument : node.getArguments()) {
                    if (!argument.accept(this, null)) {
                      return false;
                    }
                  }
                  return true;
                }

                @Override
                protected Boolean defaultAction(Tree node, Void unused) {
                  Object constValue = ASTHelpers.constValue(node);
                  return constValue != null;
                }

                public Boolean reduce(Boolean lhs, Boolean rhs) {
                  return firstNonNull(lhs, false) && firstNonNull(rhs, false);
                }
              },
              null),
          false);
    }
  }

  /**
   * A matcher that matches a {@code @CompileTimeConstant final} identifier}. These can either be a
   * method parameter or a class field.
   */
  private static final class FinalCompileTimeConstantIdentifierMatcher
      implements Matcher<ExpressionTree> {

    @Override
    public boolean matches(ExpressionTree t, VisitorState state) {
      if (t.getKind() != Tree.Kind.IDENTIFIER) {
        return false;
      }
      Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) ASTHelpers.getSymbol(t);
      Symbol owner = varSymbol.owner;
      ElementKind ownerKind = owner.getKind();
      // Check that the identifier is a formal method/constructor parameter or a class field.
      if (ownerKind != ElementKind.METHOD
          && ownerKind != ElementKind.CONSTRUCTOR
          && ownerKind != ElementKind.CLASS) {
        return false;
      }
      // Check that the symbol is final
      if (!isConsideredFinal(varSymbol)) {
        return false;
      }
      // Check if the symbol has the @CompileTimeConstant annotation.
      if (hasCompileTimeConstantAnnotation(state, varSymbol)) {
        return true;
      }
      return false;
    }
  }

  // public since this is also used by CompileTimeConstantChecker.
  public static boolean hasCompileTimeConstantAnnotation(VisitorState state, Symbol symbol) {
    Symbol annotation = COMPILE_TIME_CONSTANT_ANNOTATION.get(state);
    // If we can't look up the annotation in the current VisitorState, then presumably it couldn't
    // be present on a Symbol we're inspecting.
    return annotation != null && symbol.attribute(annotation) != null;
  }
}
