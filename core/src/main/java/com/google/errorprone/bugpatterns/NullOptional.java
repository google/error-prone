/*
 * Copyright 2020 The Error Prone Authors.
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
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.methodCallInDeclarationOfThrowingRunnable;
import static com.google.errorprone.predicates.TypePredicates.anyOf;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

/** Flags passing literal null to {@code Optional}-accepting APIs. */
@BugPattern(
    summary =
        "Passing a literal null to an Optional parameter is almost certainly a mistake. Did you"
            + " mean to provide an empty Optional?",
    severity = WARNING)
public final class NullOptional extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {
  private static final TypePredicate GUAVA_OPTIONAL =
      isDescendantOf("com.google.common.base.Optional");
  private static final TypePredicate OPTIONAL =
      anyOf(GUAVA_OPTIONAL, isDescendantOf("java.util.Optional"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    handleMethodInvocation(getSymbol(tree), tree.getArguments(), state);
    return NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    handleMethodInvocation(getSymbol(tree), tree.getArguments(), state);
    return NO_MATCH;
  }

  private void handleMethodInvocation(
      @Nullable MethodSymbol symbol, List<? extends ExpressionTree> arguments, VisitorState state) {
    if (symbol == null) {
      return;
    }
    Iterator<VarSymbol> parameters = symbol.getParameters().iterator();
    VarSymbol parameter = null;
    Type parameterType = null;
    for (ExpressionTree argument : arguments) {
      parameter = parameters.hasNext() ? parameters.next() : parameter;
      parameterType = parameter.type;
      if (symbol.isVarArgs() && !parameters.hasNext()) {
        parameterType = ((ArrayType) parameter.type).elemtype;
      }
      if (argument.getKind() == Kind.NULL_LITERAL
          && OPTIONAL.apply(parameterType, state)
          && !hasDirectAnnotationWithSimpleName(parameter, "Nullable")) {
        if (methodCallInDeclarationOfThrowingRunnable(state)) {
          return;
        }
        SuggestedFix.Builder fix = SuggestedFix.builder();
        fix.replace(
            argument,
            String.format(
                "%s.%s()",
                qualifyType(state, fix, parameterType.tsym),
                GUAVA_OPTIONAL.apply(parameterType, state) ? "absent" : "empty"));
        state.reportMatch(describeMatch(argument, fix.build()));
      }
    }
  }
}
