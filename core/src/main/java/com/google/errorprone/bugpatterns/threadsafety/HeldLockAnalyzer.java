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

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isDescendantOfMethod;
import static com.google.errorprone.matchers.Matchers.methodSelect;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByExpression.Select;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/**
 * Analyzes a method body, tracking the set of held locks and checking accesses to guarded members.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class HeldLockAnalyzer {

  /**
   * Listener interface for accesses to guarded members.
   */
  public interface LockEventListener {
    
    /**
     * Handle a guarded member access.
     * 
     * @param tree The member access expression.
     * @param guard The member's guard expression.
     * @param locks The set of held locks.
     */
    void handleGuardedAccess(ExpressionTree tree, GuardedByExpression guard, HeldLockSet locks);
  }

  /**
   * Analyze a method body, tracking the set of held locks and checking accesses to guarded members.
   */
  public static void analyze(VisitorState state, LockEventListener listener) {
    new LockScanner(state, listener).scan(state.getPath(), HeldLockSet.empty());
  }

  private static class LockScanner extends TreePathScanner<Void, HeldLockSet> {

    private final VisitorState visitorState;
    private final LockEventListener listener;

    private static final GuardedByExpression.Factory F = new GuardedByExpression.Factory();

    private LockScanner(VisitorState visitorState, LockEventListener listener) {
      this.visitorState = visitorState;
      this.listener = listener;
    }

    @Override
    public Void visitMethod(MethodTree tree, HeldLockSet locks) {
      // Synchronized instance methods hold the 'this' lock; synchronized static methods
      // hold the Class lock for the enclosing class.
      Set<Modifier> mods = tree.getModifiers().getFlags();
      if (mods.contains(Modifier.SYNCHRONIZED)) {
        Symbol owner = (((JCTree.JCMethodDecl) tree).sym.owner);
        GuardedByExpression lock =
            mods.contains(Modifier.STATIC) ? F.classLiteral(owner) : F.thisliteral(owner);
        locks = locks.plus(lock);
      }

      // @GuardedBy annotations on methods are trusted for declarations, and checked
      // for invocations.
      String guard = GuardedByUtils.getGuardValue(tree);
      if (guard != null) {
        Symbol enclosingClass = ASTHelpers.getSymbol(tree).owner;
        GuardedByExpression bound;
        try {
          bound = GuardedByBinder.bindString(guard,
              GuardedBySymbolResolver.fromVisitorState(enclosingClass, visitorState),
              visitorState.context);
          locks = locks.plus(bound);
        } catch (IllegalGuardedBy unused) {
          // Errors about bad @GuardedBy expressions are handled earlier.
        }
      }

      return super.visitMethod(tree, locks);
    }

    @Override
    public Void visitTry(TryTree tree, HeldLockSet locks) {
      // TODO(cushon) - recognize common try-with-resources patterns
      // Currently there is no standard implementation of an AutoCloseable lock resource to detect.
      scan(tree.getResources(), locks);

      // Cheesy try/finally heuristic: assume that all locks released in the finally
      // are held for the entire body of the try statement.
      scan(tree.getBlock(), 
          locks.plusAll(ReleasedLockFinder.find(tree.getFinallyBlock(), visitorState)));

      scan(tree.getCatches(), locks);
      scan(tree.getFinallyBlock(), locks);
      return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree tree, HeldLockSet locks) {
      // The synchronized expression is held in the body of the synchronized statement:
      scan(tree.getBlock(),
          locks.plus(GuardedByBinder.bindExpression(
              ((JCTree.JCParens) tree.getExpression()).getExpression())));
      return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, HeldLockSet p) {
      checkMatch(tree, p);
      return super.visitMemberSelect(tree, p);
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, HeldLockSet p) {
      checkMatch(tree, p);
      return super.visitIdentifier(tree, p);
    }

    private void checkMatch(ExpressionTree tree, HeldLockSet locks) {
      String guardString = GuardedByUtils.getGuardValue(tree);
      if (guardString == null) {
        return;
      }

      GuardedByExpression boundGuard = null;
      try {
        GuardedBySymbolResolver context = GuardedBySymbolResolver.from(
            ASTHelpers.getSymbol(tree).owner,
            (JCTree.JCCompilationUnit) getCurrentPath().getCompilationUnit(),
            visitorState.context,
            null);
        GuardedByExpression guard =
            GuardedByBinder.bindString(guardString, context, visitorState.context);
        boundGuard = ExpectedLockCalculator.from((JCTree.JCExpression) tree, guard);
      } catch (IllegalGuardedBy unused) {
        // Errors about bad @GuardedBy expressions are handled earlier.
      }

      if (boundGuard != null) {
        listener.handleGuardedAccess(tree, boundGuard, locks);
      }

      return;
    }
  }

  /**
   * Find the locks that are released in the given tree.
   * (e.g. the 'finally' clause of a try/finally)
   */
  private static class ReleasedLockFinder extends TreeScanner<Void, Void> {

    static Collection<GuardedByExpression> find(Tree tree, VisitorState state) {
      if (tree == null) {
        return Collections.emptyList();
      }
      ReleasedLockFinder finder = new ReleasedLockFinder(state);
      tree.accept(finder, null);
      return finder.locks;
    }

    private static final String LOCK_CLASS = "java.util.concurrent.locks.Lock";
    private static final String READ_WRITE_LOCK_CLASS = "java.util.concurrent.locks.ReadWriteLock";
    private static final String MONITOR_CLASS = "com.google.common.util.concurrent.Monitor";

    /** Matcher for methods that release lock resources. */
    private static final Matcher<MethodInvocationTree> LOCK_RELEASE_MATCHER = methodSelect(anyOf(
        isDescendantOfMethod(LOCK_CLASS, "unlock()"),
        isDescendantOfMethod(MONITOR_CLASS, "leave()")));

    /** Matcher for ReadWriteLock lock accessors. */
    private static final Matcher<ExpressionTree> READ_WRITE_RELEASE_MATCHER =
        Matchers.expressionMethodSelect(
            anyOf(
                isDescendantOfMethod(READ_WRITE_LOCK_CLASS, "readLock()"),
                isDescendantOfMethod(READ_WRITE_LOCK_CLASS, "writeLock()")));

    private final VisitorState state;
    private final Set<GuardedByExpression> locks = new HashSet<GuardedByExpression>();

    private ReleasedLockFinder(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
      if (LOCK_RELEASE_MATCHER.matches(tree, state)) {
        GuardedByExpression node = GuardedByBinder.bindExpression((JCTree.JCExpression) tree);
        GuardedByExpression receiver = ((GuardedByExpression.Select) node).base;
        locks.add(receiver);

        // The analysis interprets members guarded by {@link ReadWriteLock}s as requiring that
        // either the read or write lock is held for all accesses, but doesn't enforce a policy
        // for which of the two is held. Technically the write lock should be required while writing
        // to the guarded member and the read lock should be used for all other accesses, but in
        // practice the write lock is frequently held while performing a mutating operation on the
        // object stored in the field (e.g. inserting into a List).
        // TODO(cushon): investigate a better way to specify the contract for ReadWriteLocks.
        if (READ_WRITE_RELEASE_MATCHER.matches(ASTHelpers.getReceiver(tree), state)) {
          locks.add(((Select) receiver).base);
        }
      }
      return null;
    }
  }

  static class ExpectedLockCalculator {

    private static final GuardedByExpression.Factory F = new GuardedByExpression.Factory();

    /**
     * Determine the lock expression that needs to be held when accessing a specific guarded
     * member.
     *
     * If the lock expression resolves to an instance member, the result will be a select
     * expression with the same base as the original guarded member access.
     *
     * For example:
     * <code>
     * class MyClass {
     *   final Object mu = new Object();
     *   @GuardedBy("mu")
     *   int x;
     * }
     * void m(MyClass myClass) {
     *   myClass.x++;
     * }
     * </code>
     *
     * To determine the lock that must be held when accessing myClass.x, 
     * from is called with "myClass.x" and "mu", and returns "myClass.mu".
     */
    static GuardedByExpression from(
        JCTree.JCExpression guardedMemberExpression, GuardedByExpression guard) {
      if (guard.sym().isStatic() || guard.sym().getKind() == ElementKind.CLASS) {
        return guard;
      }

      GuardedByExpression guardedMember = GuardedByBinder.bindExpression(guardedMemberExpression);
      return helper(guard, ((GuardedByExpression.Select) guardedMember).base);
    }

    private static GuardedByExpression helper(
        GuardedByExpression lockExpression, GuardedByExpression memberAccess) {
      switch (lockExpression.kind()) {
        case SELECT: {
          GuardedByExpression.Select lockSelect = (GuardedByExpression.Select) lockExpression;
          return F.select(helper(lockSelect.base, memberAccess), lockSelect.sym());
        }
        case THIS_LITERAL:
          return memberAccess;
        default:
          throw new IllegalGuardedBy(lockExpression.toString());
      }
    }
  }
}
