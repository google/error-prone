/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.inLoop;
import static com.google.errorprone.matchers.Matchers.allOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/**
 * Finds instances of structurally modifying a Collection when iterating through it using an
 * enhanced for loop. These modifications always result in a ConcurrentModificationException being
 * thrown.
 * 
 * @author anishvisaria98@gmail.com (Anish Visaria)
 */
@BugPattern(name = "ModifyCollectionInEnhancedForLoop",
    summary = "This code will cause a ConcurrentModificationException to be thrown.",
    explanation = "This code structurally modifies the Collection while iterating through it. "
        + "Use Iterator methods to modify a Collection while iterating through it.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class ModifyCollectionInEnhancedForLoop extends BugChecker
    implements MethodInvocationTreeMatcher {

  static final Matcher<MethodInvocationTree> MODIFY_MATCHER =
      allOf(
          anyOf(instanceMethod().onDescendantOf("java.util.Collection").named("add"),
              instanceMethod().onDescendantOf("java.util.Collection").named("addAll"),
              instanceMethod().onDescendantOf("java.util.Collection").named("clear"),
              instanceMethod().onDescendantOf("java.util.Collection").named("remove"),
              instanceMethod().onDescendantOf("java.util.Collection").named("removeAll"),
              instanceMethod().onDescendantOf("java.util.Collection").named("retainAll")),
          inLoop());

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (MODIFY_MATCHER.matches(tree, state)) {
      TreePath path = state.getPath().getParentPath();
      Tree node = path.getLeaf();
      while (path != null) {
        if (node.getKind() == Kind.METHOD || node.getKind() == Kind.CLASS)
          return Description.NO_MATCH;
        if (node.getKind() == Kind.ENHANCED_FOR_LOOP && sameCollection(node, tree))
          return describeMatch(tree);
        path = path.getParentPath();
        node = path.getLeaf();
      }
    }
    return Description.NO_MATCH;
  }

  /**
   * Checks whether the Collection modified is the same Collection that is being iterated through.
   */
  private static boolean sameCollection(Tree node, MethodInvocationTree tree) {
    if (node instanceof EnhancedForLoopTree) {
      EnhancedForLoopTree efl = (EnhancedForLoopTree) node;
      JCExpression methodSelect = (JCExpression) tree.getMethodSelect();
      if (methodSelect instanceof JCFieldAccess) {
        JCFieldAccess fieldAccess = (JCFieldAccess) methodSelect;
        return ASTHelpers.sameVariable(fieldAccess.getExpression(), efl.getExpression());
      }
    }
    return false;
  }



}
