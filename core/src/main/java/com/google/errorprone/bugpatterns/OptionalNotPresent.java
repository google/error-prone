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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreeScanner;
import java.util.Iterator;
import javax.annotation.Nullable;
import javax.lang.model.element.Name;

/** @author mariasam@google.com (Maria Sam) */
@BugPattern(
    name = "OptionalNotPresent",
    summary =
        "One should not call optional.get() inside an if statement that checks "
            + "!optional.isPresent",
    severity = WARNING)
public class OptionalNotPresent extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> OPTIONAL_PRESENT =
      instanceMethod()
          .onDescendantOfAny("com.google.common.base.Optional", "java.util.Optional")
          .named("isPresent");

  private static final Matcher<ExpressionTree> OPTIONAL_EMPTY =
      instanceMethod().onDescendantOf("java.util.Optional").named("isEmpty");

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState visitorState) {
    Iterator<Tree> iter = visitorState.getPath().iterator();
    Tree upTree;
    if (OPTIONAL_PRESENT.matches(methodInvocationTree, visitorState)) {
      // using an iterator to make sure that only !optional.isPresent() matches and not
      // !(optional.isPresent() || foo == 7)
      iter.next();
      upTree = iter.next();
      if (!(upTree instanceof UnaryTree) || upTree.getKind() != Kind.LOGICAL_COMPLEMENT) {
        return NO_MATCH;
      }
    } else if (OPTIONAL_EMPTY.matches(methodInvocationTree, visitorState)) {
      iter = visitorState.getPath().iterator();
      upTree = methodInvocationTree;
    } else {
      return NO_MATCH;
    }
    IfTree ifTree = null;
    ifTree = possibleIf(ifTree, upTree, iter);
    if (ifTree == null) {
      return NO_MATCH;
    }
    TreeScannerInside treeScannerInside = new TreeScannerInside();
    ExpressionTree optionalVar = ASTHelpers.getReceiver(methodInvocationTree);
    if (optionalVar == null) {
      return NO_MATCH;
    }
    treeScannerInside.scan(ifTree.getThenStatement(), optionalVar);
    if (treeScannerInside.hasGet && !treeScannerInside.hasAssignment) {
      return describeMatch(methodInvocationTree);
    }
    return NO_MATCH;
  }

  @Nullable
  private static IfTree possibleIf(IfTree ifTree, Tree upTree, Iterator<Tree> iter) {
    while (iter.hasNext()) {
      // if it's in the body of an if statement, and not the condition, then it does not apply,
      // so return null
      if (upTree instanceof BlockTree) {
        return null;
      }
      if (upTree instanceof BinaryTree) {
        // If an "or" is in the if condition, then it does not apply
        if (upTree.getKind() == Kind.CONDITIONAL_OR) {
          return null;
        }
      }
      // if it finds an if tree, that means at this point it was not in the body of the then/else
      // statements, so !optional.isPresent() is most definitely in the if condition
      if (upTree instanceof IfTree) {
        ifTree = (IfTree) upTree;
        break;
      }
      // If tree is not a BlockTree or an or, it keeps going up
      upTree = iter.next();
    }
    return ifTree;
  }

  private static class TreeScannerInside extends TreeScanner<Void, ExpressionTree> {

    private boolean hasGet = false;
    private boolean hasAssignment = false;

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, ExpressionTree optionalVar) {
      if (tree.getArguments().stream().anyMatch(m -> ASTHelpers.sameVariable(m, optionalVar))) {
        hasAssignment = true;
      }
      ExpressionTree receiver = ASTHelpers.getReceiver(tree);
      if (receiver != null && ASTHelpers.sameVariable(receiver, optionalVar)) {
        ExpressionTree treeIdent = tree.getMethodSelect();
        if (treeIdent instanceof MemberSelectTree) {
          Name identifier = ((MemberSelectTree) treeIdent).getIdentifier();
          if (identifier.contentEquals("get") || identifier.contentEquals("orElseThrow")) {
            hasGet = true;
          }
        }
      }
      return super.visitMethodInvocation(tree, optionalVar);
    }

    @Override
    public Void visitAssignment(AssignmentTree assignmentTree, ExpressionTree optionalVar) {
      if (ASTHelpers.sameVariable(assignmentTree.getVariable(), optionalVar)) {
        hasAssignment = true;
      }
      return super.visitAssignment(assignmentTree, optionalVar);
    }
  }
}
