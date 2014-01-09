/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.bugpatterns.BugChecker.*;

import com.google.errorprone.BugPattern.Suppressibility;
import com.google.errorprone.bugpatterns.*;

import com.sun.source.tree.*;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans the parsed AST, looking for violations of any of the enabled checks.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorProneScanner extends Scanner {

  private Set<Class<? extends Annotation>> customSuppressionAnnotations =
      new HashSet<Class<? extends Annotation>>();

  /**
   * Selects which checks should be enabled when the compile is run.
   */
  public interface EnabledPredicate {
    boolean isEnabled(Class<? extends BugChecker> check, BugPattern annotation);

    /**
     * Selects all checks which are annotated with maturity = MATURE.
     */
    public static final EnabledPredicate DEFAULT_CHECKS = new EnabledPredicate() {
      @Override
      public boolean isEnabled(Class<? extends BugChecker> checkerClass, BugPattern annotation) {
        return annotation.maturity() == MATURE;
      }
    };
  }

  /**
   * Create a scanner that only enables a single matcher.
   */
  public static Scanner forMatcher(final Class<?> checkerClass) {
    return new ErrorProneScanner(new EnabledPredicate() {
      @Override
      public boolean isEnabled(Class<? extends BugChecker> check, BugPattern annotation) {
        return check.equals(checkerClass);
      }
    });
  }

  // TODO: discover all @BugPattern-annotated classes
  @SuppressWarnings("unchecked")
  private static final List<? extends Class<? extends BugChecker>> ALL_CHECKERS = Arrays.asList(
      SelfEquals.class,
      OrderingFrom.class,
      PreconditionsCheckNotNull.class,
      PreconditionsExpensiveString.class,
      PreconditionsCheckNotNullPrimitive.class,
      CollectionIncompatibleType.class,
      ArrayEquals.class,
      ArrayToString.class,
      ReturnValueIgnored.class,
      NonRuntimeAnnotation.class,
      InvalidPatternSyntax.class,
      ModifyingCollectionWithItself.class,
      PreconditionsTooManyArgs.class,
      CheckReturnValue.class,
      DeadException.class,
      InjectAssistedInjectAndInjectOnConstructors.class,
      InjectMoreThanOneQualifier.class,
      InjectMoreThanOneScopeAnnotationOnClass.class,
      InjectScopeAnnotationOnInterfaceOrAbstractClass.class,
      InjectOverlappingQualifierAndScopeAnnotation.class,
      FallThroughSuppression.class,
      SuppressWarningsDeprecated.class,
      InjectJavaxInjectOnAbstractMethod.class,
      EmptyIfStatement.class,
      EmptyStatement.class,
      NumericEquality.class,
      StringEquality.class,
      SelfEquality.class,
      BadShiftAmount.class,
      ArrayToStringConcatenation.class,
      ComparisonOutOfRange.class,
      SelfAssignment.class,
      GuiceAssistedParameters.class,
      CovariantEquals.class,
      JUnit3TestNotRun.class,
      JUnit4TestNotRun.class,
      TryFailThrowable.class,
      WrongParameterPackage.class,
      LongLiteralLowerCaseSuffix.class,
      UnneededConditionalOperator.class,
      ArrayToStringCompoundAssignment.class,
      InjectScopeOrQualifierAnnotationRetention.class,
      InjectInvalidTargetingOnScopingAnnotation.class,
      GuiceAssistedInjectScoping.class,
      GuiceOverridesGuiceInjectableMethod.class,
      GuiceOverridesJavaxInjectableMethod.class,
      InjectAssistedInjectAndInjectOnSameConstructor.class,
      InjectMoreThanOneInjectableConstructor.class,
      InjectJavaxInjectOnFinalField.class,
      GuiceInjectOnFinalField.class,
      ClassCanBeStatic.class,
      ElementsCountedInLoop.class,
      ProtoFieldNullComparison.class,
      WaitNotInLoop.class,
      DepAnn.class,
      DivZero.class,
      Overrides.class,
      Finally.class,
      StaticAccessedFromInstance.class
  );

  @SuppressWarnings("unchecked")
  public ErrorProneScanner(EnabledPredicate predicate) {
    try {
      int enabledCount = 0;
      for (final Class<? extends BugChecker> checkerClass: ALL_CHECKERS) {
        if (predicate.isEnabled(checkerClass, checkerClass.getAnnotation(BugPattern.class))) {
          BugChecker checker = checkerClass.newInstance();
          registerNodeTypes(checker);
          enabledCount++;
        }
      }
      if (enabledCount <= 0) {
        throw new IllegalStateException("ErrorProneScanner created with no enabled checks");
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not reflectively create error prone matchers", e);
    }
  }

  /**
   * Create an error-prone scanner for a non-hardcoded set of checkers.
   *
   * @param checkers The checkers that this scanner should use.
   */
  public ErrorProneScanner(BugChecker... checkers) {
    for (BugChecker checker : checkers) {
      registerNodeTypes(checker);
    }
  }

  @Override
  protected Set<Class<? extends Annotation>> getCustomSuppressionAnnotations() {
    return customSuppressionAnnotations;
  }

  private final List<AnnotationTreeMatcher> annotationMatchers =
      new ArrayList<AnnotationTreeMatcher>();
  private final List<ArrayAccessTreeMatcher> arrayAccessMatchers =
      new ArrayList<ArrayAccessTreeMatcher>();
  private final List<ArrayTypeTreeMatcher> arrayTypeMatchers =
      new ArrayList<ArrayTypeTreeMatcher>();
  private final List<AssertTreeMatcher> assertMatchers =
      new ArrayList<AssertTreeMatcher>();
  private final List<AssignmentTreeMatcher> assignmentMatchers =
      new ArrayList<AssignmentTreeMatcher>();
  private final List<BinaryTreeMatcher> binaryMatchers =
      new ArrayList<BinaryTreeMatcher>();
  private final List<BlockTreeMatcher> blockMatchers =
      new ArrayList<BlockTreeMatcher>();
  private final List<BreakTreeMatcher> breakMatchers =
      new ArrayList<BreakTreeMatcher>();
  private final List<CaseTreeMatcher> caseMatchers =
      new ArrayList<CaseTreeMatcher>();
  private final List<CatchTreeMatcher> catchMatchers =
      new ArrayList<CatchTreeMatcher>();
  private final List<ClassTreeMatcher> classMatchers =
      new ArrayList<ClassTreeMatcher>();
  private final List<CompilationUnitTreeMatcher> compilationUnitMatchers =
      new ArrayList<CompilationUnitTreeMatcher>();
  private final List<CompoundAssignmentTreeMatcher> compoundAssignmentMatchers =
      new ArrayList<CompoundAssignmentTreeMatcher>();
  private final List<ConditionalExpressionTreeMatcher> conditionalExpressionMatchers =
      new ArrayList<ConditionalExpressionTreeMatcher>();
  private final List<ContinueTreeMatcher> continueMatchers =
      new ArrayList<ContinueTreeMatcher>();
  private final List<DoWhileLoopTreeMatcher> doWhileLoopMatchers =
      new ArrayList<DoWhileLoopTreeMatcher>();
  private final List<EmptyStatementTreeMatcher> emptyStatementMatchers =
      new ArrayList<EmptyStatementTreeMatcher>();
  private final List<EnhancedForLoopTreeMatcher> enhancedForLoopMatchers =
      new ArrayList<EnhancedForLoopTreeMatcher>();
  private final List<ExpressionStatementTreeMatcher> expressionStatementMatchers =
      new ArrayList<ExpressionStatementTreeMatcher>();
  private final List<ForLoopTreeMatcher> forLoopMatchers =
      new ArrayList<ForLoopTreeMatcher>();
  private final List<IdentifierTreeMatcher> identifierMatchers =
      new ArrayList<IdentifierTreeMatcher>();
  private final List<IfTreeMatcher> ifMatchers =
      new ArrayList<IfTreeMatcher>();
  private final List<ImportTreeMatcher> importMatchers =
      new ArrayList<ImportTreeMatcher>();
  private final List<InstanceOfTreeMatcher> instanceOfMatchers =
      new ArrayList<InstanceOfTreeMatcher>();
  private final List<LabeledStatementTreeMatcher> labeledStatementMatchers =
      new ArrayList<LabeledStatementTreeMatcher>();
  private final List<LiteralTreeMatcher> literalMatchers =
      new ArrayList<LiteralTreeMatcher>();
  private final List<MemberSelectTreeMatcher> memberSelectMatchers =
      new ArrayList<MemberSelectTreeMatcher>();
  private final List<MethodTreeMatcher> methodMatchers =
      new ArrayList<MethodTreeMatcher>();
  private final List<MethodInvocationTreeMatcher> methodInvocationMatchers =
      new ArrayList<MethodInvocationTreeMatcher>();
  private final List<ModifiersTreeMatcher> modifiersMatchers =
      new ArrayList<ModifiersTreeMatcher>();
  private final List<NewArrayTreeMatcher> newArrayMatchers =
      new ArrayList<NewArrayTreeMatcher>();
  private final List<NewClassTreeMatcher> newClassMatchers =
      new ArrayList<NewClassTreeMatcher>();
  private final List<ParameterizedTypeTreeMatcher> parameterizedTypeMatchers =
      new ArrayList<ParameterizedTypeTreeMatcher>();
  private final List<ParenthesizedTreeMatcher> parenthesizedMatchers =
      new ArrayList<ParenthesizedTreeMatcher>();
  private final List<PrimitiveTypeTreeMatcher> primitiveTypeMatchers =
      new ArrayList<PrimitiveTypeTreeMatcher>();
  private final List<ReturnTreeMatcher> returnMatchers =
      new ArrayList<ReturnTreeMatcher>();
  private final List<SwitchTreeMatcher> switchMatchers =
      new ArrayList<SwitchTreeMatcher>();
  private final List<SynchronizedTreeMatcher> synchronizedMatchers =
      new ArrayList<SynchronizedTreeMatcher>();
  private final List<ThrowTreeMatcher> throwMatchers =
      new ArrayList<ThrowTreeMatcher>();
  private final List<TryTreeMatcher> tryMatchers =
      new ArrayList<TryTreeMatcher>();
  private final List<TypeCastTreeMatcher> typeCastMatchers =
      new ArrayList<TypeCastTreeMatcher>();
  private final List<TypeParameterTreeMatcher> typeParameterMatchers =
      new ArrayList<TypeParameterTreeMatcher>();
  private final List<UnaryTreeMatcher> unaryMatchers =
      new ArrayList<UnaryTreeMatcher>();
  private final List<VariableTreeMatcher> variableMatchers =
      new ArrayList<VariableTreeMatcher>();
  private final List<WhileLoopTreeMatcher> whileLoopMatchers =
      new ArrayList<WhileLoopTreeMatcher>();
  private final List<WildcardTreeMatcher> wildcardMatchers =
      new ArrayList<WildcardTreeMatcher>();

  private void registerNodeTypes(BugChecker checker) {
    if (checker.getSuppressibility() == Suppressibility.CUSTOM_ANNOTATION) {
      customSuppressionAnnotations.add(checker.getCustomSuppressionAnnotation());
    }

    if (checker instanceof AnnotationTreeMatcher) {
      annotationMatchers.add((AnnotationTreeMatcher) checker);
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
    if (checker instanceof LabeledStatementTreeMatcher) {
      labeledStatementMatchers.add((LabeledStatementTreeMatcher) checker);
    }
    if (checker instanceof LiteralTreeMatcher) {
      literalMatchers.add((LiteralTreeMatcher) checker);
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
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchAnnotation(tree, state), tree, state);
    }
    return super.visitAnnotation(tree, visitorState);
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ArrayAccessTreeMatcher matcher : arrayAccessMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchArrayAccess(tree, state), tree, state);
    }
    return super.visitArrayAccess(tree, visitorState);
  }

  @Override
  public Void visitArrayType(ArrayTypeTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ArrayTypeTreeMatcher matcher : arrayTypeMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchArrayType(tree, state), tree, state);
    }
    return super.visitArrayType(tree, visitorState);
  }

  @Override
  public Void visitAssert(AssertTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (AssertTreeMatcher matcher : assertMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchAssert(tree, state), tree, state);
    }
    return super.visitAssert(tree, visitorState);
  }

  @Override
  public Void visitAssignment(AssignmentTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (AssignmentTreeMatcher matcher : assignmentMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchAssignment(tree, state), tree, state);
    }
    return super.visitAssignment(tree, visitorState);
  }

  @Override
  public Void visitBinary(BinaryTree tree,  VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (BinaryTreeMatcher matcher : binaryMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchBinary(tree, state), tree, state);
    }
    return super.visitBinary(tree, state);
  }

  @Override
  public Void visitBlock(BlockTree tree,  VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (BlockTreeMatcher matcher : blockMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchBlock(tree, state), tree, state);
    }
    return super.visitBlock(tree, state);
  }

  @Override
  public Void visitBreak(BreakTree tree,  VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (BreakTreeMatcher matcher : breakMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchBreak(tree, state), tree, state);
    }
    return super.visitBreak(tree, state);
  }

  @Override
  public Void visitCase(CaseTree tree,  VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (CaseTreeMatcher matcher : caseMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchCase(tree, state), tree, state);
    }
    return super.visitCase(tree, state);
  }

  @Override
  public Void visitCatch(CatchTree tree,  VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (CatchTreeMatcher matcher : catchMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchCatch(tree, state), tree, state);
    }
    return super.visitCatch(tree, state);
  }

  @Override
  public Void visitClass(ClassTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ClassTreeMatcher matcher : classMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchClass(tree, state), tree, state);
    }
    return super.visitClass(tree, visitorState);
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (CompilationUnitTreeMatcher matcher : compilationUnitMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchCompilationUnit(tree, state), tree, state);
    }
    return super.visitCompilationUnit(tree, visitorState);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (CompoundAssignmentTreeMatcher matcher : compoundAssignmentMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchCompoundAssignment(tree, state), tree, state);
    }
    return super.visitCompoundAssignment(tree, visitorState);
  }

  @Override
  public Void visitConditionalExpression(
      ConditionalExpressionTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ConditionalExpressionTreeMatcher matcher : conditionalExpressionMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchConditionalExpression(tree, state), tree, state);
    }
    return super.visitConditionalExpression(tree, visitorState);
  }

  @Override
  public Void visitContinue(ContinueTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ContinueTreeMatcher matcher : continueMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchContinue(tree, state), tree, state);
    }
    return super.visitContinue(tree, visitorState);
  }

  @Override
  public Void visitDoWhileLoop(DoWhileLoopTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (DoWhileLoopTreeMatcher matcher : doWhileLoopMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchDoWhileLoop(tree, state), tree, state);
    }
    return super.visitDoWhileLoop(tree, visitorState);
  }

  @Override
  public Void visitEmptyStatement(EmptyStatementTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (EmptyStatementTreeMatcher matcher : emptyStatementMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchEmptyStatement(tree, state), tree, state);
    }
    return super.visitEmptyStatement(tree, visitorState);
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (EnhancedForLoopTreeMatcher matcher : enhancedForLoopMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchEnhancedForLoop(tree, state), tree, state);
    }
    return super.visitEnhancedForLoop(tree, visitorState);
  }

  // Intentionally skip visitErroneous -- we don't analyze malformed expressions.

  @Override
  public Void visitExpressionStatement(ExpressionStatementTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ExpressionStatementTreeMatcher matcher : expressionStatementMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchExpressionStatement(tree, state), tree, state);
    }
    return super.visitExpressionStatement(tree, visitorState);
  }

  @Override
  public Void visitForLoop(ForLoopTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ForLoopTreeMatcher matcher : forLoopMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchForLoop(tree, state), tree, state);
    }
    return super.visitForLoop(tree, visitorState);
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (IdentifierTreeMatcher matcher : identifierMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchIdentifier(tree, state), tree, state);
    }
    return super.visitIdentifier(tree, visitorState);
  }

  @Override
  public Void visitIf(IfTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (IfTreeMatcher matcher : ifMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchIf(tree, state), tree, state);
    }
    return super.visitIf(tree, visitorState);
  }

  @Override
  public Void visitImport(ImportTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ImportTreeMatcher matcher : importMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchImport(tree, state), tree, state);
    }
    return super.visitImport(tree, visitorState);
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (InstanceOfTreeMatcher matcher : instanceOfMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchInstanceOf(tree, state), tree, state);
    }
    return super.visitInstanceOf(tree, visitorState);
  }

  @Override
  public Void visitLabeledStatement(LabeledStatementTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (LabeledStatementTreeMatcher matcher : labeledStatementMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchLabeledStatement(tree, state), tree, state);
    }
    return super.visitLabeledStatement(tree, visitorState);
  }

  @Override
  public Void visitLiteral(LiteralTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (LiteralTreeMatcher matcher : literalMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchLiteral(tree, state), tree, state);
    }
    return super.visitLiteral(tree, visitorState);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (MemberSelectTreeMatcher matcher : memberSelectMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchMemberSelect(tree, state), tree, state);
    }
    return super.visitMemberSelect(tree, visitorState);
  }

  @Override
  public Void visitMethod(MethodTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (MethodTreeMatcher matcher : methodMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchMethod(tree, state), tree, state);
    }
    return super.visitMethod(tree, visitorState);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (MethodInvocationTreeMatcher matcher : methodInvocationMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchMethodInvocation(tree, state), tree, state);
    }
    return super.visitMethodInvocation(tree, state);
  }

  @Override
  public Void visitModifiers(ModifiersTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ModifiersTreeMatcher matcher : modifiersMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchModifiers(tree, state), tree, state);
    }
    return super.visitModifiers(tree, state);
  }

  @Override
  public Void visitNewArray(NewArrayTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (NewArrayTreeMatcher matcher : newArrayMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchNewArray(tree, state), tree, state);
    }
    return super.visitNewArray(tree, state);
  }

  @Override
  public Void visitNewClass(NewClassTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (NewClassTreeMatcher matcher : newClassMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchNewClass(tree, state), tree, state);
    }
    return super.visitNewClass(tree, visitorState);
  }

  // Intentionally skip visitOther. It seems to be used only for let expressions, which are
  // generated by javac to implement autoboxing. We are only interested in source-level constructs.

  @Override
  public Void visitParameterizedType(ParameterizedTypeTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ParameterizedTypeTreeMatcher matcher : parameterizedTypeMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchParameterizedType(tree, state), tree, state);
    }
    return super.visitParameterizedType(tree, visitorState);
  }

  @Override
  public Void visitParenthesized(ParenthesizedTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ParenthesizedTreeMatcher matcher : parenthesizedMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchParenthesized(tree, state), tree, state);
    }
    return super.visitParenthesized(tree, visitorState);
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (PrimitiveTypeTreeMatcher matcher : primitiveTypeMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchPrimitiveType(tree, state), tree, state);
    }
    return super.visitPrimitiveType(tree, visitorState);
  }

  @Override
  public Void visitReturn(ReturnTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ReturnTreeMatcher matcher : returnMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchReturn(tree, state), tree, state);
    }
    return super.visitReturn(tree, visitorState);
  }

  @Override
  public Void visitSwitch(SwitchTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (SwitchTreeMatcher matcher : switchMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchSwitch(tree, state), tree, state);
    }
    return super.visitSwitch(tree, visitorState);
  }

  @Override
  public Void visitSynchronized(SynchronizedTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (SynchronizedTreeMatcher matcher : synchronizedMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchSynchronized(tree, state), tree, state);
    }
    return super.visitSynchronized(tree, visitorState);
  }

  @Override
  public Void visitThrow(ThrowTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (ThrowTreeMatcher matcher : throwMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchThrow(tree, state), tree, state);
    }
    return super.visitThrow(tree, visitorState);
  }

  @Override
  public Void visitTry(TryTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (TryTreeMatcher matcher : tryMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchTry(tree, state), tree, state);
    }
    return super.visitTry(tree, visitorState);
  }

  @Override
  public Void visitTypeCast(TypeCastTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (TypeCastTreeMatcher matcher : typeCastMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchTypeCast(tree, state), tree, state);
    }
    return super.visitTypeCast(tree, visitorState);
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (TypeParameterTreeMatcher matcher : typeParameterMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchTypeParameter(tree, state), tree, state);
    }
    return super.visitTypeParameter(tree, visitorState);
  }

  @Override
  public Void visitUnary(UnaryTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (UnaryTreeMatcher matcher : unaryMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchUnary(tree, state), tree, state);
    }
    return super.visitUnary(tree, visitorState);
  }

  // Intentionally skip visitUnionType -- this is not available in Java 6.

  @Override
  public Void visitVariable(VariableTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (VariableTreeMatcher matcher : variableMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchVariable(tree, state), tree, state);
    }
    return super.visitVariable(tree, visitorState);
  }

  @Override
  public Void visitWhileLoop(WhileLoopTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (WhileLoopTreeMatcher matcher : whileLoopMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchWhileLoop(tree, state), tree, state);
    }
    return super.visitWhileLoop(tree, visitorState);
  }

  @Override
  public Void visitWildcard(WildcardTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (WildcardTreeMatcher matcher : wildcardMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchWildcard(tree, state), tree, state);
    }
    return super.visitWildcard(tree, visitorState);
  }
}
