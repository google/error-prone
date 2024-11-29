/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.common.base.Ascii.toLowerCase;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFix.mergeFixes;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.SourceVersion.supportsPatternMatchingInstanceof;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.InstanceOfTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import javax.lang.model.SourceVersion;
import org.jspecify.annotations.Nullable;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = WARNING,
    summary = "This code can be simplified to use a pattern-matching instanceof.")
public final class PatternMatchingInstanceof extends BugChecker implements InstanceOfTreeMatcher {

  @Override
  public Description matchInstanceOf(InstanceOfTree instanceOfTree, VisitorState state) {
    if (!supportsPatternMatchingInstanceof(state.context)) {
      return NO_MATCH;
    }
    var enclosingIf = findEnclosingIf(instanceOfTree, state);
    if (enclosingIf != null) {
      var replaceExistingVariable = checkForImmediateVariable(instanceOfTree, state, enclosingIf);
      if (replaceExistingVariable != NO_MATCH) {
        return replaceExistingVariable;
      }
      if (getSymbol(instanceOfTree.getExpression()) instanceof VarSymbol varSymbol) {
        Type targetType = getType(instanceOfTree.getType());
        var allCasts = findAllCasts(varSymbol, enclosingIf.getThenStatement(), targetType, state);
        if (!allCasts.isEmpty()) {
          // This is a gamble as to an appropriate name. We could make sure it doesn't clash with
          // anything in scope, but that's effort.
          String name = generateVariableName(targetType, state);

          return describeMatch(
              instanceOfTree,
              SuggestedFix.builder()
                  .postfixWith(instanceOfTree, " " + name)
                  .merge(
                      allCasts.stream()
                          .map(c -> SuggestedFix.replace(c, name))
                          .collect(mergeFixes()))
                  .build());
        }
      }
    }
    // TODO(ghm): Handle things other than just ifs. It'd be great to refactor `foo instanceof Bar
    // && ((Bar) foo).baz()`.
    return NO_MATCH;
  }

  private static String generateVariableName(Type targetType, VisitorState state) {
    Type unboxed = state.getTypes().unboxedType(targetType);
    String simpleName = targetType.tsym.getSimpleName().toString();
    String lowerFirstLetter = toLowerCase(String.valueOf(simpleName.charAt(0)));
    String camelCased = lowerFirstLetter + simpleName.substring(1);
    if (SourceVersion.isKeyword(camelCased)
        || (unboxed != null && unboxed.getTag() != TypeTag.NONE)) {
      return lowerFirstLetter;
    }
    return camelCased;
  }

  private Description checkForImmediateVariable(
      InstanceOfTree instanceOfTree, VisitorState state, IfTree enclosingIf) {
    var body = enclosingIf.getThenStatement();
    if (!(body instanceof BlockTree block)) {
      return NO_MATCH;
    }
    if (block.getStatements().isEmpty()) {
      return NO_MATCH;
    }
    var firstStatement = block.getStatements().get(0);
    if (!(firstStatement instanceof VariableTree variableTree)) {
      return NO_MATCH;
    }
    if (!(variableTree.getInitializer() instanceof TypeCastTree typeCast)) {
      return NO_MATCH;
    }
    // TODO(ghm): Relax the requirement of this being an exact same symbol. Handle expressions?
    if (!(state
            .getTypes()
            .isSubtype(getType(instanceOfTree.getType()), getType(variableTree.getType()))
        && getSymbol(instanceOfTree.getExpression()) instanceof VarSymbol varSymbol
        && varSymbol.equals(getSymbol(typeCast.getExpression())))) {
      return NO_MATCH;
    }
    return describeMatch(
        firstStatement,
        SuggestedFix.builder()
            .delete(variableTree)
            .postfixWith(instanceOfTree, " " + variableTree.getName().toString())
            .build());
  }

  /**
   * Finds the enclosing IfTree if the provided {@code instanceof} is guaranteed to imply the then
   * branch.
   */
  // TODO(ghm): handle _inverted_ ifs.
  private static @Nullable IfTree findEnclosingIf(InstanceOfTree tree, VisitorState state) {
    Tree last = tree;
    for (Tree parent : state.getPath().getParentPath()) {
      switch (parent.getKind()) {
        case CONDITIONAL_AND, PARENTHESIZED -> {}
        case IF -> {
          if (((IfTree) parent).getCondition() == last) {
            return (IfTree) parent;
          } else {
            return null;
          }
        }
        default -> {
          return null;
        }
      }
      last = parent;
    }
    return null;
  }

  /** Finds all casts of {@code symbol} which are cast to {@code targetType} within {@code tree}. */
  private static ImmutableSet<Tree> findAllCasts(
      VarSymbol symbol, StatementTree tree, Type targetType, VisitorState state) {
    ImmutableSet.Builder<Tree> usages = ImmutableSet.builder();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitTypeCast(TypeCastTree node, Void unused) {
        if (getSymbol(node.getExpression()) instanceof VarSymbol v) {
          if (v.equals(symbol) && state.getTypes().isSubtype(targetType, getType(node.getType()))) {
            usages.add(
                getCurrentPath().getParentPath().getLeaf() instanceof ParenthesizedTree p
                    ? p
                    : node);
          }
        }
        return super.visitTypeCast(node, null);
      }
    }.scan(new TreePath(new TreePath(state.getPath().getCompilationUnit()), tree), null);
    return usages.build();
  }
}
