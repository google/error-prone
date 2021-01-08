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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getGeneratedBy;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.FindIdentifiers.findIdent;
import static com.sun.tools.javac.code.Kinds.KindSelector.VAL_TYP;

import com.google.common.base.Ascii;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.util.Position;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Name;

/** Flags uses of fully qualified names which are not ambiguous if imported. */
@BugPattern(
    name = "UnnecessarilyFullyQualified",
    severity = SeverityLevel.WARNING,
    summary = "This fully qualified name is unambiguous to the compiler if imported.")
public final class UnnecessarilyFullyQualified extends BugChecker
    implements CompilationUnitTreeMatcher {

  private static final ImmutableSet<String> EXEMPTED_NAMES =
      ImmutableSet.of(
          );

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    if (tree.getTypeDecls().stream()
        .anyMatch(
            t -> getSymbol(tree) != null && !getGeneratedBy(getSymbol(tree), state).isEmpty())) {
      return NO_MATCH;
    }
    if (isPackageInfo(tree)) {
      return NO_MATCH;
    }
    Table<Name, TypeSymbol, List<TreePath>> table = HashBasedTable.create();
    Set<Name> identifiersSeen = new HashSet<>();
    new SuppressibleTreePathScanner<Void, Void>() {
      @Override
      public Void visitImport(ImportTree importTree, Void unused) {
        return null;
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
        if (!shouldIgnore()) {
          handle(getCurrentPath());
        }
        return super.visitMemberSelect(memberSelectTree, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
        identifiersSeen.add(identifierTree.getName());
        return null;
      }

      private boolean shouldIgnore() {
        // Don't report duplicate hits if we're not at the tail of a series of member selects on
        // classes.
        Tree parentTree = getCurrentPath().getParentPath().getLeaf();
        return parentTree instanceof MemberSelectTree
            && getSymbol(parentTree) instanceof ClassSymbol;
      }

      private void handle(TreePath path) {
        MemberSelectTree tree = (MemberSelectTree) path.getLeaf();
        if (!isFullyQualified(tree)) {
          return;
        }
        if (BadImport.BAD_NESTED_CLASSES.contains(tree.getIdentifier().toString())) {
          if (tree.getExpression() instanceof MemberSelectTree
              && getSymbol(tree.getExpression()) instanceof ClassSymbol) {
            handle(new TreePath(path, tree.getExpression()));
          }
          return;
        }
        Symbol symbol = getSymbol(tree);
        if (!(symbol instanceof ClassSymbol)) {
          return;
        }
        if (state.getEndPosition(tree) == Position.NOPOS) {
          return;
        }
        List<TreePath> treePaths = table.get(tree.getIdentifier(), symbol.type.tsym);
        if (treePaths == null) {
          treePaths = new ArrayList<>();
          table.put(tree.getIdentifier(), symbol.type.tsym, treePaths);
        }
        treePaths.add(path);
      }

      private boolean isFullyQualified(MemberSelectTree tree) {
        AtomicBoolean isFullyQualified = new AtomicBoolean();
        new SimpleTreeVisitor<Void, Void>() {
          @Override
          public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
            return visit(memberSelectTree.getExpression(), null);
          }

          @Override
          public Void visitIdentifier(IdentifierTree identifierTree, Void aVoid) {
            if (getSymbol(identifierTree) instanceof PackageSymbol) {
              isFullyQualified.set(true);
            }
            return null;
          }
        }.visit(tree, null);
        return isFullyQualified.get();
      }
    }.scan(state.getPath(), null);

    for (Map.Entry<Name, Map<TypeSymbol, List<TreePath>>> rows : table.rowMap().entrySet()) {
      Name name = rows.getKey();
      Map<TypeSymbol, List<TreePath>> types = rows.getValue();
      // Skip places where the same simple name refers to multiple types.
      if (types.size() > 1) {
        continue;
      }
      // Skip weird Android classes which don't look like classes.
      if (Ascii.isLowerCase(name.charAt(0))) {
        continue;
      }
      if (identifiersSeen.contains(name)) {
        continue;
      }
      String nameString = name.toString();
      if (EXEMPTED_NAMES.contains(nameString)) {
        continue;
      }
      List<TreePath> pathsToFix = getOnlyElement(types.values());
      if (pathsToFix.stream()
          .anyMatch(path -> findIdent(nameString, state.withPath(path), VAL_TYP) != null)) {
        continue;
      }
      SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
      fixBuilder.addImport(getOnlyElement(types.keySet()).getQualifiedName().toString());
      for (TreePath path : pathsToFix) {
        fixBuilder.replace(path.getLeaf(), nameString);
      }
      SuggestedFix fix = fixBuilder.build();
      for (TreePath path : pathsToFix) {
        state.reportMatch(describeMatch(path.getLeaf(), fix));
      }
    }
    return NO_MATCH;
  }

  private boolean isPackageInfo(CompilationUnitTree tree) {
    String name = tree.getSourceFile().getName();
    int idx = name.lastIndexOf('/');
    if (idx != -1) {
      name = name.substring(idx + 1);
    }
    return name.equals("package-info.java");
  }
}
