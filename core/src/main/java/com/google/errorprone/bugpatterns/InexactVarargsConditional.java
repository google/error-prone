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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Types;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "InexactVarargsConditional",
  summary = "Conditional expression in varargs call contains array and non-array arguments",
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class InexactVarargsConditional extends BugChecker implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (!sym.isVarArgs()) {
      return NO_MATCH;
    }
    if (tree.getArguments().size() != sym.getParameters().size()) {
      // explicit varargs call with more actuals than formals
      return NO_MATCH;
    }
    Tree arg = getLast(tree.getArguments());
    if (!(arg instanceof ConditionalExpressionTree)) {
      return NO_MATCH;
    }
    Types types = state.getTypes();
    if (types.isArray(getType(arg))) {
      return NO_MATCH;
    }
    ConditionalExpressionTree cond = (ConditionalExpressionTree) arg;
    boolean trueIsArray = types.isArray(getType(cond.getTrueExpression()));
    if (!(trueIsArray ^ types.isArray(getType(cond.getFalseExpression())))) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String qualified =
        SuggestedFixes.qualifyType(
            state, fix, types.elemtype(getLast(sym.getParameters()).asType()));
    Tree toFix = !trueIsArray ? cond.getTrueExpression() : cond.getFalseExpression();
    fix.prefixWith(toFix, String.format("new %s[] {", qualified)).postfixWith(toFix, "}");
    return describeMatch(tree, fix.build());
  }
}
