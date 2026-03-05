/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Minimize the amount of logic in assertThrows", severity = WARNING)
public class AssertThrowsMinimizer extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      staticMethod().onClass("org.junit.Assert").named("assertThrows");

  private final ConstantExpressions constantExpressions;
  private final boolean useVarType;

  @Inject
  AssertThrowsMinimizer(ConstantExpressions constantExpressions, ErrorProneFlags flags) {
    this.constantExpressions = constantExpressions;
    this.useVarType = flags.getBoolean("AssertThrowsMinimizer:UseVarType").orElse(false);
  }

  record Hoist(ExpressionTree site, String name) {}

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    if (!(state.getPath().getParentPath().getLeaf() instanceof StatementTree parent)) {
      // We need a scope to declare variables in, assertThrows is usually an expression statement or
      // a variable initializer
      return NO_MATCH;
    }
    if (!(tree.getArguments().getLast() instanceof LambdaExpressionTree lambdaExpressionTree)) {
      return NO_MATCH;
    }
    MethodInvocationTree runnable;
    switch (lambdaExpressionTree.getBody()) {
      case BlockTree blockTree -> {
        if (blockTree.getStatements().size() != 1) {
          return NO_MATCH;
        }
        if (!(getOnlyElement(blockTree.getStatements())
                instanceof ExpressionStatementTree expressionStatementTree
            && expressionStatementTree.getExpression()
                instanceof MethodInvocationTree methodInvocationTree)) {
          return NO_MATCH;
        }
        runnable = methodInvocationTree;
      }
      case MethodInvocationTree methodInvocationTree -> runnable = methodInvocationTree;
      default -> {
        return NO_MATCH;
      }
    }
    ImmutableList<Hoist> toHoist =
        Streams.concat(
                Stream.ofNullable(getReceiver(runnable))
                    .map(r -> new Hoist(r, receiverVariableName(r))),
                Streams.zip(
                    runnable.getArguments().stream(),
                    getSymbol(runnable).getParameters().stream(),
                    (ExpressionTree a, VarSymbol p) -> new Hoist(a, p.getSimpleName().toString())))
            .filter(h -> needsHoisting(h.site(), state))
            .collect(toImmutableList());
    if (toHoist.isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    StringBuilder hoistedVariables = new StringBuilder();
    for (Hoist hoist : toHoist) {
      String identifier = avoidShadowing(hoist.name(), state);
      hoistedVariables.append(
          String.format(
              "%s %s = %s;\n",
              useVarType ? "var" : SuggestedFixes.qualifyType(state, fix, getType(hoist.site())),
              identifier,
              state.getSourceForNode(hoist.site())));
      fix.replace(hoist.site(), identifier);
    }
    fix.prefixWith(parent, hoistedVariables.toString());
    if (lambdaExpressionTree.getBody() instanceof BlockTree blockTree) {
      fix.replace(getStartPosition(blockTree), getStartPosition(runnable), "");
      fix.replace(state.getEndPosition(runnable), state.getEndPosition(blockTree), "");
    }
    return describeMatch(tree, fix.build());
  }

  private static String receiverVariableName(ExpressionTree tree) {
    return CaseFormat.UPPER_CAMEL.to(
        CaseFormat.LOWER_CAMEL, getType(tree).asElement().getSimpleName().toString());
  }

  private boolean needsHoisting(ExpressionTree tree, VisitorState state) {
    boolean unqualifiedIdentifier =
        switch (tree) {
          case IdentifierTree identifierTree -> true;
          case MemberSelectTree memberSelectTree ->
              memberSelectTree.getExpression() instanceof IdentifierTree identifierTree
                  && identifierTree.getName().contentEquals("this");
          default -> false;
        };
    if (unqualifiedIdentifier && getSymbol(tree).getKind() == ElementKind.FIELD) {
      return false;
    }
    // This is an imperfect heuristic. These expressions aren't guaranteed not to throw, but may be
    // less valuable to hoist.
    return constantExpressions.constantExpression(tree, state).isEmpty();
  }

  // Stolen from PatternMatchingInstanceof
  // TODO: cushon - add to SuggestedFixes?
  private static String avoidShadowing(String name, VisitorState state) {
    var idents =
        FindIdentifiers.findAllIdents(state).stream()
            .map(s -> s.getSimpleName().toString())
            .collect(toImmutableSet());
    return IntStream.iterate(1, i -> i + 1)
        .mapToObj(i -> i == 1 ? name : (name + i))
        .filter(n -> !idents.contains(n))
        .findFirst()
        .get();
  }
}
