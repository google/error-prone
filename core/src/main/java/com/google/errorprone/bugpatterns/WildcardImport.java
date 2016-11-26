/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Scope.StarImportScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.TreeScanner;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "WildcardImport",
  summary = "Wildcard imports, static or otherwise, should not be used",
  category = JDK,
  severity = SUGGESTION,

  linkType = CUSTOM,
  link = "https://google.github.io/styleguide/javaguide.html#s3.3.1-wildcard-imports"
  )
public class WildcardImport extends BugChecker implements CompilationUnitTreeMatcher {

  /** Maximum number of members to import before switching to qualified names. */
  public static final int MAX_MEMBER_IMPORTS = 20;

  /** A type or member that needs to be imported.*/
  @AutoValue
  abstract static class TypeToImport {

    /** Returns the simple name of the import. */
    abstract String name();

    /** Returns the owner of the imported type or member. */
    abstract Symbol owner();

    /** Returns true if the import needs to be static (i.e. the import is for a field or method). */
    abstract boolean isStatic();

    static TypeToImport create(String name, Symbol owner, boolean stat) {
      return new AutoValue_WildcardImport_TypeToImport(name, owner, stat);
    }

    private void addFix(SuggestedFix.Builder fix) {
      String qualifiedName = owner().getQualifiedName() + "." + name();
      if (isStatic()) {
        fix.addStaticImport(qualifiedName);
      } else {
        fix.addImport(qualifiedName);
      }
    }
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableList<ImportTree> wildcardImports = getWildcardImports(tree.getImports());
    if (wildcardImports.isEmpty()) {
      return NO_MATCH;
    }

    // Find all of the types that need to be imported.
    Set<TypeToImport> typesToImport = ImportCollector.collect((JCCompilationUnit) tree);

    // Group the imported types by the on-demand import they replace.
    Multimap<ImportTree, TypeToImport> toFix = groupImports(wildcardImports, typesToImport);

    Fix fix = createFix(wildcardImports, toFix, state);
    if (fix.isEmpty()) {
      return NO_MATCH;
    }
    return describeMatch(wildcardImports.get(0), fix);
  }

  /**
   * Creates a multimap from existing on-demand imports to the single-type imports we're going to
   * add, e.g.: java.util.* -> [java.util.List, java.util.ArrayList]
   */
  private Multimap<ImportTree, TypeToImport> groupImports(
      ImmutableList<ImportTree> wildcardImports, Set<TypeToImport> typesToImport) {
    Multimap<ImportTree, TypeToImport> toFix = LinkedListMultimap.create();
    for (TypeToImport type : typesToImport) {
      toFix.put(findMatchingWildcardImport(wildcardImports, type), type);
    }
    return toFix;
  }

  /** Find an on-demand import matching the given single-type import specification. */
  @Nullable
  private ImportTree findMatchingWildcardImport(
      ImmutableList<ImportTree> wildcardImports, TypeToImport type) {
    for (ImportTree importTree : wildcardImports) {
      // Get the name of the on-demand import's scope, e.g. 'java.util.*' -> 'java.util'. It's
      // guaranteed to be a MemberSelectTree by getWildcardImports().
      String importBase =
          ((MemberSelectTree) importTree.getQualifiedIdentifier()).getExpression().toString();
      if (type.owner().getQualifiedName().contentEquals(importBase)) {
        return importTree;
      }
    }
    throw new AssertionError("could not find import for: " + type);
  }

  /** Collect all on demand imports. */
  private static ImmutableList<ImportTree> getWildcardImports(List<? extends ImportTree> imports) {
    ImmutableList.Builder<ImportTree> result = ImmutableList.builder();
    for (ImportTree tree : imports) {
      // javac represents on-demand imports as a member select where the selected name is '*'.
      Tree ident = tree.getQualifiedIdentifier();
      if (!(ident instanceof MemberSelectTree)) {
        continue;
      }
      MemberSelectTree select = (MemberSelectTree) ident;
      if (select.getIdentifier().contentEquals("*")) {
        result.add(tree);
      }
    }
    return result.build();
  }

