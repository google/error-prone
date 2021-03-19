/*
 * Copyright 2013 The Error Prone Authors.
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
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.suppliers.Supplier;
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
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Name;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * A base class for implementing bug checkers. The {@code BugChecker} supplies a Scanner
 * implementation for this checker, making it easy to use a single checker. Subclasses should also
 * implement one or more of the {@code *Matcher} interfaces in this class to declare which tree node
 * types to match against.
 *
 * @author Colin Decker
 * @author Eddie Aftandilian (eaftan@google.com)
 */
public abstract class BugChecker implements Suppressible, Serializable {
  private final BugCheckerInfo info;
  private final BiPredicate<Set<? extends Name>, VisitorState> checkSuppression;

  public BugChecker() {
    info = BugCheckerInfo.create(getClass());
    checkSuppression = suppressionPredicate(info.customSuppressionAnnotations());
  }

  private static BiPredicate<Set<? extends Name>, VisitorState> suppressionPredicate(
      Set<Class<? extends Annotation>> suppressionClasses) {
    switch (suppressionClasses.size()) {
      case 0:
        return (annos, state) -> false;
      case 1:
        {
          Supplier<Name> self =
              VisitorState.memoize(
                  state -> state.getName(Iterables.getOnlyElement(suppressionClasses).getName()));
          return (annos, state) -> annos.contains(self.get(state));
        }
      default:
        {
          Supplier<Set<? extends Name>> self =
              VisitorState.memoize(
                  state ->
                      suppressionClasses.stream()
                          .map(Class::getName)
                          .map(state::getName)
                          .collect(toImmutableSet()));
          return (annos, state) -> !Collections.disjoint(self.get(state), annos);
        }
    }
  }

  /** Helper to create a Description for the common case where there is a fix. */
  @CheckReturnValue
  protected Description describeMatch(Tree node, Fix fix) {
    return buildDescription(node).addFix(fix).build();
  }

  /** Helper to create a Description for the common case where there is a fix. */
  @CheckReturnValue
  protected Description describeMatch(JCTree node, Fix fix) {
    return describeMatch((Tree) node, fix);
  }

  /** Helper to create a Description for the common case where there is a fix. */
  @CheckReturnValue
  protected Description describeMatch(DiagnosticPosition position, Fix fix) {
    return buildDescription(position).addFix(fix).build();
  }

  /** Helper to create a Description for the common case where there is no fix. */
  @CheckReturnValue
  protected Description describeMatch(Tree node) {
    return buildDescription(node).build();
  }

  /** Helper to create a Description for the common case where there is an {@link Optional} fix. */
  @CheckReturnValue
  protected Description describeMatch(Tree node, Optional<? extends Fix> fix) {
    return buildDescription(node).addFix(fix).build();
  }

  /** Helper to create a Description for the common case where there is an {@link Optional} fix. */
  @CheckReturnValue
  protected Description describeMatch(DiagnosticPosition position, Optional<? extends Fix> fix) {
    return buildDescription(position).addFix(fix).build();
  }

  /**
   * Returns a Description builder, which allows you to customize the diagnostic with a custom
   * message or multiple fixes.
   */
  @CheckReturnValue
  public Description.Builder buildDescription(Tree node) {
    return Description.builder(node, canonicalName(), linkUrl(), defaultSeverity(), message());
  }

  /**
   * Returns a Description builder, which allows you to customize the diagnostic with a custom
   * message or multiple fixes.
   */
  @CheckReturnValue
  public Description.Builder buildDescription(DiagnosticPosition position) {
    return Description.builder(position, canonicalName(), linkUrl(), defaultSeverity(), message());
  }

  /**
   * Returns a Description builder, which allows you to customize the diagnostic with a custom
   * message or multiple fixes.
   */
  // This overload exists purely to disambiguate for JCTree.
  @CheckReturnValue
  public Description.Builder buildDescription(JCTree tree) {
    return Description.builder(tree, canonicalName(), linkUrl(), defaultSeverity(), message());
  }

  @Deprecated
  public static Description.Builder buildDescriptionFromChecker(Tree node, BugChecker checker) {
    return Description.builder(
        Preconditions.checkNotNull(node),
        checker.canonicalName(),
        checker.linkUrl(),
        checker.defaultSeverity(),
        checker.message());
  }

  @Override
  public String canonicalName() {
    return info.canonicalName();
  }

  @Override
  public Set<String> allNames() {
    return info.allNames();
  }

  public String message() {
    return info.message();
  }

  public SeverityLevel defaultSeverity() {
    return info.defaultSeverity();
  }

  public SeverityLevel severity(Map<String, SeverityLevel> severities) {
    return firstNonNull(severities.get(canonicalName()), defaultSeverity());
  }

