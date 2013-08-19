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

import com.google.errorprone.bugpatterns.*;
import com.sun.source.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scans the parsed AST, looking for violations of any of the enabled checks.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorProneScanner extends Scanner {

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
      FallThroughSuppression.class,
      SuppressWarningsDeprecated.class,
      EmptyIfStatement.class,
      EmptyStatement.class,
      InvalidNumericEquality.class,
      InvalidStringEquality.class,
      SelfEquality.class,
      BadShiftAmount.class,
      ArrayToStringConcatenation.class,
      ComparisonOutOfRange.class,
      SelfAssignment.class,
      GuiceAssistedParameters.class,
      CovariantEquals.class,
      JUnit4TestNotRun.class,
      WrongParameterPackage.class,
      LongLiteralLowerCaseSuffix.class,
      UnneededConditionalOperator.class,
      ArrayToStringCompoundAssignment.class,
      InjectScopeOrQualifierAnnotationRetention.class,
      InjectInvalidTargetingOnScopingAnnotation.class,
      GuiceAssistedInjectScoping.class,
      InjectJavaxInjectOnFinalField.class,
      GuiceInjectOnFinalField.class
  );

  @SuppressWarnings("unchecked")
  public ErrorProneScanner(EnabledPredicate predicate) {
    try {
      for (final Class<? extends BugChecker> checkerClass: ALL_CHECKERS) {
        if (predicate.isEnabled(checkerClass, checkerClass.getAnnotation(BugPattern.class))) {
          BugChecker checker = checkerClass.newInstance();
          registerNodeTypes(checker);
        }
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

  private final List<MethodInvocationTreeMatcher> methodInvocationMatchers =
      new ArrayList<MethodInvocationTreeMatcher>();
  private final List<NewClassTreeMatcher> newClassMatchers =
      new ArrayList<NewClassTreeMatcher>();
  private final List<AnnotationTreeMatcher> annotationMatchers =
      new ArrayList<AnnotationTreeMatcher>();
  private final List<EmptyStatementTreeMatcher> emptyStatementMatchers =
      new ArrayList<EmptyStatementTreeMatcher>();
  private final List<AssignmentTreeMatcher> assignmentMatchers =
      new ArrayList<AssignmentTreeMatcher>();
  private final List<VariableTreeMatcher> variableMatchers =
      new ArrayList<VariableTreeMatcher>();
  private final List<MethodTreeMatcher> methodMatchers =
      new ArrayList<MethodTreeMatcher>();
  private final List<LiteralTreeMatcher> literalMatchers =
      new ArrayList<LiteralTreeMatcher>();
  private final List<ConditionalExpressionTreeMatcher> conditionalExpressionMatchers =
      new ArrayList<ConditionalExpressionTreeMatcher>();
  private final List<BinaryTreeMatcher> binaryExpressionMatchers =
      new ArrayList<BinaryTreeMatcher>();
  private final List<CompoundAssignmentTreeMatcher> compoundAssignmentMatchers =
      new ArrayList<CompoundAssignmentTreeMatcher>();
  private final List<ClassTreeMatcher> classMatchers =
      new ArrayList<ClassTreeMatcher>();

  private void registerNodeTypes(BugChecker checker) {
    if (checker instanceof MethodInvocationTreeMatcher)
      this.methodInvocationMatchers.add((MethodInvocationTreeMatcher) checker);
    if (checker instanceof NewClassTreeMatcher)
      this.newClassMatchers.add((NewClassTreeMatcher) checker);
    if (checker instanceof AnnotationTreeMatcher)
      this.annotationMatchers.add((AnnotationTreeMatcher) checker);
    if (checker instanceof EmptyStatementTreeMatcher)
      this.emptyStatementMatchers.add((EmptyStatementTreeMatcher) checker);
    if (checker instanceof AssignmentTreeMatcher)
      this.assignmentMatchers.add((AssignmentTreeMatcher) checker);
    if (checker instanceof VariableTreeMatcher)
      this.variableMatchers.add((VariableTreeMatcher) checker);
    if (checker instanceof MethodTreeMatcher)
      this.methodMatchers.add((MethodTreeMatcher) checker);
    if (checker instanceof LiteralTreeMatcher)
      this.literalMatchers.add((LiteralTreeMatcher) checker);
    if (checker instanceof ConditionalExpressionTreeMatcher)
      this.conditionalExpressionMatchers.add((ConditionalExpressionTreeMatcher) checker);
    if (checker instanceof BinaryTreeMatcher)
      this.binaryExpressionMatchers.add((BinaryTreeMatcher) checker);
    if (checker instanceof CompoundAssignmentTreeMatcher)
      this.compoundAssignmentMatchers.add((CompoundAssignmentTreeMatcher) checker);
    if (checker instanceof ClassTreeMatcher)
      this.classMatchers.add((ClassTreeMatcher) checker);
  }

  @Override
  public Void visitMethodInvocation(
      MethodInvocationTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (MethodInvocationTreeMatcher matcher : methodInvocationMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchMethodInvocation(tree, state), tree, state);
    }
    return super.visitMethodInvocation(tree, state);
  }

  @Override
  public Void visitBinary(BinaryTree tree,  VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (BinaryTreeMatcher matcher : binaryExpressionMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchBinary(tree, state), tree, state);
    }
    return super.visitBinary(tree, state);
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
  public Void visitEmptyStatement(
      EmptyStatementTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (EmptyStatementTreeMatcher matcher : emptyStatementMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchEmptyStatement(tree, state), tree, state);
    }
    return super.visitEmptyStatement(tree, visitorState);
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
  public Void visitVariable(VariableTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (VariableTreeMatcher matcher : variableMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchVariable(tree, state), tree, state);
    }
    return super.visitVariable(tree, visitorState);
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
  public Void visitLiteral(LiteralTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (LiteralTreeMatcher matcher : literalMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchLiteral(tree, state), tree, state);
    }
    return super.visitLiteral(tree, visitorState);
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
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, VisitorState visitorState) {
    VisitorState state = visitorState.withPath(getCurrentPath());
    for (CompoundAssignmentTreeMatcher matcher : compoundAssignmentMatchers) {
      if (isSuppressed(matcher)) continue;
      reportMatch(matcher.matchCompoundAssignment(tree, state), tree, state);
    }
    return super.visitCompoundAssignment(tree, visitorState);
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
}
