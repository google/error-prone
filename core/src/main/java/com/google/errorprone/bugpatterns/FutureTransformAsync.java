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

import static com.google.common.collect.Sets.union;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getThrownExceptions;
import static com.google.errorprone.util.ASTHelpers.isCheckedExceptionType;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ClosingFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;

/** See summary for details. */
@BugPattern(
    summary = "Use transform instead of transformAsync when all returns are an immediate future.",
    severity = WARNING)
public final class FutureTransformAsync extends BugChecker implements MethodInvocationTreeMatcher {

  private enum Method {
    TRANSFORM_ASYNC(TRANSFORM_ASYNC_MATCHER, "transform", false),
    CALL_ASYNC(CALL_ASYNC_MATCHER, "call", true);

    @SuppressWarnings("ImmutableEnumChecker")
    private final Matcher<ExpressionTree> matcher;

    private final String fixedName;
    private final boolean canThrowCheckedException;

    private Method(
        Matcher<ExpressionTree> matcher, String fixedName, boolean canThrowCheckedException) {
      this.matcher = matcher;
      this.fixedName = fixedName;
      this.canThrowCheckedException = canThrowCheckedException;
    }
  }

  private static final ImmutableSet<String> CLASSES_WITH_TRANSFORM_ASYNC_STATIC_METHOD =
      ImmutableSet.of(Futures.class.getName());

  private static final ImmutableSet<String> CLASSES_WITH_TRANSFORM_ASYNC_INSTANCE_METHOD =
      ImmutableSet.of(ClosingFuture.class.getName(), FluentFuture.class.getName());

  private static final ImmutableSet<String> CLASSES_WITH_CALL_ASYNC_INSTANCE_METHOD =
      ImmutableSet.of(Futures.FutureCombiner.class.getName());

  private static final Matcher<ExpressionTree> TRANSFORM_ASYNC_MATCHER =
      anyOf(
          staticMethod()
              .onClassAny(CLASSES_WITH_TRANSFORM_ASYNC_STATIC_METHOD)
              .named("transformAsync"),
          instanceMethod()
              .onExactClassAny(CLASSES_WITH_TRANSFORM_ASYNC_INSTANCE_METHOD)
              .named("transformAsync"));

  private static final Matcher<ExpressionTree> CALL_ASYNC_MATCHER =
      instanceMethod().onExactClassAny(CLASSES_WITH_CALL_ASYNC_INSTANCE_METHOD).named("callAsync");

  private static final Matcher<ExpressionTree> IMMEDIATE_FUTURE =
      staticMethod()
          .onClassAny(
              union(
                  CLASSES_WITH_TRANSFORM_ASYNC_STATIC_METHOD,
                  CLASSES_WITH_TRANSFORM_ASYNC_INSTANCE_METHOD))
          .named("immediateFuture");

