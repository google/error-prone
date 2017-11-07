/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ForkJoinTask;
import java.util.regex.Pattern;

/** See BugPattern annotation. */
@BugPattern(
  name = "FutureReturnValueIgnored",
  summary =
      "Return value of methods returning Future must be checked. Ignoring returned Futures "
          + "suppresses exceptions thrown from the code that completes the Future.",
  explanation =
      "Methods that return `java.util.concurrent.Future` and its subclasses "
          + "generally indicate errors by returning a future that eventually fails.\n\n"
          + "If you don’t check the return value of these methods, you will never find out if they "
          + "threw an exception.",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE
)
public final class FutureReturnValueIgnored extends AbstractReturnValueIgnored {

  private static final Matcher<ExpressionTree> BLACKLIST =
      anyOf(
          // ForkJoinTask#fork has side-effects and returns 'this', so it's reasonable to ignore
          // the return value.
          instanceMethod()
              .onDescendantOf(ForkJoinTask.class.getName())
              .named("fork")
              .withParameters(),
          // CompletionService is intended to be used in a way where the Future returned
          // from submit is discarded, because the Futures are available later via e.g. take()
          instanceMethod().onDescendantOf(CompletionService.class.getName()).named("submit"),
          // IntelliJ's executeOnPooledThread wraps the Callable/Runnable in one that catches
          // Throwable, so it can't fail (unless logging the Throwable also throws, but there's
          // nothing much to be done at that point).
          instanceMethod()
              .onDescendantOf("com.intellij.openapi.application.Application")
              .named("executeOnPooledThread"),
          // ChannelFuture#addListern(s) returns itself for chaining. Any exception during the
          // future execution should be dealt by the listener(s).
          instanceMethod()
              .onDescendantOf("io.netty.channel.ChannelFuture")
              .withNameMatching(Pattern.compile("addListeners?")),
          instanceMethod()
              .onExactClass("java.util.concurrent.CompletableFuture")
              .withNameMatching(Pattern.compile("completeAsync|orTimeout|completeOnTimeout")));

  private static final Matcher<ExpressionTree> MATCHER =
      new Matcher<ExpressionTree>() {
        @Override
        public boolean matches(ExpressionTree tree, VisitorState state) {
          Type futureType =
              Objects.requireNonNull(state.getTypeFromString("java.util.concurrent.Future"));
          Symbol untypedSymbol = ASTHelpers.getSymbol(tree);
          if (!(untypedSymbol instanceof MethodSymbol)) {
            return false;
          }
          MethodSymbol sym = (MethodSymbol) untypedSymbol;
          if (hasAnnotation(sym, CanIgnoreReturnValue.class, state)) {
            return false;
          }
          for (MethodSymbol superSym : ASTHelpers.findSuperMethods(sym, state.getTypes())) {
            // There are interfaces annotated with @CanIgnoreReturnValue (like Guava's Function)
            // whose return value really shouldn't be ignored - as a heuristic, check if the super's
            // method is returning a future subtype.
            if (hasAnnotation(superSym, CanIgnoreReturnValue.class, state)
                && ASTHelpers.isSubtype(
                    ASTHelpers.getUpperBound(superSym.getReturnType(), state.getTypes()),
                    futureType,
                    state)) {
              return false;
            }
          }
          if (BLACKLIST.matches(tree, state)) {
            return false;
          }
          Type returnType = sym.getReturnType();
          return ASTHelpers.isSubtype(
              ASTHelpers.getUpperBound(returnType, state.getTypes()), futureType, state);
        }
      };

  @Override
  public Matcher<? super ExpressionTree> specializedMatcher() {
    return MATCHER;
  }
}
