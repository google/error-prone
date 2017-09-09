/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.common.base.Verify.verify;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "UnnecessaryTypeArgument",
  summary = "Non-generic methods should not be invoked with type arguments",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class UnnecessaryTypeArgument extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return check(tree, tree.getTypeArguments(), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return check(tree, tree.getTypeArguments(), state);
  }

  private Description check(Tree tree, List<? extends Tree> arguments, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (!(sym instanceof MethodSymbol)) {
      return Description.NO_MATCH;
    }
    MethodSymbol methodSymbol = (MethodSymbol) sym;

    int expected = methodSymbol.getTypeParameters().size();
    int actual = arguments.size();

    if (actual <= expected) {
      return Description.NO_MATCH;
    }

    for (MethodSymbol superMethod : ASTHelpers.findSuperMethods(methodSymbol, state.getTypes())) {
      if (!superMethod.getTypeParameters().isEmpty()) {
        // Exempt methods that override generic methods to preserve the substitutability of the
        // two types.
        return Description.NO_MATCH;
      }
    }

    return describeMatch(tree, buildFix(tree, arguments, state));
  }

  /** Constructor a fix that deletes the set of type arguments. */
  private Fix buildFix(Tree tree, List<? extends Tree> arguments, VisitorState state) {

    JCTree node = (JCTree) tree;
    int startAbsolute = node.getStartPosition();
    int lower = ((JCTree) arguments.get(0)).getStartPosition() - startAbsolute;
    int upper = state.getEndPosition(arguments.get(arguments.size() - 1)) - startAbsolute;

    CharSequence source = state.getSourceForNode(node);

    while (lower >= 0 && source.charAt(lower) != '<') {
      lower--;
    }
    while (upper < source.length() && source.charAt(upper) != '>') {
      upper++;
    }

    // There's a small chance that the fix will be incorrect because there's a '<' or '>' in
    // a comment (e.g. `this.</*<*/T/*>*/>f()`), it should never be the case that we don't find
    // any angle brackets.
    verify(source.charAt(lower) == '<' && source.charAt(upper) == '>');

    Fix fix = SuggestedFix.replace(startAbsolute + lower, startAbsolute + upper + 1, "");
    return fix;
  }
}
