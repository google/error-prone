/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Type;
import java.util.concurrent.atomic.AtomicInteger;

/** @author mariasam@google.com (Maria Sam) */
@BugPattern(
    name = "ThreadJoinLoop",
    summary =
        "Thread.join needs to be surrounded by a loop until it succeeds, "
            + "as in Uninterruptibles.joinUninterruptibly.",
    severity = SeverityLevel.WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ThreadJoinLoop extends BugChecker implements MethodInvocationTreeMatcher {

  private static final MethodNameMatcher MATCH_THREAD_JOIN =
      instanceMethod().onDescendantOf("java.lang.Thread").named("join");

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    String threadString;
    if (methodInvocationTree.getMethodSelect() instanceof MemberSelectTree) {
      threadString =
          state.getSourceForNode(
              ((MemberSelectTree) methodInvocationTree.getMethodSelect()).getExpression());
    } else {
      threadString = "this";
    }
    // if it is thread.join with a timeout, ignore (too many special cases in codebase
    // with calculating time with declared variables)
    if (!methodInvocationTree.getArguments().isEmpty()) {
      return Description.NO_MATCH;
    }
    if (!MATCH_THREAD_JOIN.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }
    TreePath tryTreePath =
        ASTHelpers.findPathFromEnclosingNodeToTopLevel(state.getPath(), TryTree.class);
    if (tryTreePath == null) {
      return Description.NO_MATCH;
    }
    WhileLoopTree pathToLoop = ASTHelpers.findEnclosingNode(tryTreePath, WhileLoopTree.class);

    // checks to make sure that if there is a while loop with only one statement (the try catch
    // block)
    boolean hasWhileLoopOneStatement = false;
    if (pathToLoop != null) {
      Tree statements = pathToLoop.getStatement();
      if (statements instanceof BlockTree && ((BlockTree) statements).getStatements().size() == 1) {
        hasWhileLoopOneStatement = true;
      }
    }

    // Scans the try tree block for any other method invocations so that we do not accidentally
    // delete important actions when replacing.
    TryTree tryTree = (TryTree) tryTreePath.getLeaf();
    if (hasOtherInvocationsOrAssignments(methodInvocationTree, tryTree, state)) {
      return Description.NO_MATCH;
    }
    if (tryTree.getFinallyBlock() != null) {
      return Description.NO_MATCH;
    }
    Type interruptedType = state.getSymtab().interruptedExceptionType;
    for (CatchTree tree : tryTree.getCatches()) {
      Type typeSym = getType(tree.getParameter().getType());
      if (ASTHelpers.isCastable(typeSym, interruptedType, state)) {
        // replaces the while loop with the try block or replaces just the try block
        if (tree.getBlock().getStatements().stream()
            .allMatch(s -> s instanceof EmptyStatementTree)) {
          SuggestedFix.Builder fix = SuggestedFix.builder();
          String uninterruptibles =
              SuggestedFixes.qualifyType(
                  state, fix, "com.google.common.util.concurrent.Uninterruptibles");
          fix.replace(
              hasWhileLoopOneStatement ? pathToLoop : tryTree,
              String.format("%s.joinUninterruptibly(%s);", uninterruptibles, threadString));
          return describeMatch(methodInvocationTree, fix.build());
        }
      }
    }
    return Description.NO_MATCH;
  }

  private static boolean hasOtherInvocationsOrAssignments(
      MethodInvocationTree methodInvocationTree, TryTree tryTree, VisitorState state) {
    AtomicInteger count = new AtomicInteger(0);
    Type threadType = state.getTypeFromString("java.lang.Thread");
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        if (!tree.equals(methodInvocationTree)) {
          count.incrementAndGet();
        }
        return super.visitMethodInvocation(tree, null);
      }

      @Override
      public Void visitAssignment(AssignmentTree tree, Void unused) {
        if (isSubtype(getType(tree.getVariable()), threadType, state)) {
          count.incrementAndGet();
        }
        return super.visitAssignment(tree, null);
      }
    }.scan(tryTree.getBlock(), null);
    return count.get() > 0;
  }
}
