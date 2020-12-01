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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.base.CaseFormat;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
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

  private static final String PATTERN_CLASS = "java.util.regex.Pattern";
  private static final Supplier<Type> PATTERN_TYPE = Suppliers.typeFromString(PATTERN_CLASS);

  private static final Matcher<ExpressionTree> PATTERN_COMPILE_CHECK =
      Matchers.<ExpressionTree>anyOf(staticMethod().onClass(PATTERN_CLASS).named("compile"));

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof ClassTree) {
      return Description.NO_MATCH;
    }
    if (!ASTHelpers.isSameType(getType(tree.getType()), PATTERN_TYPE.get(state), state)) {
      return Description.NO_MATCH;
    }
    if (!(tree.getInitializer() instanceof MethodInvocationTree)) {
      return Description.NO_MATCH;
    }
    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree.getInitializer();
    if (!PATTERN_COMPILE_CHECK.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }
    if (!methodInvocationTree.getArguments().stream()
        .allMatch(ConstantPatternCompile::isArgStaticAndConstant)) {
      return Description.NO_MATCH;
    }
    MethodTree outerMethodTree = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    if (outerMethodTree == null) {
      return Description.NO_MATCH;
    }
    MethodSymbol sym = ASTHelpers.getSymbol(outerMethodTree);
    boolean canUseStatic =
        (sym != null && sym.owner.enclClass().getNestingKind() == NestingKind.TOP_LEVEL)
            || outerMethodTree.getModifiers().getFlags().contains(Modifier.STATIC);

    String originalVariableTreeString = state.getSourceForNode(tree);
    String varName = tree.getName().toString();
    String upperUnderscoreVarName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, varName);

    int typeEndPos = state.getEndPosition(tree.getType());
    // handle implicit lambda parameter types
    int searchOffset = typeEndPos == -1 ? 0 : (typeEndPos - getStartPosition(tree));
    int pos = state.getSourceForNode(tree).indexOf(varName, searchOffset);
    String variableReplacedString =
        new StringBuilder(originalVariableTreeString)
            .replace(pos, pos + varName.length(), upperUnderscoreVarName)
            .toString();

    String modifiers = "private " + (canUseStatic ? "static " : "") + "final ";
    String variableTreeString = "\n" + modifiers + variableReplacedString + "\n";

    SuggestedFix fix =
        SuggestedFix.builder()
            .merge(renameVariableOccurrences(tree, upperUnderscoreVarName, state))
            .postfixWith(outerMethodTree, variableTreeString)
            .delete(tree)
            .build();
    return describeMatch(tree, fix);
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

  private static SuggestedFix renameVariableOccurrences(
      VariableTree tree, String replacement, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    Symbol.VarSymbol sym = getSymbol(tree);
    ((JCTree) state.getPath().getCompilationUnit())
        .accept(
            new TreeScanner() {
              @Override
              public void visitIdent(JCTree.JCIdent tree) {
                if (sym.equals(getSymbol(tree))) {
                  fix.replace(tree, replacement);
                }
              }

              @Override
              public void visitSelect(JCTree.JCFieldAccess tree) {
                if (sym.equals(getSymbol(tree))) {
                  fix.replace(
                      state.getEndPosition(tree.getExpression()),
                      state.getEndPosition(tree),
                      "." + replacement);
                }
                super.visitSelect(tree);
              }
            });
    return fix.build();
  }
}
