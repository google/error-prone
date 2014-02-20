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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.selectedIsInstance;
import static com.google.errorprone.matchers.Matchers.staticFieldAccess;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "StaticAccessedFromInstance",
    summary = "A static variable or method should not be accessed from an instance",
    explanation =
        "A static variable or method should never be accessed from an instance.  This hides the " +
        "fact that the variable or method is static and not an instance variable or method.",
    category = JDK, severity = ERROR, maturity = MATURE, altNames = "static")
public class StaticAccessedFromInstance extends BugChecker implements MemberSelectTreeMatcher {

  @SuppressWarnings("unchecked")
  private static final Matcher<ExpressionTree> staticAccessedFromInstanceMatcher = allOf(
      anyOf(
          staticMethod("*", "*"),
          staticFieldAccess()),
      kindIs(Kind.MEMBER_SELECT),
      selectedIsInstance());

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    if (!staticAccessedFromInstanceMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return describeMatch(tree, getSuggestedFix(tree, state));
  }

  private static Fix getSuggestedFix(MemberSelectTree tree, VisitorState state) {
    MethodInvocationTree methodInvocationTree = null;
    Tree parentNode = state.getPath().getParentPath().getLeaf();
    if (parentNode.getKind() == Kind.METHOD_INVOCATION) {
      methodInvocationTree = (MethodInvocationTree) parentNode;
    }

    JCFieldAccess fieldAccess = (JCFieldAccess) tree;

    // Get class symbol for the owner of the variable that was accessed.
    Symbol ownerSym = ASTHelpers.getSymbol(fieldAccess).owner;
    if (!(ownerSym instanceof ClassSymbol)) {
      return null;
    }

    // Get class symbol for the class we are currently analyzing.
    Tree classDecl = ASTHelpers.findEnclosingNode(state.getPath().getParentPath(), ClassTree.class);
    if (classDecl == null) {
      return null;
    }
    Symbol currClassSym = ASTHelpers.getSymbol(classDecl);
    if (!(currClassSym instanceof ClassSymbol)) {
      return null;
    }

    SuggestedFix fix = new SuggestedFix();
    if (currClassSym.equals(ownerSym) && methodInvocationTree != null) {
      // If owner is the same as the current class, then just use the bare method name. Don't do
      // this for fields, because they may share a simple name with a local in the same scope.
      fix.replace(tree, fieldAccess.getIdentifier().toString());
    } else {
      // Replace the operand of the field access expression with the simple name of the class.
      Symbol packageSym = ownerSym.packge();
      fix.replace(fieldAccess.getExpression(), ownerSym.getSimpleName().toString());

      // Don't import implicitly imported packages (java.lang.* and current package).
      // TODO(cushon): move this logic into addImport?
      if (!packageSym.toString().equals("java.lang") && !packageSym.equals(currClassSym.packge())) {
        fix.addImport(ownerSym.toString());
      }
    }
    return fix;
  }
}
