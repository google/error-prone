/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.inLoop;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.WaitMatchers.waitMethod;
import static com.google.errorprone.matchers.WaitMatchers.waitMethodWithTimeout;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree.JCIf;

/** @author eaftan@google.com (Eddie Aftandilian) */
// TODO(eaftan): Doesn't handle the case that the enclosing method is always called in a loop.
@BugPattern(
  name = "WaitNotInLoop",
  summary =
      "Because of spurious wakeups, Object.wait() and Condition.await() must always be "
          + "called in a loop",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE
)
public class WaitNotInLoop extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String MESSAGE_TEMPLATE =
      "Because of spurious wakeups, %s must always be called in a loop";

  private static final Matcher<MethodInvocationTree> matcher = allOf(waitMethod, not(inLoop()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!matcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Description.Builder description = buildDescription(tree);
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym != null) {
      description.setMessage(String.format(MESSAGE_TEMPLATE, sym));
    }

    // If this looks like the "Wait until a condition becomes true" case from the wiki content,
    // rewrite the enclosing if to a while.  Other fixes are too complicated to construct
    // mechanically, so we provide detailed instructions in the wiki content.
    if (!waitMethodWithTimeout.matches(tree, state)) {
      JCIf enclosingIf = ASTHelpers.findEnclosingNode(state.getPath().getParentPath(), JCIf.class);
      if (enclosingIf != null && enclosingIf.getElseStatement() == null) {
        CharSequence ifSource = state.getSourceForNode(enclosingIf);
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
