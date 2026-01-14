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
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.SourceVersion;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

/** Bugpattern to rename unused variables to _. */
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
    for (Map.Entry<Symbol, VariableTree> unused : finder.unused.entrySet()) {
      state.reportMatch(
          describeMatch(
              unused.getValue(), SuggestedFixes.renameVariable(unused.getValue(), "_", state)));
    }
    return NO_MATCH;
  }

  private final class VariableFinder extends TreePathScanner<Void, Void> {
    private final VisitorState state;

    private final Map<Symbol, VariableTree> unused = new HashMap<>();

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
      this.unused.remove(symbol);
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
      boolean allowedUnnamed =
          switch (symbol.getKind()) {
            case LOCAL_VARIABLE, EXCEPTION_PARAMETER, RESOURCE_VARIABLE, BINDING_VARIABLE -> true;
            case PARAMETER ->
                getCurrentPath().getParentPath().getLeaf() instanceof LambdaExpressionTree;
            default -> false;
          };
      if (!allowedUnnamed) {
        return;
      }
      unused.put(symbol, tree);
    }
  }
}
