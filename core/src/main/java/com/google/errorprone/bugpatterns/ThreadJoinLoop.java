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

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Objects;

/** @author mariasam@google.com (Maria Sam) */
@BugPattern(
  name = "ThreadJoinLoop",
  summary =
      "Thread.join needs to be surrounded by a loop until it succeeds, "
          + "as in Uninterruptibles.joinUninterruptibly.",
  explanation =
      "Thread.join() can be interrupted, and so requires users to catch "
          + "InterruptedException. Most users should be looping "
          + "until the join() actually succeeds.",
  category = JDK,
  severity = SeverityLevel.WARNING,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ThreadJoinLoop extends BugChecker implements MethodInvocationTreeMatcher {

  private static final MethodNameMatcher MATCH_THREAD_JOIN =
      Matchers.instanceMethod().onDescendantOf("java.lang.Thread").named("join");

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState visitorState) {
    String threadString;
    if (methodInvocationTree.getMethodSelect() instanceof MemberSelectTree) {
      threadString =
          ((MemberSelectTree) methodInvocationTree.getMethodSelect()).getExpression().toString();
    } else {
      threadString = "this";
    }
    // if it is thread.join with a timeout, ignore (too many special cases in codebase
    // with calculating time with declared variables)
    if (!methodInvocationTree.getArguments().isEmpty()) {
      return Description.NO_MATCH;
    }
    if (MATCH_THREAD_JOIN.matches(methodInvocationTree, visitorState)) {
      TreePath treePath =
          ASTHelpers.findPathFromEnclosingNodeToTopLevel(visitorState.getPath(), TryTree.class);
      if (treePath == null) {
        return Description.NO_MATCH;
      }
      TreePath pathToLoop =
          ASTHelpers.findPathFromEnclosingNodeToTopLevel(treePath, WhileLoopTree.class);

      // checks to make sure that if there is a while loop with only one statement (the try catch
      // block)
      boolean hasWhileLoopOneStatement = false;
      if (pathToLoop != null) {
        Tree statements = ((WhileLoopTree) pathToLoop.getLeaf()).getStatement();
        if (statements instanceof BlockTree) {
          if (((BlockTree) statements).getStatements().size() == 1) {
            hasWhileLoopOneStatement = true;
          }
        }
      }

      Type interruptedType = visitorState.getSymtab().interruptedExceptionType;
      Type exceptionType = visitorState.getSymtab().exceptionType;
      TryTree tryTree = (TryTree) treePath.getLeaf();
      // scans the try tree block for any other actions so that we do not accidentally delete
      // important actions when replacing
      TreeScannerMethodInvocations treeScanner = new TreeScannerMethodInvocations();
      treeScanner.scan(tryTree.getBlock(), methodInvocationTree.toString());
      if (treeScanner.count > 0) {
        return Description.NO_MATCH;
      }
      if (tryTree.getFinallyBlock() != null) {
        return Description.NO_MATCH;
      }
      List<? extends CatchTree> catches = tryTree.getCatches();
      for (CatchTree tree : catches) {
        Type typeSym = ASTHelpers.getType(tree.getParameter().getType());
        if (Objects.equals(interruptedType, typeSym) || Objects.equals(exceptionType, typeSym)) {
          List<? extends StatementTree> statementTrees = tree.getBlock().getStatements();
          // replaces the while loop with the try block or replaces just the try block
          if (statementTrees.isEmpty()
              || (statementTrees.size() == 1 && statementTrees.get(0).toString().equals(";"))) {
            SuggestedFix.Builder builder = SuggestedFix.builder();
            builder.replace(
                hasWhileLoopOneStatement ? pathToLoop.getLeaf() : tryTree,
                "Uninterruptibles.joinUninterruptibly(" + threadString + ");");
            builder.addImport("com.google.common.util.concurrent.Uninterruptibles");
            return describeMatch(methodInvocationTree, builder.build());
          }
        }
      }
    }
    return Description.NO_MATCH;
  }

  private static class TreeScannerMethodInvocations extends TreeScanner<Void, String> {

    private int count = 0;

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, String methodString) {
      if (!tree.toString().contains(methodString)) {
        count++;
      }
      return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, String methodString) {
      if (ASTHelpers.getType(tree.getVariable()).toString().equals("java.lang.Thread")) {
        count++;
      }
      return null;
    }
  }
}
