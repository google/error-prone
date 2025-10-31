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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ThrowTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import javax.lang.model.element.ElementKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Prefer LinkageError for rethrowing ReflectiveOperationException as unchecked",
    severity = WARNING)
public class RethrowReflectiveOperationExceptionAsLinkageError extends BugChecker
    implements ThrowTreeMatcher {
  private static final String ASSERTION_ERROR = "java.lang.AssertionError";
  private static final String REFLECTIVE_OPERATION_EXCEPTION =
      "java.lang.ReflectiveOperationException";
  private static final Matcher<ExpressionTree> MATCHER = constructor().forClass(ASSERTION_ERROR);

  @Override
  public Description matchThrow(ThrowTree throwTree, VisitorState state) {
    if (!MATCHER.matches(throwTree.getExpression(), state)) {
      return NO_MATCH;
    }
    NewClassTree newClassTree = (NewClassTree) throwTree.getExpression();
    List<? extends ExpressionTree> arguments = newClassTree.getArguments();
    if (arguments.isEmpty() || arguments.size() > 2) {
      return NO_MATCH;
    }
    Symbol cause = ASTHelpers.getSymbol(Iterables.getLast(arguments));
    if (cause == null || !isReflectiveOperationException(state, cause)) {
      return NO_MATCH;
    }
    String message =
        arguments.size() == 1
            ? String.format("%s.getMessage()", cause.getSimpleName())
            : state.getSourceForNode(arguments.get(0));
    return describeMatch(
        newClassTree,
        SuggestedFix.replace(
            newClassTree,
            String.format("new LinkageError(%s, %s)", message, cause.getSimpleName())));
  }

  private static boolean isReflectiveOperationException(VisitorState state, Symbol symbol) {
    return isSameType(symbol.asType(), JAVA_LANG_REFLECTIVEOPERATIONEXCEPTION.get(state), state)
        && symbol.getKind().equals(ElementKind.EXCEPTION_PARAMETER);
  }

  private static final Supplier<Type> JAVA_LANG_REFLECTIVEOPERATIONEXCEPTION =
      VisitorState.memoize(state -> state.getTypeFromString(REFLECTIVE_OPERATION_EXCEPTION));
}
