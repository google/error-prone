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

import com.google.errorprone.bugpatterns.*;
import com.google.errorprone.matchers.DescribingMatcher;
import com.sun.source.tree.*;

import java.util.ArrayList;
import java.util.List;

import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

/**
 * Scans the parsed AST, looking for violations of any of the enabled checks.
 * @author Alex Eagle (alexeagle@google.com)
 */
public class ErrorProneScanner extends Scanner {

  /**
   * Selects which checks should be enabled when the compile is run.
   */
  public interface EnabledPredicate {
    boolean isEnabled(Class<? extends DescribingMatcher<?>> check, BugPattern annotation);

    /**
     * Selects all checks which are annotated with maturity = MATURE.
     */
    public static final EnabledPredicate DEFAULT_CHECKS = new EnabledPredicate() {
      @Override
      public boolean isEnabled(Class<? extends DescribingMatcher<?>> check, BugPattern annotation) {
        return annotation.maturity() == MATURE;
      }
    };
  }

  // Special case, needed until Eddie's refactoring to allow a single @BugPattern class to
  // match multiple tree nodes.
  private final Iterable<DescribingMatcher<Tree>> selfAssignmentMatchers;

  private final Iterable<DescribingMatcher<MethodInvocationTree>> methodInvocationMatchers;
  private final Iterable<DescribingMatcher<NewClassTree>> newClassMatchers;
  private final Iterable<DescribingMatcher<AnnotationTree>> annotationMatchers;
  private final Iterable<DescribingMatcher<EmptyStatementTree>> emptyStatementMatchers;
  private final Iterable<DescribingMatcher<AssignmentTree>> assignmentMatchers;
  private final Iterable<DescribingMatcher<VariableTree>> variableMatchers;
  private final Iterable<DescribingMatcher<MethodTree>> methodMatchers;
  private final Iterable<DescribingMatcher<LiteralTree>> literalMatchers;
  private final Iterable<DescribingMatcher<ConditionalExpressionTree>>
      conditionalExpressionMatchers;
  private final Iterable<DescribingMatcher<BinaryTree>> binaryExpressionMatchers;
  private final Iterable<DescribingMatcher<CompoundAssignmentTree>> compoundAssignmentMatchers;
  private final Iterable<DescribingMatcher<ClassTree>> classMatchers;

  @SuppressWarnings("unchecked")
  public ErrorProneScanner(EnabledPredicate enabled) {
    try {
      this.methodInvocationMatchers = createChecks(enabled,
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
          CheckReturnValue.class);
      this.newClassMatchers = createChecks(enabled, DeadException.class);
      this.annotationMatchers = createChecks(enabled,
          InjectAssistedInjectAndInjectOnConstructors.class,
          InjectMoreThanOneQualifier.class,
          InjectMoreThanOneScopeAnnotationOnClass.class,
          InjectScopeAnnotationOnInterfaceOrAbstractClass.class,
          FallThroughSuppression.class,
          SuppressWarningsDeprecated.class);
      this.emptyStatementMatchers = createChecks(enabled,
          EmptyIfStatement.class,
          EmptyStatement.class);
      this.binaryExpressionMatchers = createChecks(enabled,
          InvalidNumericEquality.class,
          InvalidStringEquality.class,
          SelfEquality.class,
          BadShiftAmount.class,
          ArrayToStringConcatenation.class,
          ComparisonOutOfRange.class);
      this.selfAssignmentMatchers = createChecks(enabled, SelfAssignment.class);
      this.assignmentMatchers = createChecks(enabled);
      this.variableMatchers = createChecks(enabled,
          GuiceAssistedParameters.class);
      this.methodMatchers = createChecks(enabled,
          CovariantEquals.class,
          JUnit4TestNotRun.class,
          WrongParameterPackage.class);
      this.literalMatchers = createChecks(enabled, LongLiteralLowerCaseSuffix.class);
      this.conditionalExpressionMatchers = createChecks(enabled, UnneededConditionalOperator.class);
      this.compoundAssignmentMatchers = createChecks(enabled, ArrayToStringCompoundAssignment.class);
      this.classMatchers = createChecks(enabled,
          InjectScopeOrQualifierAnnotationRetention.class,
          InjectInvalidTargetingOnScopingAnnotation.class,
          GuiceAssistedInjectScoping.class);
    } catch (Exception e) {
      throw new RuntimeException("Could not reflectively create error prone matchers", e);
    }
  }

  /**
   * Create a scanner that only enables a single matcher. Useful for testing.
   */
  public static Scanner forMatcher(final Class<?> matcherClass) {
    return new ErrorProneScanner(new EnabledPredicate() {
      @Override
      public boolean isEnabled(Class<? extends DescribingMatcher<?>> check, BugPattern unused) {
        return check.equals(matcherClass);
      }
    });
  }

