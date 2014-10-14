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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
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
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
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

import java.io.Serializable;
import java.lang.annotation.Annotation;
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

  /**
   * The canonical name of this check.  Corresponds to the {@code name} attribute from its
   * {@code BugPattern} annotation.
   */
  private final String canonicalName;

  /**
   * Additional identifiers for this check, to be checked for in @SuppressWarnings annotations.
   * Corresponds to the canonical name plus all {@code altName}s from its {@code BugPattern}
   * annotation.
   */
  private final ImmutableSet<String> allNames;

  /**
   * The error message to print in compiler diagnostics when this check triggers.  Corresponds to
   * the {@code summary} attribute from its {@code BugPattern}.
   */
  private final String message;

  /**
   * The type of diagnostic (error or warning) to emit when this check triggers.  Corresponds to
   * the {@code severity} attribute from its {@code BugPattern}.
   */
  private final SeverityLevel severity;

  /**
   * The link URL to display in the diagnostic message when this check triggers.  Computed from
   * the {@code link} and {@code linkType} attributes from its {@code BugPattern}.  May be null
   * if no link should be displayed.
   */
  private final String linkUrl;

  /**
   * Whether this check may be disabled.  Corresponds to the {@code disableable} attribute from
   * its {@code BugPattern}.
   */
  private final boolean disableable;

  /**
   * Whether this check may be suppressed.  Corresponds to the {@code suppressibility} attribute
   * from its {@code BugPattern}.
   */
  private final Suppressibility suppressibility;

  /**
   * A custom suppression annotation for this check.  Computed from the {@code suppressibility} and
   * {@code customSuppressionAnnotation} attributes from its {@code BugPattern}.  May be null if
   * there is no custom suppression annotation for this check.
   */
  private final Class<? extends Annotation> customSuppressionAnnotation;

  public BugChecker() {
    BugPattern pattern = this.getClass().getAnnotation(BugPattern.class);
    try {
      BugPatternValidator.validate(pattern);
    } catch (ValidationException e) {
      throw new IllegalStateException(e);
    }

    canonicalName = pattern.name();
    allNames = ImmutableSet.<String>builder()
        .add(canonicalName)
        .add(pattern.altNames())
        .build();
    message = pattern.summary();
    severity = pattern.severity();
    linkUrl = createLinkUrl(pattern);
    disableable = pattern.disableable();
    suppressibility = pattern.suppressibility();
    if (suppressibility == Suppressibility.CUSTOM_ANNOTATION) {
      customSuppressionAnnotation = pattern.customSuppressionAnnotation();
    } else {
      customSuppressionAnnotation = null;
    }
  }

  private static String createLinkUrl(BugPattern pattern) {
    switch (pattern.linkType()) {
      case WIKI:
        return "http://code.google.com/p/error-prone/wiki/" + pattern.name();
      case CUSTOM:
        // annotation.link() must be provided.
        if (pattern.link().isEmpty()) {
          throw new IllegalStateException("If linkType element of @BugPattern is CUSTOM, "
              + "a link element must also be provided.");
        }
        return pattern.link();
      case NONE:
        return null;
      default:
        throw new IllegalStateException("Unexpected value for linkType element of @BugPattern: "
            + pattern.linkType());
    }
  }

  /**
   * Helper to create a Description for the common case where there is a fix.
   */
  protected Description describeMatch(Tree node, Fix fix) {
    return buildDescription(node)
        .addFix(fix)
        .build();
  }

  /**
   * Helper to create a Description for the common case where there is no fix.
   */
  protected Description describeMatch(Tree node) {
    return buildDescription(node).build();
  }

  /**
   * Returns a Description builder, which allows you to customize the diagnostic with a custom
   * message or multiple fixes.
   */
  protected Description.Builder buildDescription(Tree node) {
    return Description.builder(node, this);
  }

  @Override
  public String canonicalName() {
    return canonicalName;
  }

  @Override
  public Set<String> allNames() {
    return allNames;
  }

  public String message() {
    return message;
  }

  public SeverityLevel severity() {
    return severity;
  }

  public String linkUrl() {
    return linkUrl;
  }

  @Override
  public boolean disableable() {
    return disableable;
  }

  @Override
  public Suppressibility suppressibility() {
    return suppressibility;
  }

  @Override
  public Class<? extends Annotation> customSuppressionAnnotation() {
    return customSuppressionAnnotation;
  }

  public final Scanner createScanner() {
    return new ErrorProneScanner(this);
  }

  public static interface AnnotationTreeMatcher extends Suppressible, Disableable {
    Description matchAnnotation(AnnotationTree tree, VisitorState state);
  }

  public static interface AnnotatedTypeTreeMatcher extends Suppressible, Disableable {
    Description matchAnnotatedType(AnnotatedTypeTree tree, VisitorState state);
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

  public static interface IntersectionTypeTreeMatcher extends Suppressible, Disableable {
    Description matchIntersectionType(IntersectionTypeTree tree, VisitorState state);
  }

  public static interface LabeledStatementTreeMatcher extends Suppressible, Disableable {
    Description matchLabeledStatement(LabeledStatementTree tree, VisitorState state);
  }

  public static interface LambdaExpressionTreeMatcher extends Suppressible, Disableable {
    Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state);
  }

  public static interface LiteralTreeMatcher extends Suppressible, Disableable {
    Description matchLiteral(LiteralTree tree, VisitorState state);
  }

  public static interface MemberReferenceTreeMatcher extends Suppressible, Disableable {
    Description matchMemberReference(MemberReferenceTree tree, VisitorState state);
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

  public static interface UnionTypeTreeMatcher extends Suppressible, Disableable {
    Description matchUnionType(UnionTypeTree tree, VisitorState state);
  }

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
