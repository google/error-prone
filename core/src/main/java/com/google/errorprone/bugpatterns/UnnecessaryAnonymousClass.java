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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.canBeRemoved;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;
import static com.google.errorprone.util.ASTHelpers.isStatic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.ErrorProneEndPosTable;
import com.google.errorprone.fixes.Replacement;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Implementing a functional interface is unnecessary; prefer to implement the functional"
            + " interface method directly and use a method reference instead.",
    severity = WARNING)
public class UnnecessaryAnonymousClass extends BugChecker implements VariableTreeMatcher {

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (tree.getInitializer() == null) {
      return NO_MATCH;
    }
    if (!(tree.getInitializer() instanceof NewClassTree classTree)) {
      return NO_MATCH;
    }
    if (classTree.getClassBody() == null) {
      return NO_MATCH;
    }
    ImmutableList<? extends Tree> members =
        classTree.getClassBody().getMembers().stream()
            .filter(
                x -> !(x instanceof MethodTree methodTree && isGeneratedConstructor(methodTree)))
            .collect(toImmutableList());
    if (members.size() != 1) {
      return NO_MATCH;
    }
    Tree member = getOnlyElement(members);
    if (!(member instanceof MethodTree implementation)) {
      return NO_MATCH;
    }
    VarSymbol varSym = getSymbol(tree);
    if (varSym.getKind() != ElementKind.FIELD
        || !canBeRemoved(varSym)
        || !varSym.getModifiers().contains(Modifier.FINAL)) {
      return NO_MATCH;
    }
    Type type = getType(tree.getType());
    if (type == null || !state.getTypes().isFunctionalInterface(type)) {
      return NO_MATCH;
    }
    MethodSymbol methodSymbol = getSymbol(implementation);
    Symbol descriptorSymbol = state.getTypes().findDescriptorSymbol(type.tsym);
    if (!methodSymbol.getSimpleName().contentEquals(descriptorSymbol.getSimpleName())) {
      return NO_MATCH;
    }
    if (!methodSymbol.overrides(
        descriptorSymbol, enclosingClass(methodSymbol), state.getTypes(), false)) {
      return NO_MATCH;
    }
    if (tree.getModifiers().getAnnotations().stream()
        .anyMatch(at -> getSymbol(at).getQualifiedName().contentEquals("org.mockito.Spy"))) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();

    // Derive new method name from identifier.
    String newName =
        varSym.isStatic()
            ? UPPER_UNDERSCORE.converterTo(LOWER_CAMEL).convert(tree.getName().toString())
            : tree.getName().toString();
    fixBuilder.merge(SuggestedFixes.renameMethod(implementation, newName, state));

    // Make non-final.
    SuggestedFixes.removeModifiers(tree, state, Modifier.FINAL).ifPresent(fixBuilder::merge);

    // Convert the anonymous class and variable assignment to a method definition.
    fixBuilder.merge(trimToMethodDef(tree, state, implementation));

