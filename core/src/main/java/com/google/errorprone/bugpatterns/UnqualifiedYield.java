/*
 * Copyright 2022 The Error Prone Authors.
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
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.findPathFromEnclosingNodeToTopLevel;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "In recent versions of Java, 'yield' is a contextual keyword, and calling an unqualified"
            + " method with that name is an error.",
    severity = WARNING)
public class UnqualifiedYield extends BugChecker implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    ExpressionTree select = tree.getMethodSelect();
    if (!(select instanceof IdentifierTree)) {
      return NO_MATCH;
    }
    if (!((IdentifierTree) select).getName().contentEquals("yield")) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String qualifier = getQualifier(state, getSymbol(tree), fix);
    return describeMatch(tree, fix.prefixWith(select, qualifier + ".").build());
  }

  private static String getQualifier(
      VisitorState state, MethodSymbol sym, SuggestedFix.Builder fix) {
    if (sym.isStatic()) {
      return qualifyType(state, fix, sym.owner.enclClass());
    }
    TreePath path = findPathFromEnclosingNodeToTopLevel(state.getPath(), ClassTree.class);
    if (sym.isMemberOf(getSymbol((ClassTree) path.getLeaf()), state.getTypes())) {
      return "this";
    }
    while (true) {
      path = findPathFromEnclosingNodeToTopLevel(path, ClassTree.class);
      ClassSymbol enclosingClass = getSymbol((ClassTree) path.getLeaf());
      if (sym.isMemberOf(enclosingClass, state.getTypes())) {
        return qualifyType(state, fix, enclosingClass) + ".this";
      }
    }
  }
}
