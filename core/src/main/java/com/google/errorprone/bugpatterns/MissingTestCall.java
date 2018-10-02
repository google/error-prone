/*
 * Copyright 2018 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.ProvidesFix.NO_FIX;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodClassMatcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * Matches test helpers which require a terminating method to be called.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "MissingTestCall",
    summary = "A terminating method call is required for a test helper to have any effect.",
    severity = ERROR,
    providesFix = NO_FIX)
public final class MissingTestCall extends BugChecker implements MethodTreeMatcher {

  private static final MethodClassMatcher EQUALS_TESTER =
      instanceMethod().onDescendantOf("com.google.common.testing.EqualsTester");
  private static final MethodClassMatcher REFACTORING_HELPER =
      instanceMethod().onDescendantOf("com.google.errorprone.BugCheckerRefactoringTestHelper");
  private static final MethodClassMatcher COMPILATION_HELPER =
      instanceMethod().onDescendantOf("com.google.errorprone.CompilationTestHelper");

  private static final ImmutableSet<MethodPairing> PAIRINGS =
      ImmutableSet.of(
          MethodPairing.of(
              "EqualsTester",
              EQUALS_TESTER.named("addEqualityGroup"),
              EQUALS_TESTER.named("testEquals")),
          MethodPairing.of(
              "BugCheckerRefactoringTestHelper",
              REFACTORING_HELPER.namedAnyOf(
                  "addInput",
                  "addInputLines",
                  "addInputFile",
                  "addOutput",
                  "addOutputLines",
                  "addOutputFile"),
              REFACTORING_HELPER.named("doTest")),
          MethodPairing.of(
              "CompilationTestHelper",
              COMPILATION_HELPER.namedAnyOf("addSourceLines", "addSourceFile"),
              COMPILATION_HELPER.named("doTest")));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!JUnitMatchers.TEST_CASE.matches(tree, state)) {
      return NO_MATCH;
    }
    Set<MethodPairing> required = new HashSet<>();
    Set<MethodPairing> called = new HashSet<>();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
        for (MethodPairing pairing : PAIRINGS) {
          if (pairing.ifCall().matches(node, state)) {
            if (!isField(getUltimateReceiver(node))
                || isLastStatementInBlock(state.findPathToEnclosing(StatementTree.class))) {
              required.add(pairing);
            }
          }
          if (pairing.mustCall().matches(node, state)) {
            called.add(pairing);
          }
        }
        return super.visitMethodInvocation(node, null);
      }
    }.scan(state.getPath(), null);
    return Sets.difference(required, called).stream()
        .findFirst()
        .map(
            p ->
                buildDescription(tree)
                    .setMessage(
                        String.format(
                            "%s requires a terminating method call to have any effect.", p.name()))
                    .build())
        .orElse(NO_MATCH);
  }

  @Nullable
  private static ExpressionTree getUltimateReceiver(ExpressionTree tree) {
    ExpressionTree receiver = getReceiver(tree);
    while (receiver instanceof MemberSelectTree || receiver instanceof MethodInvocationTree) {
      tree = receiver;
      receiver = getReceiver(receiver);
    }
    return receiver;
  }

  private static boolean isField(@Nullable ExpressionTree tree) {
    if (!(tree instanceof IdentifierTree)) {
      return false;
    }
    Symbol symbol = getSymbol(tree);
    return symbol != null && symbol.getKind() == ElementKind.FIELD;
  }

  private static boolean isLastStatementInBlock(@Nullable TreePath pathToStatement) {
    if (pathToStatement == null) {
      return false;
    }
    Tree parent = pathToStatement.getParentPath().getLeaf();
    if (!(parent instanceof BlockTree)) {
      return false;
    }
    return getLast(((BlockTree) parent).getStatements()).equals(pathToStatement.getLeaf());
  }

  @AutoValue
  abstract static class MethodPairing {
    abstract String name();

    abstract Matcher<ExpressionTree> ifCall();

    abstract Matcher<ExpressionTree> mustCall();

    private static MethodPairing of(
        String name, Matcher<ExpressionTree> ifCall, Matcher<ExpressionTree> mustCall) {
      return new AutoValue_MissingTestCall_MethodPairing(name, ifCall, mustCall);
    }
  }
}