  public String linkUrl() {
    return info.linkUrl();
  }

  @Override
  public boolean supportsSuppressWarnings() {
    return info.supportsSuppressWarnings();
  }

  public boolean disableable() {
    return info.disableable();
  }

  @Override
  public Set<Class<? extends Annotation>> customSuppressionAnnotations() {
    return info.customSuppressionAnnotations();
  }

  @Override
  public boolean suppressedByAnyOf(Set<Name> annotations, VisitorState s) {
    return checkSuppression.test(annotations, s);
  }

  @Override
  public boolean inspectGeneratedCode() {
    return info.inspectGeneratedCode();
  }

  /**
   * Returns true if the given tree is annotated with a {@code @SuppressWarnings} that disables this
   * bug checker.
   */
  public boolean isSuppressed(Tree tree) {
    return isSuppressed(ASTHelpers.getAnnotation(tree, SuppressWarnings.class));
  }

  /**
   * Returns true if the given symbol is annotated with a {@code @SuppressWarnings} that disables
   * this bug checker.
   */
  public boolean isSuppressed(Symbol symbol) {
    return isSuppressed(ASTHelpers.getAnnotation(symbol, SuppressWarnings.class));
  }

  private boolean isSuppressed(SuppressWarnings suppression) {
    return suppression != null
        && !Collections.disjoint(Arrays.asList(suppression.value()), allNames());
  }

  public interface AnnotationTreeMatcher extends Suppressible {
    Description matchAnnotation(AnnotationTree tree, VisitorState state);
  }

  public interface AnnotatedTypeTreeMatcher extends Suppressible {
    Description matchAnnotatedType(AnnotatedTypeTree tree, VisitorState state);
  }

  public interface ArrayAccessTreeMatcher extends Suppressible {
    Description matchArrayAccess(ArrayAccessTree tree, VisitorState state);
  }

  public interface ArrayTypeTreeMatcher extends Suppressible {
    Description matchArrayType(ArrayTypeTree tree, VisitorState state);
  }

  public interface AssertTreeMatcher extends Suppressible {
    Description matchAssert(AssertTree tree, VisitorState state);
  }

  public interface AssignmentTreeMatcher extends Suppressible {
    Description matchAssignment(AssignmentTree tree, VisitorState state);
  }

  public interface BinaryTreeMatcher extends Suppressible {
    Description matchBinary(BinaryTree tree, VisitorState state);
  }

  public interface BlockTreeMatcher extends Suppressible {
    Description matchBlock(BlockTree tree, VisitorState state);
  }

  public interface BreakTreeMatcher extends Suppressible {
    Description matchBreak(BreakTree tree, VisitorState state);
  }

  public interface CaseTreeMatcher extends Suppressible {
    Description matchCase(CaseTree tree, VisitorState state);
  }

  public interface CatchTreeMatcher extends Suppressible {
    Description matchCatch(CatchTree tree, VisitorState state);
  }

  public interface ClassTreeMatcher extends Suppressible {
    Description matchClass(ClassTree tree, VisitorState state);
  }