  private static <T extends Tree> Iterable<DescribingMatcher<T>> createChecks(
      EnabledPredicate predicate, Class<? extends DescribingMatcher<T>>... matchers)
      throws IllegalAccessException, InstantiationException {
    List<DescribingMatcher<T>> result = new ArrayList<DescribingMatcher<T>>();
    for (Class<? extends DescribingMatcher<T>> matcher : matchers) {
      if (predicate.isEnabled(matcher, matcher.getAnnotation(BugPattern.class))) {
        result.add(matcher.newInstance());
      }
    }
    return result;
  }

  @Override
  public Void visitMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    for (DescribingMatcher<MethodInvocationTree> matcher : methodInvocationMatchers) {
      evaluateMatch(methodInvocationTree, state, matcher);
    }
    return super.visitMethodInvocation(methodInvocationTree, state);
  }

  @Override
  public Void visitBinary(BinaryTree binaryExpressionTree,  VisitorState state) {
    for (DescribingMatcher<BinaryTree> matcher : binaryExpressionMatchers) {
      evaluateMatch(binaryExpressionTree, state, matcher);
    }
    return super.visitBinary(binaryExpressionTree, state);
  }

  @Override
  public Void visitNewClass(NewClassTree newClassTree, VisitorState visitorState) {
    for (DescribingMatcher<NewClassTree> matcher : newClassMatchers) {
      evaluateMatch(newClassTree, visitorState, matcher);
    }
    return super.visitNewClass(newClassTree, visitorState);
  }

  @Override
  public Void visitAnnotation(AnnotationTree annotationTree, VisitorState visitorState) {
    for (DescribingMatcher<AnnotationTree> matcher : annotationMatchers) {
      evaluateMatch(annotationTree, visitorState, matcher);
    }
    return super.visitAnnotation(annotationTree, visitorState);
  }

  @Override
  public Void visitEmptyStatement(
      EmptyStatementTree emptyStatementTree, VisitorState visitorState) {
    for (DescribingMatcher<EmptyStatementTree> matcher : emptyStatementMatchers) {
      evaluateMatch(emptyStatementTree, visitorState, matcher);
    }
    return super.visitEmptyStatement(emptyStatementTree, visitorState);
  }

  @Override
  public Void visitAssignment(AssignmentTree assignmentTree, VisitorState visitorState) {
    for (DescribingMatcher<AssignmentTree> matcher : assignmentMatchers) {
      evaluateMatch(assignmentTree, visitorState, matcher);
    }
    for (DescribingMatcher<Tree> selfAssignmentMatcher : selfAssignmentMatchers) {
      evaluateMatch(assignmentTree, visitorState, selfAssignmentMatcher);
    }
    return super.visitAssignment(assignmentTree, visitorState);
  }
  
  @Override
  public Void visitVariable(VariableTree variableTree, VisitorState visitorState) {
    for (DescribingMatcher<VariableTree> matcher : variableMatchers) {
      evaluateMatch(variableTree, visitorState, matcher);
    }
    for (DescribingMatcher<Tree> selfAssignmentMatcher : selfAssignmentMatchers) {
      evaluateMatch(variableTree, visitorState, selfAssignmentMatcher);
    }
    return super.visitVariable(variableTree, visitorState);
  }

  @Override
  public Void visitMethod(MethodTree node, VisitorState visitorState) {
    for (DescribingMatcher<MethodTree> matcher : methodMatchers) {
      evaluateMatch(node, visitorState, matcher);
    }
    return super.visitMethod(node, visitorState);
  }

  @Override
  public Void visitLiteral(LiteralTree literalTree, VisitorState visitorState) {
    for (DescribingMatcher<LiteralTree> matcher : literalMatchers) {
      evaluateMatch(literalTree, visitorState, matcher);
    }
    return super.visitLiteral(literalTree, visitorState);
  }

  @Override
  public Void visitConditionalExpression(
      ConditionalExpressionTree conditionalExpressionTree, VisitorState visitorState) {
    for (DescribingMatcher<ConditionalExpressionTree> matcher : conditionalExpressionMatchers) {
      evaluateMatch(conditionalExpressionTree, visitorState, matcher);
    }
    return super.visitConditionalExpression(conditionalExpressionTree, visitorState);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree node, VisitorState visitorState) {
    for (DescribingMatcher<CompoundAssignmentTree> compoundAssignmentMatcher : compoundAssignmentMatchers) {
      evaluateMatch(node, visitorState, compoundAssignmentMatcher);
    }
    return super.visitCompoundAssignment(node, visitorState);
  }

  @Override
  public Void visitClass(ClassTree node, VisitorState visitorState) {
    for (DescribingMatcher<ClassTree> classMatcher : classMatchers) {
      evaluateMatch(node, visitorState, classMatcher);
    }
    return super.visitClass(node, visitorState);
  }
}
