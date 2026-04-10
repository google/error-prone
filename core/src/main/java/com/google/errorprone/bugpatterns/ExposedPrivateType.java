/*
 * Copyright 2026 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.InjectMatchers.HAS_INJECT_ANNOTATION;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes.Visibility;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ElementKind;

/** Detects private member classes that are referenced in signatures of non-private members. */
@BugPattern(
    summary =
        "Private member classes should not be referenced in signatures of non-private members.",
    severity = WARNING)
public final class ExposedPrivateType extends BugChecker
    implements MethodTreeMatcher, VariableTreeMatcher, ClassTreeMatcher {

  private static boolean isEffectivelyPrivate(Tree tree) {
    Symbol symbol = getSymbol(tree);
    return symbol == null || ASTHelpers.isEffectivelyPrivate(symbol);
  }

  private static boolean shouldCheck(Tree tree, VisitorState state) {
    if (isEffectivelyPrivate(tree)) {
      return false;
    }
    if (HAS_INJECT_ANNOTATION.matches(tree, state)) {
      return false;
    }
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return false;
    }
    return true;
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!shouldCheck(tree, state)) {
      return NO_MATCH;
    }
    SignatureScanner scanner = new SignatureScanner();
    scanner.scan(tree.getReturnType(), null);
    scanner.scan(tree.getParameters(), null);
    scanner.scan(tree.getThrows(), null);
    scanner.scan(tree.getTypeParameters(), null);
    return scanner.report(tree, state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (!shouldCheck(tree, state)) {
      return NO_MATCH;
    }
    VarSymbol sym = getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    if (sym.getKind() != ElementKind.FIELD) {
      return NO_MATCH;
    }
    SignatureScanner scanner = new SignatureScanner();
    scanner.scan(tree.getType(), null);
    return scanner.report(tree, state);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!shouldCheck(tree, state)) {
      return NO_MATCH;
    }
    SignatureScanner scanner = new SignatureScanner();
    scanner.scan(tree.getExtendsClause(), null);
    scanner.scan(tree.getImplementsClause(), null);
    scanner.scan(tree.getTypeParameters(), null);
    return scanner.report(tree, state);
  }

  private final class SignatureScanner extends TreeScanner<Void, Void> {
    private final List<Tree> usages = new ArrayList<>();

    SignatureScanner() {}

    @Override
    public Void visitModifiers(ModifiersTree node, Void unused) {
      // Ignore annotations, including parameter annotations
      return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void unused) {
      checkType(node);
      return super.visitIdentifier(node, null);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void unused) {
      checkType(node);
      return super.visitMemberSelect(node, null);
    }

    private void checkType(Tree node) {
      Symbol symbol = getSymbol(node);
      if (symbol != null && ASTHelpers.isEffectivelyPrivate(symbol)) {
        usages.add(node);
      }
    }

    private Description report(Tree tree, VisitorState state) {
      if (usages.isEmpty()) {
        return NO_MATCH;
      }
      Description.Builder description = buildDescription(usages.getFirst());

      if (shouldReduceVisibility(tree, state)) {
        description.addFix(Visibility.PRIVATE.refactor(tree, state));
      }

      SuggestedFix.Builder increaseVisibility = SuggestedFix.builder();

      Visibility visibility = Visibility.from(tree);
      Trees trees = JavacTrees.instance(state.context);
      ImmutableSet<Symbol> found =
          usages.stream()
              .map(usage -> getSymbol(usage))
              .filter(symbol -> symbol != null)
              .collect(toImmutableSet());
      found.stream()
          .map(trees::getTree)
          .filter(declaration -> declaration != null)
          .forEach(
              declaration -> increaseVisibility.merge(visibility.refactor(declaration, state)));
      description.addFix(increaseVisibility.build());
      description.setMessage(
          String.format(
              "Signatures of non-private members should not reference private classes: %s",
              found.stream().map(s -> s.getSimpleName()).collect(joining(", "))));
      return description.build();
    }
  }

  private static boolean shouldReduceVisibility(Tree tree, VisitorState state) {
    if (!(tree instanceof MethodTree methodTree)) {
      return true;
    }
    MethodSymbol methodSymbol = getSymbol(methodTree);
    if (ASTHelpers.streamSuperMethods(methodSymbol, state.getTypes()).findAny().isPresent()) {
      return false;
    }
    if (JUnitMatchers.TEST_CASE.matches(methodTree, state)) {
      return false;
    }
    return true;
  }
}
