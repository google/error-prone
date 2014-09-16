/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.receiverSameAsArgument;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.VARIABLE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.EditDistance;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

/**
 * @author scottjohnson@google.com (Scott Johnson)
 */
@BugPattern(name = "ModifyingCollectionWithItself",
    summary = "Modifying a collection with itself",
    explanation = "Modifying a collection with itself is almost never what is intended. " +
        "collection.addAll(collection) and collection.retainAll(collection) are both no-ops, " +
        "and collection.removeAll(collection) is equivalent to collection.clear().",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class ModifyingCollectionWithItself extends BugChecker
    implements MethodInvocationTreeMatcher {

  /**
   * Matches calls to addAll, containsAll, removeAll, and retainAll on itself
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree t, VisitorState state) {
    if (allOf(anyOf(
        methodSelect(instanceMethod(
            Matchers.<ExpressionTree>isSubtypeOf("java.util.Collection"), "addAll")),
        methodSelect(instanceMethod(
            Matchers.<ExpressionTree>isSubtypeOf("java.util.Collection"), "removeAll")),
        methodSelect(instanceMethod(
            Matchers.<ExpressionTree>isSubtypeOf("java.util.Collection"), "containsAll")),
        methodSelect(instanceMethod(
            Matchers.<ExpressionTree>isSubtypeOf("java.util.Collection"), "retainAll"))),
        receiverSameAsArgument(0)).matches(t, state)) {
      return describe(t, state);
    }
    return Description.NO_MATCH;
  }

  /**
   * We expect that the lhs is a field and the rhs is an identifier, specifically
   * a parameter to the method.  We base our suggested fixes on this expectation.
   *
   * Case 1: If lhs is a field and rhs is an identifier, find a method parameter
   * of the same type and similar name and suggest it as the rhs.  (Guess that they
   * have misspelled the identifier.)
   *
   * Case 2: If lhs is a field and rhs is not an identifier, find a method parameter
   * of the same type and similar name and suggest it as the rhs.
   *
   * Case 3: If lhs is not a field and rhs is an identifier, find a class field
   * of the same type and similar name and suggest it as the lhs.
   *
   * Case 4: Otherwise replace with literal meaning of functionality
   */
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {

    // the statement that is the parent of the self-assignment expression
    Tree parent = state.getPath().getParentPath().getLeaf();

    ExpressionTree lhs = ASTHelpers.getReceiver(methodInvocationTree);
    ExpressionTree rhs = methodInvocationTree.getArguments().get(0);
    
    // default fix for methods
    Fix fix = SuggestedFix.delete(parent);
    if (methodSelect(instanceMethod(Matchers.<ExpressionTree>anything(), "removeAll"))
        .matches(methodInvocationTree, state)) {
      fix = SuggestedFix.replace(methodInvocationTree, lhs + ".clear()");
    }

    if (lhs.getKind() == MEMBER_SELECT) {
      // find a method parameter of the same type and similar name and suggest it
      // as the rhs

      // rhs should be either identifier or field access
      assert(rhs.getKind() == IDENTIFIER || rhs.getKind() == MEMBER_SELECT);

      // get current name of rhs
      String rhsName = null;
      if (rhs.getKind() == IDENTIFIER) {
        rhsName = ((JCIdent) rhs).name.toString();
      } else if (rhs.getKind() == MEMBER_SELECT) {
        rhsName = ((JCFieldAccess) rhs).name.toString();
      }

      // find method parameters of the type "Collection"
      TreePath path = state.getPath();
      while (path != null && path.getLeaf().getKind() != METHOD) {
        path = path.getParentPath();
      }
      JCMethodDecl method = (JCMethodDecl) path.getLeaf();
      int minEditDistance = Integer.MAX_VALUE;
      String replacement = null;
      for (JCVariableDecl var : method.params) {
        if (variableType(isSubtypeOf("java.util.Collection")).matches(var, state)) {
          int editDistance = EditDistance.getEditDistance(rhsName, var.name.toString());
          if (editDistance < minEditDistance) {
            // pick one with minimum edit distance
            minEditDistance = editDistance;
            replacement = var.name.toString();
          }
        }
      }
      if (replacement != null) {
        // suggest replacing rhs with the parameter
        fix = SuggestedFix.replace(rhs, replacement);
      }
    } else if (rhs.getKind() == IDENTIFIER) {
      // find a field of the same type and similar name and suggest it as the lhs

      // lhs should be identifier
      assert(lhs.getKind() == IDENTIFIER);

      // get current name of lhs
      String lhsName = ((JCIdent) rhs).name.toString();

      // find class instance fields of the type "Collection"
      TreePath path = state.getPath();
      while (path != null && path.getLeaf().getKind() != CLASS) {
        path = path.getParentPath();
      }
      JCClassDecl klass = (JCClassDecl) path.getLeaf();
      int minEditDistance = Integer.MAX_VALUE;
      String replacement = null;
      for (JCTree member : klass.getMembers()) {
        if (member.getKind() == VARIABLE) {
          JCVariableDecl var = (JCVariableDecl) member;
          if (!Flags.isStatic(var.sym)
              && variableType(isSubtypeOf("java.util.Collection")).matches(var, state)) {
            int editDistance = EditDistance.getEditDistance(lhsName, var.name.toString());
            if (editDistance < minEditDistance) {
              // pick one with minimum edit distance
              minEditDistance = editDistance;
              replacement = var.name.toString();
            }
          }
        }
      }
      if (replacement != null) {
        // suggest replacing lhs with the field
        fix = SuggestedFix.replace(lhs, "this." + replacement);
      }
    }

    return describeMatch(methodInvocationTree, fix);
  }
}
