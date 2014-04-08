/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Suppressibility;
import com.google.errorprone.BugPatternValidator;
import com.google.errorprone.ErrorProneScanner;
import com.google.errorprone.Scanner;
import com.google.errorprone.ValidationException;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Disableable;
import com.google.errorprone.matchers.Suppressible;

import com.sun.source.tree.*;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A base class for implementing bug checkers. The {@code BugChecker} supplies a Scanner
 * implementation for this checker, making it easy to use a single checker.
 * Subclasses should also implement one or more of the {@code *Checker} interfaces in this class
 * to declare which tree node types to match against.
 *
 * @author Colin Decker
 * @author Eddie Aftandilian (eaftan@google.com)
 */
public abstract class BugChecker implements Suppressible, Disableable, Serializable {

  protected final String canonicalName;
  /**
   * A collection of IDs for this check, to be checked for in @SuppressWarnings annotations.
   */
  protected final Set<String> allNames;
  protected final BugPattern pattern;
  protected final boolean disableable;
  protected final BugPattern.Suppressibility suppressibility;
  protected final Class<? extends Annotation> customSuppressionAnnotation;

  public BugChecker() {
    pattern = this.getClass().getAnnotation(BugPattern.class);
    try {
      BugPatternValidator.validate(pattern);
    } catch (ValidationException e) {
      throw new IllegalStateException(e);
    }
    canonicalName = pattern.name();
    allNames = new HashSet<String>();
    allNames.add(canonicalName);
    allNames.addAll(Arrays.asList(pattern.altNames()));
    disableable = pattern.disableable();
    suppressibility = pattern.suppressibility();
    if (suppressibility == Suppressibility.CUSTOM_ANNOTATION) {
      customSuppressionAnnotation = pattern.customSuppressionAnnotation();
    } else {
      customSuppressionAnnotation = null;
    }
  }

  /**
   * Helper to create a Description for the common case where the diagnostic message is not
   * parameterized.
   */
  protected Description describeMatch(Tree node, Fix fix) {
    return new Description(node, getDiagnosticMessage(), fix, pattern.severity());
  }

  /**
   * Generate the compiler diagnostic message based on information in the @BugPattern annotation.
   *
   * <p>If the formatSummary element of the annotation has been set, then use format string
   * substitution to generate the message.  Otherwise, just use the summary element directly.
   *
   * @param args Arguments referenced by the format specifiers in the annotation's formatSummary
   *     element
   * @return The compiler diagnostic message.
   */
  protected String getDiagnosticMessage(Object... args) {
    String summary;
    if (!pattern.formatSummary().isEmpty()) {
      if (args.length == 0) {
        throw new IllegalStateException("Compiler error message expects a format string, but "
            + "no arguments were provided");
      }
      summary = String.format(pattern.formatSummary(), args);
    } else {
      summary = pattern.summary();
    }
    return "[" + pattern.name() + "] " + summary + getLink();
  }

  /**
   * Construct the link text to include in the compiler error message.
   */
  private String getLink() {
    switch (pattern.linkType()) {
      case WIKI:
        return "\n  (see http://code.google.com/p/error-prone/wiki/" + pattern.name() + ")";
      case CUSTOM:
        // annotation.link() must be provided.
        if (pattern.link().isEmpty()) {
          throw new IllegalStateException("If linkType element of @BugPattern is CUSTOM, "
              + "a link element must also be provided.");
        }
        return  "\n  (see " + pattern.link() + ")";
      case NONE:
        return "";
      default:
        throw new IllegalStateException("Unexpected value for linkType element of @BugPattern: "
            + pattern.linkType());
    }
  }

  @Override
  public String getCanonicalName() {
    return canonicalName;
  }

  @Override
  public Set<String> getAllNames() {
    return allNames;
  }

  @Override
  public boolean isDisableable() {
    return disableable;
  }

  @Override
  public BugPattern.Suppressibility getSuppressibility() {
    return suppressibility;
  }

