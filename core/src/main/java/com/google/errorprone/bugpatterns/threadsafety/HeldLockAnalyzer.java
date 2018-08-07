/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.concurrent.UnlockMethod;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByExpression.Kind;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByExpression.Select;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 * A method body analyzer. Responsible for tracking the set of held locks, and checking accesses to
 * guarded members.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class HeldLockAnalyzer {

  /** Listener interface for accesses to guarded members. */
  public interface LockEventListener {

    /**
     * Handles a guarded member access.
     *
     * @param tree The member access expression.
     * @param guard The member's guard expression.
     * @param locks The set of held locks.
     */
    void handleGuardedAccess(ExpressionTree tree, GuardedByExpression guard, HeldLockSet locks);
  }

  /**
   * Analyzes a method body, tracking the set of held locks and checking accesses to guarded
   * members.
   */
  public static void analyze(
      VisitorState state, LockEventListener listener, Predicate<Tree> isSuppressed) {
    HeldLockSet locks = HeldLockSet.empty();
    locks = handleMonitorGuards(state, locks);
    new LockScanner(state, listener, isSuppressed).scan(state.getPath(), locks);
  }

  // Don't use Class#getName() for inner classes, we don't want `Monitor$Guard`
  private static final String MONITOR_GUARD_CLASS =
      "com.google.common.util.concurrent.Monitor.Guard";

  private static HeldLockSet handleMonitorGuards(VisitorState state, HeldLockSet locks) {
    JCNewClass newClassTree = ASTHelpers.findEnclosingNode(state.getPath(), JCNewClass.class);
    if (newClassTree == null) {
      return locks;
    }
    Symbol clazzSym = ASTHelpers.getSymbol(newClassTree.clazz);
    if (!(clazzSym instanceof ClassSymbol)) {
      return locks;
    }
    if (!((ClassSymbol) clazzSym).fullname.contentEquals(MONITOR_GUARD_CLASS)) {
      return locks;
    }
    Optional<GuardedByExpression> lockExpression =
        GuardedByBinder.bindExpression(
            Iterables.getOnlyElement(newClassTree.getArguments()), state);
    if (!lockExpression.isPresent()) {
      return locks;
    }
    return locks.plus(lockExpression.get());
  }

  private static class LockScanner extends TreePathScanner<Void, HeldLockSet> {

    private final VisitorState visitorState;
    private final LockEventListener listener;
    private final Predicate<Tree> isSuppressed;

    private static final GuardedByExpression.Factory F = new GuardedByExpression.Factory();

    private LockScanner(
        VisitorState visitorState, LockEventListener listener, Predicate<Tree> isSuppressed) {
      this.visitorState = visitorState;
      this.listener = listener;
      this.isSuppressed = isSuppressed;
    }

    @Override
    public Void visitMethod(MethodTree tree, HeldLockSet locks) {
      // Synchronized instance methods hold the 'this' lock; synchronized static methods
      // hold the Class lock for the enclosing class.
      Set<Modifier> mods = tree.getModifiers().getFlags();
      if (mods.contains(Modifier.SYNCHRONIZED)) {
        Symbol owner = (((JCTree.JCMethodDecl) tree).sym.owner);
        GuardedByExpression lock =
            mods.contains(Modifier.STATIC) ? F.classLiteral(owner) : F.thisliteral();
        locks = locks.plus(lock);
      }

      // @GuardedBy annotations on methods are trusted for declarations, and checked
      // for invocations.
      for (String guard : GuardedByUtils.getGuardValues(tree, visitorState)) {
        Optional<GuardedByExpression> bound =
            GuardedByBinder.bindString(guard, GuardedBySymbolResolver.from(tree, visitorState));
        if (bound.isPresent()) {
          locks = locks.plus(bound.get());
        }
      }

      return super.visitMethod(tree, locks);
    }

    @Override
    public Void visitTry(TryTree tree, HeldLockSet locks) {
      scan(tree.getResources(), locks);

      List<? extends Tree> resources = tree.getResources();
      scan(resources, locks);

      // Cheesy try/finally heuristic: assume that all locks released in the finally
      // are held for the entirety of the try and catch statements.
      Collection<GuardedByExpression> releasedLocks =
          ReleasedLockFinder.find(tree.getFinallyBlock(), visitorState);
      if (resources.isEmpty()) {
        scan(tree.getBlock(), locks.plusAll(releasedLocks));
      } else {
        // We don't know what to do with the try-with-resources block.
        // TODO(cushon) - recognize common try-with-resources patterns. Currently there is no
        // standard implementation of an AutoCloseable lock resource to detect.
      }
      scan(tree.getCatches(), locks.plusAll(releasedLocks));
      scan(tree.getFinallyBlock(), locks);
      return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree tree, HeldLockSet locks) {
      // The synchronized expression is held in the body of the synchronized statement:
      Optional<GuardedByExpression> lockExpression =
          GuardedByBinder.bindExpression((JCExpression) tree.getExpression(), visitorState);
      scan(tree.getBlock(), lockExpression.isPresent() ? locks.plus(lockExpression.get()) : locks);
      return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, HeldLockSet locks) {
      checkMatch(tree, locks);
      return super.visitMemberSelect(tree, locks);
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, HeldLockSet locks) {
      checkMatch(tree, locks);
      return super.visitIdentifier(tree, locks);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, HeldLockSet locks) {
      scan(tree.getEnclosingExpression(), locks);
      scan(tree.getIdentifier(), locks);
      scan(tree.getTypeArguments(), locks);
      scan(tree.getArguments(), locks);
      // Don't descend into bodies of anonymous class declarations;
      // their method declarations will be analyzed separately.
      return null;
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree node, HeldLockSet heldLockSet) {
      // Don't descend into lambdas; they will be analyzed separately.
      return null;
    }

    @Override
    public Void visitVariable(VariableTree node, HeldLockSet locks) {
      if (!isSuppressed.apply(node)) {
        return super.visitVariable(node, locks);
      } else {
        return null;
      }
    }

    private void checkMatch(ExpressionTree tree, HeldLockSet locks) {
      for (String guardString : GuardedByUtils.getGuardValues(tree, visitorState)) {
        GuardedByBinder.bindString(guardString, GuardedBySymbolResolver.from(tree, visitorState))
            .ifPresent(
                guard -> {
                  Optional<GuardedByExpression> boundGuard =
                      ExpectedLockCalculator.from((JCTree.JCExpression) tree, guard, visitorState);
                  if (!boundGuard.isPresent()) {
                    // We couldn't resolve a guarded by expression in the current scope, so we can't
                    // guarantee the access is protected and must report an error to be safe.
                    listener.handleGuardedAccess(
                        tree, new GuardedByExpression.Factory().error(guardString), locks);
                    return;
                  }
                  listener.handleGuardedAccess(tree, boundGuard.get(), locks);
                });
      }
    }
  }

  /** An abstraction over the lock classes we understand. */
  @AutoValue
  abstract static class LockResource {

    /** The fully-qualified name of the lock class. */
    abstract String className();

    /** The method that acquires the lock. */
    abstract String lockMethod();

    /** The method that releases the lock. */
    abstract String unlockMethod();

    public Matcher<ExpressionTree> createUnlockMatcher() {
      return instanceMethod().onDescendantOf(className()).named(unlockMethod());
    }

    public Matcher<ExpressionTree> createLockMatcher() {
      return instanceMethod().onDescendantOf(className()).named(lockMethod());
    }

    static LockResource create(String className, String lockMethod, String unlockMethod) {
      return new AutoValue_HeldLockAnalyzer_LockResource(className, lockMethod, unlockMethod);
    }
  }

  /** The set of supported lock classes. */
  private static final ImmutableList<LockResource> LOCK_RESOURCES =
      ImmutableList.of(
          LockResource.create("java.util.concurrent.locks.Lock", "lock", "unlock"),
          LockResource.create("com.google.common.util.concurrent.Monitor", "enter", "leave"),
          LockResource.create("java.util.concurrent.Semaphore", "acquire", "release"));

  private static class LockOperationFinder extends TreeScanner<Void, Void> {

    static Collection<GuardedByExpression> find(
        Tree tree, VisitorState state, Matcher<ExpressionTree> lockOperationMatcher) {
      if (tree == null) {
        return Collections.emptyList();
      }
      LockOperationFinder finder = new LockOperationFinder(state, lockOperationMatcher);
      tree.accept(finder, null);
      return finder.locks;
    }

    private static final String READ_WRITE_LOCK_CLASS = "java.util.concurrent.locks.ReadWriteLock";

    private final Matcher<ExpressionTree> lockOperationMatcher;

    /** Matcher for ReadWriteLock lock accessors. */
    private static final Matcher<ExpressionTree> READ_WRITE_ACCESSOR_MATCHER =
        Matchers.<ExpressionTree>anyOf(
            instanceMethod().onDescendantOf(READ_WRITE_LOCK_CLASS).named("readLock"),
            instanceMethod().onDescendantOf(READ_WRITE_LOCK_CLASS).named("writeLock"));

    private final VisitorState state;
    private final Set<GuardedByExpression> locks = new HashSet<>();

    private LockOperationFinder(VisitorState state, Matcher<ExpressionTree> lockOperationMatcher) {
      this.state = state;
      this.lockOperationMatcher = lockOperationMatcher;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
      handleReleasedLocks(tree);
      handleUnlockAnnotatedMethods(tree);
      return null;
    }

    /**
     * Checks for locks that are released directly. Currently only {@link
     * java.util.concurrent.locks.Lock#unlock()} is supported.
     *
     * <p>TODO(cushon): Semaphores, CAS, ... ?
     */
    private void handleReleasedLocks(MethodInvocationTree tree) {
      if (!lockOperationMatcher.matches(tree, state)) {
        return;
      }
      Optional<GuardedByExpression> node =
          GuardedByBinder.bindExpression((JCExpression) tree, state);
      if (node.isPresent()) {
        GuardedByExpression receiver = ((GuardedByExpression.Select) node.get()).base();
        locks.add(receiver);

        // The analysis interprets members guarded by {@link ReadWriteLock}s as requiring that
        // either the read or write lock is held for all accesses, but doesn't enforce a policy
        // for which of the two is held. Technically the write lock should be required while
        // writing to the guarded member and the read lock should be used for all other accesses,
        // but in practice the write lock is frequently held while performing a mutating operation
        // on the object stored in the field (e.g. inserting into a List).
        // TODO(cushon): investigate a better way to specify the contract for ReadWriteLocks.
        if ((tree.getMethodSelect() instanceof MemberSelectTree)
            && READ_WRITE_ACCESSOR_MATCHER.matches(ASTHelpers.getReceiver(tree), state)) {
          locks.add(((Select) receiver).base());
        }
      }
    }

    /** Checks {@link UnlockMethod}-annotated methods. */
    private void handleUnlockAnnotatedMethods(MethodInvocationTree tree) {
      UnlockMethod annotation = ASTHelpers.getAnnotation(tree, UnlockMethod.class);
      if (annotation == null) {
        return;
      }
      for (String lockString : annotation.value()) {
        Optional<GuardedByExpression> guard =
            GuardedByBinder.bindString(lockString, GuardedBySymbolResolver.from(tree, state));
        // TODO(cushon): http://docs.oracle.com/javase/8/docs/api/java/util/Optional.html#ifPresent
        if (guard.isPresent()) {
          Optional<GuardedByExpression> lock =
              ExpectedLockCalculator.from((JCExpression) tree, guard.get(), state);
          if (lock.isPresent()) {
            locks.add(lock.get());
          }
        }
      }
    }
  }

  /**
   * Find the locks that are released in the given tree. (e.g. the 'finally' clause of a
   * try/finally)
   */
  static class ReleasedLockFinder {

    /** Matcher for methods that release lock resources. */
    private static final Matcher<ExpressionTree> UNLOCK_MATCHER =
        Matchers.<ExpressionTree>anyOf(unlockMatchers());

    private static Iterable<Matcher<ExpressionTree>> unlockMatchers() {
      return Iterables.transform(
          LOCK_RESOURCES,
          new Function<LockResource, Matcher<ExpressionTree>>() {
            @Override
            public Matcher<ExpressionTree> apply(LockResource res) {
              return res.createUnlockMatcher();
            }
          });
    }

    static Collection<GuardedByExpression> find(Tree tree, VisitorState state) {
      return LockOperationFinder.find(tree, state, UNLOCK_MATCHER);
    }
  }

  /**
   * Find the locks that are acquired in the given tree. (e.g. the body of a @LockMethod-annotated
   * method.)
   */
  static class AcquiredLockFinder {

    /** Matcher for methods that acquire lock resources. */
    private static final Matcher<ExpressionTree> LOCK_MATCHER =
        Matchers.<ExpressionTree>anyOf(unlockMatchers());

    private static Iterable<Matcher<ExpressionTree>> unlockMatchers() {
      return Iterables.transform(
          LOCK_RESOURCES,
          new Function<LockResource, Matcher<ExpressionTree>>() {
            @Override
            public Matcher<ExpressionTree> apply(LockResource res) {
              return res.createLockMatcher();
            }
          });
    }

    static Collection<GuardedByExpression> find(Tree tree, VisitorState state) {
      return LockOperationFinder.find(tree, state, LOCK_MATCHER);
    }
  }

  static class ExpectedLockCalculator {

    private static final GuardedByExpression.Factory F = new GuardedByExpression.Factory();

    /**
     * Determine the lock expression that needs to be held when accessing a specific guarded member.
     *
     * <p>If the lock expression resolves to an instance member, the result will be a select
     * expression with the same base as the original guarded member access.
     *
     * <p>For example:
     *
     * <pre>{@code
     * class MyClass {
     *   final Object mu = new Object();
     *   @GuardedBy("mu")
     *   int x;
     * }
     * void m(MyClass myClass) {
     *   myClass.x++;
     * }
     * }</pre>
     *
     * To determine the lock that must be held when accessing myClass.x, from is called with
     * "myClass.x" and "mu", and returns "myClass.mu".
     */
    static Optional<GuardedByExpression> from(
        JCTree.JCExpression guardedMemberExpression,
        GuardedByExpression guard,
        VisitorState state) {

      if (isGuardReferenceAbsolute(guard)) {
        return Optional.of(guard);
      }

      Optional<GuardedByExpression> guardedMember =
          GuardedByBinder.bindExpression(guardedMemberExpression, state);

      if (!guardedMember.isPresent()) {
        return Optional.empty();
      }

      GuardedByExpression memberBase = ((GuardedByExpression.Select) guardedMember.get()).base();
      return Optional.of(helper(guard, memberBase));
    }

    /**
     * Returns true for guard expressions that require an 'absolute' reference, i.e. where the
     * expression to access the lock is always the same, regardless of how the guarded member is
     * accessed.
     *
     * <p>E.g.:
     *
     * <ul>
     *   <li>class object: 'TypeName.class'
     *   <li>static access: 'TypeName.member'
     *   <li>enclosing instance: 'Outer.this'
     *   <li>enclosing instance member: 'Outer.this.member'
     * </ul>
     */
    private static boolean isGuardReferenceAbsolute(GuardedByExpression guard) {

      GuardedByExpression instance = guard.kind() == Kind.SELECT ? getSelectInstance(guard) : guard;

      return instance.kind() != Kind.THIS;
    }

    /** Gets the base expression of a (possibly nested) member select expression. */
    private static GuardedByExpression getSelectInstance(GuardedByExpression guard) {
      if (guard instanceof Select) {
        return getSelectInstance(((Select) guard).base());
      }
      return guard;
    }

    private static GuardedByExpression helper(
        GuardedByExpression lockExpression, GuardedByExpression memberAccess) {
      switch (lockExpression.kind()) {
        case SELECT:
          {
            GuardedByExpression.Select lockSelect = (GuardedByExpression.Select) lockExpression;
            return F.select(helper(lockSelect.base(), memberAccess), lockSelect.sym());
          }
        case THIS:
          return memberAccess;
        default:
          throw new IllegalGuardedBy(lockExpression.toString());
      }
    }
  }
}
