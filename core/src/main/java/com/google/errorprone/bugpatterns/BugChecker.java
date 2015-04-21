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

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.BugPattern.Suppressibility;
import com.google.errorprone.BugPatternValidator;
import com.google.errorprone.ValidationException;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
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
import com.sun.source.tree.CompilationUnitTree;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.tools.JavaFileObject;

/**
 * A base class for implementing bug checkers. The {@code BugChecker} supplies a Scanner
 * implementation for this checker, making it easy to use a single checker.
 * Subclasses should also implement one or more of the {@code *Checker} interfaces in this class
 * to declare which tree node types to match against.
 *
 * @author Colin Decker
 * @author Eddie Aftandilian (eaftan@google.com)
 */
public abstract class BugChecker implements Suppressible, Serializable {

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
   * The default type of diagnostic (error or warning) to emit when this check triggers.
   */
  private final SeverityLevel defaultSeverity;

  /**
   * The maturity of this checker.  Used to decide whether to enable this check.  Corresponds to
   * the {@code maturity} attribute from its {@code BugPattern}.
   */
  private final MaturityLevel maturity;

  /**
   * The link URL to display in the diagnostic message when this check triggers.  Computed from
   * the {@code link} and {@code linkType} attributes from its {@code BugPattern}.  May be null
   * if no link should be displayed.
   */
  private final String linkUrl;

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
    maturity = pattern.maturity();
    defaultSeverity = pattern.severity();
    linkUrl = createLinkUrl(pattern);
    suppressibility = pattern.suppressibility();
    if (suppressibility == Suppressibility.CUSTOM_ANNOTATION) {
      customSuppressionAnnotation = pattern.customSuppressionAnnotation();
    } else {
      customSuppressionAnnotation = null;
    }
  }

  private static final String URL_FORMAT = "http://errorprone.info/bugpattern/%s";

  private static String createLinkUrl(BugPattern pattern) {
    switch (pattern.linkType()) {
      case AUTOGENERATED:
        return String.format(URL_FORMAT, pattern.name());
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
  @CheckReturnValue
  protected Description describeMatch(Tree node, Fix fix) {
    return buildDescription(node)
        .addFix(fix)
        .build();
  }

  /**
   * Helper to create a Description for the common case where there is no fix.
   */
  @CheckReturnValue
  protected Description describeMatch(Tree node) {
    return buildDescription(node).build();
  }

  /**
   * Returns a Description builder, which allows you to customize the diagnostic with a custom
   * message or multiple fixes.
   */
  @CheckReturnValue
  protected Description.Builder buildDescription(Tree node) {
    return buildDescriptionFromChecker(node, this);
  }

  /**
   * Returns a new builder for {@link Description}s.
   *
   * @param node the node where the error is
   * @param checker the {@code BugChecker} instance that is producing this {@code Description}
   */
  @CheckReturnValue
  public static Description.Builder buildDescriptionFromChecker(Tree node, BugChecker checker) {
    return Description.builder(
        Preconditions.checkNotNull(node),
        checker.canonicalName(),
        checker.linkUrl(),
        checker.defaultSeverity(),
        checker.message());
  }

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

  public MaturityLevel maturity() {
    return maturity;
  }

  public SeverityLevel defaultSeverity() {
    return defaultSeverity;
  }

  public SeverityLevel severity(Map<String, SeverityLevel> severities) {
    return firstNonNull(severities.get(canonicalName), defaultSeverity);
  }

  public String linkUrl() {
    return linkUrl;
  }

  @Override
  public Suppressibility suppressibility() {
    return suppressibility;
  }

  @Override
  public Class<? extends Annotation> customSuppressionAnnotation() {
    return customSuppressionAnnotation;
  }

  public static interface AnnotationTreeMatcher extends Suppressible {
    Description matchAnnotation(AnnotationTree tree, VisitorState state);
  }

  public static interface AnnotatedTypeTreeMatcher extends Suppressible {
    Description matchAnnotatedType(AnnotatedTypeTree tree, VisitorState state);
  }

  public static interface ArrayAccessTreeMatcher extends Suppressible {
    Description matchArrayAccess(ArrayAccessTree tree, VisitorState state);
  }

  public static interface ArrayTypeTreeMatcher extends Suppressible {
    Description matchArrayType(ArrayTypeTree tree, VisitorState state);
  }

  public static interface AssertTreeMatcher extends Suppressible {
    Description matchAssert(AssertTree tree, VisitorState state);
  }

  public static interface AssignmentTreeMatcher extends Suppressible {
    Description matchAssignment(AssignmentTree tree, VisitorState state);
  }

  public static interface BinaryTreeMatcher extends Suppressible {
    Description matchBinary(BinaryTree tree, VisitorState state);
  }

  public static interface BlockTreeMatcher extends Suppressible {
    Description matchBlock(BlockTree tree, VisitorState state);
  }

  public static interface BreakTreeMatcher extends Suppressible {
    Description matchBreak(BreakTree tree, VisitorState state);
  }

  public static interface CaseTreeMatcher extends Suppressible {
    Description matchCase(CaseTree tree, VisitorState state);
  }

  public static interface CatchTreeMatcher extends Suppressible {
    Description matchCatch(CatchTree tree, VisitorState state);
  }

  public static interface ClassTreeMatcher extends Suppressible {
    Description matchClass(ClassTree tree, VisitorState state);
  }

  /**
   * The information that is safe for a {@link CompilationUnitTreeMatcher} to access.
   *
   * <p>Error-prone does not support matching entire compilation unit trees, due to a limitation of
   * javac. Class declarations must be inspected one at a time via {@link ClassTreeMatcher}.
   *
   * <p>CAUTION: checks can still access the compilation unit tree using
   * {@link VisitorState#getPath()}, but the AST nodes for type declarations may be in an
   * inconsistent state.
   */
  @AutoValue
  public abstract static class CompilationUnitTreeInfo {

    /**
     * Information about the top-level types in a compilation unit.
     */
    @AutoValue
    public abstract static class DeclarationInfo {
      public abstract String name();
      public abstract Tree.Kind kind();

      public static DeclarationInfo create(String name, Tree.Kind kind) {
        return new AutoValue_BugChecker_CompilationUnitTreeInfo_DeclarationInfo(name, kind);
      }
    }

    /** Wrapper for {@link CompilationUnitTree#getPackageAnnotations()}. */
    public abstract List<? extends AnnotationTree> packageAnnotations();

    /** Wrapper for {@link CompilationUnitTree#getPackageName()}. */
    public abstract Optional<ExpressionTree> packageName();

    /** Wrapper for {@link CompilationUnitTree#getImports()}. */
    public abstract List<? extends ImportTree> imports();

    /** Wrapper for {@link CompilationUnitTree#getTypeDecls()}. */
    public abstract ImmutableList<DeclarationInfo> typeDeclarations();

    /** Wrapper for {@link CompilationUnitTree#getSourceFile()}. */
    public abstract JavaFileObject sourceFile();

    public static CompilationUnitTreeInfo create(final CompilationUnitTree node) {
      final ImmutableList.Builder<DeclarationInfo> members = ImmutableList.builder();
      for (Tree tree : node.getTypeDecls()) {
        if (tree instanceof ClassTree) {
          ClassTree classTree = (ClassTree) tree;
          members.add(
              DeclarationInfo.create(classTree.getSimpleName().toString(), classTree.getKind()));
        }
      }
      return new AutoValue_BugChecker_CompilationUnitTreeInfo(
          node.getPackageAnnotations(),
          Optional.fromNullable(node.getPackageName()),
          node.getImports(),
          members.build(),
          node.getSourceFile());
    }
  }

  public static interface CompilationUnitTreeMatcher extends Suppressible {
    Description matchCompilationUnit(CompilationUnitTreeInfo info, VisitorState state);
  }

  public static interface CompoundAssignmentTreeMatcher extends Suppressible {
    Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state);
  }

  public static interface ConditionalExpressionTreeMatcher
      extends Suppressible {
    Description matchConditionalExpression(ConditionalExpressionTree tree, VisitorState state);
  }

  public static interface ContinueTreeMatcher extends Suppressible {
    Description matchContinue(ContinueTree tree, VisitorState state);
  }

  public static interface DoWhileLoopTreeMatcher extends Suppressible {
    Description matchDoWhileLoop(DoWhileLoopTree tree, VisitorState state);
  }

  public static interface EmptyStatementTreeMatcher extends Suppressible {
    Description matchEmptyStatement(EmptyStatementTree tree, VisitorState state);
  }

  public static interface EnhancedForLoopTreeMatcher extends Suppressible {
    Description matchEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state);
  }

  // Intentionally skip ErroneousTreeMatcher -- we don't analyze malformed expressions.

  public static interface ExpressionStatementTreeMatcher extends Suppressible {
    Description matchExpressionStatement(ExpressionStatementTree tree, VisitorState state);
  }

  public static interface ForLoopTreeMatcher extends Suppressible {
    Description matchForLoop(ForLoopTree tree, VisitorState state);
  }

  public static interface IdentifierTreeMatcher extends Suppressible {
    Description matchIdentifier(IdentifierTree tree, VisitorState state);
  }

  public static interface IfTreeMatcher extends Suppressible {
    Description matchIf(IfTree tree, VisitorState state);
  }

  public static interface ImportTreeMatcher extends Suppressible {
    Description matchImport(ImportTree tree, VisitorState state);
  }

  public static interface InstanceOfTreeMatcher extends Suppressible {
    Description matchInstanceOf(InstanceOfTree tree, VisitorState state);
  }

  public static interface IntersectionTypeTreeMatcher extends Suppressible {
    Description matchIntersectionType(IntersectionTypeTree tree, VisitorState state);
  }

  public static interface LabeledStatementTreeMatcher extends Suppressible {
    Description matchLabeledStatement(LabeledStatementTree tree, VisitorState state);
  }

  public static interface LambdaExpressionTreeMatcher extends Suppressible {
    Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state);
  }

  public static interface LiteralTreeMatcher extends Suppressible {
    Description matchLiteral(LiteralTree tree, VisitorState state);
  }

  public static interface MemberReferenceTreeMatcher extends Suppressible {
    Description matchMemberReference(MemberReferenceTree tree, VisitorState state);
  }

  public static interface MemberSelectTreeMatcher extends Suppressible {
    Description matchMemberSelect(MemberSelectTree tree, VisitorState state);
  }

  public static interface MethodTreeMatcher extends Suppressible {
    Description matchMethod(MethodTree tree, VisitorState state);
  }

  public static interface MethodInvocationTreeMatcher extends Suppressible {
    Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state);
  }

  public static interface ModifiersTreeMatcher extends Suppressible {
    Description matchModifiers(ModifiersTree tree, VisitorState state);
  }

  public static interface NewArrayTreeMatcher extends Suppressible {
    Description matchNewArray(NewArrayTree tree, VisitorState state);
  }

  public static interface NewClassTreeMatcher extends Suppressible {
    Description matchNewClass(NewClassTree tree, VisitorState state);
  }

  // Intentionally skip OtherTreeMatcher. It seems to be used only for let expressions, which are
  // generated by javac to implement autoboxing. We are only interested in source-level constructs.

  public static interface ParameterizedTypeTreeMatcher extends Suppressible {
    Description matchParameterizedType(ParameterizedTypeTree tree, VisitorState state);
  }

  public static interface ParenthesizedTreeMatcher extends Suppressible {
    Description matchParenthesized(ParenthesizedTree tree, VisitorState state);
  }

  public static interface PrimitiveTypeTreeMatcher extends Suppressible {
    Description matchPrimitiveType(PrimitiveTypeTree tree, VisitorState state);
  }

  public static interface ReturnTreeMatcher extends Suppressible {
    Description matchReturn(ReturnTree tree, VisitorState state);
  }

  public static interface SwitchTreeMatcher extends Suppressible {
    Description matchSwitch(SwitchTree tree, VisitorState state);
  }

  public static interface SynchronizedTreeMatcher extends Suppressible {
    Description matchSynchronized(SynchronizedTree tree, VisitorState state);
  }

  public static interface ThrowTreeMatcher extends Suppressible {
    Description matchThrow(ThrowTree tree, VisitorState state);
  }

  public static interface TryTreeMatcher extends Suppressible {
    Description matchTry(TryTree tree, VisitorState state);
  }

  public static interface TypeCastTreeMatcher extends Suppressible {
    Description matchTypeCast(TypeCastTree tree, VisitorState state);
  }

  public static interface TypeParameterTreeMatcher extends Suppressible {
    Description matchTypeParameter(TypeParameterTree tree, VisitorState state);
  }

  public static interface UnaryTreeMatcher extends Suppressible {
    Description matchUnary(UnaryTree tree, VisitorState state);
  }

  public static interface UnionTypeTreeMatcher extends Suppressible {
    Description matchUnionType(UnionTypeTree tree, VisitorState state);
  }

  public static interface VariableTreeMatcher extends Suppressible {
    Description matchVariable(VariableTree tree, VisitorState state);
  }

  public static interface WhileLoopTreeMatcher extends Suppressible {
    Description matchWhileLoop(WhileLoopTree tree, VisitorState state);
  }

  public static interface WildcardTreeMatcher extends Suppressible {
    Description matchWildcard(WildcardTree tree, VisitorState state);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BugChecker)) {
      return false;
    }
    BugChecker that = (BugChecker) obj;
    return this.canonicalName().equals(that.canonicalName())
        && this.defaultSeverity().equals(that.defaultSeverity())
        && this.maturity().equals(that.maturity())
        && this.suppressibility().equals(that.suppressibility());
  }

  @Override
  public int hashCode() {
    return Objects.hash(canonicalName(), defaultSeverity(), maturity(), suppressibility());
  }
}