  @Override
  public Class<? extends Annotation> getCustomSuppressionAnnotation() {
    return customSuppressionAnnotation;
  }

  public final Scanner createScanner() {
    return ErrorProneScanner.forMatcher(this.getClass());
  }

  public static interface AnnotationTreeMatcher extends Suppressible, Disableable {
    Description matchAnnotation(AnnotationTree tree, VisitorState state);
  }

  public static interface ArrayAccessTreeMatcher extends Suppressible, Disableable {
    Description matchArrayAccess(ArrayAccessTree tree, VisitorState state);
  }

  public static interface ArrayTypeTreeMatcher extends Suppressible, Disableable {
    Description matchArrayType(ArrayTypeTree tree, VisitorState state);
  }

  public static interface AssertTreeMatcher extends Suppressible, Disableable {
    Description matchAssert(AssertTree tree, VisitorState state);
  }

  public static interface AssignmentTreeMatcher extends Suppressible, Disableable {
    Description matchAssignment(AssignmentTree tree, VisitorState state);
  }

  public static interface BinaryTreeMatcher extends Suppressible, Disableable {
    Description matchBinary(BinaryTree tree, VisitorState state);
  }

  public static interface BlockTreeMatcher extends Suppressible, Disableable {
    Description matchBlock(BlockTree tree, VisitorState state);
  }

  public static interface BreakTreeMatcher extends Suppressible, Disableable {
    Description matchBreak(BreakTree tree, VisitorState state);
  }

  public static interface CaseTreeMatcher extends Suppressible, Disableable {
    Description matchCase(CaseTree tree, VisitorState state);
  }

  public static interface CatchTreeMatcher extends Suppressible, Disableable {
    Description matchCatch(CatchTree tree, VisitorState state);
  }

  public static interface ClassTreeMatcher extends Suppressible, Disableable {
    Description matchClass(ClassTree tree, VisitorState state);
  }

  /**
   * Error-prone does not support matching entire compilation unit trees, due to a limitation of
   * javac. Class declarations must be inspected one at a time via {@link ClassTreeMatcher}.
   */
  public static interface CompilationUnitTreeMatcher extends Suppressible, Disableable {
    Description matchCompilationUnit(
        List<? extends AnnotationTree> packageAnnotations,
        ExpressionTree packageName,
        List<? extends ImportTree> imports,
        VisitorState state);
  }

