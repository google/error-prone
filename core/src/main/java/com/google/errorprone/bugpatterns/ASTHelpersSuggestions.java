/*
 * Copyright 2022 The Error Prone Authors.
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
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

/** A BugPattern; see the summary */
@BugPattern(summary = "Prefer ASTHelpers instead of calling this API directly", severity = WARNING)
public class ASTHelpersSuggestions extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> SYMBOL =
      instanceMethod()
          .onExactClass("com.sun.tools.javac.code.Symbol")
          .namedAnyOf("isDirectlyOrIndirectlyLocal", "isLocal", "packge", "isStatic");

  private static final Matcher<ExpressionTree> SCOPE =
      instanceMethod().onDescendantOf("com.sun.tools.javac.code.Scope");

  private static final ImmutableMap<String, String> NAMES =
      ImmutableMap.of(
          "packge", "enclosingPackage",
          "isDirectlyOrIndirectlyLocal", "isLocal");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    ExpressionTree receiver = getReceiver(tree);
    if (receiver == null) {
      return NO_MATCH;
    }
    if (SYMBOL.matches(tree, state)) {
      MethodSymbol sym = getSymbol(tree);
      String name = sym.getSimpleName().toString();
      name = NAMES.getOrDefault(name, name);
      return describeMatch(
          tree,
          SuggestedFix.builder()
              .addStaticImport("com.google.errorprone.util.ASTHelpers." + name)
              .prefixWith(tree, name + "(")
              .replace(state.getEndPosition(receiver), state.getEndPosition(tree), ")")
              .build());
    }
    if (SCOPE.matches(tree, state)) {
      MethodSymbol sym = getSymbol(tree);
      Type filter = COM_SUN_TOOLS_JAVAC_UTIL_FILTER.get(state);
      Type predicate = JAVA_UTIL_FUNCTION_PREDICATE.get(state);
      if (sym.getParameters().stream()
          .anyMatch(
              p ->
                  isSameType(filter, p.asType(), state)
                      || isSameType(predicate, p.asType(), state))) {
        return describeMatch(
            tree,
            SuggestedFix.builder()
                .addStaticImport("com.google.errorprone.util.ASTHelpers.scope")
                .prefixWith(receiver, "scope(")
                .postfixWith(receiver, ")")
                .build());
      }
    }
    return NO_MATCH;
  }

  private static final Supplier<Type> COM_SUN_TOOLS_JAVAC_UTIL_FILTER =
      VisitorState.memoize(state -> state.getTypeFromString("com.sun.tools.javac.util.Filter"));

  private static final Supplier<Type> JAVA_UTIL_FUNCTION_PREDICATE =
      VisitorState.memoize(state -> state.getTypeFromString("java.util.function.Predicate"));
}