  /** Collects all uses of on demand-imported types and static members in a compilation unit. */
  static class ImportCollector extends TreeScanner {

    private final StarImportScope wildcardScope;
    private final Set<TypeToImport> seen = new LinkedHashSet<>();

    ImportCollector(StarImportScope wildcardScope) {
      this.wildcardScope = wildcardScope;
    }

    public static Set<TypeToImport> collect(JCCompilationUnit tree) {
      ImportCollector collector = new ImportCollector(tree.starImportScope);
      collector.scan(tree);
      return collector.seen;
    }

    @Override
    public void visitImport(JCTree.JCImport tree) {
      // skip imports
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl method) {
      if (ASTHelpers.isGeneratedConstructor(method)) {
        // Skip types in the signatures of synthetic constructors
        scan(method.body);
      } else {
        super.visitMethodDef(method);
      }
    }

    @Override
    public void visitIdent(JCIdent tree) {
      Symbol sym = tree.sym;
      if (sym == null) {
        return;
      }
      sym = sym.baseSymbol();
      if (wildcardScope.includes(sym)) {
        if (sym.owner.getQualifiedName().contentEquals("java.lang")) {
          return;
        }
        switch (sym.kind) {
          case TYP:
            seen.add(TypeToImport.create(sym.getSimpleName().toString(), sym.owner, false));
            break;
          case VAR:
          case MTH:
            seen.add(TypeToImport.create(sym.getSimpleName().toString(), sym.owner, true));
            break;
          default:
            return;
        }
      }
    }
  }

  /** Creates a {@link Fix} that replaces wildcard imports. */
  static Fix createFix(
      ImmutableList<ImportTree> wildcardImports,
      Multimap<ImportTree, TypeToImport> toFix,
      VisitorState state) {
    final SuggestedFix.Builder fix = SuggestedFix.builder();
    for (ImportTree importToDelete : wildcardImports) {
      String importSpecification = importToDelete.getQualifiedIdentifier().toString();
      if (importToDelete.isStatic()) {
        fix.removeStaticImport(importSpecification);
      } else {
        fix.removeImport(importSpecification);
      }
    }
    Multimap<Symbol, TypeToImport> importsByOwner = LinkedHashMultimap.create();
    for (TypeToImport toImport : toFix.values()) {
      importsByOwner.put(toImport.owner(), toImport);
    }
    for (Map.Entry<Symbol, Collection<TypeToImport>> entries : importsByOwner.asMap().entrySet()) {
      final Symbol owner = entries.getKey();
      if (entries.getValue().size() > MAX_MEMBER_IMPORTS) {
        qualifiedNameFix(fix, owner, state);
      } else {
        for (TypeToImport toImport : toFix.values()) {
          toImport.addFix(fix);
        }
      }
    }
    return fix.build();
  }

  /**
   * Add an import for {@code owner}, and qualify all on demand imported references to members of
   * owner by owner's simple name.
   */
  private static void qualifiedNameFix(
      final SuggestedFix.Builder fix, final Symbol owner, VisitorState state) {
    fix.addImport(owner.getQualifiedName().toString());
    final JCCompilationUnit unit = (JCCompilationUnit) state.getPath().getCompilationUnit();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        Symbol sym = ASTHelpers.getSymbol(tree);
        if (sym == null) {
          return null;
        }
        Tree parent = getCurrentPath().getParentPath().getLeaf();
        if (parent.getKind() == Tree.Kind.CASE
            && ((CaseTree) parent).getExpression().equals(tree)
            && sym.owner.getKind() == ElementKind.ENUM) {
          // switch cases can refer to enum constants by simple name without importing them
          return null;
        }
        if (sym.owner.equals(owner) && unit.starImportScope.includes(sym)) {
          fix.prefixWith(tree, owner.getSimpleName() + ".");
        }
        return null;
      }
    }.scan(unit, null);
  }
}
