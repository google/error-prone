/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.findSuperMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

/** Removes overrides which purely pass through to the method in the super class. */
@BugPattern(
    name = "RedundantOverride",
    summary = "This override is redundant, and can be removed.",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class RedundantOverride extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol methodSymbol = getSymbol(tree);
    if (methodSymbol == null) {
      return NO_MATCH;
    }
    Optional<MethodSymbol> maybeSuperMethod = findSuperMethod(methodSymbol, state.getTypes());
    if (!maybeSuperMethod.isPresent()) {
      return NO_MATCH;
    }
    MethodSymbol superMethod = maybeSuperMethod.get();
    if (tree.getBody() == null || tree.getBody().getStatements().size() != 1) {
      return NO_MATCH;
    }
    StatementTree statement = tree.getBody().getStatements().get(0);
    ExpressionTree expression = getSingleInvocation(statement);
    if (expression == null) {
      return NO_MATCH;
    }
    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) expression;
    if (!getSymbol(methodInvocationTree).equals(superMethod)) {
      return NO_MATCH;
    }
    ExpressionTree receiver = getReceiver(methodInvocationTree);
    if (!(receiver instanceof IdentifierTree)) {
      return NO_MATCH;
    }
    if (!((IdentifierTree) receiver).getName().contentEquals("super")) {
      return NO_MATCH;
    }
    // Exempt Javadocs; the override might be here to add documentation.
    DocCommentTree docCommentTree =
        JavacTrees.instance(state.context).getDocCommentTree(state.getPath());
    if (docCommentTree != null) {
      return NO_MATCH;
    }
    // Exempt broadening of visibility.
    if (!methodSymbol.getModifiers().equals(superMethod.getModifiers())) {
      return NO_MATCH;
    }
    // Overriding a protected member in another package broadens the visibility to the new package.
    if (methodSymbol.getModifiers().contains(Modifier.PROTECTED)
        && !Objects.equals(superMethod.packge(), methodSymbol.packge())) {
      return NO_MATCH;
    }
    // Exempt any change in annotations (aside from @Override).
    ImmutableSet<Symbol> superAnnotations = getAnnotations(superMethod);
    ImmutableSet<Symbol> methodAnnotations = getAnnotations(methodSymbol);
    if (!Sets.difference(
            Sets.symmetricDifference(superAnnotations, methodAnnotations),
            ImmutableSet.of(state.getSymtab().overrideType.tsym))
        .isEmpty()) {
      return NO_MATCH;
    }
    for (int i = 0; i < tree.getParameters().size(); ++i) {
      if (!(methodInvocationTree.getArguments().get(i) instanceof IdentifierTree)) {
        return NO_MATCH;
      }
      if (!getSymbol(tree.getParameters().get(i))
          .equals(getSymbol(methodInvocationTree.getArguments().get(i)))) {
        return NO_MATCH;
      }
    }
    // Exempt if there are comments within the body. (Do this last, as it's expensive.)
    if (ErrorProneTokens.getTokens(state.getSourceForNode(tree.getBody()), state.context).stream()
        .anyMatch(t -> !t.comments().isEmpty())) {
      return NO_MATCH;
    }

    return describeMatch(tree, SuggestedFix.delete(tree));
  }

  @Nullable
  private static MethodInvocationTree getSingleInvocation(StatementTree statement) {
    return statement.accept(
        new SimpleTreeVisitor<MethodInvocationTree, Void>() {
          @Override
          public MethodInvocationTree visitReturn(ReturnTree returnTree, Void unused) {
            return visit(returnTree.getExpression(), null);
          }

          @Override
          public MethodInvocationTree visitExpressionStatement(
              ExpressionStatementTree expressionStatement, Void unused) {
            return visit(expressionStatement.getExpression(), null);
          }

          @Override
          public MethodInvocationTree visitMethodInvocation(
              MethodInvocationTree methodInvocationTree, Void unused) {
            return methodInvocationTree;
          }
        },
        null);
  }

  private static ImmutableSet<Symbol> getAnnotations(MethodSymbol symbol) {
    return symbol.getRawAttributes().stream().map(a -> a.type.tsym).collect(toImmutableSet());
  }
}