  private static final Matcher<ExpressionTree> IMMEDIATE_VOID_FUTURE =
      staticMethod()
          .onClassAny(
              union(
                  CLASSES_WITH_TRANSFORM_ASYNC_STATIC_METHOD,
                  CLASSES_WITH_TRANSFORM_ASYNC_INSTANCE_METHOD))
          .named("immediateVoidFuture");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return getMethod(tree, state)
        .map(method -> matchMethodInvocation(method, tree, state))
        .orElse(NO_MATCH);
  }

  private Description matchMethodInvocation(
      Method method, MethodInvocationTree tree, VisitorState state) {
    // Find the lambda expression. The transformAsync() / callAsync() methods might have different
    // number of arguments, but they all have a single lambda. In case of transformAsync(), discard
    // the lambdas that throw checked exceptions, since they cannot be supported by transform().
    Optional<LambdaExpressionTree> lambda =
        tree.getArguments().stream()
            .filter(LambdaExpressionTree.class::isInstance)
            .map(arg -> (LambdaExpressionTree) arg)
            .filter(
                lambdaTree ->
                    method.canThrowCheckedException || !throwsCheckedException(lambdaTree, state))
            .findFirst();

    return lambda
        .map(lambdaTree -> handleTransformAsync(method, tree, lambdaTree, state))
        .orElse(NO_MATCH);
  }

  private static Optional<Method> getMethod(MethodInvocationTree tree, VisitorState state) {
    return EnumSet.allOf(Method.class).stream()
        .filter(method -> method.matcher.matches(tree, state))
        .findFirst();
  }

  private Description handleTransformAsync(
      Method method, MethodInvocationTree tree, LambdaExpressionTree lambda, VisitorState state) {
    HashSet<ExpressionTree> returnExpressions = new HashSet<>();
    if (lambda.getBody() instanceof ExpressionTree) {
      returnExpressions.add((ExpressionTree) lambda.getBody());
    } else if (lambda.getBody() instanceof BlockTree) {
      new TreePathScanner<Void, Void>() {
        @Override
        public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
          // don't descend into lambdas (to handle nested lambdas)
          return null;
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
          // don't descend into classes (to handle nested classes)
          return null;
        }

        @Override
        public Void visitReturn(ReturnTree tree, Void unused) {
          returnExpressions.add(tree.getExpression());
          // Don't descend deeper into returns, since we already checked the body of this return.
          return null;
        }
      }.scan(TreePath.getPath(state.getPath().getCompilationUnit(), lambda.getBody()), null);
    } else {
      return NO_MATCH;
    }

    boolean areAllImmediateFutures =
        returnExpressions.stream()
            .allMatch(
                expression ->
                    expression instanceof MethodInvocationTree
                        && (IMMEDIATE_FUTURE.matches(expression, state)
                            || IMMEDIATE_VOID_FUTURE.matches(expression, state)));

    if (areAllImmediateFutures) {
      SuggestedFix.Builder fix = SuggestedFix.builder();
      suggestFixTransformAsyncToTransform(method, tree, state, fix);
      for (ExpressionTree expression : returnExpressions) {
        suggestFixRemoveImmediateFuture((MethodInvocationTree) expression, state, fix);
      }
      state.reportMatch(describeMatch(tree, fix.build()));
    }

    return NO_MATCH;
  }

  /** Returns true if the lambda throws a checked exception. */
  private static boolean throwsCheckedException(LambdaExpressionTree lambda, VisitorState state) {
    return getThrownExceptions(lambda.getBody(), state).stream()
        .anyMatch(type -> isCheckedExceptionType(type, state));
  }

  /**
   * Suggests fix to replace transformAsync/callAsync with transform/call.
   *
   * <p>If the transformAsync is imported as a static method, it takes care of adding the equivalent
   * import static for transform.
   */
  private static void suggestFixTransformAsyncToTransform(
      Method method, MethodInvocationTree tree, VisitorState state, SuggestedFix.Builder fix) {
    ExpressionTree methodSelect = tree.getMethodSelect();
    if (state.getSourceForNode(methodSelect).equals("transformAsync")) {
      Symbol symbol = getSymbol(methodSelect);
      String className = enclosingClass(symbol).getQualifiedName().toString();
      fix.addStaticImport(className + "." + "transform");
    }
    fix.merge(SuggestedFixes.renameMethodInvocation(tree, method.fixedName, state));
  }

  /** Suggests fix to remove the immediateFuture or immediateVoidFuture call. */
  private static void suggestFixRemoveImmediateFuture(
      MethodInvocationTree tree, VisitorState state, SuggestedFix.Builder fix) {
    String typeArgument = "";
    String argument = "";
    if (IMMEDIATE_FUTURE.matches(tree, state)) {
      var typeArguments = tree.getTypeArguments();
      if (typeArguments.size() == 1) {
        typeArgument = state.getSourceForNode(typeArguments.get(0));
      }
      argument = state.getSourceForNode(tree.getArguments().get(0));
    } else if (IMMEDIATE_VOID_FUTURE.matches(tree, state)) {
      typeArgument = "Void";
      argument = "null";
    }
    String fixString =
        typeArgument.isEmpty() ? argument : String.format("(%s) %s", typeArgument, argument);
    fix.replace(tree, fixString);
  }
}
