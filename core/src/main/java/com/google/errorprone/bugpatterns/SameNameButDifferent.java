/*
 * Copyright 2019 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.findPathFromEnclosingNodeToTopLevel;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.FindIdentifiers.findIdent;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Joiner;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.ElementKind;

/** Looks for types being shadowed by other types in a way that may be confusing. */
@BugPattern(
    name = "SameNameButDifferent",
    summary = "This type name shadows another in a way that may be confusing.",
    severity = WARNING)
public final class SameNameButDifferent extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    Table<String, TypeSymbol, List<TreePath>> table = HashBasedTable.create();
    new SuppressibleTreePathScanner<Void, Void>() {
      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
        if (!shouldIgnore()) {
          handle(memberSelectTree);
        }
        return super.visitMemberSelect(memberSelectTree, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
        if (shouldIgnore()) {
          return null;
        }
        if (!(getSymbol(identifierTree) instanceof ClassSymbol)) {
          return null;
        }
        TreePath enclosingClass =
            findPathFromEnclosingNodeToTopLevel(getCurrentPath(), ClassTree.class);
        if (enclosingClass != null
            && getSymbol(enclosingClass.getLeaf()) == getSymbol(identifierTree)) {
          return null;
        }
        handle(identifierTree);
        return null;
      }

      private boolean shouldIgnore() {
        // Don't report duplicate hits if we're not at the tail of a series of member selects on
        // classes.
        Tree parentTree = getCurrentPath().getParentPath().getLeaf();
        return parentTree instanceof MemberSelectTree
            && getSymbol(parentTree) instanceof ClassSymbol;
      }

      private void handle(Tree tree) {
        if (tree instanceof IdentifierTree
            && ((IdentifierTree) tree).getName().contentEquals("Builder")) {
          return;
        }
        String treeSource = state.getSourceForNode(tree);
        if (treeSource == null) {
          return;
        }
        Symbol symbol = getSymbol(tree);
        if (symbol instanceof ClassSymbol) {
          List<TreePath> treePaths = table.get(treeSource, symbol.type.tsym);
          if (treePaths == null) {
            treePaths = new ArrayList<>();
            table.put(treeSource, symbol.type.tsym, treePaths);
          }
          treePaths.add(getCurrentPath());
        }
      }
    }.scan(state.getPath(), null);

    // Keep any (simpleName, typeSymbol) entries which shadow a class name outside the enclosing
    // class.
    Table<String, TypeSymbol, List<TreePath>> trimmedTable = HashBasedTable.create();
    for (Map.Entry<String, Map<TypeSymbol, List<TreePath>>> row : table.rowMap().entrySet()) {
      Map<TypeSymbol, List<TreePath>> columns = row.getValue();
      if (columns.size() <= 1) {
        continue;
      }
      for (Map.Entry<TypeSymbol, List<TreePath>> cell : columns.entrySet()) {
        if (cell.getValue().stream().anyMatch(treePath -> shadowsClass(state, treePath))) {
          trimmedTable.put(row.getKey(), cell.getKey(), cell.getValue());
        }
      }
    }

    for (Map.Entry<String, Map<TypeSymbol, List<TreePath>>> row :
        trimmedTable.rowMap().entrySet()) {
      String simpleName = row.getKey();
      Map<TypeSymbol, List<TreePath>> columns = row.getValue();

      SuggestedFix.Builder fix = SuggestedFix.builder();
      if (columns.size() > 1) {
        for (Map.Entry<TypeSymbol, List<TreePath>> cell : columns.entrySet()) {
          for (TreePath treePath : cell.getValue()) {
            TypeSymbol typeSymbol = cell.getKey();
            getBetterImport(typeSymbol, simpleName)
                .ifPresent(
                    imp -> {
                      String qualifiedName = qualifyType(state.withPath(treePath), fix, imp);
                      String newSimpleName = qualifiedName + "." + simpleName;
                      fix.replace(treePath.getLeaf(), newSimpleName);
                    });
          }
        }
        String message =
            String.format(
                "The name `%s` refers to %s within this file. It may be confusing to have the same"
                    + " name refer to multiple types. Consider qualifying them for clarity.",
                simpleName,
                columns.keySet().stream()
                    .map(t -> t.getQualifiedName().toString())
                    .collect(joining(", ", "[", "]")));
        for (List<TreePath> treePaths : trimmedTable.row(simpleName).values()) {
          for (TreePath treePath : treePaths) {
            state.reportMatch(
                buildDescription(treePath.getLeaf())
                    .setMessage(message)
                    .addFix(fix.build())
                    .build());
          }
        }
      }
    }
    return NO_MATCH;
  }

  private static boolean shadowsClass(VisitorState state, TreePath treePath) {
    if (!(treePath.getLeaf() instanceof IdentifierTree)) {
      return true;
    }

    TreePath enclosingClass = findPathFromEnclosingNodeToTopLevel(treePath, ClassTree.class);
    String name = ((IdentifierTree) treePath.getLeaf()).getName().toString();
    return findIdent(name, state.withPath(enclosingClass), KindSelector.VAL_TYP) != null;
  }

  private static Optional<Symbol> getBetterImport(TypeSymbol classSymbol, String simpleName) {
    Symbol owner = classSymbol;
    long dots = simpleName.chars().filter(c -> c == '.').count();
    for (long i = 0; i < dots + 1; ++i) {
      owner = owner.owner;
    }
    if (owner instanceof ClassSymbol) {
      return Optional.of(owner);
    }
    return Optional.empty();
  }

  /** Try to qualify the type, or return the full name. */
  public static String qualifyType(VisitorState state, SuggestedFix.Builder fix, Symbol sym) {
    Deque<String> names = new ArrayDeque<>();
    for (Symbol curr = sym; curr != null; curr = curr.owner) {
      names.addFirst(curr.getSimpleName().toString());
      Symbol found = findIdent(curr.getSimpleName().toString(), state, KindSelector.VAL_TYP);
      if (found == curr) {
        break;
      }
      if (curr.getKind() == ElementKind.PACKAGE) {
        return sym.getQualifiedName().toString();
      }
      if (found == null) {
        fix.addImport(curr.getQualifiedName().toString());
        break;
      }
    }
    return Joiner.on('.').join(names);
  }
}
