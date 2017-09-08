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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
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
import com.sun.tools.javac.code.Symbol;
import java.util.Iterator;
import java.util.Objects;

/** @author mariasam@google.com (Maria Sam) */
@BugPattern(
  name = "OptionalNotPresent",
  category = JDK,
  summary =
      "One should not call optional.get() inside an if statement that checks "
          + "!optional.isPresent",
  severity = WARNING
)
public class OptionalNotPresent extends BugChecker implements MethodInvocationTreeMatcher {

  private static final MethodNameMatcher GOOGLE_OPTIONAL_PRESENT =
      Matchers.instanceMethod()
          .onDescendantOf(com.google.common.base.Optional.class.getName())
          .named("isPresent");

  private static final MethodNameMatcher OPTIONAL_PRESENT =
      Matchers.instanceMethod().onDescendantOf("java.util.Optional").named("isPresent");

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState visitorState) {
    if (GOOGLE_OPTIONAL_PRESENT.matches(methodInvocationTree, visitorState)
        || OPTIONAL_PRESENT.matches(methodInvocationTree, visitorState)) {
      Symbol optionalVar = ASTHelpers.getSymbol(ASTHelpers.getReceiver(methodInvocationTree));
      // using an iterator to make sure that only !optional.isPresent() matches and not
      // !(optional.isPresent() || foo == 7)
      Iterator<Tree> iter = visitorState.getPath().iterator();
      iter.next();
      Tree upTree = iter.next();
      if (!(upTree instanceof UnaryTree) || upTree.getKind() != Kind.LOGICAL_COMPLEMENT) {
        return Description.NO_MATCH;
      }
      IfTree ifTree = null;
      ifTree = possibleIf(ifTree, upTree, iter);
      if (ifTree == null) {
        return Description.NO_MATCH;
      }
      TreeScannerInside treeScannerInside = new TreeScannerInside();
      treeScannerInside.scan(ifTree.getThenStatement(), optionalVar);
      if (treeScannerInside.hasGet && !treeScannerInside.hasAssignment) {
        return describeMatch(methodInvocationTree);
      }
    }
    return Description.NO_MATCH;
  }

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

  private static class TreeScannerInside extends TreeScanner<Void, Symbol> {

    private boolean hasGet = false;
    private boolean hasAssignment = false;

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Symbol optionalVar) {
      if (tree.getArguments()
          .stream()
          .anyMatch(m -> Objects.equals(ASTHelpers.getSymbol(m), optionalVar))) {
        hasAssignment = true;
      }
      if (Objects.equals(ASTHelpers.getSymbol(ASTHelpers.getReceiver(tree)), optionalVar)) {
        ExpressionTree treeIdent = tree.getMethodSelect();
        if (treeIdent instanceof MemberSelectTree) {
          if (((MemberSelectTree) treeIdent).getIdentifier().contentEquals("get")) {
            hasGet = true;
          }
        }
      }
      return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree assignmentTree, Symbol optionalVar) {
      if (Objects.equals(ASTHelpers.getSymbol(assignmentTree.getVariable()), optionalVar)) {
        hasAssignment = true;
      }
      return null;
    }
  }
}
