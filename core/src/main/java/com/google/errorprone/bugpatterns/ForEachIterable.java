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
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.getUpperBound;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "ForEachIterable",
    summary = "This loop can be replaced with an enhanced for loop.",
    severity = SUGGESTION)
public class ForEachIterable extends BugChecker implements VariableTreeMatcher {

  private static final Matcher<ExpressionTree> HAS_NEXT =
      instanceMethod().onDescendantOf("java.util.Iterator").named("hasNext");

  private static final Matcher<ExpressionTree> NEXT =
      instanceMethod().onDescendantOf("java.util.Iterator").named("next");

  private static final Matcher<ExpressionTree> ITERATOR =
      instanceMethod().onDescendantOf("java.lang.Iterable").named("iterator").withParameters();

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (!ASTHelpers.isSameType(getType(tree.getType()), state.getSymtab().iteratorType, state)) {
      return NO_MATCH;
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof BlockTree) {
      return matchWhile(tree, (BlockTree) parent, state);
    }
    if (parent instanceof ForLoopTree) {
      return matchFor(tree, (ForLoopTree) parent, state);
    }
    return NO_MATCH;
  }

  /**
   * Match for loops that can be rewritten to enhanced-for, e.g.:
   *
   * <pre>{@code
   * for (Iterator<T> iterator = list.iterator(); iterator.hasNext(); ) {
   *   doSomething(iterator.next());
   * }
   * }</pre>
   */
  private Description matchFor(VariableTree tree, ForLoopTree forTree, VisitorState state) {
    List<? extends StatementTree> initializer = forTree.getInitializer();
    if (initializer.size() != 1 || !getOnlyElement(initializer).equals(tree)) {
      return NO_MATCH;
    }
    if (!forTree.getUpdate().isEmpty()) {
      return NO_MATCH;
    }
    return match(
        tree,
        state,
        ((JCTree) forTree).getStartPosition(),
        forTree.getCondition(),
        forTree.getStatement());
  }

  private Description matchWhile(VariableTree tree, BlockTree blockTree, VisitorState state) {
    List<? extends StatementTree> statements = blockTree.getStatements();
    int nextIdx = statements.indexOf(tree) + 1;
    if (nextIdx >= statements.size()) {
      return NO_MATCH;
    }
    StatementTree next = statements.get(nextIdx);
    if (!(next instanceof WhileLoopTree)) {
      return NO_MATCH;
    }
    VarSymbol iterator = getSymbol(tree);
    for (int i = nextIdx + 1; i < statements.size(); i++) {
      if (!findUses(state, statements.get(i), iterator).isEmpty()) {
        return NO_MATCH;
      }
    }
    WhileLoopTree whileLoop = (WhileLoopTree) next;
    return match(
        tree,
        state,
        ((JCTree) tree).getStartPosition(),
        whileLoop.getCondition(),
        whileLoop.getStatement());
  }

  private Description match(
      VariableTree tree,
      VisitorState state,
      int startPosition,
      ExpressionTree condition,
      StatementTree body) {
    if (tree.getInitializer() == null || !ITERATOR.matches(tree.getInitializer(), state)) {
      return NO_MATCH;
    }
    VarSymbol iterator = getSymbol(tree);
    if (!isHasNext(iterator, stripParentheses(condition), state)) {
      return NO_MATCH;
    }
    ImmutableList<TreePath> uses = findUses(state, body, iterator);
    if (uses.size() != 1 || !uses.stream().allMatch(p -> isNext(tree, state, p))) {
      return NO_MATCH;
    }
    Type iteratorType =
        state.getTypes().asSuper(getType(tree.getType()), state.getSymtab().iteratorType.tsym);
    if (iteratorType == null || iteratorType.getTypeArguments().isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    VariableTree existingVariable = existingVariable(iterator, body, state);
    String replacement;
    if (existingVariable != null) {
      replacement = existingVariable.getName().toString();
      fix.delete(existingVariable);
    } else {
      replacement = "element";
      uses.forEach(
          p -> {
            TreePath path = p.getParentPath().getParentPath();
            switch (path.getParentPath().getLeaf().getKind()) {
              case EXPRESSION_STATEMENT:
                fix.delete(path.getParentPath().getLeaf());
                break;
              default:
                fix.replace(path.getLeaf(), replacement);
                break;
            }
          });
    }
    Type elementType = getOnlyElement(iteratorType.getTypeArguments());
    if (elementType.hasTag(TypeTag.WILDCARD)) {
      elementType = getUpperBound(elementType, state.getTypes());
    }
    fix.replace(
        startPosition,
        ((JCTree) body).getStartPosition(),
        String.format(
            "for (%s %s : %s) ",
            SuggestedFixes.prettyType(state, fix, elementType),
            replacement,
            state.getSourceForNode(getReceiver(tree.getInitializer()))));
    return describeMatch(tree, fix.build());
  }

  private ImmutableList<TreePath> findUses(
      VisitorState state, StatementTree body, VarSymbol iterator) {
    ImmutableList.Builder<TreePath> uses = ImmutableList.builder();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
        if (iterator.equals(getSymbol(identifierTree))) {
          uses.add(getCurrentPath());
        }
        return super.visitIdentifier(identifierTree, null);
      }
    }.scan(state.withPath(new TreePath(state.getPath().getParentPath(), body)).getPath(), null);
    return uses.build();
  }

  private static VariableTree existingVariable(
      VarSymbol varSymbol, StatementTree body, VisitorState state) {
    if (!(body instanceof BlockTree)) {
      return null;
    }
    List<? extends StatementTree> statements = ((BlockTree) body).getStatements();
    if (statements.isEmpty()) {
      return null;
    }
    StatementTree first = statements.iterator().next();
    if (!(first instanceof VariableTree)) {
      return null;
    }
    VariableTree variableTree = (VariableTree) first;
    if (variableTree.getInitializer() == null) {
      return null;
    }
    if (!NEXT.matches(variableTree.getInitializer(), state)) {
      return null;
    }
    if (!varSymbol.equals(ASTHelpers.getSymbol(getReceiver(variableTree.getInitializer())))) {
      return null;
    }
    return variableTree;
  }

  private static boolean isNext(VariableTree tree, VisitorState state, TreePath p) {
    Tree parentTree = p.getParentPath().getLeaf();
    if (!(parentTree instanceof ExpressionTree)) {
      return false;
    }
    ExpressionTree parent = (ExpressionTree) parentTree;
    return NEXT.matches(parent, state) && getSymbol(tree).equals(getSymbol(getReceiver(parent)));
  }

  private static boolean isHasNext(
      VarSymbol iterator, ExpressionTree condition, VisitorState state) {
    return HAS_NEXT.matches(condition, state) && iterator.equals(getSymbol(getReceiver(condition)));
  }
}
