/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.bugpatterns.StaticImports.StaticImportInfo;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;

import java.util.Set;

/**
 * @author gak@google.com (Gregory Kick)
 */
@BugPattern(
  name = "RemoveUnusedImports",
  summary = "Unused import",
  explanation = "Unused import",
  category = JDK,
  maturity = EXPERIMENTAL,
  severity = SUGGESTION
)
/* TODO(gak): should this match the entire compilation unit or each individual import?  In either
 * case, rescanning the entire compilation unit for each import is a huge waste and we should fix
 * that. */
public final class RemoveUnusedImports extends BugChecker implements ImportTreeMatcher {
  @Override
  public Description matchImport(ImportTree importTree, VisitorState state) {
    CompilationUnitTree compilationUnit = state.getPath().getCompilationUnit();
    Set<Symbol> importedSymbols = getImportedSymbols(importTree, state);

    if (importedSymbols.isEmpty()) {
      return NO_MATCH;
    }

    boolean used =
        new TreeSymbolScanner(compilationUnit, JavacTrees.instance(state.context))
            .scan(compilationUnit, importedSymbols);

    if (used) {
      return NO_MATCH;
    }

    return describeMatch(importTree, SuggestedFix.delete(importTree));
  }

  private static final class TreeSymbolScanner extends TreePathScanner<Boolean, Set<Symbol>> {
    final DocTreeSymbolScanner docTreeSymbolScanner;
    final CompilationUnitTree compilationUnit;
    final JavacTrees trees;

    private TreeSymbolScanner(CompilationUnitTree compilationUnit, JavacTrees trees) {
      this.docTreeSymbolScanner = new DocTreeSymbolScanner();
      this.compilationUnit = compilationUnit;
      this.trees = trees;
    }

    /** Skip the imports themselves when checking for usage. */
    @Override
    public Boolean visitImport(ImportTree importTree, Set<Symbol> symbols) {
      return false;
    }

    boolean containsSymbol(Tree tree, Set<Symbol> searchSymbols) {
      if (tree == null) {
        return false;
      }
      Symbol symbol = getSymbol(tree);
      if (symbol == null) {
        return false;
      }
      return searchSymbols.contains(symbol.baseSymbol());
    }

    @Override
    public Boolean visitIdentifier(IdentifierTree tree, Set<Symbol> searchSymbols) {
      return containsSymbol(tree, searchSymbols);
    }

    @Override
    public Boolean scan(Tree tree, Set<Symbol> searchSymbols) {
      if (tree == null) {
        return false;
      }
      return reduce(scanJavadoc(searchSymbols), super.scan(tree, searchSymbols));
    }

    private boolean scanJavadoc(Set<Symbol> searchSymbols) {
      if (getCurrentPath() == null) {
        return false;
      }
      DocCommentTree commentTree = trees.getDocCommentTree(getCurrentPath());
      if (commentTree == null) {
        return false;
      }
      return docTreeSymbolScanner.scan(
          new DocTreePath(getCurrentPath(), commentTree), searchSymbols);
    }

    @Override
    public Boolean reduce(Boolean a, Boolean b) {
      return (a == null ? false : a) || (b == null ? false : b);
    }

    /**
     * For the time being, this will just report any symbol referenced from javadoc as a usage.
     * TODO(gak): improve this so that we can remove imports used only from javadoc and replace the
     * usages with fully-qualified names.
     */
    final class DocTreeSymbolScanner extends DocTreePathScanner<Boolean, Set<Symbol>> {
      @Override
      public Boolean visitReference(ReferenceTree referenceTree, Set<Symbol> searchSymbols) {
        Optional<Symbol> symbolForReference = getSymbolForReference(getCurrentPath());
        if (symbolForReference.isPresent()) {
          /* If the signature is for a member (includes a #) then we need to get the symbol for the
           * owner because that's what will have been imported. */
          Symbol possiblyImportedSymbol =
              referenceTree.getSignature().contains("#")
                  ? symbolForReference.get().owner
                  : symbolForReference.get();
          return searchSymbols.contains(possiblyImportedSymbol);
        }
        return false;
      }

      @Override
      public Boolean reduce(Boolean a, Boolean b) {
        return (a == null ? false : a) || (b == null ? false : b);
      }

      Optional<Symbol> getSymbolForReference(DocTreePath path) {
        return Optional.fromNullable((Symbol) trees.getElement(path));
      }
    }
  }

  private static Set<Symbol> getImportedSymbols(ImportTree importTree, VisitorState state) {
    if (importTree.isStatic()) {
      StaticImportInfo staticImportInfo = StaticImports.tryCreate(importTree, state);
      return staticImportInfo == null ? ImmutableSet.<Symbol>of() : staticImportInfo.members();
    } else {
      return Optional.fromNullable(getSymbol(importTree.getQualifiedIdentifier())).asSet();
    }
  }
}
