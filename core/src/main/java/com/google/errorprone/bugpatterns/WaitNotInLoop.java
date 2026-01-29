/*
 * Copyright 2013 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.inLoop;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree.JCIf;
import java.util.regex.Pattern;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
// TODO(eaftan): Doesn't handle the case that the enclosing method is always called in a loop.
@BugPattern(
    summary =
        "Because of spurious wakeups, Object.wait() and Condition.await() must always be "
            + "called in a loop",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public class WaitNotInLoop extends BugChecker implements MethodInvocationTreeMatcher {
  private static final String OBJECT_FQN = "java.lang.Object";
  private static final String CONDITION_FQN = "java.util.concurrent.locks.Condition";

  private static final Matcher<MethodInvocationTree> UNINTERRUPTIBLES_AWAIT_CONDITION =
      anyOf(
          staticMethod()
              .onClass("com.google.common.util.concurrent.Uninterruptibles")
              .named("awaitUninterruptibly")
              .withParameters(
                  "java.util.concurrent.locks.Condition", "long", "java.util.concurrent.TimeUnit"),
          staticMethod()
              .onClass("com.google.common.util.concurrent.Uninterruptibles")
              .named("awaitUninterruptibly")
              .withParameters("java.util.concurrent.locks.Condition", "java.time.Duration"));

  private static final Matcher<MethodInvocationTree> WAIT_METHOD =
      anyOf(
          instanceMethod().onExactClass(OBJECT_FQN).named("wait"),
          instanceMethod()
              .onDescendantOf(CONDITION_FQN)
              .withNameMatching(Pattern.compile("await.*")),
          UNINTERRUPTIBLES_AWAIT_CONDITION);

  private static final Matcher<MethodInvocationTree> WAIT_METHOD_WITH_TIMEOUT =
      anyOf(
          instanceMethod().onExactClass(OBJECT_FQN).named("wait").withParameters("long"),
          instanceMethod().onExactClass(OBJECT_FQN).named("wait").withParameters("long", "int"),
          instanceMethod()
              .onDescendantOf(CONDITION_FQN)
              .named("await")
              .withParameters("long", "java.util.concurrent.TimeUnit"),
          instanceMethod().onDescendantOf(CONDITION_FQN).named("awaitNanos"),
          instanceMethod().onDescendantOf(CONDITION_FQN).named("awaitUntil"),
          staticMethod().onClass("com.google.common.time.Durations").named("wait"),
          UNINTERRUPTIBLES_AWAIT_CONDITION);

  private static final Matcher<MethodInvocationTree> matcher = allOf(WAIT_METHOD, not(inLoop()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!matcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Description.Builder description = buildDescription(tree);
    MethodSymbol sym = getSymbol(tree);
    description.setMessage(
        String.format("Because of spurious wakeups, %s must always be called in a loop", sym));

    // If this looks like the "Wait until a condition becomes true" case from the wiki content,
    // rewrite the enclosing if to a while.  Other fixes are too complicated to construct
    // mechanically, so we provide detailed instructions in the wiki content.
    if (!WAIT_METHOD_WITH_TIMEOUT.matches(tree, state)) {
      JCIf enclosingIf = findEnclosingNode(state.getPath().getParentPath(), JCIf.class);
      if (enclosingIf != null && enclosingIf.getElseStatement() == null) {
        String ifSource = state.getSourceForNode(enclosingIf);
        if (ifSource == null) {
          // Source isn't available, so we can't construct a fix
          return description.build();
        }
        String replacement = ifSource.toString().replaceFirst("if", "while");
        return description.addFix(SuggestedFix.replace(enclosingIf, replacement)).build();
      }
    }

    return description.build();
  }
}
