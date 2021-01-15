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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.FindIdentifiers.findIdent;

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MultiMatcher;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.Position;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Discourages using multiple names to refer to the same type within a file (e.g. both {@code
 * OuterClass.InnerClass} and {@code InnerClass}).
 */
@BugPattern(
    name = "DifferentNameButSame",
    severity = SeverityLevel.WARNING,
    summary =
        "This type is referred to in different ways within this file, which may be confusing.",
    tags = StandardTags.STYLE)
public final class DifferentNameButSame extends BugChecker implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    Table<Symbol, String, List<TreePath>> names = HashBasedTable.create();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitImport(ImportTree importTree, Void unused) {
        return null;
      }

      @Override
      public Void visitCase(CaseTree caseTree, Void unused) {
        return null;
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
        if (getCurrentPath().getParentPath().getLeaf() instanceof MemberSelectTree) {
          MemberSelectTree tree = (MemberSelectTree) getCurrentPath().getParentPath().getLeaf();
          Symbol superSymbol = getSymbol(tree);
          if (superSymbol instanceof ClassSymbol) {
            return super.visitMemberSelect(memberSelectTree, null);
          }
        }
        handle(memberSelectTree);
        return super.visitMemberSelect(memberSelectTree, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
        Tree parent = getCurrentPath().getParentPath().getLeaf();
        if (parent instanceof NewClassTree) {
          NewClassTree newClassTree = (NewClassTree) parent;
          if (newClassTree.getIdentifier().equals(identifierTree)
              && newClassTree.getEnclosingExpression() != null) {
            // don't try to fix instantiations with explicit enclosing instances, e.g. `a.new B();`
            return null;
          }
        }
        handle(identifierTree);
        return super.visitIdentifier(identifierTree, null);
      }

      private void handle(Tree tree) {
        if (state.getEndPosition(tree) == Position.NOPOS) {
          return;
        }
        Symbol symbol = getSymbol(tree);
        if (!(symbol instanceof ClassSymbol)) {
          return;
        }
        String name = tree.toString();
        List<TreePath> treePaths = names.get(symbol, name);
        if (treePaths == null) {
          treePaths = new ArrayList<>();
          names.put(symbol, name, treePaths);
        }
        treePaths.add(getCurrentPath());
      }
    }.scan(tree, null);

    for (Map.Entry<Symbol, Map<String, List<TreePath>>> entry : names.rowMap().entrySet()) {
      Symbol symbol = entry.getKey();
      // Skip generic symbols; we need to do a lot more work to check the type parameters match at
      // each level.
      if (isGeneric(symbol)) {
        continue;
      }
      if (isDefinedInThisFile(symbol, tree)) {
        continue;
      }
      Map<String, List<TreePath>> references = entry.getValue();
      if (references.size() == 1) {
        continue;
      }
      // Skip if any look to be fully qualified: this will be mentioned by a different check.
      if (references.keySet().stream().anyMatch(n -> Ascii.isLowerCase(n.charAt(0)))) {
        continue;
      }
      ImmutableList<String> namesByPreference =
          references.entrySet().stream()
              .sorted(REPLACEMENT_PREFERENCE)
              .map(Map.Entry::getKey)
              .collect(toImmutableList());
      for (String name : namesByPreference) {
        ImmutableList<TreePath> sites =
            references.entrySet().stream()
                .filter(e -> !e.getKey().equals(name))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .collect(toImmutableList());
        if (!(symbol instanceof MethodSymbol) && !visibleAndReferToSameThing(name, sites, state)) {
          continue;
        }
        if (BadImport.BAD_NESTED_CLASSES.contains(name)) {
          continue;
        }
        List<String> components = DOT_SPLITTER.splitToList(name);
        SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
        for (TreePath site : sites) {
          fixBuilder.merge(createFix(site, components, state));
        }
        SuggestedFix fix = fixBuilder.build();
        for (TreePath path : sites) {
          state.reportMatch(describeMatch(path.getLeaf(), fix));
        }
        break;
      }
    }
    return NO_MATCH;
  }

  private boolean isDefinedInThisFile(Symbol symbol, CompilationUnitTree tree) {
    return tree.getTypeDecls().stream()
        .anyMatch(
            t -> {
              Symbol topLevelClass = getSymbol(t);
              return topLevelClass instanceof ClassSymbol
                  && symbol.isEnclosedBy((ClassSymbol) topLevelClass);
            });
  }

  private static boolean isGeneric(Symbol symbol) {
    for (Symbol s = symbol; s != null; s = s.owner) {
      if (!s.getTypeParameters().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static SuggestedFix createFix(
      TreePath path, List<String> components, VisitorState state) {
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    StringBuilder stringBuilder = new StringBuilder();
    components.stream()
        .limit(components.size() - 1)
        .forEachOrdered(c -> stringBuilder.append(c).append("."));
    Tree site = path.getLeaf();
    Tree parent = path.getParentPath().getLeaf();
    if (canHaveTypeUseAnnotations(parent)) {
      for (AnnotationTree annotation :
          HAS_TYPE_USE_ANNOTATION.multiMatchResult(parent, state).matchingNodes()) {
        if (state.getEndPosition(annotation) < getStartPosition(site)) {
          fixBuilder.delete(annotation);
        }
        stringBuilder.append(state.getSourceForNode(annotation)).append(" ");
      }
    }
    stringBuilder.append(getLast(components));
    return fixBuilder.replace(site, stringBuilder.toString()).build();
  }

  private static boolean canHaveTypeUseAnnotations(Tree tree) {
    return tree instanceof AnnotatedTypeTree
        || tree instanceof MethodTree
        || tree instanceof VariableTree;
  }

  /**
   * Checks whether the symbol with {@code name} is visible from the position encoded in {@code
   * state}.
   *
   * <p>This is not fool-proof by any means: it doesn't check that the symbol actually has the same
   * meaning.
   */
  private static boolean visibleAndReferToSameThing(
      String name, ImmutableList<TreePath> locations, VisitorState state) {
    String firstComponent = name.contains(".") ? name.substring(0, name.indexOf(".")) : name;
    Set<Symbol> idents = new HashSet<>();
    for (TreePath path : locations) {
      VisitorState stateWithPath = state.withPath(path);
      if (findIdent(firstComponent, stateWithPath, KindSelector.VAR) != null) {
        return false;
      }
      Symbol symbol = findIdent(firstComponent, stateWithPath, KindSelector.VAL_TYP_PCK);
      if (symbol == null) {
        return false;
      }
      idents.add(symbol);
    }
    return idents.size() == 1;
  }

  private static final Comparator<Map.Entry<String, List<TreePath>>> REPLACEMENT_PREFERENCE =
      Comparator.<Map.Entry<String, List<TreePath>>>comparingInt(e -> e.getKey().length())
          .thenComparing(Map.Entry::getKey);

  private static final Splitter DOT_SPLITTER = Splitter.on('.');

  private static final MultiMatcher<Tree, AnnotationTree> HAS_TYPE_USE_ANNOTATION =
      annotations(AT_LEAST_ONE, (t, state) -> isTypeAnnotation(t));

  private static boolean isTypeAnnotation(AnnotationTree t) {
    Symbol annotationSymbol = getSymbol(t.getAnnotationType());
    if (annotationSymbol == null) {
      return false;
    }
    Target target = annotationSymbol.getAnnotation(Target.class);
    if (target == null) {
      return false;
    }
    List<ElementType> value = Arrays.asList(target.value());
    return value.contains(ElementType.TYPE_USE) || value.contains(ElementType.TYPE_PARAMETER);
  }
}
