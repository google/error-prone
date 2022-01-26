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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.fixes.SuggestedFixes.addModifiers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.SERIALIZATION_METHODS;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.CanBeStaticAnalyzer.CanBeStaticResult;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    altNames = "static-method",
    summary = "A private method that does not reference the enclosing instance can be static",
    severity = SUGGESTION)
public class MethodCanBeStatic extends BugChecker implements CompilationUnitTreeMatcher {
  private final FindingOutputStyle findingOutputStyle;

  public MethodCanBeStatic(ErrorProneFlags flags) {
    boolean findingPerSite = flags.getBoolean("MethodCanBeStatic:FindingPerSite").orElse(false);
    this.findingOutputStyle =
        findingPerSite ? FindingOutputStyle.FINDING_PER_SITE : FindingOutputStyle.ONE_FINDING;
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    Map<MethodSymbol, MethodDetails> nodes = new HashMap<>();
    new TreePathScanner<Void, Void>() {
      private int suppressions = 0;

      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        if (isSuppressed(classTree)) {
          suppressions++;
          super.visitClass(classTree, null);
          suppressions--;
        } else {
          super.visitClass(classTree, null);
        }
        return null;
      }

      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        if (isSuppressed(tree)) {
          suppressions++;
          matchMethod(tree);
          super.visitMethod(tree, null);
          suppressions--;
        } else {
          matchMethod(tree);
          super.visitMethod(tree, null);
        }
        return null;
      }

      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        if (isSuppressed(variableTree)) {
          suppressions++;
          super.visitVariable(variableTree, null);
          suppressions--;
        } else {
          super.visitVariable(variableTree, null);
        }
        return null;
      }

      private void matchMethod(MethodTree tree) {
        MethodSymbol sym = ASTHelpers.getSymbol(tree);
        if (sym.isStatic()) {
          nodes.put(sym, new MethodDetails(tree, true, ImmutableSet.of()));
        } else {
          CanBeStaticResult result = CanBeStaticAnalyzer.canBeStaticResult(tree, sym, state);
          boolean isExcluded = isExcluded(tree, state);
          nodes.put(
              sym,
              new MethodDetails(
                  tree,
                  result.canPossiblyBeStatic() && !isExcluded && suppressions == 0,
                  result.methodsReferenced()));
        }
      }
    }.scan(state.getPath(), null);

    propagateNonStaticness(nodes);
    nodes
        .entrySet()
        .removeIf(
            entry -> entry.getValue().tree.getModifiers().getFlags().contains(Modifier.STATIC));
    return generateDescription(nodes, state);
  }

  private static void propagateNonStaticness(Map<MethodSymbol, MethodDetails> nodes) {
    for (Map.Entry<MethodSymbol, MethodDetails> entry : nodes.entrySet()) {
      MethodSymbol sym = entry.getKey();
      MethodDetails methodDetails = entry.getValue();
      for (MethodSymbol use : methodDetails.methodsReferenced) {
        if (nodes.containsKey(use)) {
          nodes.get(use).referencedBy.add(sym);
        }
      }

      if (referencesExternalMethods(methodDetails, nodes.keySet())) {
        methodDetails.couldPossiblyBeStatic = false;
      }
    }

    Set<MethodSymbol> toVisit = new HashSet<>(nodes.keySet());
    while (!toVisit.isEmpty()) {
      Set<MethodSymbol> nextVisit = new HashSet<>();
      for (MethodSymbol sym : toVisit) {
        MethodDetails methodDetails = nodes.get(sym);
        if (methodDetails.couldPossiblyBeStatic) {
          continue;
        }
        for (MethodSymbol user : methodDetails.referencedBy) {
          if (!nodes.get(user).couldPossiblyBeStatic) {
            continue;
          }
          nodes.get(user).couldPossiblyBeStatic = false;
          nodes.get(user).methodsReferenced.remove(sym);
          nextVisit.add(user);
        }
      }
      toVisit = nextVisit;
    }
  }

  private Description generateDescription(
      Map<MethodSymbol, MethodDetails> nodes, VisitorState state) {
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    fixBuilder.setShortDescription("Make static");
    Set<MethodTree> affectedTrees = new HashSet<>();
    for (Map.Entry<MethodSymbol, MethodDetails> entry : nodes.entrySet()) {
      MethodSymbol sym = entry.getKey();
      MethodDetails methodDetails = entry.getValue();
      boolean noExternalMethods = !referencesExternalMethods(methodDetails, nodes.keySet());
      if (methodDetails.couldPossiblyBeStatic && noExternalMethods) {
        addModifiers(methodDetails.tree, state, Modifier.STATIC)
            .map(f -> fixQualifiers(state, sym, f))
            .ifPresent(fixBuilder::merge);
        affectedTrees.add(methodDetails.tree);
      }
    }
    return findingOutputStyle.report(affectedTrees, fixBuilder.build(), state, this);
  }

  private static boolean referencesExternalMethods(
      MethodDetails methodDetails, Set<MethodSymbol> localMethods) {
    return !Sets.difference(methodDetails.methodsReferenced, localMethods).isEmpty();
  }

  /**
   * Replace instance references to the method with static access (e.g. `this.foo(...)` ->
   * `EnclosingClass.foo(...)` and `this::foo` to `EnclosingClass::foo`).
   */
  private SuggestedFix fixQualifiers(VisitorState state, MethodSymbol sym, SuggestedFix f) {
    SuggestedFix.Builder builder = SuggestedFix.builder().merge(f);
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        fixQualifier(tree, tree.getExpression());
        return super.visitMemberSelect(tree, unused);
      }

      @Override
      public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
        fixQualifier(tree, tree.getQualifierExpression());
        return super.visitMemberReference(tree, unused);
      }

      private void fixQualifier(Tree tree, ExpressionTree qualifierExpression) {
        if (sym.equals(ASTHelpers.getSymbol(tree))) {
          builder.replace(qualifierExpression, sym.owner.enclClass().getSimpleName().toString());
        }
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return builder.build();
  }

  private static boolean isExcluded(MethodTree tree, VisitorState state) {
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return true;
    }
    if (sym.isConstructor()
        || sym.getModifiers().contains(Modifier.NATIVE)
        || sym.getModifiers().contains(Modifier.SYNCHRONIZED)) {
      return true;
    }
    if (!sym.isPrivate()) {
      // Methods that override other methods, or that are overridden, can't be static.
      // We conservatively warn only for private methods.
      return true;
    }
    switch (sym.owner.enclClass().getNestingKind()) {
      case TOP_LEVEL:
        break;
      case MEMBER:
        if (sym.owner.enclClass().hasOuterInstance()) {
          return true;
        }
        break;
      case LOCAL:
      case ANONYMOUS:
        return true;
    }
    return SERIALIZATION_METHODS.matches(tree, state);
  }

  /** Information about a {@link MethodSymbol} and whether it can be made static. */
  private static final class MethodDetails {
    private final MethodTree tree;
    private boolean couldPossiblyBeStatic;
    private final Set<MethodSymbol> methodsReferenced;
    private final Set<MethodSymbol> referencedBy = new HashSet<>();

    private MethodDetails(
        MethodTree tree, boolean couldPossiblyBeStatic, Set<MethodSymbol> methodsReferenced) {
      this.tree = tree;
      this.couldPossiblyBeStatic = couldPossiblyBeStatic;
      this.methodsReferenced = new HashSet<>(methodsReferenced);
    }
  }

  /**
   * Encapsulates how we should report findings. We support reporting a finding on either every
   * affected (can be static) method, or just the first one in the file.
   */
  private enum FindingOutputStyle {
    ONE_FINDING {
      @Override
      public Description report(
          Set<MethodTree> affectedTrees, SuggestedFix fix, VisitorState state, BugChecker checker) {
        return affectedTrees.stream()
            .min(Comparator.comparingInt(t -> getStartPosition(t)))
            .map(t -> checker.describeMatch(t.getModifiers(), fix))
            .orElse(NO_MATCH);
      }
    },
    FINDING_PER_SITE {
      @Override
      public Description report(
          Set<MethodTree> affectedTrees, SuggestedFix fix, VisitorState state, BugChecker checker) {
        for (MethodTree tree : affectedTrees) {
          state.reportMatch(checker.describeMatch(tree.getModifiers(), fix));
        }
        return NO_MATCH;
      }
    };

    public abstract Description report(
        Set<MethodTree> affectedTrees, SuggestedFix fix, VisitorState state, BugChecker checker);
  }
}
