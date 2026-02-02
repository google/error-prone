/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getEnclosedElements;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.StaticImports.StaticImportInfo;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConstantCaseLabelTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.DCTree.DCReference;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.Name;
import org.jspecify.annotations.Nullable;

/**
 * @author gak@google.com (Gregory Kick)
 */
@BugPattern(
    summary = "Unused imports",
    severity = SUGGESTION,
    documentSuppression = false,
    tags = StandardTags.STYLE)
public final class RemoveUnusedImports extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(
      CompilationUnitTree compilationUnitTree, VisitorState state) {
    ImmutableSetMultimap<ImportTree, Symbol> importedSymbols =
        getImportedSymbols(compilationUnitTree, state);

    if (importedSymbols.isEmpty()) {
      return NO_MATCH;
    }

    LinkedHashSet<ImportTree> unusedImports = new LinkedHashSet<>(importedSymbols.keySet());
    new TreeSymbolScanner(JavacTrees.instance(state.context), state)
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
    ImmutableSet<String> unusedImportSimpleNames =
        unusedImports.stream()
            .map(tree -> getSimpleName(tree).toString())
            .collect(toImmutableSet());
    ImmutableSetMultimap<String, String> actualMeanings =
        actualMeanings(unusedImportSimpleNames, state);
    return buildDescription(unusedImports.iterator().next())
        .addFix(fixBuilder.build())
        .setMessage(
            "Unused imports: "
                + unusedImports.stream()
                    .map(
                        tree -> {
                          String name = state.getSourceForNode(tree.getQualifiedIdentifier());
                          var meanings = actualMeanings.get(getSimpleName(tree).toString());
                          if (meanings.isEmpty()) {
                            return name;
                          }
                          return String.format(
                              "%s (this name appears in the file, but resolves to %s)",
                              name, meanings.stream().collect(joining()));
                        })
                    .collect(joining(", ")))
        .build();
  }

  private static Name getSimpleName(ImportTree importTree) {
    return importTree.getQualifiedIdentifier() instanceof IdentifierTree idTree
        ? idTree.getName()
        : ((MemberSelectTree) importTree.getQualifiedIdentifier()).getIdentifier();
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
    private final DocTreeSymbolScanner docTreeSymbolScanner;
    private final JavacTrees trees;
    private final VisitorState state;

    private TreeSymbolScanner(JavacTrees trees, VisitorState state) {
      this.docTreeSymbolScanner = new DocTreeSymbolScanner();
      this.trees = trees;
      this.state = state;
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
      var path = getCurrentPath();
      if (path.getParentPath().getLeaf() instanceof ConstantCaseLabelTree caseLabelTree) {
        var caseType = getType(caseLabelTree.getConstantExpression());
        var switchType =
            getType(
                getSwitchExpression(
                    path.getParentPath().getParentPath().getParentPath().getLeaf()));
        // Exceptionally, a ConstantCaseLabel used in an enum of a matching switch type does not
        // require an import.
        if (caseType.tsym.isEnum() && isSameType(caseType, switchType, state)) {
          return null;
        }
      }
      sink.accept(symbol.baseSymbol());
      return null;
    }

    private static ExpressionTree getSwitchExpression(Tree tree) {
      return switch (tree) {
        case SwitchTree switchTree -> switchTree.getExpression();
        case SwitchExpressionTree switchExpressionTree -> switchExpressionTree.getExpression();
        default -> throw new AssertionError("Case not inside switch statement");
      };
    }

    @Override
    public Void visitClass(ClassTree node, SymbolSink symbolSink) {
      if (node.getKind().equals(Tree.Kind.RECORD)) {
        getEnclosedElements(getSymbol(node)).stream()
            .flatMap(e -> e.getAnnotationMirrors().stream())
            .map(a -> (Symbol) a.getAnnotationType().asElement())
            .forEach(symbolSink::accept);
      }
      return super.visitClass(node, symbolSink);
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
        trees.getElement(getCurrentPath());
        TreeScanner<Void, SymbolSink> nonRecursiveScanner =
            new TreeScanner<Void, SymbolSink>() {
              @Override
              public Void visitIdentifier(IdentifierTree tree, SymbolSink sink) {
                Symbol sym = ASTHelpers.getSymbol(tree);
                if (sym != null) {
                  sink.accept(sym);
                }
                return null;
              }
            };
        DCReference reference = (DCReference) referenceTree;
        nonRecursiveScanner.scan(reference.qualifierExpression, sink);
        nonRecursiveScanner.scan(reference.paramTypes, sink);
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

  private static ImmutableSetMultimap<String, String> actualMeanings(
      Set<String> simpleNames, VisitorState state) {
    ImmutableSetMultimap.Builder<String, String> meanings = ImmutableSetMultimap.builder();
    new TreeScanner<Void, Void>() {
      // Don't descend into imports so we don't report those as meanings.
      @Override
      public Void visitImport(ImportTree tree, Void unused) {
        return null;
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        var symbol = getSymbol(tree);
        if (simpleNames.contains(symbol.getSimpleName().toString())) {
          meanings.put(
              symbol.getSimpleName().toString(),
              symbol instanceof MethodSymbol || symbol instanceof VarSymbol
                  ? symbol.owner.getQualifiedName() + "#" + symbol.getSimpleName()
                  : symbol.getQualifiedName().toString());
        }
        return null;
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return meanings.build();
  }
}