    // Replace all uses of the identifier with a method reference.
    return replaceUsesWithMethodReference(newName, varSym, implementation, state)
        .map(mrr -> describeMatch(tree, fixBuilder.merge(mrr).build()))
        .orElse(NO_MATCH);
  }

  /** Remove anonymous class definition beginning and end, leaving only the method definition. */
  private static SuggestedFix trimToMethodDef(
      VariableTree varDefinitionTree, VisitorState state, MethodTree implementation) {
    int methodModifiersEndPos = state.getEndPosition(implementation.getModifiers());
    if (methodModifiersEndPos == -1) {
      methodModifiersEndPos = getStartPosition(implementation);
    }
    int methodDefEndPos = state.getEndPosition(implementation);
    int varModifiersEndPos = state.getEndPosition(varDefinitionTree.getModifiers()) + 1;
    int varDefEndPos = state.getEndPosition(varDefinitionTree);
    return SuggestedFix.builder()
        .replace(varModifiersEndPos, methodModifiersEndPos, "")
        .replace(methodDefEndPos, varDefEndPos, "")
        .build();
  }

  /**
   * Replace all uses of {@code varSym} within the enclosing compilation unit with a method
   * reference, as specified by {@code newName}.
   */
  private static Optional<SuggestedFix> replaceUsesWithMethodReference(
      String newName, Symbol varSym, MethodTree implementation, VisitorState state) {
    // Extract method body.
    BlockTree methodBody = implementation.getBody();

    // Scan entire compilation unit to replace all uses of the variable with method references.
    CompilationUnitTree compilationUnit = state.getPath().getCompilationUnit();
    ReplaceUsesScanner replaceUsesScanner = new ReplaceUsesScanner(varSym, newName, state);
    replaceUsesScanner.scan(compilationUnit, null);

    return replaceUsesScanner
        .getFixes()
        .map(fix -> ensureFixesDoNotOverlap(methodBody, compilationUnit, fix, state));
  }

  private static SuggestedFix ensureFixesDoNotOverlap(
      Tree methodBody, CompilationUnitTree compilationUnit, SuggestedFix fix, VisitorState state) {
    StringBuilder methodBodySource =
        new StringBuilder(Objects.requireNonNull(state.getSourceForNode(methodBody)));
    Range<Integer> methodBodyPositionRange =
        Range.closedOpen(getStartPosition(methodBody), state.getEndPosition(methodBody));
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    // Apply each fix generated by ReplaceUsesScanner to fixBuilder.
    for (Replacement replacement :
        fix.getReplacements(ErrorProneEndPosTable.create(compilationUnit))) {
      if (replacement.range().isConnected(methodBodyPositionRange)) {
        // If the usage replacement overlaps with the method body, apply it to the method body
        // source directly, to avoid fix collisions when we replace the whole thing.
        methodBodySource.replace(
            replacement.startPosition() - methodBodyPositionRange.lowerEndpoint(),
            replacement.endPosition() - methodBodyPositionRange.lowerEndpoint(),
            replacement.replaceWith());
      } else {
        // Otherwise, just add directly to fixBuilder.
        fixBuilder.replace(
            replacement.startPosition(), replacement.endPosition(), replacement.replaceWith());
      }
    }
    return fixBuilder.replace(methodBody, methodBodySource.toString()).build();
  }

  private static class ReplaceUsesScanner extends TreePathScanner<Void, Void> {

    private final Symbol sym;
    private final String newName;
    private final VisitorState state;
    private final SuggestedFix.Builder fix = SuggestedFix.builder();
    private boolean failed = false;

    ReplaceUsesScanner(Symbol sym, String newName, VisitorState state) {
      this.sym = sym;
      this.newName = newName;
      this.state = state;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void unused) {
      if (Objects.equals(getSymbol(node), sym)) {
        fix.merge(replaceUseWithMethodReference(node, state.withPath(getCurrentPath())));
      }
      return super.visitMemberSelect(node, null);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void unused) {
      if (Objects.equals(getSymbol(node), sym)) {
        fix.merge(replaceUseWithMethodReference(node, state.withPath(getCurrentPath())));
      }
      return super.visitIdentifier(node, null);
    }

    /**
     * Replace the given {@code node} with the method reference specified by {@code this.newName}.
     */
    private @Nullable SuggestedFix replaceUseWithMethodReference(
        ExpressionTree node, VisitorState state) {
      Tree parent = state.getPath().getParentPath().getLeaf();
      if (parent instanceof MemberSelectTree memberSelectTree
          && memberSelectTree.getExpression().equals(node)) {
        Symbol symbol = getSymbol(parent);
        // If anything other than the abstract method is used on this anonymous class, we can't hope
        // to generate a fix.
        if (symbol.getKind() != ElementKind.METHOD
            || !symbol.getModifiers().contains(Modifier.ABSTRACT)) {
          failed = true;
          return null;
        }
        Tree receiver = node instanceof IdentifierTree ? null : getReceiver(node);
        return SuggestedFix.replace(
            receiver != null ? state.getEndPosition(receiver) : getStartPosition(node),
            state.getEndPosition(parent),
            newName);
      } else {
        Symbol sym = getSymbol(node);
        return SuggestedFix.replace(
            node,
            String.format(
                "%s::%s", isStatic(sym) ? enclosingClass(sym).getSimpleName() : "this", newName));
      }
    }

    Optional<SuggestedFix> getFixes() {
      return failed ? Optional.empty() : Optional.of(fix.build());
    }
  }
}