  public interface CompilationUnitTreeMatcher extends Suppressible {
    Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state);
  }

  public interface CompoundAssignmentTreeMatcher extends Suppressible {
    Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state);
  }

  public interface ConditionalExpressionTreeMatcher extends Suppressible {
    Description matchConditionalExpression(ConditionalExpressionTree tree, VisitorState state);
  }

  public interface ContinueTreeMatcher extends Suppressible {
    Description matchContinue(ContinueTree tree, VisitorState state);
  }

  public interface DoWhileLoopTreeMatcher extends Suppressible {
    Description matchDoWhileLoop(DoWhileLoopTree tree, VisitorState state);
  }

  public interface EmptyStatementTreeMatcher extends Suppressible {
    Description matchEmptyStatement(EmptyStatementTree tree, VisitorState state);
  }

  public interface EnhancedForLoopTreeMatcher extends Suppressible {
    Description matchEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state);
  }

  // Intentionally skip ErroneousTreeMatcher -- we don't analyze malformed expressions.

  public interface ExpressionStatementTreeMatcher extends Suppressible {
    Description matchExpressionStatement(ExpressionStatementTree tree, VisitorState state);
  }

  public interface ForLoopTreeMatcher extends Suppressible {
    Description matchForLoop(ForLoopTree tree, VisitorState state);
  }

  public interface IdentifierTreeMatcher extends Suppressible {
    Description matchIdentifier(IdentifierTree tree, VisitorState state);
  }

  public interface IfTreeMatcher extends Suppressible {
    Description matchIf(IfTree tree, VisitorState state);
  }

  public interface ImportTreeMatcher extends Suppressible {
    Description matchImport(ImportTree tree, VisitorState state);
  }

  public interface InstanceOfTreeMatcher extends Suppressible {
    Description matchInstanceOf(InstanceOfTree tree, VisitorState state);
  }

  public interface IntersectionTypeTreeMatcher extends Suppressible {
    Description matchIntersectionType(IntersectionTypeTree tree, VisitorState state);
  }

  public interface LabeledStatementTreeMatcher extends Suppressible {
    Description matchLabeledStatement(LabeledStatementTree tree, VisitorState state);
  }

  public interface LambdaExpressionTreeMatcher extends Suppressible {
    Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state);
  }

  public interface LiteralTreeMatcher extends Suppressible {
    Description matchLiteral(LiteralTree tree, VisitorState state);
  }

  public interface MemberReferenceTreeMatcher extends Suppressible {
    Description matchMemberReference(MemberReferenceTree tree, VisitorState state);
  }

  public interface MemberSelectTreeMatcher extends Suppressible {
    Description matchMemberSelect(MemberSelectTree tree, VisitorState state);
  }

  public interface MethodTreeMatcher extends Suppressible {
    Description matchMethod(MethodTree tree, VisitorState state);
  }

  public interface MethodInvocationTreeMatcher extends Suppressible {
    Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state);
  }

  public interface ModifiersTreeMatcher extends Suppressible {
    Description matchModifiers(ModifiersTree tree, VisitorState state);
  }

  public interface NewArrayTreeMatcher extends Suppressible {
    Description matchNewArray(NewArrayTree tree, VisitorState state);
  }

  public interface NewClassTreeMatcher extends Suppressible {
    Description matchNewClass(NewClassTree tree, VisitorState state);
  }

  // Intentionally skip OtherTreeMatcher. It seems to be used only for let expressions, which are
  // generated by javac to implement autoboxing. We are only interested in source-level constructs.

  public interface ParameterizedTypeTreeMatcher extends Suppressible {
    Description matchParameterizedType(ParameterizedTypeTree tree, VisitorState state);
  }

  public interface ParenthesizedTreeMatcher extends Suppressible {
    Description matchParenthesized(ParenthesizedTree tree, VisitorState state);
  }

  public interface PrimitiveTypeTreeMatcher extends Suppressible {
    Description matchPrimitiveType(PrimitiveTypeTree tree, VisitorState state);
  }

  public interface ReturnTreeMatcher extends Suppressible {
    Description matchReturn(ReturnTree tree, VisitorState state);
  }

  public interface SwitchTreeMatcher extends Suppressible {
    Description matchSwitch(SwitchTree tree, VisitorState state);
  }

  public interface SynchronizedTreeMatcher extends Suppressible {
    Description matchSynchronized(SynchronizedTree tree, VisitorState state);
  }

  public interface ThrowTreeMatcher extends Suppressible {
    Description matchThrow(ThrowTree tree, VisitorState state);
  }

  public interface TryTreeMatcher extends Suppressible {
    Description matchTry(TryTree tree, VisitorState state);
  }

  public interface TypeCastTreeMatcher extends Suppressible {
    Description matchTypeCast(TypeCastTree tree, VisitorState state);
  }

  public interface TypeParameterTreeMatcher extends Suppressible {
    Description matchTypeParameter(TypeParameterTree tree, VisitorState state);
  }

  public interface UnaryTreeMatcher extends Suppressible {
    Description matchUnary(UnaryTree tree, VisitorState state);
  }

  public interface UnionTypeTreeMatcher extends Suppressible {
    Description matchUnionType(UnionTypeTree tree, VisitorState state);
  }

  public interface VariableTreeMatcher extends Suppressible {
    Description matchVariable(VariableTree tree, VisitorState state);
  }

  public interface WhileLoopTreeMatcher extends Suppressible {
    Description matchWhileLoop(WhileLoopTree tree, VisitorState state);
  }

  public interface WildcardTreeMatcher extends Suppressible {
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
        && this.supportsSuppressWarnings() == that.supportsSuppressWarnings()
        && this.customSuppressionAnnotations().equals(that.customSuppressionAnnotations());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        canonicalName(),
        defaultSeverity(),
        supportsSuppressWarnings(),
        customSuppressionAnnotations());
  }

  /** A {@link TreePathScanner} which skips trees which are suppressed for this check. */
  protected class SuppressibleTreePathScanner<A, B> extends TreePathScanner<A, B> {
    @Override
    public final A scan(Tree tree, B b) {
      boolean isSuppressible =
          tree instanceof ClassTree || tree instanceof MethodTree || tree instanceof VariableTree;
      return isSuppressible && isSuppressed(tree) ? null : super.scan(tree, b);
    }
  }
}