  public static interface CompoundAssignmentTreeMatcher extends Suppressible, Disableable {
    Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state);
  }

  public static interface ConditionalExpressionTreeMatcher
      extends Suppressible, Disableable {
    Description matchConditionalExpression(ConditionalExpressionTree tree, VisitorState state);
  }

  public static interface ContinueTreeMatcher extends Suppressible, Disableable {
    Description matchContinue(ContinueTree tree, VisitorState state);
  }

  public static interface DoWhileLoopTreeMatcher extends Suppressible, Disableable {
    Description matchDoWhileLoop(DoWhileLoopTree tree, VisitorState state);
  }

  public static interface EmptyStatementTreeMatcher extends Suppressible, Disableable {
    Description matchEmptyStatement(EmptyStatementTree tree, VisitorState state);
  }

  public static interface EnhancedForLoopTreeMatcher extends Suppressible, Disableable {
    Description matchEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state);
  }

  // Intentionally skip ErroneousTreeMatcher -- we don't analyze malformed expressions.

  public static interface ExpressionStatementTreeMatcher extends Suppressible, Disableable {
    Description matchExpressionStatement(ExpressionStatementTree tree, VisitorState state);
  }

  public static interface ForLoopTreeMatcher extends Suppressible, Disableable {
    Description matchForLoop(ForLoopTree tree, VisitorState state);
  }

  public static interface IdentifierTreeMatcher extends Suppressible, Disableable {
    Description matchIdentifier(IdentifierTree tree, VisitorState state);
  }

  public static interface IfTreeMatcher extends Suppressible, Disableable {
    Description matchIf(IfTree tree, VisitorState state);
  }

  public static interface ImportTreeMatcher extends Suppressible, Disableable {
    Description matchImport(ImportTree tree, VisitorState state);
  }

  public static interface InstanceOfTreeMatcher extends Suppressible, Disableable {
    Description matchInstanceOf(InstanceOfTree tree, VisitorState state);
  }

  public static interface LabeledStatementTreeMatcher extends Suppressible, Disableable {
    Description matchLabeledStatement(LabeledStatementTree tree, VisitorState state);
  }

  public static interface LiteralTreeMatcher extends Suppressible, Disableable {
    Description matchLiteral(LiteralTree tree, VisitorState state);
  }

  public static interface MemberSelectTreeMatcher extends Suppressible, Disableable {
    Description matchMemberSelect(MemberSelectTree tree, VisitorState state);
  }

  public static interface MethodTreeMatcher extends Suppressible, Disableable {
    Description matchMethod(MethodTree tree, VisitorState state);
  }

  public static interface MethodInvocationTreeMatcher extends Suppressible, Disableable {
    Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state);
  }

  public static interface ModifiersTreeMatcher extends Suppressible, Disableable {
    Description matchModifiers(ModifiersTree tree, VisitorState state);
  }

  public static interface NewArrayTreeMatcher extends Suppressible, Disableable {
    Description matchNewArray(NewArrayTree tree, VisitorState state);
  }

  public static interface NewClassTreeMatcher extends Suppressible, Disableable {
    Description matchNewClass(NewClassTree tree, VisitorState state);
  }

  // Intentionally skip OtherTreeMatcher. It seems to be used only for let expressions, which are
  // generated by javac to implement autoboxing. We are only interested in source-level constructs.

  public static interface ParameterizedTypeTreeMatcher extends Suppressible, Disableable {
    Description matchParameterizedType(ParameterizedTypeTree tree, VisitorState state);
  }

  public static interface ParenthesizedTreeMatcher extends Suppressible, Disableable {
    Description matchParenthesized(ParenthesizedTree tree, VisitorState state);
  }

  public static interface PrimitiveTypeTreeMatcher extends Suppressible, Disableable {
    Description matchPrimitiveType(PrimitiveTypeTree tree, VisitorState state);
  }

  public static interface ReturnTreeMatcher extends Suppressible, Disableable {
    Description matchReturn(ReturnTree tree, VisitorState state);
  }

  public static interface SwitchTreeMatcher extends Suppressible, Disableable {
    Description matchSwitch(SwitchTree tree, VisitorState state);
  }

  public static interface SynchronizedTreeMatcher extends Suppressible, Disableable {
    Description matchSynchronized(SynchronizedTree tree, VisitorState state);
  }

  public static interface ThrowTreeMatcher extends Suppressible, Disableable {
    Description matchThrow(ThrowTree tree, VisitorState state);
  }

  public static interface TryTreeMatcher extends Suppressible, Disableable {
    Description matchTry(TryTree tree, VisitorState state);
  }

  public static interface TypeCastTreeMatcher extends Suppressible, Disableable {
    Description matchTypeCast(TypeCastTree tree, VisitorState state);
  }

  public static interface TypeParameterTreeMatcher extends Suppressible, Disableable {
    Description matchTypeParameter(TypeParameterTree tree, VisitorState state);
  }

  public static interface UnaryTreeMatcher extends Suppressible, Disableable {
    Description matchUnary(UnaryTree tree, VisitorState state);
  }

  // Intentionally skip UnionTypeTreeMatcher -- this is not available in Java 6.

  public static interface VariableTreeMatcher extends Suppressible, Disableable {
    Description matchVariable(VariableTree tree, VisitorState state);
  }

  public static interface WhileLoopTreeMatcher extends Suppressible, Disableable {
    Description matchWhileLoop(WhileLoopTree tree, VisitorState state);
  }

  public static interface WildcardTreeMatcher extends Suppressible, Disableable {
    Description matchWildcard(WildcardTree tree, VisitorState state);
  }
}
