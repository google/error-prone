/*
 * Copyright 2011 The Error Prone Authors.
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

package com.google.errorprone.scanner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneError;
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
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.util.ASTHelpers;
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
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scans the parsed AST, looking for violations of any of the enabled checks.
 *
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorProneScanner extends Scanner {

  private final Set<Class<? extends Annotation>> customSuppressionAnnotations = new HashSet<>();

  private final Map<String, SeverityLevel> severities;
  private final ImmutableSet<BugChecker> bugCheckers;

  /**
   * Create an error-prone scanner for a non-hardcoded set of checkers.
   *
   * @param checkers The checkers that this scanner should use.
   */
  public ErrorProneScanner(BugChecker... checkers) {
    this(Arrays.asList(checkers));
  }

  private static Map<String, BugPattern.SeverityLevel> defaultSeverities(
      Iterable<BugChecker> checkers) {
    ImmutableMap.Builder<String, BugPattern.SeverityLevel> builder = ImmutableMap.builder();
    for (BugChecker check : checkers) {
      builder.put(check.canonicalName(), check.defaultSeverity());
    }
    return builder.build();
  }

  /**
   * Create an error-prone scanner for a non-hardcoded set of checkers.
   *
   * @param checkers The checkers that this scanner should use.
   */
  public ErrorProneScanner(Iterable<BugChecker> checkers) {
    this(checkers, defaultSeverities(checkers));
  }

  /**
   * Create an error-prone scanner for a non-hardcoded set of checkers.
   *
   * @param checkers The checkers that this scanner should use.
   * @param severities The default check severities.
   */
  public ErrorProneScanner(Iterable<BugChecker> checkers, Map<String, SeverityLevel> severities) {
    this.bugCheckers = ImmutableSet.copyOf(checkers);
    this.severities = severities;
    for (BugChecker checker : this.bugCheckers) {
      registerNodeTypes(checker);
    }
  }

  @Override
  protected Set<Class<? extends Annotation>> getCustomSuppressionAnnotations() {
    return customSuppressionAnnotations;
  }

  private final List<AnnotationTreeMatcher> annotationMatchers = new ArrayList<>();
  private final List<AnnotatedTypeTreeMatcher> annotatedTypeMatchers = new ArrayList<>();
  private final List<ArrayAccessTreeMatcher> arrayAccessMatchers = new ArrayList<>();
  private final List<ArrayTypeTreeMatcher> arrayTypeMatchers = new ArrayList<>();
  private final List<AssertTreeMatcher> assertMatchers = new ArrayList<>();
  private final List<AssignmentTreeMatcher> assignmentMatchers = new ArrayList<>();
  private final List<BinaryTreeMatcher> binaryMatchers = new ArrayList<>();
  private final List<BlockTreeMatcher> blockMatchers = new ArrayList<>();
  private final List<BreakTreeMatcher> breakMatchers = new ArrayList<>();
  private final List<CaseTreeMatcher> caseMatchers = new ArrayList<>();
  private final List<CatchTreeMatcher> catchMatchers = new ArrayList<>();
  private final List<ClassTreeMatcher> classMatchers = new ArrayList<>();
  private final List<CompilationUnitTreeMatcher> compilationUnitMatchers = new ArrayList<>();
  private final List<CompoundAssignmentTreeMatcher> compoundAssignmentMatchers = new ArrayList<>();
  private final List<ConditionalExpressionTreeMatcher> conditionalExpressionMatchers =
      new ArrayList<>();
  private final List<ContinueTreeMatcher> continueMatchers = new ArrayList<>();
  private final List<DoWhileLoopTreeMatcher> doWhileLoopMatchers = new ArrayList<>();
  private final List<EmptyStatementTreeMatcher> emptyStatementMatchers = new ArrayList<>();
  private final List<EnhancedForLoopTreeMatcher> enhancedForLoopMatchers = new ArrayList<>();
  private final List<ExpressionStatementTreeMatcher> expressionStatementMatchers =
      new ArrayList<>();
  private final List<ForLoopTreeMatcher> forLoopMatchers = new ArrayList<>();
  private final List<IdentifierTreeMatcher> identifierMatchers = new ArrayList<>();
  private final List<IfTreeMatcher> ifMatchers = new ArrayList<>();
  private final List<ImportTreeMatcher> importMatchers = new ArrayList<>();
  private final List<InstanceOfTreeMatcher> instanceOfMatchers = new ArrayList<>();
  private final List<IntersectionTypeTreeMatcher> intersectionTypeMatchers = new ArrayList<>();
  private final List<LabeledStatementTreeMatcher> labeledStatementMatchers = new ArrayList<>();
  private final List<LambdaExpressionTreeMatcher> lambdaExpressionMatchers = new ArrayList<>();
  private final List<LiteralTreeMatcher> literalMatchers = new ArrayList<>();
  private final List<MemberReferenceTreeMatcher> memberReferenceMatchers = new ArrayList<>();
  private final List<MemberSelectTreeMatcher> memberSelectMatchers = new ArrayList<>();
  private final List<MethodTreeMatcher> methodMatchers = new ArrayList<>();
  private final List<MethodInvocationTreeMatcher> methodInvocationMatchers = new ArrayList<>();
  private final List<ModifiersTreeMatcher> modifiersMatchers = new ArrayList<>();
  private final List<NewArrayTreeMatcher> newArrayMatchers = new ArrayList<>();
  private final List<NewClassTreeMatcher> newClassMatchers = new ArrayList<>();
  private final List<ParameterizedTypeTreeMatcher> parameterizedTypeMatchers = new ArrayList<>();
  private final List<ParenthesizedTreeMatcher> parenthesizedMatchers = new ArrayList<>();
  private final List<PrimitiveTypeTreeMatcher> primitiveTypeMatchers = new ArrayList<>();
  private final List<ReturnTreeMatcher> returnMatchers = new ArrayList<>();
  private final List<SwitchTreeMatcher> switchMatchers = new ArrayList<>();
  private final List<SynchronizedTreeMatcher> synchronizedMatchers = new ArrayList<>();
  private final List<ThrowTreeMatcher> throwMatchers = new ArrayList<>();
  private final List<TryTreeMatcher> tryMatchers = new ArrayList<>();
  private final List<TypeCastTreeMatcher> typeCastMatchers = new ArrayList<>();
  private final List<TypeParameterTreeMatcher> typeParameterMatchers = new ArrayList<>();
  private final List<UnaryTreeMatcher> unaryMatchers = new ArrayList<>();
  private final List<UnionTypeTreeMatcher> unionTypeMatchers = new ArrayList<>();
  private final List<VariableTreeMatcher> variableMatchers = new ArrayList<>();
  private final List<WhileLoopTreeMatcher> whileLoopMatchers = new ArrayList<>();
  private final List<WildcardTreeMatcher> wildcardMatchers = new ArrayList<>();

  private void registerNodeTypes(BugChecker checker) {
    customSuppressionAnnotations.addAll(checker.customSuppressionAnnotations());

    if (checker instanceof AnnotationTreeMatcher) {
      annotationMatchers.add((AnnotationTreeMatcher) checker);
    }
    if (checker instanceof AnnotatedTypeTreeMatcher) {
      annotatedTypeMatchers.add((AnnotatedTypeTreeMatcher) checker);
    }
    if (checker instanceof ArrayAccessTreeMatcher) {
      arrayAccessMatchers.add((ArrayAccessTreeMatcher) checker);
    }
    if (checker instanceof ArrayTypeTreeMatcher) {
      arrayTypeMatchers.add((ArrayTypeTreeMatcher) checker);
    }
    if (checker instanceof AssertTreeMatcher) {
      assertMatchers.add((AssertTreeMatcher) checker);
    }
    if (checker instanceof AssignmentTreeMatcher) {
      assignmentMatchers.add((AssignmentTreeMatcher) checker);
    }
    if (checker instanceof BinaryTreeMatcher) {
      binaryMatchers.add((BinaryTreeMatcher) checker);
    }
    if (checker instanceof BlockTreeMatcher) {
      blockMatchers.add((BlockTreeMatcher) checker);
    }
    if (checker instanceof BreakTreeMatcher) {
      breakMatchers.add((BreakTreeMatcher) checker);
    }
    if (checker instanceof CaseTreeMatcher) {
      caseMatchers.add((CaseTreeMatcher) checker);
    }
    if (checker instanceof CatchTreeMatcher) {
      catchMatchers.add((CatchTreeMatcher) checker);
    }
    if (checker instanceof ClassTreeMatcher) {
      classMatchers.add((ClassTreeMatcher) checker);
    }
    if (checker instanceof CompilationUnitTreeMatcher) {
      compilationUnitMatchers.add((CompilationUnitTreeMatcher) checker);
    }
    if (checker instanceof CompoundAssignmentTreeMatcher) {
      compoundAssignmentMatchers.add((CompoundAssignmentTreeMatcher) checker);
    }
    if (checker instanceof ConditionalExpressionTreeMatcher) {
      conditionalExpressionMatchers.add((ConditionalExpressionTreeMatcher) checker);
    }
    if (checker instanceof ContinueTreeMatcher) {
      continueMatchers.add((ContinueTreeMatcher) checker);
    }
    if (checker instanceof DoWhileLoopTreeMatcher) {
      doWhileLoopMatchers.add((DoWhileLoopTreeMatcher) checker);
    }
    if (checker instanceof EmptyStatementTreeMatcher) {
      emptyStatementMatchers.add((EmptyStatementTreeMatcher) checker);
    }
    if (checker instanceof EnhancedForLoopTreeMatcher) {
      enhancedForLoopMatchers.add((EnhancedForLoopTreeMatcher) checker);
    }
    if (checker instanceof ExpressionStatementTreeMatcher) {
      expressionStatementMatchers.add((ExpressionStatementTreeMatcher) checker);
    }
    if (checker instanceof ForLoopTreeMatcher) {
      forLoopMatchers.add((ForLoopTreeMatcher) checker);
    }
    if (checker instanceof IdentifierTreeMatcher) {
      identifierMatchers.add((IdentifierTreeMatcher) checker);
    }
    if (checker instanceof IfTreeMatcher) {
      ifMatchers.add((IfTreeMatcher) checker);
    }
    if (checker instanceof ImportTreeMatcher) {
      importMatchers.add((ImportTreeMatcher) checker);
    }
    if (checker instanceof InstanceOfTreeMatcher) {
      instanceOfMatchers.add((InstanceOfTreeMatcher) checker);
    }
    if (checker instanceof IntersectionTypeTreeMatcher) {
      intersectionTypeMatchers.add((IntersectionTypeTreeMatcher) checker);
    }
    if (checker instanceof LabeledStatementTreeMatcher) {
      labeledStatementMatchers.add((LabeledStatementTreeMatcher) checker);
    }
    if (checker instanceof LambdaExpressionTreeMatcher) {
      lambdaExpressionMatchers.add((LambdaExpressionTreeMatcher) checker);
    }
    if (checker instanceof LiteralTreeMatcher) {
      literalMatchers.add((LiteralTreeMatcher) checker);
    }
    if (checker instanceof MemberReferenceTreeMatcher) {
      memberReferenceMatchers.add((MemberReferenceTreeMatcher) checker);
    }
    if (checker instanceof MemberSelectTreeMatcher) {
      memberSelectMatchers.add((MemberSelectTreeMatcher) checker);
    }
    if (checker instanceof MethodTreeMatcher) {
      methodMatchers.add((MethodTreeMatcher) checker);
    }
    if (checker instanceof MethodInvocationTreeMatcher) {
      methodInvocationMatchers.add((MethodInvocationTreeMatcher) checker);
    }
    if (checker instanceof ModifiersTreeMatcher) {
      modifiersMatchers.add((ModifiersTreeMatcher) checker);
    }
    if (checker instanceof NewArrayTreeMatcher) {
      newArrayMatchers.add((NewArrayTreeMatcher) checker);
    }
    if (checker instanceof NewClassTreeMatcher) {
      newClassMatchers.add((NewClassTreeMatcher) checker);
    }
    if (checker instanceof ParameterizedTypeTreeMatcher) {
      parameterizedTypeMatchers.add((ParameterizedTypeTreeMatcher) checker);
    }
    if (checker instanceof ParenthesizedTreeMatcher) {
      parenthesizedMatchers.add((ParenthesizedTreeMatcher) checker);
    }
    if (checker instanceof PrimitiveTypeTreeMatcher) {
      primitiveTypeMatchers.add((PrimitiveTypeTreeMatcher) checker);
    }
    if (checker instanceof ReturnTreeMatcher) {
      returnMatchers.add((ReturnTreeMatcher) checker);
    }
    if (checker instanceof SwitchTreeMatcher) {
      switchMatchers.add((SwitchTreeMatcher) checker);
    }
    if (checker instanceof SynchronizedTreeMatcher) {
      synchronizedMatchers.add((SynchronizedTreeMatcher) checker);
    }
    if (checker instanceof ThrowTreeMatcher) {
      throwMatchers.add((ThrowTreeMatcher) checker);
    }
    if (checker instanceof TryTreeMatcher) {
      tryMatchers.add((TryTreeMatcher) checker);
    }
    if (checker instanceof TypeCastTreeMatcher) {
      typeCastMatchers.add((TypeCastTreeMatcher) checker);
    }
    if (checker instanceof TypeParameterTreeMatcher) {
      typeParameterMatchers.add((TypeParameterTreeMatcher) checker);
    }
    if (checker instanceof UnaryTreeMatcher) {
      unaryMatchers.add((UnaryTreeMatcher) checker);
    }
    if (checker instanceof UnionTypeTreeMatcher) {
      unionTypeMatchers.add((UnionTypeTreeMatcher) checker);
    }
    if (checker instanceof VariableTreeMatcher) {
      variableMatchers.add((VariableTreeMatcher) checker);
    }
    if (checker instanceof WhileLoopTreeMatcher) {
      whileLoopMatchers.add((WhileLoopTreeMatcher) checker);
    }
    if (checker instanceof WildcardTreeMatcher) {
      wildcardMatchers.add((WildcardTreeMatcher) checker);
    }
  }

  @Override
  public Void visitAnnotation(AnnotationTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (AnnotationTreeMatcher matcher : annotationMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchAnnotation(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitAnnotation(tree, state);
  }

  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (AnnotatedTypeTreeMatcher matcher : annotatedTypeMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchAnnotatedType(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitAnnotatedType(tree, state);
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ArrayAccessTreeMatcher matcher : arrayAccessMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchArrayAccess(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitArrayAccess(tree, state);
  }

  @Override
  public Void visitArrayType(ArrayTypeTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ArrayTypeTreeMatcher matcher : arrayTypeMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchArrayType(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitArrayType(tree, state);
  }

  @Override
  public Void visitAssert(AssertTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (AssertTreeMatcher matcher : assertMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchAssert(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitAssert(tree, state);
  }

  @Override
  public Void visitAssignment(AssignmentTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (AssignmentTreeMatcher matcher : assignmentMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchAssignment(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitAssignment(tree, state);
  }

  @Override
  public Void visitBinary(BinaryTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (BinaryTreeMatcher matcher : binaryMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchBinary(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitBinary(tree, state);
  }

  @Override
  public Void visitBlock(BlockTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (BlockTreeMatcher matcher : blockMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchBlock(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitBlock(tree, state);
  }

  @Override
  public Void visitBreak(BreakTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (BreakTreeMatcher matcher : breakMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchBreak(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitBreak(tree, state);
  }

  @Override
  public Void visitCase(CaseTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (CaseTreeMatcher matcher : caseMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchCase(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitCase(tree, state);
  }

  @Override
  public Void visitCatch(CatchTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (CatchTreeMatcher matcher : catchMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchCatch(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitCatch(tree, state);
  }

  @Override
  public Void visitClass(ClassTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ClassTreeMatcher matcher : classMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchClass(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitClass(tree, state);
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (CompilationUnitTreeMatcher matcher : compilationUnitMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchCompilationUnit(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitCompilationUnit(tree, state);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (CompoundAssignmentTreeMatcher matcher : compoundAssignmentMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchCompoundAssignment(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitCompoundAssignment(tree, state);
  }

  @Override
  public Void visitConditionalExpression(
      ConditionalExpressionTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ConditionalExpressionTreeMatcher matcher : conditionalExpressionMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchConditionalExpression(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitConditionalExpression(tree, state);
  }

  @Override
  public Void visitContinue(ContinueTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ContinueTreeMatcher matcher : continueMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchContinue(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitContinue(tree, state);
  }

  @Override
  public Void visitDoWhileLoop(DoWhileLoopTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (DoWhileLoopTreeMatcher matcher : doWhileLoopMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchDoWhileLoop(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitDoWhileLoop(tree, state);
  }

  @Override
  public Void visitEmptyStatement(EmptyStatementTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (EmptyStatementTreeMatcher matcher : emptyStatementMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchEmptyStatement(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitEmptyStatement(tree, state);
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (EnhancedForLoopTreeMatcher matcher : enhancedForLoopMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchEnhancedForLoop(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitEnhancedForLoop(tree, state);
  }

  // Intentionally skip visitErroneous -- we don't analyze malformed expressions.

  @Override
  public Void visitExpressionStatement(ExpressionStatementTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ExpressionStatementTreeMatcher matcher : expressionStatementMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchExpressionStatement(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitExpressionStatement(tree, state);
  }

  @Override
  public Void visitForLoop(ForLoopTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ForLoopTreeMatcher matcher : forLoopMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchForLoop(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitForLoop(tree, state);
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (IdentifierTreeMatcher matcher : identifierMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchIdentifier(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitIdentifier(tree, state);
  }

  @Override
  public Void visitIf(IfTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (IfTreeMatcher matcher : ifMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchIf(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitIf(tree, state);
  }

  @Override
  public Void visitImport(ImportTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ImportTreeMatcher matcher : importMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchImport(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitImport(tree, state);
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (InstanceOfTreeMatcher matcher : instanceOfMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchInstanceOf(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitInstanceOf(tree, state);
  }

  @Override
  public Void visitIntersectionType(IntersectionTypeTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (IntersectionTypeTreeMatcher matcher : intersectionTypeMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchIntersectionType(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitIntersectionType(tree, state);
  }

  @Override
  public Void visitLabeledStatement(LabeledStatementTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (LabeledStatementTreeMatcher matcher : labeledStatementMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchLabeledStatement(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitLabeledStatement(tree, state);
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (LambdaExpressionTreeMatcher matcher : lambdaExpressionMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchLambdaExpression(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitLambdaExpression(tree, state);
  }

  @Override
  public Void visitLiteral(LiteralTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (LiteralTreeMatcher matcher : literalMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchLiteral(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitLiteral(tree, state);
  }

  @Override
  public Void visitMemberReference(MemberReferenceTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (MemberReferenceTreeMatcher matcher : memberReferenceMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchMemberReference(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitMemberReference(tree, state);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (MemberSelectTreeMatcher matcher : memberSelectMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchMemberSelect(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitMemberSelect(tree, state);
  }

  @Override
  public Void visitMethod(MethodTree tree, VisitorState visitorState) {
    // Ignore synthetic constructors:
    if (ASTHelpers.isGeneratedConstructor(tree)) {
      return null;
    }

    VisitorState state = visitorState.withPath(getCurrentPath());
    for (MethodTreeMatcher matcher : methodMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchMethod(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitMethod(tree, state);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (MethodInvocationTreeMatcher matcher : methodInvocationMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchMethodInvocation(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitMethodInvocation(tree, state);
  }

  @Override
  public Void visitModifiers(ModifiersTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ModifiersTreeMatcher matcher : modifiersMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchModifiers(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitModifiers(tree, state);
  }

  @Override
  public Void visitNewArray(NewArrayTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (NewArrayTreeMatcher matcher : newArrayMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchNewArray(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitNewArray(tree, state);
  }

  @Override
  public Void visitNewClass(NewClassTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (NewClassTreeMatcher matcher : newClassMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchNewClass(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitNewClass(tree, state);
  }

  // Intentionally skip visitOther. It seems to be used only for let expressions, which are
  // generated by javac to implement autoboxing. We are only interested in source-level constructs.

  @Override
  public Void visitParameterizedType(ParameterizedTypeTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ParameterizedTypeTreeMatcher matcher : parameterizedTypeMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchParameterizedType(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitParameterizedType(tree, state);
  }

  @Override
  public Void visitParenthesized(ParenthesizedTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ParenthesizedTreeMatcher matcher : parenthesizedMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchParenthesized(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitParenthesized(tree, state);
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (PrimitiveTypeTreeMatcher matcher : primitiveTypeMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchPrimitiveType(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitPrimitiveType(tree, state);
  }

  @Override
  public Void visitReturn(ReturnTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ReturnTreeMatcher matcher : returnMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchReturn(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitReturn(tree, state);
  }

  @Override
  public Void visitSwitch(SwitchTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (SwitchTreeMatcher matcher : switchMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchSwitch(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitSwitch(tree, state);
  }

  @Override
  public Void visitSynchronized(SynchronizedTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (SynchronizedTreeMatcher matcher : synchronizedMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchSynchronized(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitSynchronized(tree, state);
  }

  @Override
  public Void visitThrow(ThrowTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ThrowTreeMatcher matcher : throwMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchThrow(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitThrow(tree, state);
  }

  @Override
  public Void visitTry(TryTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (TryTreeMatcher matcher : tryMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchTry(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitTry(tree, state);
  }

  @Override
  public Void visitTypeCast(TypeCastTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (TypeCastTreeMatcher matcher : typeCastMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchTypeCast(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitTypeCast(tree, state);
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (TypeParameterTreeMatcher matcher : typeParameterMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchTypeParameter(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitTypeParameter(tree, state);
  }

  @Override
  public Void visitUnary(UnaryTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (UnaryTreeMatcher matcher : unaryMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchUnary(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitUnary(tree, state);
  }

  @Override
  public Void visitUnionType(UnionTypeTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (UnionTypeTreeMatcher matcher : unionTypeMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchUnionType(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitUnionType(tree, state);
  }

  @Override
  public Void visitVariable(VariableTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (VariableTreeMatcher matcher : variableMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchVariable(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitVariable(tree, state);
  }

  @Override
  public Void visitWhileLoop(WhileLoopTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (WhileLoopTreeMatcher matcher : whileLoopMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchWhileLoop(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitWhileLoop(tree, state);
  }

  @Override
  public Void visitWildcard(WildcardTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (WildcardTreeMatcher matcher : wildcardMatchers) {
      if (!isSuppressed(matcher, state.errorProneOptions())) {
        try {
          reportMatch(matcher.matchWildcard(tree, state), state);
        } catch (Throwable t) {
          handleError(matcher, t);
        }
      }
    }
    return super.visitWildcard(tree, state);
  }

  /**
   * Handles an exception thrown by an individual BugPattern. By default, wraps the exception in an
   * {@link ErrorProneError} and rethrows. May be overridden by subclasses, for example to log the
   * error and continue.
   */
  @Override
  protected void handleError(Suppressible s, Throwable t) {
    if (t instanceof ErrorProneError) {
      throw (ErrorProneError) t;
    }
    if (t instanceof CompletionFailure) {
      throw (CompletionFailure) t;
    }
    TreePath path = getCurrentPath();
    throw new ErrorProneError(
        s.canonicalName(),
        t,
        (DiagnosticPosition) path.getLeaf(),
        path.getCompilationUnit().getSourceFile());
  }

  @Override
  public Map<String, SeverityLevel> severityMap() {
    return severities;
  }

  public ImmutableSet<BugChecker> getBugCheckers() {
    return this.bugCheckers;
  }
}
