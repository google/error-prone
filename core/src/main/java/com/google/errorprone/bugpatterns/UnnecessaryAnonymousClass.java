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
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
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
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.ArrayList;
import java.util.Objects;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/**
 * A {@link BugChecker}; see {@code @BugPattern} for details.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
    name = "UnnecessaryAnonymousClass",
    summary =
        "Implementing a functional interface is unnecessary; prefer to implement the functional"
            + " interface method directly and use a method reference instead.",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public class UnnecessaryAnonymousClass extends BugChecker implements VariableTreeMatcher {

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (tree.getInitializer() == null) {
      return NO_MATCH;
    }
    if (!(tree.getInitializer() instanceof NewClassTree)) {
      return NO_MATCH;
    }
    NewClassTree classTree = (NewClassTree) tree.getInitializer();
    if (classTree.getClassBody() == null) {
      return NO_MATCH;
    }
    ImmutableList<? extends Tree> members =
        classTree.getClassBody().getMembers().stream()
            .filter(x -> !(x instanceof MethodTree && isGeneratedConstructor((MethodTree) x)))
            .collect(toImmutableList());
    if (members.size() != 1) {
      return NO_MATCH;
    }
    Tree member = getOnlyElement(members);
    if (!(member instanceof MethodTree)) {
      return NO_MATCH;
    }
    Symbol varSym = getSymbol(tree);
    if (varSym == null
        || varSym.getKind() != ElementKind.FIELD
        || !varSym.isPrivate()
        || !varSym.getModifiers().contains(Modifier.FINAL)) {
      return NO_MATCH;
    }
    MethodTree implementation = (MethodTree) member;
    Type type = getType(tree.getType());
    if (type == null || !state.getTypes().isFunctionalInterface(type)) {
      return NO_MATCH;
    }
    MethodSymbol methodSymbol = getSymbol(implementation);
    if (methodSymbol == null) {
      return NO_MATCH;
    }
    Symbol descriptorSymbol = state.getTypes().findDescriptorSymbol(type.tsym);
    if (!methodSymbol.getSimpleName().contentEquals(descriptorSymbol.getSimpleName())) {
      return NO_MATCH;
    }
    if (!methodSymbol.overrides(
        descriptorSymbol, methodSymbol.owner.enclClass(), state.getTypes(), false)) {
      return NO_MATCH;
    }
    if (state.isAndroidCompatible()) {
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
    fixBuilder.merge(replaceUsesWithMethodReference(newName, varSym, implementation, state));

    return describeMatch(tree, fixBuilder.build());
  }

  /** Remove anonymous class definition beginning and end, leaving only the method definition. */
  private static SuggestedFix trimToMethodDef(
      VariableTree varDefinitionTree, VisitorState state, MethodTree implementation) {
    int methodModifiersEndPos = state.getEndPosition(implementation.getModifiers());
    if (methodModifiersEndPos == -1) {
      methodModifiersEndPos = ((JCTree) implementation).getStartPosition();
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
  private static SuggestedFix replaceUsesWithMethodReference(
      String newName, Symbol varSym, MethodTree implementation, VisitorState state) {
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    ArrayList<Fix> fixes = new ArrayList<>();

    // Extract method body.
    JCTree methodBody = (JCTree) implementation.getBody();
    StringBuilder methodBodySource =
        new StringBuilder(Objects.requireNonNull(state.getSourceForNode(methodBody)));
    Range<Integer> methodBodyPositionRange =
        Range.closedOpen(methodBody.getStartPosition(), state.getEndPosition(methodBody));

    // Scan entire compilation unit to replace all uses of the variable with method references.
    JCCompilationUnit compilationUnit = (JCCompilationUnit) state.getPath().getCompilationUnit();
    new ReplaceUsesScanner(varSym, newName, state).scan(compilationUnit, fixes);

    // Apply each fix generated by ReplaceUsesScanner to fixBuilder.
    fixes.stream()
        .flatMap(fix -> fix.getReplacements(compilationUnit.endPositions).stream())
        .forEach(
            replacement -> {
              if (replacement.range().isConnected(methodBodyPositionRange)) {
                // If the usage replacement overlaps with the method body, apply it to the method
                // body source directly, to avoid fix collisions when we replace the whole thing.
                methodBodySource.replace(
                    replacement.startPosition() - methodBodyPositionRange.lowerEndpoint(),
                    replacement.endPosition() - methodBodyPositionRange.lowerEndpoint(),
                    replacement.replaceWith());
              } else {
                // Otherwise, just add directly to fixBuilder.
                fixBuilder.replace(
                    replacement.startPosition(),
                    replacement.endPosition(),
                    replacement.replaceWith());
              }
            });
    return fixBuilder.replace(methodBody, methodBodySource.toString()).build();
  }

  private static class ReplaceUsesScanner extends TreePathScanner<Void, ArrayList<Fix>> {

    private final Symbol sym;
    private final String newName;
    private final VisitorState state;

    ReplaceUsesScanner(Symbol sym, String newName, VisitorState state) {
      this.sym = sym;
      this.newName = newName;
      this.state = state;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, ArrayList<Fix> fixes) {
      if (Objects.equals(getSymbol(node), sym)) {
        replaceUseWithMethodReference(node, fixes, state.withPath(getCurrentPath()));
      }
      return super.visitMemberSelect(node, fixes);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, ArrayList<Fix> fixes) {
      if (Objects.equals(getSymbol(node), sym)) {
        replaceUseWithMethodReference(node, fixes, state.withPath(getCurrentPath()));
      }
      return super.visitIdentifier(node, fixes);
    }

    /**
     * Replace the given {@code node} with the method reference specified by {@code this.newName}.
     * Append the resulting {@link Fix} to {@code fixes}.
     */
    private void replaceUseWithMethodReference(
        ExpressionTree node, ArrayList<Fix> fixes, VisitorState state) {
      Tree parent = state.getPath().getParentPath().getLeaf();
      if (parent instanceof MemberSelectTree
          && ((MemberSelectTree) parent).getExpression().equals(node)) {
        Tree receiver = node.getKind() == Tree.Kind.IDENTIFIER ? null : getReceiver(node);
        fixes.add(
            SuggestedFix.replace(
                receiver != null
                    ? state.getEndPosition(receiver)
                    : ((JCTree) node).getStartPosition(),
                state.getEndPosition(parent),
                newName));
      } else {
        Symbol sym = getSymbol(node);
        fixes.add(
            SuggestedFix.replace(
                node,
                String.format(
                    "%s::%s",
                    sym.isStatic() ? sym.owner.enclClass().getSimpleName() : "this", newName)));
      }
    }
  }
}
