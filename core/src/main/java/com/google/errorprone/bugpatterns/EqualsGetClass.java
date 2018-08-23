/*
 * Copyright 2018 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import javax.lang.model.element.Modifier;

/**
 * Discourages the use of {@link Object#getClass()} when implementing {@link Object#equals(Object)}
 * for non-final classes.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "EqualsGetClass",
    summary =
        "Overriding Object#equals in a non-final class by using getClass rather than instanceof "
            + "breaks substitutability of subclasses.",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class EqualsGetClass extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> GET_CLASS =
      instanceMethod().onDescendantOf("java.lang.Object").named("getClass");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!GET_CLASS.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    TreePath methodTreePath = state.findPathToEnclosing(MethodTree.class);
    if (methodTreePath == null) {
      return Description.NO_MATCH;
    }
    ClassTree classTree = state.findEnclosing(ClassTree.class);
    // Using getClass is harmless if a class is final (although instanceof provides the null check
    // for free).
    if (classTree == null || classTree.getModifiers().getFlags().contains(Modifier.FINAL)) {
      return Description.NO_MATCH;
    }
    ClassSymbol classSymbol = getSymbol(classTree);
    if (classSymbol == null || classSymbol.isAnonymous()) {
      return Description.NO_MATCH;
    }
    MethodTree methodTree = (MethodTree) methodTreePath.getLeaf();
    if (!equalsMethodDeclaration().matches(methodTree, state)) {
      return Description.NO_MATCH;
    }
    VariableTree parameter = getOnlyElement(methodTree.getParameters());
    ExpressionTree receiver = getReceiver(tree);
    VarSymbol symbol = getSymbol(parameter);
    if (symbol == null
        || receiver == null
        || receiver.getKind() != Kind.IDENTIFIER
        || !symbol.equals(getSymbol(receiver))) {
      return Description.NO_MATCH;
    }
    EqualsFixer fixer = new EqualsFixer(symbol, getSymbol(classTree), state);
    fixer.scan(methodTreePath, null);
    return describeMatch(methodTree, fixer.getFix());
  }

  private static class EqualsFixer extends TreePathScanner<Void, Void> {

    private static final Matcher<ExpressionTree> GET_CLASS =
        instanceMethod().onDescendantOf("java.lang.Object").named("getClass").withParameters();

    private static final Matcher<ExpressionTree> THIS_CLASS =
        anyOf(
            allOf(GET_CLASS, (tree, unused) -> matchesThis(tree)),
            (tree, unused) -> matchesClass(tree));

    private static final Matcher<ExpressionTree> EQUALS =
        instanceMethod()
            .onDescendantOf("java.lang.Object")
            .named("equals")
            .withParameters("java.lang.Object");

    private static boolean matchesThis(ExpressionTree tree) {
      ExpressionTree receiver = getReceiver(tree);
      return receiver == null || getSymbol(receiver).getSimpleName().contentEquals("this");
    }

    private static boolean matchesClass(ExpressionTree tree) {
      Symbol symbol = getSymbol(tree);
      if (!(symbol instanceof VarSymbol)) {
        return false;
      }
      VarSymbol varSymbol = (VarSymbol) symbol;
      return varSymbol.getSimpleName().contentEquals("class");
    }

    private static boolean matchesNull(ExpressionTree tree) {
      return tree.getKind() == Kind.NULL_LITERAL;
    }

    private final Symbol parameter;
    private final ClassSymbol classSymbol;
    private final VisitorState state;
    private final SuggestedFix.Builder fix = SuggestedFix.builder();

    private final Matcher<ExpressionTree> isParameter;
    private final Matcher<ExpressionTree> otherClass;

    /** Whether we managed to rewrite a {@code getClass}. */
    private boolean matchedGetClass = false;

    /** Whether we failed to generate a satisfactory fix for a boolean replacement. */
    private boolean failed = false;

    private EqualsFixer(Symbol parameter, ClassSymbol classSymbol, VisitorState visitorState) {
      this.parameter = parameter;
      this.classSymbol = classSymbol;
      this.state = visitorState;

      this.isParameter = (tree, state) -> parameter.equals(getSymbol(tree));
      this.otherClass =
          allOf(GET_CLASS, (tree, state) -> parameter.equals(getSymbol(getReceiver(tree))));
    }

    @Override
    public Void visitBinary(BinaryTree binaryTree, Void unused) {
      if (binaryTree.getKind() != Kind.NOT_EQUAL_TO && binaryTree.getKind() != Kind.EQUAL_TO) {
        return super.visitBinary(binaryTree, null);
      }
      if (matchesEitherWay(binaryTree, isParameter, (tree, s) -> matchesNull(tree))) {
        if (binaryTree.getKind() == Kind.NOT_EQUAL_TO) {
          makeAlwaysTrue();
        }
        if (binaryTree.getKind() == Kind.EQUAL_TO) {
          makeAlwaysFalse();
        }
        return null;
      }
      if (matchesEitherWay(binaryTree, THIS_CLASS, otherClass)) {
        matchedGetClass = true;
        String instanceOf =
            String.format(
                "%s instanceof %s",
                parameter.getSimpleName().toString(), classSymbol.getSimpleName().toString());
        if (binaryTree.getKind() == Kind.EQUAL_TO) {
          fix.replace(binaryTree, instanceOf);
        }
        if (binaryTree.getKind() == Kind.NOT_EQUAL_TO) {
          fix.replace(binaryTree, String.format("!(%s)", instanceOf));
        }
      }
      return super.visitBinary(binaryTree, null);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
      if (!EQUALS.matches(node, state)) {
        return null;
      }
      ExpressionTree argument = getOnlyElement(node.getArguments());
      ExpressionTree receiver = getReceiver(node);
      if (receiver == null) {
        return null;
      }
      if (matchesEitherWay(argument, receiver, THIS_CLASS, otherClass)) {
        matchedGetClass = true;
        fix.replace(
            node,
            String.format(
                "%s instanceof %s",
                parameter.getSimpleName().toString(), classSymbol.getSimpleName().toString()));
      }
      return super.visitMethodInvocation(node, null);
    }

    private boolean matchesEitherWay(
        BinaryTree binaryTree, Matcher<ExpressionTree> matcherA, Matcher<ExpressionTree> matcherB) {
      return matchesEitherWay(
          binaryTree.getLeftOperand(), binaryTree.getRightOperand(), matcherA, matcherB);
    }

    private boolean matchesEitherWay(
        ExpressionTree treeA,
        ExpressionTree treeB,
        Matcher<ExpressionTree> matcherA,
        Matcher<ExpressionTree> matcherB) {
      return (matcherA.matches(treeA, state) && matcherB.matches(treeB, state))
          || (matcherA.matches(treeB, state) && matcherB.matches(treeA, state));
    }

    private void makeAlwaysTrue() {
      removeFromBinary(Kind.CONDITIONAL_AND);
    }

    private void makeAlwaysFalse() {
      TreePath enclosingPath = getCurrentPath().getParentPath();
      while (enclosingPath.getLeaf() instanceof ParenthesizedTree) {
        enclosingPath = enclosingPath.getParentPath();
      }
      Tree enclosing = enclosingPath.getLeaf();
      if (enclosing instanceof IfTree) {
        IfTree ifTree = (IfTree) enclosing;
        if (ifTree.getElseStatement() == null) {
          fix.replace(ifTree, "");
        } else {
          int stripExtra = ifTree.getElseStatement() instanceof BlockTree ? 1 : 0;
          fix.replace(
                  ((JCTree) ifTree).getStartPosition(),
                  ((JCTree) ifTree.getElseStatement()).getStartPosition() + stripExtra,
                  "")
              .replace(
                  state.getEndPosition(ifTree.getElseStatement()) - stripExtra,
                  state.getEndPosition(ifTree.getElseStatement()),
                  "");
        }
        return;
      }
      removeFromBinary(Kind.CONDITIONAL_OR);
    }

    private void removeFromBinary(Kind ifKind) {
      TreePath outsideParensPath = getCurrentPath().getParentPath();
      TreePath justInsideBinaryPath = getCurrentPath();
      while (outsideParensPath.getLeaf() instanceof ParenthesizedTree) {
        justInsideBinaryPath = outsideParensPath;
        outsideParensPath = outsideParensPath.getParentPath();
      }
      Tree superTree = outsideParensPath.getLeaf();
      if (superTree.getKind() != ifKind) {
        failed = true;
        return;
      }
      BinaryTree superBinary = (BinaryTree) superTree;
      if (superBinary.getLeftOperand().equals(justInsideBinaryPath.getLeaf())) {
        removeLeftOperand(superBinary);
      } else {
        removeRightOperand(superBinary);
      }
    }

    private void removeLeftOperand(BinaryTree superBinary) {
      fix.replace(
          ((JCTree) superBinary.getLeftOperand()).getStartPosition(),
          ((JCTree) superBinary.getRightOperand()).getStartPosition(),
          "");
    }

    private void removeRightOperand(BinaryTree superBinary) {
      fix.replace(
          state.getEndPosition(superBinary.getLeftOperand()),
          state.getEndPosition(superBinary.getRightOperand()),
          "");
    }

    private SuggestedFix getFix() {
      return matchedGetClass && !failed ? fix.build() : SuggestedFix.builder().build();
    }
  }
}
