/*
 * Copyright 2018 The Error Prone Authors.
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
import static com.google.errorprone.fixes.SuggestedFixes.addModifiers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.isPrimitiveType;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** @author andrew@gaul.org (Andrew Gaul) */
@BugPattern(
  name = "FieldCanBeStatic",
  summary = "A final field initialized at compile-time with an instance of an immutable type can be static.",
  severity = SUGGESTION,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class FieldCanBeStatic extends BugChecker implements VariableTreeMatcher {

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    VarSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null || sym.getKind() != ElementKind.FIELD) {
      return NO_MATCH;
    }
    if (!sym.getModifiers().contains(Modifier.FINAL)) {
      return NO_MATCH;
    }
    if (sym.isStatic()) {
      return NO_MATCH;
    }
    Tree targetType = tree.getType();
    if (!isPrimitiveType().matches(targetType, state) &&
        !isSubtype(getType(tree), state.getTypeFromString("java.lang.String"), state)) {
      return NO_MATCH;
    }
    ExpressionTree tagExpr = tree.getInitializer();
    if (tagExpr == null) {
      return NO_MATCH;
    }
    if (tagExpr.getKind() != Kind.BOOLEAN_LITERAL &&
        tagExpr.getKind() != Kind.CHAR_LITERAL &&
        tagExpr.getKind() != Kind.DOUBLE_LITERAL &&
        tagExpr.getKind() != Kind.FLOAT_LITERAL &&
        tagExpr.getKind() != Kind.INT_LITERAL &&
        tagExpr.getKind() != Kind.LONG_LITERAL &&
        tagExpr.getKind() != Kind.NULL_LITERAL &&
        tagExpr.getKind() != Kind.STRING_LITERAL) {
      return NO_MATCH;
    }
    return describeMatch(tree.getModifiers(), addModifiers(tree, state, Modifier.STATIC));
  }
}
