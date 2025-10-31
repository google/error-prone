/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import java.util.List;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Nesting Modules.combine() here is unnecessary.", severity = WARNING)
public final class GuiceNestedCombine extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> MODULES_COMBINE_METHOD =
      staticMethod().onClass("com.google.inject.util.Modules").named("combine");

  private static final Supplier<Type> MODULE = typeFromString("com.google.inject.Module");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MODULES_COMBINE_METHOD.matches(tree, state)) {
      return NO_MATCH;
    }
    var module = MODULE.get(state);
    if (module == null
        || tree.getArguments().isEmpty()
        || !tree.getArguments().stream().allMatch(a -> isSubtype(getType(a), module, state))) {
      return NO_MATCH;
    }

    var parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof MethodInvocationTree methodInvocationTree)) {
      return NO_MATCH;
    }
    if (!isInVarargsPosition(tree, methodInvocationTree, state)) {
      return NO_MATCH;
    }

    var fix =
        SuggestedFix.builder()
            .replace(getStartPosition(tree), getStartPosition(tree.getArguments().get(0)), "")
            .replace(
                state.getEndPosition(getLast(tree.getArguments())), state.getEndPosition(tree), "")
            .build();
    return describeMatch(tree, fix);
  }

  private static boolean isInVarargsPosition(
      ExpressionTree argTree, MethodInvocationTree methodInvocationTree, VisitorState state) {
    var methodSymbol = getSymbol(methodInvocationTree);
    if (!methodSymbol.isVarArgs()) {
      return false;
    }
    int parameterCount = methodSymbol.getParameters().size();
    List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();
    // Don't match if we're passing an array into a varargs parameter, but do match if there are
    // other parameters along with it.
    return (arguments.size() > parameterCount || !state.getTypes().isArray(getType(argTree)))
        && arguments.indexOf(argTree) >= parameterCount - 1;
  }
}
