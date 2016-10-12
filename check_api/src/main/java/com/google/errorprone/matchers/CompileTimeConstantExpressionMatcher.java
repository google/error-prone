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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
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

  private static final String COMPILE_TIME_CONSTANT_ANNOTATION =
      CompileTimeConstant.class.getName();

  @SuppressWarnings("unchecked")
  private final Matcher<ExpressionTree> matcher =
      Matchers.anyOf(
          // TODO(xtof): Consider utilising mdempsky's closed-over-addition matcher
          // (perhaps extended for other arithmetic operations).
          new ExpressionWithConstValueMatcher(),
          Matchers.kindIs(Tree.Kind.NULL_LITERAL),
          new FinalCompileTimeConstantParameterMatcher());

  @Override
  public boolean matches(ExpressionTree t, VisitorState state) {
    return matcher.matches(t, state);
  }

  // TODO(xtof): Perhaps some of these matchers could be generally useful, in which case they should
  // be moved into c.g.errorprone.matchers.

  /**
   * A matcher for {@link ExpressionTree}s for which the java compiler can compute a constant value
   * (except a literal {@code null}).
   */
  private static final class ExpressionWithConstValueMatcher implements Matcher<ExpressionTree> {

    @Override
    public boolean matches(ExpressionTree t, VisitorState state) {
      Object constValue = ((JCTree.JCExpression) t).type.constValue();
      return constValue != null;
    }
  }

  /** A matcher that matches a {@code @CompileTimeConstant final} parameter}. */
  private static final class FinalCompileTimeConstantParameterMatcher
      implements Matcher<ExpressionTree> {

    @Override
    public boolean matches(ExpressionTree t, VisitorState state) {
      if (t.getKind() != Tree.Kind.IDENTIFIER) {
        return false;
      }
      Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) ASTHelpers.getSymbol(t);
      Symbol owner = varSymbol.owner;
      ElementKind ownerKind = owner.getKind();
      // Check that the identifier is a formal method/constructor parameter.
      if (ownerKind != ElementKind.METHOD && ownerKind != ElementKind.CONSTRUCTOR) {
        return false;
      }
      // Check that the symbol is final
      if ((varSymbol.flags() & Flags.FINAL) != Flags.FINAL
          && (varSymbol.flags() & Flags.EFFECTIVELY_FINAL) != Flags.EFFECTIVELY_FINAL) {
        return false;
      }
      // Check if the symbol has the @CompileTimeConstant annotation.
      if (hasCompileTimeConstantAnnotation(state, varSymbol)) {
        return true;
      }
      return false;
    }
  }

  private static boolean hasAttribute(Symbol symbol, String name, VisitorState state) {
    Symbol annotation = state.getSymbolFromString(name);
    // If we can't look up the annotation in the current VisitorState, then presumably it couldn't
    // be present on a Symbol we're inspecting.
    return annotation != null && symbol.attribute(annotation) != null;
  }

  // public since this is also used by CompileTimeConstantTypeAnnotationChecker.
  public static boolean hasCompileTimeConstantAnnotation(VisitorState state, Symbol symbol) {
    return hasAttribute(symbol, COMPILE_TIME_CONSTANT_ANNOTATION, state);
  }
}
