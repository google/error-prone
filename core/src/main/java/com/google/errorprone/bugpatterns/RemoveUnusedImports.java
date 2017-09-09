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
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
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
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Types.SimpleVisitor;
import com.sun.tools.javac.tree.DCTree.DCReference;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/** @author gak@google.com (Gregory Kick) */
@BugPattern(
  name = "RemoveUnusedImports",
  summary = "Unused imports",
  explanation = "This import is unused.",
  category = JDK,
  severity = SUGGESTION,
  documentSuppression = false,
  tags = StandardTags.STYLE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public final class RemoveUnusedImports extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(
      CompilationUnitTree compilationUnitTree, VisitorState state) {
    final ImmutableSetMultimap<ImportTree, Symbol> importedSymbols =
        getImportedSymbols(compilationUnitTree, state);

    if (importedSymbols.isEmpty()) {
      return NO_MATCH;
    }

    final Set<ImportTree> unusedImports = new HashSet<>(importedSymbols.keySet());
    new TreeSymbolScanner(JavacTrees.instance(state.context), state.getTypes())
        .scan(
            compilationUnitTree,
            new SymbolSink() {
              @Override
              public boolean keepScanning() {
                return !unusedImports.isEmpty();
              }

              @Override
              public void accept(Symbol symbol) {
                unusedImports.removeAll(importedSymbols.inverse().get(symbol));
              }
            });

    if (unusedImports.isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    for (ImportTree unusedImport : unusedImports) {
      fixBuilder.delete(unusedImport);
    }
    return describeMatch(unusedImports.iterator().next(), fixBuilder.build());
  }

  /**
   * A callback that provides actions for whenever you encounter a {@link Symbol}. Also provides a
   * hook by which you can stop scanning.
   */
  private interface SymbolSink {
    boolean keepScanning();

    void accept(Symbol symbol);
  }

  private static final class TreeSymbolScanner extends TreePathScanner<Void, SymbolSink> {
    final DocTreeSymbolScanner docTreeSymbolScanner;
    final JavacTrees trees;
    final Types types;

    private TreeSymbolScanner(JavacTrees trees, Types types) {
      this.types = types;
      this.docTreeSymbolScanner = new DocTreeSymbolScanner();
      this.trees = trees;
    }

    /** Skip the imports themselves when checking for usage. */
    @Override
    public Void visitImport(ImportTree importTree, SymbolSink usedSymbols) {
      return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, SymbolSink sink) {
      if (tree == null) {
        return null;
      }
      Symbol symbol = getSymbol(tree);
      if (symbol == null) {
        return null;
      }
      sink.accept(symbol.baseSymbol());
      return null;
    }

    @Override
    public Void scan(Tree tree, SymbolSink sink) {
      if (!sink.keepScanning()) {
        return null;
      }
      if (tree == null) {
        return null;
      }
      scanJavadoc(sink);
      return super.scan(tree, sink);
    }

    private void scanJavadoc(SymbolSink sink) {
      if (getCurrentPath() == null) {
        return;
      }
      DocCommentTree commentTree = trees.getDocCommentTree(getCurrentPath());
      if (commentTree == null) {
        return;
      }
      docTreeSymbolScanner.scan(new DocTreePath(getCurrentPath(), commentTree), sink);
    }

    /**
     * For the time being, this will just report any symbol referenced from javadoc as a usage.
     * TODO(gak): improve this so that we can remove imports used only from javadoc and replace the
     * usages with fully-qualified names.
     */
    final class DocTreeSymbolScanner extends DocTreePathScanner<Void, SymbolSink> {
      @Override
      public Void visitReference(ReferenceTree referenceTree, SymbolSink sink) {
        // do this first, it attributes the referenceTree as a side-effect
        Symbol symbolForReference = (Symbol) trees.getElement(getCurrentPath());
        JCTree base = ((DCReference) referenceTree).qualifierExpression;
        // Only the base type in a qualified name needs to be imported.
        // e.g. `Map.Entry` is a use of `Map`, `java.util.List` does not require an import
        while (base instanceof JCFieldAccess) {
          base = ((JCFieldAccess) base).selected;
        }
        if (base instanceof JCIdent) {
          sink.accept(((JCIdent) base).sym);
        }
        // Record uses inside method parameters.
        if (symbolForReference instanceof MethodSymbol) {
          for (VarSymbol parameter : ((MethodSymbol) symbolForReference).getParameters()) {
            parameter.type.accept(
                new SimpleVisitor<Void, Void>() {
                  @Override
                  public Void visitArrayType(ArrayType type, Void unused) {
                    type.getComponentType().accept(this, null);
                    return null;
                  }

                  @Override
                  public Void visitType(Type type, Void unused) {
                    sink.accept(types.erasure(type).tsym);
                    return null;
                  }
                },
                null);
          }
        }
        return null;
      }
    }
  }

  private static ImmutableSetMultimap<ImportTree, Symbol> getImportedSymbols(
      CompilationUnitTree compilationUnitTree, VisitorState state) {
    ImmutableSetMultimap.Builder<ImportTree, Symbol> builder = ImmutableSetMultimap.builder();
    for (ImportTree importTree : compilationUnitTree.getImports()) {
      builder.putAll(importTree, getImportedSymbols(importTree, state));
    }
    return builder.build();
  }

  private static ImmutableSet<Symbol> getImportedSymbols(
      ImportTree importTree, VisitorState state) {
    if (importTree.isStatic()) {
      StaticImportInfo staticImportInfo = StaticImports.tryCreate(importTree, state);
      return staticImportInfo == null ? ImmutableSet.<Symbol>of() : staticImportInfo.members();
    } else {
      @Nullable Symbol importedSymbol = getSymbol(importTree.getQualifiedIdentifier());
      return importedSymbol == null ? ImmutableSet.<Symbol>of() : ImmutableSet.of(importedSymbol);
    }
  }
}
