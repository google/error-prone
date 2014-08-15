/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "ThreadSafe",
    summary = "Checks for unguarded accesses to fields and methods with @GuardedBy annotations",
    explanation = "The @GuardedBy annotation is used to associate a lock with a fields or methods."
        + " Accessing a guarded field or invoking a guarded method should only be done when the"
        + " specified lock is held. Unguarded accesses are not thread safe.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class ThreadSafe extends GuardedByValidator implements BugChecker.VariableTreeMatcher,
    BugChecker.MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, final VisitorState state) {
    // Constructors are free to mutate guarded state without holding the necessary locks. It is
    // assumed that all objects are thread-local during initialization.
    if (ASTHelpers.getSymbol(tree).isConstructor()) {
      return Description.NO_MATCH;
    }

    HeldLockAnalyzer.analyze(state, new HeldLockAnalyzer.LockEventListener() {
      @Override
      public void handleGuardedAccess(
          ExpressionTree tree, GuardedByExpression guard, HeldLockSet live) {
        report(tree, ThreadSafe.this.checkGuardedAccess(tree, guard, live), state);
      }
    });

    return GuardedByUtils.isGuardedByValid(tree, state)
        ? Description.NO_MATCH
        : describeInvalidGuardedBy(tree);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    // Only field declarations can have @GuardedBy annotations, so filter out parameters and
    // local variables (which are also represented as {@link VariableTree}s in the AST).
    if (!ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class).getMembers()
        .contains(tree)) {
      return Description.NO_MATCH;
    }

    return GuardedByUtils.isGuardedByValid(tree, state)
        ? Description.NO_MATCH
        : describeInvalidGuardedBy(tree);
  }

  private Description describeInvalidGuardedBy(Tree tree) {
    // Re-use the validation message from {@link GuardedByValidator}.
    // TODO(user) - consolidate the checks once the clean-up is done; ThreadSafe is intended to
    // subsume GuardedByValidator.
    String message = GuardedByValidator.class.getAnnotation(BugPattern.class).summary();
    // TODO(user) - this message will have a wiki link to ThreadSafe, not GuardedByValidator.
    // Think about the best way to present the information from GuardedByValidator's explanation
    // field -- should it be a separate page or part of the ThreadSafe page?
    return Description.builder(tree, pattern)
        .setMessage(message)
        .build();
  }

  protected Description checkGuardedAccess(Tree tree, GuardedByExpression guard,
      HeldLockSet locks) {
    if (!locks.allLocks().contains(guard)) {
      String message = String.format("Expected %s to be held, instead found %s", guard, locks);
      // TODO(user) - this fix is a debugging aid, remove it before productionizing the check.
      Fix fix = SuggestedFix.prefixWith(tree, String.format("/* %s */", message));
      return Description.builder(tree, pattern)
          .setMessage(message)
          .setFix(fix)
          .build();
    }
    return Description.NO_MATCH;
  }

  // TODO(user) - this is kind of a hack. Provide an abstraction for matchers that need to do
  // stateful visiting? (e.g. a traversal that passes along a set of held locks...)
  private void report(Tree tree, Description description, VisitorState state) {
    state.getMatchListener().onMatch(tree);
    state.getDescriptionListener().onDescribed(description);
  }
}
