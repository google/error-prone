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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.receiverOfInvocation;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Objects;
import javax.lang.model.element.ElementKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Always pass 'false' to 'Future.cancel()', unless you are propagating a"
            + " cancellation-with-interrupt from another caller",
    severity = WARNING)
public class Interruption extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> CANCEL =
      instanceMethod()
          .onDescendantOf("java.util.concurrent.Future")
          .named("cancel")
          .withParameters("boolean");

  private static final Matcher<MethodInvocationTree> INTERRUPT_OTHER_THREAD =
      allOf(
          toType(
              MethodInvocationTree.class,
              instanceMethod()
                  .onDescendantOf("java.lang.Thread")
                  .named("interrupt")
                  .withNoParameters()),
          not(
              receiverOfInvocation(
                  staticMethod()
                      .onDescendantOf("java.lang.Thread")
                      .named("currentThread")
                      .withNoParameters())));

  private static final Matcher<ExpressionTree> WAS_INTERRUPTED =
      instanceMethod()
          .onDescendantOf("com.google.common.util.concurrent.AbstractFuture")
          .named("wasInterrupted")
          .withNoParameters();

  private static final Supplier<Symbol> JAVA_UTIL_CONCURRENT_FUTURE =
      memoize(state -> state.getSymbolFromString("java.util.concurrent.Future"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH;
    }
    // Thread.interrupt() other than Thread.currentThread().interrupt() (handles restoring an
    // interrupt after catching it)
    if (INTERRUPT_OTHER_THREAD.matches(tree, state)) {
      return buildDescription(tree)
          .setMessage(
              "Thread.interrupt should not be called, except to record the interrupt status on the"
                  + " current thread when dealing with InterruptedException")
          .build();
    }
    // Future.cancel calls ...
    if (!CANCEL.matches(tree, state)) {
      return NO_MATCH;
    }
    // ... unless they pass a literal 'false'
    ExpressionTree argument = getOnlyElement(tree.getArguments());
    if (Objects.equals(constValue(argument, Boolean.class), Boolean.FALSE)) {
      return NO_MATCH;
    }
    // ... OR cancel(AbstractFuture.wasInterrupted())
    if (WAS_INTERRUPTED.matches(argument, state)) {
      return NO_MATCH;
    }
    // ... OR
    //
    //    @Override
    //    public boolean cancel(boolean mayInterruptIfRunning) {
    //  ...
    //      someFuture.cancel(mayInterruptIfRunning);
    //  ...
    //    }
    if (delegatingCancelMethod(state, argument)) {
      return NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.replace(argument, "false"));
  }

  private boolean delegatingCancelMethod(VisitorState state, ExpressionTree argument) {
    Symbol sym = getSymbol(argument);
    if (sym == null) {
      return false;
    }
    if (!sym.getKind().equals(ElementKind.PARAMETER)) {
      return false;
    }
    MethodSymbol methodSymbol = (MethodSymbol) sym.owner;
    if (methodSymbol.getParameters().size() != 1
        || !methodSymbol.getSimpleName().contentEquals("cancel")) {
      return false;
    }
    Symbol.ClassSymbol classSymbol = enclosingClass(methodSymbol);
    if (!classSymbol.isSubClass(JAVA_UTIL_CONCURRENT_FUTURE.get(state), state.getTypes())) {
      return false;
    }
    return true;
  }
}
