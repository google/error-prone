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
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "GuardedByChecker",
    summary = "Checks for unguarded accesses to fields and methods with @GuardedBy annotations",
    explanation = "The @GuardedBy annotation is used to associate a lock with a fields or methods."
        + " Accessing a guarded field or invoking a guarded method should only be done when the"
        + " specified lock is held. Unguarded accesses are not thread safe.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class GuardedByChecker extends GuardedByValidator implements BugChecker.VariableTreeMatcher,
    BugChecker.MethodTreeMatcher {

  private static final String JUC_READ_WRITE_LOCK = "java.util.concurrent.locks.ReadWriteLock";

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
        report(tree, GuardedByChecker.this.checkGuardedAccess(tree, guard, live, state), state);
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
    // TODO(user) - consolidate the checks once the clean-up is done; GuardedByChecker is intended
    // to subsume GuardedByValidator.
    String message = GuardedByValidator.class.getAnnotation(BugPattern.class).summary();
    // TODO(user) - this message will have a wiki link to GuardedBy, not GuardedByValidator.
    // Think about the best way to present the information from GuardedByValidator's explanation
    // field -- should it be a separate page or part of the GuardedBy page?
    return buildDescription(tree)
        .setMessage(message)
        .build();
  }

  protected Description checkGuardedAccess(Tree tree, GuardedByExpression guard,
      HeldLockSet locks, VisitorState state) {

    // TODO(user): support ReadWriteLocks
    //
    // A common pattern with ReadWriteLocks is to create a copy (either a field or a local
    // variable) to refer to the read and write locks. The analysis currently can't
    // recognize that locking the copies is equivalent to locking the read or write
    // locks directly.
    //
    // Also - there are currently no annotations to specify an access policy for
    // members guarded by ReadWriteLocks. We could allow accesses when either the
    // read or write locks are held, but that's not much better than enforcing
    // nothing.
    if (isRWLock(guard, state)) {
      return Description.NO_MATCH;
    }

    if (!locks.allLocks().contains(guard)) {
      String message = String.format("Expected %s to be held, instead found %s", guard, locks);
      // TODO(user) - this fix is a debugging aid, remove it before productionizing the check.
      Fix fix = SuggestedFix.prefixWith(tree, String.format("/* %s */", message));
      return buildDescription(tree)
          .setMessage(message)
          .addFix(fix)
          .build();
    }
    return Description.NO_MATCH;
  }

  /**
   * Returns true if the lock expression corresponds to a
   * {@code java.util.concurrent.locks.ReadWriteLock}.
   */
  private static boolean isRWLock(GuardedByExpression guard, VisitorState state) {
    Symbol guardSym = guard.sym();
    if (guardSym == null) {
      return false;
    }

    Type guardType = guardSym.type;
    if (guardType == null) {
      return false;
    }

    Symbol rwLockSymbol = state.getSymbolFromString(JUC_READ_WRITE_LOCK);
    if (rwLockSymbol  == null) {
      return false;
    }

    return state.getTypes().isSubtype(guardType, rwLockSymbol.type);
  }

  // TODO(user) - this is kind of a hack. Provide an abstraction for matchers that need to do
  // stateful visiting? (e.g. a traversal that passes along a set of held locks...)
  private void report(Tree tree, Description description, VisitorState state) {
    if (description == null || description == Description.NO_MATCH) {
      return;
    }
    state.getMatchListener().onMatch(tree);
    state.getDescriptionListener().onDescribed(description);
  }
}
