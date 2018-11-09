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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.Name;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A checker that suggests deduplicating literals with existing constant variables.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
    name = "DeduplicateConstants",
    summary =
        "This expression was previously declared as a constant;"
            + " consider replacing this occurrence.",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class DeduplicateConstants extends BugChecker implements CompilationUnitTreeMatcher {

  /** A lexical scope for constant declarations. */
  static class Scope {

    /** A map from string literals to constant declarations. */
    private final HashMap<String, VarSymbol> values = new HashMap<>();
    /** Declarations that are hidden in the current scope. */
    private final Set<Name> hidden = new HashSet<>();

    /** The parent of the current scope. */
    private final Scope parent;

    Scope(Scope parent) {
      this.parent = parent;
    }

    /** Enters a new sub-scope. */
    Scope enter() {
      return new Scope(this);
    }

    /** Returns an in-scope constant variable with the given value. */
    public VarSymbol get(String value) {
      VarSymbol sym = getInternal(value);
      if (sym == null) {
        return null;
      }
      if (hidden.contains(sym.getSimpleName())) {
        return null;
      }
      return sym;
    }

    private VarSymbol getInternal(String value) {
      VarSymbol sym = values.get(value);
      if (sym != null) {
        return sym;
      }
      if (parent != null) {
        sym = parent.get(value);
        if (sym != null) {
          return sym;
        }
      }
      return null;
    }

    /** Adds a constant declaration with the given value to the current scope. */
    public void put(String value, VarSymbol sym) {
      hidden.remove(sym.getSimpleName());
      values.put(value, sym);
    }

    /**
     * Records a non-constant variable declaration that hides any previously declared constants of
     * the same name.
     */
    public void remove(VarSymbol sym) {
      hidden.add(sym.getSimpleName());
    }
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    Table<VarSymbol, Tree, SuggestedFix> fixes = HashBasedTable.create();
    new TreeScanner<Void, Scope>() {

      @Override
      public Void visitBlock(BlockTree tree, Scope scope) {
        // enter a new block scope (includes block trees for method and class bodies)
        return super.visitBlock(tree, scope.enter());
      }

      @Override
      public Void visitVariable(VariableTree tree, Scope scope) {
        // record that this variables hides previous declarations before entering its initializer
        scope.remove(ASTHelpers.getSymbol(tree));
        scan(tree.getInitializer(), scope);
        saveConstValue(tree, scope);
        return null;
      }

      @Override
      public Void visitLiteral(LiteralTree tree, Scope scope) {
        replaceLiteral(tree, scope, state);
        return super.visitLiteral(tree, scope);
      }

      private void replaceLiteral(LiteralTree tree, Scope scope, VisitorState state) {
        Object value = ASTHelpers.constValue(tree);
        if (value == null) {
          return;
        }
        VarSymbol sym = scope.get(state.getSourceForNode(tree));
        if (sym == null) {
          return;
        }
        SuggestedFix fix = SuggestedFix.replace(tree, sym.getSimpleName().toString());
        fixes.put(sym, tree, fix);
      }

      private void saveConstValue(VariableTree tree, Scope scope) {
        VarSymbol sym = ASTHelpers.getSymbol(tree);
        if (sym == null) {
          return;
        }
        if ((sym.flags() & (Flags.EFFECTIVELY_FINAL | Flags.FINAL)) == 0) {
          return;
        }
        // heuristic: long string constants are generally more interesting than short ones, or
        // than non-string constants (e.g. `""`, `0`, or `false`).
        String constValue = ASTHelpers.constValue(tree.getInitializer(), String.class);
        if (constValue == null || constValue.length() <= 1) {
          return;
        }
        scope.put(state.getSourceForNode(tree.getInitializer()), sym);
      }
    }.scan(tree, new Scope(null));
    for (Map.Entry<VarSymbol, Map<Tree, SuggestedFix>> entries : fixes.rowMap().entrySet()) {
      Map<Tree, SuggestedFix> occurrences = entries.getValue();
      if (occurrences.size() < 2) {
        // heuristic: only de-duplicate when there are two or more occurrences
        continue;
      }
      // report the finding on each occurrence, but provide a fix for all related occurrences,
      // so it works better on changed-lines only
      SuggestedFix fix = mergeFix(occurrences.values());
      occurrences.keySet().forEach(t -> state.reportMatch(describeMatch(t, fix)));
    }
    return Description.NO_MATCH;
  }

  private static SuggestedFix mergeFix(Collection<SuggestedFix> fixes) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fixes.forEach(fix::merge);
    return fix.build();
  }
}
