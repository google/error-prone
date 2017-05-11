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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.base.Joiner;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.LambdaExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByExpression.Kind;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByExpression.Select;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByUtils.GuardedByValidationResult;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "GuardedBy",
  altNames = "GuardedByChecker",
  summary = "Checks for unguarded accesses to fields and methods with @GuardedBy annotations",
  category = JDK,
  severity = ERROR
)
public class GuardedByChecker extends BugChecker
    implements VariableTreeMatcher, MethodTreeMatcher, LambdaExpressionTreeMatcher {

  private static final String JUC_READ_WRITE_LOCK = "java.util.concurrent.locks.ReadWriteLock";

  @Override
  public Description matchMethod(MethodTree tree, final VisitorState state) {
    // Constructors (and field initializers, instance initalizers, and class initalizers) are free
    // to mutate guarded state without holding the necessary locks. It is assumed that all objects
    // (and classes) are thread-local during initialization.
    if (ASTHelpers.getSymbol(tree).isConstructor()) {
      return NO_MATCH;
    }
    analyze(state);
    return validate(tree, state);
  }

  @Override
  public Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state) {
    analyze(state.withPath(new TreePath(state.getPath(), tree.getBody())));
    return NO_MATCH;
  }

  private void analyze(final VisitorState state) {
    HeldLockAnalyzer.analyze(
        state,
        (ExpressionTree tree, GuardedByExpression guard, HeldLockSet live) ->
            report(GuardedByChecker.this.checkGuardedAccess(tree, guard, live, state), state),
        this::isSuppressed);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    // We only want to check field declarations for @GuardedBy usage. The VariableTree might be
    // for a local or a parameter, but they won't have @GuardedBy annotations.
    //
    // Field initializers (like constructors) are not checked for accesses of guarded fields.
    return validate(tree, state);
  }

  protected Description checkGuardedAccess(
      Tree tree, GuardedByExpression guard, HeldLockSet locks, VisitorState state) {

    // TODO(cushon): support ReadWriteLocks
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
      return NO_MATCH;
    }

    if (locks.allLocks().contains(guard)) {
      return NO_MATCH;
    }

    // TODO(cushon): re-enable once the clean-up is done
    if (guard.kind() == GuardedByExpression.Kind.ERROR) {
      return NO_MATCH;
    }

    return buildDescription(tree).setMessage(buildMessage(guard, locks)).build();
  }

  /**
   * Construct a diagnostic message, e.g.:
   *
   * <ul>
   *   <li>This access should be guarded by 'this', which is not currently held
   *   <li>This access should be guarded by 'this'; instead found 'mu'
   *   <li>This access should be guarded by 'this'; instead found: 'mu1', 'mu2'
   * </ul>
   */
  private String buildMessage(GuardedByExpression guard, HeldLockSet locks) {
    int heldLocks = locks.allLocks().size();
    StringBuilder message = new StringBuilder();
    Select enclosing = findOuterInstance(guard);
    if (enclosing != null && !enclosingInstance(guard)) {
      if (guard == enclosing) {
        message.append(
            String.format(
                "Access should be guarded by enclosing instance '%s' of '%s',"
                    + " which is not accessible in this scope",
                enclosing.sym().owner, enclosing.base()));
      } else {
        message.append(
            String.format(
                "Access should be guarded by '%s' in enclosing instance '%s' of '%s',"
                    + " which is not accessible in this scope",
                guard.sym(), enclosing.sym().owner, enclosing.base()));
      }
      if (heldLocks > 0) {
        message.append(
            String.format("; instead found: '%s'", Joiner.on("', '").join(locks.allLocks())));
      }
      return message.toString();
    }
    message.append(String.format("This access should be guarded by '%s'", guard));
    if (guard.kind() == GuardedByExpression.Kind.ERROR) {
      message.append(", which could not be resolved");
      return message.toString();
    }
    if (heldLocks == 0) {
      message.append(", which is not currently held");
    } else {
      message.append(
          String.format("; instead found: '%s'", Joiner.on("', '").join(locks.allLocks())));
    }
    return message.toString();
  }

  private static Select findOuterInstance(GuardedByExpression expr) {
    while (expr.kind() == Kind.SELECT) {
      Select select = (Select) expr;
      if (select.sym().name.contentEquals(GuardedByExpression.ENCLOSING_INSTANCE_NAME)) {
        return select;
      }
      expr = select.base();
    }
    return null;
  }

  private boolean enclosingInstance(GuardedByExpression expr) {
    while (expr.kind() == Kind.SELECT) {
      expr = ((Select) expr).base();
      if (expr.kind() == Kind.THIS) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the lock expression corresponds to a {@code
   * java.util.concurrent.locks.ReadWriteLock}.
   */
  private static boolean isRWLock(GuardedByExpression guard, VisitorState state) {
    Type guardType = guard.type();
    if (guardType == null) {
      return false;
    }

    Symbol rwLockSymbol = state.getSymbolFromString(JUC_READ_WRITE_LOCK);
    if (rwLockSymbol == null) {
      return false;
    }

    return state.getTypes().isSubtype(guardType, rwLockSymbol.type);
  }

  // TODO(cushon) - this is a hack. Provide an abstraction for matchers that need to do
  // stateful visiting? (e.g. a traversal that passes along a set of held locks...)
  private void report(Description description, VisitorState state) {
    if (description == null || description == NO_MATCH) {
      return;
    }
    state.reportMatch(description);
  }

  /** Validates that {@code @GuardedBy} strings can be resolved. */
  Description validate(Tree tree, VisitorState state) {
    GuardedByValidationResult result = GuardedByUtils.isGuardedByValid(tree, state);
    if (result.isValid()) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage(String.format("Invalid @GuardedBy expression: %s", result.message()))
        .build();
  }
}
