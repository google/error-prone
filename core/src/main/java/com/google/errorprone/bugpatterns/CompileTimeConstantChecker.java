/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.CompileTimeConstantExpressionMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import java.util.Iterator;

/**
 * Detects invocations of methods with a parameter annotated {@code @CompileTimeConstant} such that
 * the corresponding actual parameter is not a compile-time constant expression.
 *
 * <p>
 * This type annotation checker enforces that for all method and constructor invocations, for all
 * formal parameters of the invoked method/constructor that are annotated with the
 * {@link com.google.common.annotations.CompileTimeConstant} type annotation, the
 * corresponding actual parameter is an expression that satisfies one of the following conditions:
 * <ol>
 * <li>The expression is one for which the Java compiler can determine a constant value at compile
 * time, or</li>
 * <li>the expression consists of the literal {@code null}, or</li>
 * <li>the expression consists of a single identifier, where the identifier is a formal method
 * parameter that is declared {@code final} and has the
 * {@link com.google.common.annotations.CompileTimeConstant} annotation.</li>
 * </ol>
 *
 * @see CompileTimeConstantExpressionMatcher
 */
@BugPattern(name = "CompileTimeConstant",
    summary =
        "Non-compile-time constant expression passed to parameter with "
        + "@CompileTimeConstant type annotation. If your expression is using another "
        + "@CompileTimeConstant parameter, make sure that parameter is also marked final.",
    explanation =
        "A method or constructor with one or more parameters whose declaration is "
        + "annotated with the @CompileTimeConstant type annotation must only be invoked "
        + "with corresponding actual parameters that are computed as compile-time constant "
        + "expressions, such as a literal or static final constant.",
    linkType = NONE, category = GUAVA, severity = ERROR, maturity = MATURE)
public class CompileTimeConstantChecker extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private final Matcher<ExpressionTree> compileTimeConstExpressionMatcher =
      new CompileTimeConstantExpressionMatcher();

  private final Matcher<MethodInvocationTree> methodInvocationTreeMatcher =
      new Matcher<MethodInvocationTree>() {
        @Override
        public boolean matches(MethodInvocationTree tree, VisitorState state) {
          ExpressionTree methodSelect = tree.getMethodSelect();
          Symbol sym = ASTHelpers.getSymbol(methodSelect);
          if (sym == null) {
            throw new IllegalStateException();
          }
          return matchArguments(state, (Symbol.MethodSymbol) sym, tree.getArguments().iterator());
        }
  };

  private final Matcher<NewClassTree> newClassTreeMatcher = new Matcher<NewClassTree>() {
    @Override
    public boolean matches(NewClassTree tree, VisitorState state) {
      JCTree.JCNewClass newClass = (JCTree.JCNewClass) tree;
      return matchArguments(
          state, (Symbol.MethodSymbol) newClass.constructor, tree.getArguments().iterator());
    }
  };

  /**
   * Matches formal parameters with
   * {@link com.google.common.annotations.CompileTimeConstant} annotations against
   * corresponding actual parameters.
   *
   * @param state the visitor state
   * @param calleeSymbol the method whose formal parameters to consider
   * @param actualParams the list of actual parameters
   *
   * @return {@code true} <i>iff</i> for any of the actual parameters that is annotated with
   *         {@link com.google.common.annotations.CompileTimeConstant}, the corresponding
   *         formal parameter is not a compile-time-constant expression in the sense of
   *         {@link CompileTimeConstantExpressionMatcher}.
   */
  private boolean matchArguments(VisitorState state, final Symbol.MethodSymbol calleeSymbol,
      Iterator<? extends ExpressionTree> actualParams) {
    Symbol.VarSymbol lastFormalParam = null;
    for (Symbol.VarSymbol formalParam : calleeSymbol.getParameters()) {
      lastFormalParam = formalParam;
      // It appears that for some reason, the Tree for implicit Enum constructors
      // inculdes an invocation of super(), but the target symbol has the signature
      // Enum(String, int). This resulted in NoSuchElementExceptions.
      // It is safe to return false in this case, since even if this could happen
      // in another scenario, a non-existent actual parameter can't possibly
      // be a non-constant parameter for a @CompileTimeConstant formal.
      if (!actualParams.hasNext()) {
        return false;
      }
      ExpressionTree actualParam = actualParams.next();
      if (CompileTimeConstantExpressionMatcher.hasCompileTimeConstantAnnotation(
          state, formalParam)) {
        if (!compileTimeConstExpressionMatcher.matches(actualParam, state)) {
          return true;
        }
      }
    }

    // If the last formal parameter is a vararg and has the @CompileTimeConstant annotation,
    // we need to check the remaining args as well.
    if (lastFormalParam == null || (lastFormalParam.flags() & Flags.VARARGS) == 0) {
      return false;
    }
    if (!CompileTimeConstantExpressionMatcher.hasCompileTimeConstantAnnotation(
        state, lastFormalParam)) {
      return false;
    }
    while (actualParams.hasNext()) {
      ExpressionTree actualParam = actualParams.next();
      if (!compileTimeConstExpressionMatcher.matches(actualParam, state)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!newClassTreeMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // There are no suggested fixes for this bug.
    return describeMatch(tree);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!methodInvocationTreeMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // There are no suggested fixes for this bug.
    return describeMatch(tree);
  }
}
