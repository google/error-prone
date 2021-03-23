/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.fixes.SuggestedFixes.renameVariableUsages;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

/**
 * Flags variables initialized with {@link java.util.regex.Pattern#compile(String)} calls that could
 * be constants.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "ConstantPatternCompile",
    summary = "Variables initialized with Pattern#compile calls on constants can be constants",
    severity = SUGGESTION)
public final class ConstantPatternCompile extends BugChecker implements VariableTreeMatcher {

  private static final Matcher<ExpressionTree> PATTERN_COMPILE_CHECK =
      staticMethod()
          .onClassAny(
              "java.util.regex.Pattern"
              )
          .named("compile");

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    ExpressionTree initializer = tree.getInitializer();
    if (!PATTERN_COMPILE_CHECK.matches(initializer, state)) {
      return NO_MATCH;
    }
    if (!((MethodInvocationTree) initializer)
        .getArguments().stream().allMatch(ConstantPatternCompile::isArgStaticAndConstant)) {
      return NO_MATCH;
    }
    MethodTree outerMethodTree = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    if (outerMethodTree == null) {
      return NO_MATCH;
    }
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    switch (sym.getKind()) {
      case RESOURCE_VARIABLE:
        return describeMatch(tree);
      case LOCAL_VARIABLE:
        return describeMatch(tree, fixLocal(tree, outerMethodTree, state));
      default:
        return NO_MATCH;
    }
  }

  private static SuggestedFix fixLocal(
      VariableTree tree, MethodTree outerMethodTree, VisitorState state) {
    MethodSymbol methodSymbol = ASTHelpers.getSymbol(outerMethodTree);
    boolean canUseStatic =
        (methodSymbol != null
                && methodSymbol.owner.enclClass().getNestingKind() == NestingKind.TOP_LEVEL)
            || outerMethodTree.getModifiers().getFlags().contains(Modifier.STATIC);
    String newName = LOWER_CAMEL.to(UPPER_UNDERSCORE, tree.getName().toString());
    String replacement =
        String.format(
            "private %s final %s %s = %s;",
            canUseStatic ? "static " : "",
            state.getSourceForNode(tree.getType()),
            newName,
            state.getSourceForNode(tree.getInitializer()));
    return SuggestedFix.builder()
        .merge(renameVariableUsages(tree, newName, state))
        .postfixWith(outerMethodTree, replacement)
        .delete(tree)
        .build();
  }

  private static boolean isArgStaticAndConstant(ExpressionTree arg) {
    if (ASTHelpers.constValue(arg) == null) {
      return false;
    }
    Symbol argSymbol = ASTHelpers.getSymbol(arg);
    if (argSymbol == null) {
      return true;
    }
    return (argSymbol.flags() & Flags.STATIC) != 0;
  }
}
