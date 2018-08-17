/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.FRAGILE_CODE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodClassMatcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;

/**
 * Suggests that calls to {@code Lock.lock} must be immediately followed by a {@code try-finally}
 * that calls {@code Lock.unlock}.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "LockNotBeforeTry",
    summary =
        "Calls to Lock#lock should be immediately followed by a try block which releases the lock.",
    severity = WARNING,
    tags = FRAGILE_CODE,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class LockNotBeforeTry extends BugChecker implements MethodInvocationTreeMatcher {

  private static final MethodClassMatcher LOCK_METHOD =
      instanceMethod().onDescendantOf("java.util.concurrent.locks.Lock");
  private static final Matcher<ExpressionTree> LOCK = LOCK_METHOD.named("lock");
  private static final Matcher<ExpressionTree> UNLOCK = LOCK_METHOD.named("unlock");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!LOCK.matches(tree, state)) {
      return NO_MATCH;
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof StatementTree)) {
      return NO_MATCH;
    }
    Tree enclosing = state.getPath().getParentPath().getParentPath().getLeaf();
    if (!(enclosing instanceof BlockTree)) {
      return NO_MATCH;
    }
    BlockTree block = (BlockTree) enclosing;
    int index = block.getStatements().indexOf(parent);
    if (index + 1 < block.getStatements().size()) {
      StatementTree nextStatement = block.getStatements().get(index + 1);
      if (nextStatement instanceof TryTree) {
        return NO_MATCH;
      }
    }
    return describe(tree, state.getPath().getParentPath(), state);
  }

  private Description describe(
      MethodInvocationTree lockInvocation, TreePath statementPath, VisitorState state) {
    Tree lockStatement = statementPath.getLeaf();
    ExpressionTree lockee = getReceiver(lockInvocation);
    TryTree enclosingTry = state.findEnclosing(TryTree.class);
    if (enclosingTry != null && releases(enclosingTry, lockee, state)) {
      SuggestedFix fix =
          SuggestedFix.builder()
              .replace(lockStatement, "")
              .prefixWith(enclosingTry, state.getSourceForNode(lockStatement))
              .build();
      return buildDescription(lockInvocation)
          .addFix(fix)
          .setMessage(
              String.format(
                  "Prefer obtaining the lock for %s outside the try block. That way, if #lock"
                      + " throws, the lock is not erroneously released.",
                  state.getSourceForNode(getReceiver(lockInvocation))))
          .build();
    }
    Tree enclosing = state.getPath().getParentPath().getParentPath().getLeaf();
    if (!(enclosing instanceof BlockTree)) {
      return NO_MATCH;
    }
    BlockTree block = (BlockTree) enclosing;
    int index = block.getStatements().indexOf(lockStatement);
    // Scan through the enclosing statements for a try/finally which releases this lock.
    for (StatementTree maybeTry : Iterables.skip(block.getStatements(), index + 1)) {
      if (maybeTry instanceof TryTree && releases((TryTree) maybeTry, lockee, state)) {
        SuggestedFix fix =
            SuggestedFix.builder()
                .replace(lockStatement, "")
                .prefixWith(maybeTry, state.getSourceForNode(lockStatement))
                .build();
        return buildDescription(lockInvocation)
            .addFix(fix)
            .setMessage(
                "Prefer locking *immediately* before the try block which releases the lock to"
                    + " avoid the possibility of any intermediate statements throwing.")
            .build();
      }
    }
    // Scan through the enclosing statements for an unlock at the same level.
    for (StatementTree maybeUnlock : Iterables.skip(block.getStatements(), index + 1)) {
      if (!(maybeUnlock instanceof ExpressionStatementTree)) {
        continue;
      }
      ExpressionTree expression = ((ExpressionStatementTree) maybeUnlock).getExpression();
      if (releases(expression, lockee, state)) {
        SuggestedFix fix =
            SuggestedFix.builder()
                .postfixWith(lockStatement, "try {")
                .prefixWith(maybeUnlock, "} finally {")
                .postfixWith(maybeUnlock, "}")
                .build();
        return buildDescription(lockInvocation)
            .addFix(fix)
            .setMessage(
                String.format(
                    "Prefer releasing the lock on %s inside a finally block.",
                    state.getSourceForNode(getReceiver(lockInvocation))))
            .build();
      }
    }
    return NO_MATCH;
  }

  private static boolean releases(TryTree tryTree, ExpressionTree lockee, VisitorState state) {
    if (tryTree.getFinallyBlock() == null) {
      return false;
    }
    // False if a different lock was released, true if 'lockee' was released, null otherwise.
    Boolean released =
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean reduce(Boolean r1, Boolean r2) {
            return r1 == null ? r2 : (r2 == null ? null : r1 && r2);
          }

          @Override
          public Boolean visitMethodInvocation(MethodInvocationTree node, Void unused) {
            if (UNLOCK.matches(node, state)) {
              return releases(node, lockee, state);
            }
            return super.visitMethodInvocation(node, null);
          }
        }.scan(tryTree.getFinallyBlock(), null);
    return released == null ? false : released;
  }

  private static boolean releases(ExpressionTree node, ExpressionTree lockee, VisitorState state) {
    return UNLOCK.matches(node, state)
        && state.getSourceForNode(getReceiver(node)).equals(state.getSourceForNode(lockee));
  }
}
