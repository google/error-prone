/*
 * Copyright 2022 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasNoExplicitType;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.streamReceivers;
import static javax.lang.model.element.ElementKind.LOCAL_VARIABLE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.regex.Pattern;

/** Converts some local variables to use {@code var}. */
@BugPattern(severity = WARNING, summary = "Consider using `var` here to avoid boilerplate.")
public final class Varifier extends BugChecker implements VariableTreeMatcher {
  private static final Matcher<ExpressionTree> BUILD_METHOD =
      instanceMethod().anyClass().withNameMatching(Pattern.compile("build.*"));

  private static final Matcher<ExpressionTree> BUILDER_FACTORY =
      anyOf(
          staticMethod().anyClass().withNameMatching(Pattern.compile("(builder|newBuilder).*")),
          MethodMatchers.constructor()
              .forClass(
                  (type, state) -> type.tsym.getSimpleName().toString().startsWith("Builder")));

  private static final Matcher<ExpressionTree> FACTORY_METHOD =
      allOf(
          staticMethod().anyClass().withNameMatching(Pattern.compile("(new|create|of).*")),
          (t, s) -> {
            var symbol = getSymbol(t);
            return symbol instanceof MethodSymbol
                && isSameType(((MethodSymbol) symbol).getReturnType(), symbol.owner.type, s);
          });

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    var symbol = getSymbol(tree);
    ExpressionTree initializer = tree.getInitializer();
    if (!symbol.getKind().equals(LOCAL_VARIABLE)
        || !isConsideredFinal(symbol)
        || initializer == null
        || hasNoExplicitType(tree, state)) {
      return NO_MATCH;
    }
    // Foo foo = (Foo) bar;
    if (initializer instanceof TypeCastTree
        && isSameType(
            getType(((TypeCastTree) initializer).getType()), getType(tree.getType()), state)) {
      return fix(tree);
    }
    // Foo foo = new Foo(...);
    if (initializer instanceof NewClassTree
        && isSameType(
            getType(((NewClassTree) initializer).getIdentifier()),
            getType(tree.getType()),
            state)) {
      var identifier = ((NewClassTree) initializer).getIdentifier();
      if (identifier instanceof ParameterizedTypeTree
          && ((ParameterizedTypeTree) identifier).getTypeArguments().isEmpty()) {
        return NO_MATCH;
      }
      return fix(tree);
    }
    // Foo foo = Foo.builder()...build();
    if (BUILD_METHOD.matches(initializer, state)
        && streamReceivers(initializer).anyMatch(t -> BUILDER_FACTORY.matches(t, state))) {
      return fix(tree);
    }
    // Foo foo = Foo.createFoo(..);
    if (FACTORY_METHOD.matches(initializer, state)) {
      return fix(tree);
    }
    return NO_MATCH;
  }

  private Description fix(VariableTree tree) {
    return describeMatch(tree, SuggestedFix.replace(tree.getType(), "var"));
  }
}
