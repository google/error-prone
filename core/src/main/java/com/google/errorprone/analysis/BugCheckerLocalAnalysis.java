/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.analysis;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.BugPattern.Suppressibility;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotatedTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ArrayAccessTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ArrayTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.AssertTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.BlockTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.BreakTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CaseTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CatchTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeInfo;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompoundAssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ConditionalExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ContinueTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.DoWhileLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.EmptyStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.EnhancedForLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ExpressionStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ForLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ImportTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.InstanceOfTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.IntersectionTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LabeledStatementTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LambdaExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ModifiersTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewArrayTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ParameterizedTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ParenthesizedTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.PrimitiveTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SynchronizedTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ThrowTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeParameterTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.UnaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.UnionTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.WhileLoopTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.WildcardTreeMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Adapter from {@code BugChecker} to {@code LocalAnalysis}.
 *
 * @author Louis Wasserman
 */
public final class BugCheckerLocalAnalysis extends SimpleTreeVisitor<Void, VisitorState>
    implements LocalAnalysis {
  private final BugChecker checker;

  BugCheckerLocalAnalysis(BugChecker checker) {
    this.checker = checkNotNull(checker);
  }

  @Override
  public Set<String> allNames() {
    return checker.allNames();
  }

  @Override
  public String canonicalName() {
    return checker.canonicalName();
  }

  @Override
  public Class<? extends Annotation> customSuppressionAnnotation() {
    return checker.customSuppressionAnnotation();
  }

  @Override
  public Suppressibility suppressibility() {
    return checker.suppressibility();
  }

  @Override
  public void analyze(TreePath tree, Context context, AnalysesConfig config,
      DescriptionListener listener) {
    tree.getLeaf().accept(this, new VisitorState(context, listener).withPath(tree));
  }

  @Override
  protected Void defaultAction(Tree node, VisitorState state) {
    return null;
  }

  private void report(Description description, VisitorState state) {
    if (description != Description.NO_MATCH) {
      state.reportMatch(description);
    }
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree node, VisitorState state) {
    if (checker instanceof ArrayAccessTreeMatcher) {
      ArrayAccessTreeMatcher matcher = (ArrayAccessTreeMatcher) checker;
      report(matcher.matchArrayAccess(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitAnnotation(AnnotationTree node, VisitorState state) {
    if (checker instanceof AnnotationTreeMatcher) {
      AnnotationTreeMatcher matcher = (AnnotationTreeMatcher) checker;
      report(matcher.matchAnnotation(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, VisitorState state) {
    if (checker instanceof CompilationUnitTreeMatcher) {
      CompilationUnitTreeMatcher matcher = (CompilationUnitTreeMatcher) checker;
      report(matcher.matchCompilationUnit(CompilationUnitTreeInfo.create(node), state), state);
    }
    return null;
  }

  @Override
  public Void visitImport(ImportTree node, VisitorState state) {
    if (checker instanceof ImportTreeMatcher) {
      ImportTreeMatcher matcher = (ImportTreeMatcher) checker;
      report(matcher.matchImport(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitClass(ClassTree node, VisitorState state) {
    if (checker instanceof ClassTreeMatcher) {
      ClassTreeMatcher matcher = (ClassTreeMatcher) checker;
      report(matcher.matchClass(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitMethod(MethodTree node, VisitorState state) {
    if (checker instanceof MethodTreeMatcher) {
      MethodTreeMatcher matcher = (MethodTreeMatcher) checker;
      report(matcher.matchMethod(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitVariable(VariableTree node, VisitorState state) {
    if (checker instanceof VariableTreeMatcher) {
      VariableTreeMatcher matcher = (VariableTreeMatcher) checker;
      report(matcher.matchVariable(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitEmptyStatement(EmptyStatementTree node, VisitorState state) {
    if (checker instanceof EmptyStatementTreeMatcher) {
      EmptyStatementTreeMatcher matcher = (EmptyStatementTreeMatcher) checker;
      report(matcher.matchEmptyStatement(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitBlock(BlockTree node, VisitorState state) {
    if (checker instanceof BlockTreeMatcher) {
      BlockTreeMatcher matcher = (BlockTreeMatcher) checker;
      report(matcher.matchBlock(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitDoWhileLoop(DoWhileLoopTree node, VisitorState state) {
    if (checker instanceof DoWhileLoopTreeMatcher) {
      DoWhileLoopTreeMatcher matcher = (DoWhileLoopTreeMatcher) checker;
      report(matcher.matchDoWhileLoop(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitWhileLoop(WhileLoopTree node, VisitorState state) {
    if (checker instanceof WhileLoopTreeMatcher) {
      WhileLoopTreeMatcher matcher = (WhileLoopTreeMatcher) checker;
      report(matcher.matchWhileLoop(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitForLoop(ForLoopTree node, VisitorState state) {
    if (checker instanceof ForLoopTreeMatcher) {
      ForLoopTreeMatcher matcher = (ForLoopTreeMatcher) checker;
      report(matcher.matchForLoop(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree node, VisitorState state) {
    if (checker instanceof EnhancedForLoopTreeMatcher) {
      EnhancedForLoopTreeMatcher matcher = (EnhancedForLoopTreeMatcher) checker;
      report(matcher.matchEnhancedForLoop(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitLabeledStatement(LabeledStatementTree node, VisitorState state) {
    if (checker instanceof LabeledStatementTreeMatcher) {
      LabeledStatementTreeMatcher matcher = (LabeledStatementTreeMatcher) checker;
      report(matcher.matchLabeledStatement(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitSwitch(SwitchTree node, VisitorState state) {
    if (checker instanceof SwitchTreeMatcher) {
      SwitchTreeMatcher matcher = (SwitchTreeMatcher) checker;
      report(matcher.matchSwitch(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitCase(CaseTree node, VisitorState state) {
    if (checker instanceof CaseTreeMatcher) {
      CaseTreeMatcher matcher = (CaseTreeMatcher) checker;
      report(matcher.matchCase(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitSynchronized(SynchronizedTree node, VisitorState state) {
    if (checker instanceof SynchronizedTreeMatcher) {
      SynchronizedTreeMatcher matcher = (SynchronizedTreeMatcher) checker;
      report(matcher.matchSynchronized(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitTry(TryTree node, VisitorState state) {
    if (checker instanceof TryTreeMatcher) {
      TryTreeMatcher matcher = (TryTreeMatcher) checker;
      report(matcher.matchTry(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitCatch(CatchTree node, VisitorState state) {
    if (checker instanceof CatchTreeMatcher) {
      CatchTreeMatcher matcher = (CatchTreeMatcher) checker;
      report(matcher.matchCatch(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, VisitorState state) {
    if (checker instanceof ConditionalExpressionTreeMatcher) {
      ConditionalExpressionTreeMatcher matcher = (ConditionalExpressionTreeMatcher) checker;
      report(matcher.matchConditionalExpression(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitIf(IfTree node, VisitorState state) {
    if (checker instanceof IfTreeMatcher) {
      IfTreeMatcher matcher = (IfTreeMatcher) checker;
      report(matcher.matchIf(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitExpressionStatement(ExpressionStatementTree node, VisitorState state) {
    if (checker instanceof ExpressionStatementTreeMatcher) {
      ExpressionStatementTreeMatcher matcher = (ExpressionStatementTreeMatcher) checker;
      report(matcher.matchExpressionStatement(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitBreak(BreakTree node, VisitorState state) {
    if (checker instanceof BreakTreeMatcher) {
      BreakTreeMatcher matcher = (BreakTreeMatcher) checker;
      report(matcher.matchBreak(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitContinue(ContinueTree node, VisitorState state) {
    if (checker instanceof ContinueTreeMatcher) {
      ContinueTreeMatcher matcher = (ContinueTreeMatcher) checker;
      report(matcher.matchContinue(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitReturn(ReturnTree node, VisitorState state) {
    if (checker instanceof ReturnTreeMatcher) {
      ReturnTreeMatcher matcher = (ReturnTreeMatcher) checker;
      report(matcher.matchReturn(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitThrow(ThrowTree node, VisitorState state) {
    if (checker instanceof ThrowTreeMatcher) {
      ThrowTreeMatcher matcher = (ThrowTreeMatcher) checker;
      report(matcher.matchThrow(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitAssert(AssertTree node, VisitorState state) {
    if (checker instanceof AssertTreeMatcher) {
      AssertTreeMatcher matcher = (AssertTreeMatcher) checker;
      report(matcher.matchAssert(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, VisitorState state) {
    if (checker instanceof MethodInvocationTreeMatcher) {
      MethodInvocationTreeMatcher matcher = (MethodInvocationTreeMatcher) checker;
      report(matcher.matchMethodInvocation(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitNewClass(NewClassTree node, VisitorState state) {
    if (checker instanceof NewClassTreeMatcher) {
      NewClassTreeMatcher matcher = (NewClassTreeMatcher) checker;
      report(matcher.matchNewClass(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitNewArray(NewArrayTree node, VisitorState state) {
    if (checker instanceof NewArrayTreeMatcher) {
      NewArrayTreeMatcher matcher = (NewArrayTreeMatcher) checker;
      report(matcher.matchNewArray(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree node, VisitorState state) {
    if (checker instanceof LambdaExpressionTreeMatcher) {
      LambdaExpressionTreeMatcher matcher = (LambdaExpressionTreeMatcher) checker;
      report(matcher.matchLambdaExpression(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitParenthesized(ParenthesizedTree node, VisitorState state) {
    if (checker instanceof ParenthesizedTreeMatcher) {
      ParenthesizedTreeMatcher matcher = (ParenthesizedTreeMatcher) checker;
      report(matcher.matchParenthesized(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitAssignment(AssignmentTree node, VisitorState state) {
    if (checker instanceof AssignmentTreeMatcher) {
      AssignmentTreeMatcher matcher = (AssignmentTreeMatcher) checker;
      report(matcher.matchAssignment(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree node, VisitorState state) {
    if (checker instanceof CompoundAssignmentTreeMatcher) {
      CompoundAssignmentTreeMatcher matcher = (CompoundAssignmentTreeMatcher) checker;
      report(matcher.matchCompoundAssignment(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitUnary(UnaryTree node, VisitorState state) {
    if (checker instanceof UnaryTreeMatcher) {
      UnaryTreeMatcher matcher = (UnaryTreeMatcher) checker;
      report(matcher.matchUnary(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitBinary(BinaryTree node, VisitorState state) {
    if (checker instanceof BinaryTreeMatcher) {
      BinaryTreeMatcher matcher = (BinaryTreeMatcher) checker;
      report(matcher.matchBinary(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitTypeCast(TypeCastTree node, VisitorState state) {
    if (checker instanceof TypeCastTreeMatcher) {
      TypeCastTreeMatcher matcher = (TypeCastTreeMatcher) checker;
      report(matcher.matchTypeCast(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree node, VisitorState state) {
    if (checker instanceof InstanceOfTreeMatcher) {
      InstanceOfTreeMatcher matcher = (InstanceOfTreeMatcher) checker;
      report(matcher.matchInstanceOf(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree node, VisitorState state) {
    if (checker instanceof MemberSelectTreeMatcher) {
      MemberSelectTreeMatcher matcher = (MemberSelectTreeMatcher) checker;
      report(matcher.matchMemberSelect(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitMemberReference(MemberReferenceTree node, VisitorState state) {
    if (checker instanceof MemberReferenceTreeMatcher) {
      MemberReferenceTreeMatcher matcher = (MemberReferenceTreeMatcher) checker;
      report(matcher.matchMemberReference(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, VisitorState state) {
    if (checker instanceof IdentifierTreeMatcher) {
      IdentifierTreeMatcher matcher = (IdentifierTreeMatcher) checker;
      report(matcher.matchIdentifier(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitLiteral(LiteralTree node, VisitorState state) {
    if (checker instanceof LiteralTreeMatcher) {
      LiteralTreeMatcher matcher = (LiteralTreeMatcher) checker;
      report(matcher.matchLiteral(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree node, VisitorState state) {
    if (checker instanceof PrimitiveTypeTreeMatcher) {
      PrimitiveTypeTreeMatcher matcher = (PrimitiveTypeTreeMatcher) checker;
      report(matcher.matchPrimitiveType(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitArrayType(ArrayTypeTree node, VisitorState state) {
    if (checker instanceof ArrayTypeTreeMatcher) {
      ArrayTypeTreeMatcher matcher = (ArrayTypeTreeMatcher) checker;
      report(matcher.matchArrayType(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitParameterizedType(ParameterizedTypeTree node, VisitorState state) {
    if (checker instanceof ParameterizedTypeTreeMatcher) {
      ParameterizedTypeTreeMatcher matcher = (ParameterizedTypeTreeMatcher) checker;
      report(matcher.matchParameterizedType(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitUnionType(UnionTypeTree node, VisitorState state) {
    if (checker instanceof UnionTypeTreeMatcher) {
      UnionTypeTreeMatcher matcher = (UnionTypeTreeMatcher) checker;
      report(matcher.matchUnionType(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitIntersectionType(IntersectionTypeTree node, VisitorState state) {
    if (checker instanceof IntersectionTypeTreeMatcher) {
      IntersectionTypeTreeMatcher matcher = (IntersectionTypeTreeMatcher) checker;
      report(matcher.matchIntersectionType(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree node, VisitorState state) {
    if (checker instanceof TypeParameterTreeMatcher) {
      TypeParameterTreeMatcher matcher = (TypeParameterTreeMatcher) checker;
      report(matcher.matchTypeParameter(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitWildcard(WildcardTree node, VisitorState state) {
    if (checker instanceof WildcardTreeMatcher) {
      WildcardTreeMatcher matcher = (WildcardTreeMatcher) checker;
      report(matcher.matchWildcard(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitModifiers(ModifiersTree node, VisitorState state) {
    if (checker instanceof ModifiersTreeMatcher) {
      ModifiersTreeMatcher matcher = (ModifiersTreeMatcher) checker;
      report(matcher.matchModifiers(node, state), state);
    }
    return null;
  }

  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree node, VisitorState state) {
    if (checker instanceof AnnotatedTypeTreeMatcher) {
      AnnotatedTypeTreeMatcher matcher = (AnnotatedTypeTreeMatcher) checker;
      report(matcher.matchAnnotatedType(node, state), state);
    }
    return null;
  }
}
