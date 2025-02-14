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

import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static java.lang.String.format;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = SeverityLevel.WARNING,
    summary = "This TestRule isn't annotated with @Rule, so won't be run.")
public final class RuleNotRun extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    HashMap<VarSymbol, Tree> rules = new HashMap<>(findRules(state));
    if (rules.isEmpty()) {
      return NO_MATCH;
    }
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelect, Void unused) {
        handle(memberSelect);
        return super.visitMemberSelect(memberSelect, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifier, Void unused) {
        handle(identifier);
        return super.visitIdentifier(identifier, null);
      }

      private void handle(Tree tree) {
        if (getSymbol(tree) instanceof VarSymbol varSymbol) {
          // If the reference leaks anywhere, it might be being run, e.g. via RuleChain.
          // _Most_ uses of rules just call methods on the rule, so this heuristic hopefully won't
          // miss too many true positives.
          if (!(getCurrentPath().getParentPath().getLeaf() instanceof MemberSelectTree)) {
            rules.remove(varSymbol);
          }
        }
      }
    }.scan(state.getPath(), null);
    for (Tree ruleTree : rules.values()) {
      var fix = SuggestedFix.builder();
      String rule = SuggestedFixes.qualifyType(state, fix, "org.junit.Rule");
      state.reportMatch(
          describeMatch(ruleTree, fix.prefixWith(ruleTree, format("@%s ", rule)).build()));
    }
    return NO_MATCH;
  }

  private ImmutableMap<VarSymbol, Tree> findRules(VisitorState state) {
    ImmutableMap.Builder<VarSymbol, Tree> rules = ImmutableMap.builder();
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitClass(ClassTree tree, Void unused) {
        if (!JUnitMatchers.isJUnit4TestClass.matches(tree, state)) {
          return null;
        }
        for (Tree m : tree.getMembers()) {
          if (m instanceof VariableTree vt) {
            scan(vt, null);
          }
        }
        return null;
      }

      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        VarSymbol symbol = getSymbol(tree);
        if (isSubtype(symbol.type, TEST_RULE.get(state), state)
            && STOP_ANNOTATIONS.stream()
                .noneMatch(anno -> hasDirectAnnotationWithSimpleName(symbol, anno))
            // Heuristic: rules should be public. If it's not, and is unused, we should pick it up
            // via unused analysis anyway.
            && !symbol.isPrivate()
            && !ignoreBasedOnInitialiser(tree.getInitializer(), state)) {
          rules.put(symbol, tree);
        }
        return null;
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return rules.buildOrThrow();
  }

  private static final ImmutableSet<String> STOP_ANNOTATIONS =
      ImmutableSet.of(
          // keep-sorted start
          "ClassRule", //
          "Inject",
          "Rule",
          "TightRule"
          // keep-sorted end
          );

  private static boolean ignoreBasedOnInitialiser(Tree tree, VisitorState state) {
    if (tree == null) {
      return false;
    }
    AtomicBoolean matched = new AtomicBoolean();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        return super.visitMethodInvocation(tree, null);
      }
    }.scan(tree, null);
    return matched.get();
  }

  private static final Supplier<Type> TEST_RULE =
      memoize(state -> state.getTypeFromString("org.junit.rules.TestRule"));
}
