/*
 * Copyright 2025 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.SourceVersion;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;

/** A bug pattern that suggests renaming unused variables to {@code var _}. */
@BugPattern(
    summary = "Consider renaming unused variables and lambda parameters to _",
    severity = WARNING)
public final class UnnamedVariable extends BugChecker implements CompilationUnitTreeMatcher {

  private final boolean onlyRenameVariablesNamedUnused;

  @Inject
  UnnamedVariable(ErrorProneFlags errorProneFlags) {
    this.onlyRenameVariablesNamedUnused =
        errorProneFlags.getBoolean("UnnamedVariable:OnlyRenameVariablesNamedUnused").orElse(true);
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    if (!SourceVersion.supportsUnnamedVariablesAndPatterns(state.context)) {
      return NO_MATCH;
    }
    VariableFinder finder = new VariableFinder(state);
    finder.scan(tree, null);
    finder.unusedSymbols.forEach(
        (symbol, path) -> {
          VariableTree varTree = (VariableTree) path.getLeaf();
          state.reportMatch(describeMatch(varTree, buildFix(varTree, symbol, path, state)));
        });
    return NO_MATCH;
  }

  private static SuggestedFix buildFix(
      VariableTree tree, Symbol symbol, TreePath path, VisitorState state) {
    if (symbol.getKind() == ElementKind.PARAMETER) {
      // TODO(kak): we should also drop unnecessary parens. E.g.,
      // before:  `(String unused) -> 1`
      // after:   `(_) -> 1`
      // ideally: `_ -> 1`
      return SuggestedFix.replace(tree, "_");
    }
    SuggestedFix fix = SuggestedFixes.renameVariable(tree, "_", state);
    if (canUseVar(tree, symbol, path)) {
      SuggestedFix.Builder builder = fix.toBuilder();
      SuggestedFixes.replaceVariableType(tree, "var", state).ifPresent(builder::merge);
      return builder.build();
    }
    return fix;
  }

  /**
   * Returns true if the given variable can be declared using {@code var}. This is true if the
   * variable is:
   *
   * <ul>
   *   <li>A local or resource variable.
   *   <li>Part of an enhanced for loop or has a non-null initializer.
   *   <li>Not part of a compound declaration (e.g., {@code int a, b;}).
   * </ul>
   */
  private static boolean canUseVar(VariableTree tree, Symbol symbol, TreePath path) {
    boolean isLocalOrResource =
        switch (symbol.getKind()) {
          case LOCAL_VARIABLE, RESOURCE_VARIABLE -> true;
          default -> false;
        };
    boolean isForEachOrHasInitializer =
        path.getParentPath().getLeaf() instanceof EnhancedForLoopTree
            || (tree.getInitializer() != null
                && tree.getInitializer().getKind() != Tree.Kind.NULL_LITERAL);
    boolean isNotCompound = !isPartOfCompoundDeclaration(tree, path);

    return isLocalOrResource && isForEachOrHasInitializer && isNotCompound;
  }

  /**
   * Returns true if the given variable is part of a compound declaration (e.g., {@code int a, b;}).
   * In javac, compound declarations share the same type tree object instance and start position.
   */
  private static boolean isPartOfCompoundDeclaration(VariableTree tree, TreePath path) {
    Tree parent = path.getParentPath().getLeaf();
    if (parent instanceof BlockTree blockTree) {
      return isCompound(tree, blockTree.getStatements());
    }
    if (parent instanceof ForLoopTree forLoopTree) {
      return isCompound(tree, forLoopTree.getInitializer());
    }
    if (parent instanceof TryTree tryTree) {
      return isCompound(tree, tryTree.getResources());
    }
    return false;
  }

  /**
   * Returns true if any other variable in the list of trees shares the same start position as the
   * given variable.
   */
  private static boolean isCompound(VariableTree tree, List<? extends Tree> trees) {
    for (Tree sibling : trees) {
      if (sibling instanceof VariableTree siblingVar && siblingVar != tree) {
        if (getStartPosition(sibling) == getStartPosition(tree)) {
          return true;
        }
      }
    }
    return false;
  }

  private final class VariableFinder extends TreePathScanner<Void, Void> {

    private final VisitorState state;

    private final Map<Symbol, TreePath> unusedSymbols = new HashMap<>();

    private VariableFinder(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitVariable(VariableTree tree, Void unused) {
      handleVariable(tree);
      return super.visitVariable(tree, null);
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, Void unused) {
      Symbol symbol = getSymbol(tree);
      this.unusedSymbols.remove(symbol);
      return null;
    }

    private void handleVariable(VariableTree tree) {
      // This doesn't use SuppressibleTreePathScanner because we want to keep descending into
      // trees with suppressions to look for usages of the variables.
      if (isSuppressed(tree, state)) {
        return;
      }
      Symbol symbol = getSymbol(tree);
      if (symbol.getSimpleName().isEmpty()) {
        return;
      }
      // If a variable is named 'unused' and is actually unused, there's a strong readability
      // argument for using _. For more descriptive names, there may be some readability benefit
      // to keeping the name, and there's also a small change it's only accidentally unused and
      // renaming it would sweep an UnusedVariable finding under the rug.
      //
      // TODO: cushon - consider more heuristics, like single-parameter identifiers, where the
      // rename to _ is very likely to be an improvement.
      // TODO: cushon - consider looking for variables that start with unused
      if (onlyRenameVariablesNamedUnused && !symbol.getSimpleName().contentEquals("unused")) {
        return;
      }
      TreePath path = getCurrentPath();
      boolean allowedUnnamed =
          switch (symbol.getKind()) {
            case LOCAL_VARIABLE ->
                tree.getInitializer() != null
                    || getCurrentPath().getParentPath().getLeaf() instanceof EnhancedForLoopTree;
            case EXCEPTION_PARAMETER, RESOURCE_VARIABLE, BINDING_VARIABLE -> true;
            case PARAMETER ->
                getCurrentPath().getParentPath().getLeaf() instanceof LambdaExpressionTree;
            default -> false;
          };
      if (allowedUnnamed) {
        unusedSymbols.put(symbol, path);
      }
    }
  }
}
