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
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
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
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
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
    Symbol sym = getSymbol(tree);
    if (sym == null
        || sym.getKind() != ElementKind.FIELD
        || !sym.isPrivate()
        || !sym.getModifiers().contains(Modifier.FINAL)) {
      return NO_MATCH;
    }
    MethodTree implementation = (MethodTree) member;
    Type type = getType(tree.getType());
    if (type == null || !state.getTypes().isFunctionalInterface(type)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String name =
        sym.isStatic()
            ? UPPER_UNDERSCORE.converterTo(LOWER_CAMEL).convert(tree.getName().toString())
            : tree.getName().toString();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMemberSelect(MemberSelectTree node, Void unused) {
        if (Objects.equals(getSymbol(node), sym)) {
          replaceUseWithMethodReference(fix, node, name, state.withPath(getCurrentPath()));
        }
        return super.visitMemberSelect(node, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree node, Void unused) {
        if (Objects.equals(getSymbol(node), sym)) {
          replaceUseWithMethodReference(fix, node, name, state.withPath(getCurrentPath()));
        }
        return super.visitIdentifier(node, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    SuggestedFixes.removeModifiers(tree, state, Modifier.FINAL).ifPresent(fix::merge);
    int afterModifiers = state.getEndPosition(implementation.getModifiers());
    if (afterModifiers == -1) {
      afterModifiers = ((JCTree) implementation).getStartPosition();
    }
    fix.replace(state.getEndPosition(tree.getModifiers()) + 1, afterModifiers, "")
        .replace(state.getEndPosition(implementation), state.getEndPosition(tree), "")
        // force reformatting of the method body
        .replace(implementation.getBody(), state.getSourceForNode(implementation.getBody()))
        .merge(SuggestedFixes.renameMethod(implementation, name, state));
    return describeMatch(tree, fix.build());
  }

  private static void replaceUseWithMethodReference(
      SuggestedFix.Builder fix, ExpressionTree node, String newName, VisitorState state) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof MemberSelectTree
        && ((MemberSelectTree) parent).getExpression().equals(node)) {
      Tree receiver = node.getKind() == Tree.Kind.IDENTIFIER ? null : getReceiver(node);
      fix.replace(
          receiver != null ? state.getEndPosition(receiver) : ((JCTree) node).getStartPosition(),
          state.getEndPosition(parent),
          newName);
    } else {
      Symbol sym = getSymbol(node);
      fix.replace(
          node,
          String.format(
              "%s::%s", sym.isStatic() ? sym.owner.enclClass().getSimpleName() : "this", newName));
    }
  }
}
