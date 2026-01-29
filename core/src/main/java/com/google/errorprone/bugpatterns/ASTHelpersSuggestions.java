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
import static com.google.errorprone.matchers.FieldMatchers.instanceField;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.outermostClass;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;

/** A BugPattern; see the summary */
@BugPattern(summary = "Prefer ASTHelpers instead of calling this API directly", severity = WARNING)
public class ASTHelpersSuggestions extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Supplier<Type> MODULE_SYMBOL =
      Suppliers.typeFromString("com.sun.tools.javac.code.Symbol.ModuleSymbol");

  private static final Matcher<ExpressionTree> SYMBOL =
      anyOf(
          instanceMethod()
              .onDescendantOf("com.sun.tools.javac.code.Symbol")
              .namedAnyOf("packge", "getEnclosedElements"),
          instanceMethod()
              .onClass((t, s) -> isSubtype(MODULE_SYMBOL.get(s), t, s))
              .namedAnyOf("isStatic"));

  private static final Matcher<ExpressionTree> SYMBOL_ENCLCLASS =
      instanceMethod().onDescendantOf("com.sun.tools.javac.code.Symbol").namedAnyOf("enclClass");

  private static final Matcher<ExpressionTree> SYMBOL_OWNER =
      instanceField("com.sun.tools.javac.code.Symbol", "owner");

  private static final ImmutableMap<String, String> NAMES =
      ImmutableMap.of("packge", "enclosingPackage");

  private static final String AST_HELPERS_NAME = ASTHelpers.class.getName();

  private static final Supplier<Symbol> AST_HELPERS =
      VisitorState.memoize(
          state -> JavacElements.instance(state.context).getTypeElement(AST_HELPERS_NAME));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    ExpressionTree receiver = getReceiver(tree);
    if (receiver == null) {
      return NO_MATCH;
    }
    ClassSymbol outermost =
        outermostClass(getSymbol(findEnclosingNode(state.getPath(), ClassTree.class)));
    if (outermost.getQualifiedName().contentEquals(AST_HELPERS_NAME)) {
      return NO_MATCH;
    }
    if (AST_HELPERS.get(state) == null) {
      return NO_MATCH;
    }
    if (SYMBOL.matches(tree, state)) {
      MethodSymbol sym = getSymbol(tree);
      String name = sym.getSimpleName().toString();
      name = NAMES.getOrDefault(name, name);
      return describeMatch(
          tree,
          SuggestedFix.builder()
              .addStaticImport(AST_HELPERS_NAME + "." + name)
              .prefixWith(tree, name + "(")
              .replace(state.getEndPosition(receiver), state.getEndPosition(tree), ")")
              .build());
    }
    if (SYMBOL_ENCLCLASS.matches(tree, state)) {
      // Check whether the receiver matches the instance field Symbol.owner.
      if (SYMBOL_OWNER.matches(receiver, state)) {
        // Get the receiver of the Symbol.owner expression.
        ExpressionTree receiver2 = getReceiver(receiver);
        if (receiver2 != null) {
          return describeMatch(
              tree,
              SuggestedFix.builder()
                  .addStaticImport(AST_HELPERS_NAME + ".enclosingClass")
                  .prefixWith(tree, "enclosingClass(")
                  .replace(state.getEndPosition(receiver2), state.getEndPosition(tree), ")")
                  .build());
        }
      }
    }
    return NO_MATCH;
  }
}
