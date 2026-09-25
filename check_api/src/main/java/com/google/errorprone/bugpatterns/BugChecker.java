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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.util.ASTHelpers.getDeclaredSymbol;
import static com.google.errorprone.util.ASTHelpers.getModifiers;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.SuppressionInfo;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.fixes.ErrorPronePosition;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressible;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BindingPatternTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ConstantCaseLabelTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DeconstructionPatternTree;
import com.sun.source.tree.DefaultCaseLabelTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExportsTree;
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
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.OpensTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ProvidesTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.UsesTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.tree.YieldTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Name;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Objects;
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
    return switch (suppressionClasses.size()) {
      case 0 -> (annos, state) -> false;
      case 1 -> {
        Supplier<Name> self =
            VisitorState.memoize(
                state -> state.getName(Iterables.getOnlyElement(suppressionClasses).getName()));
        yield (annos, state) -> annos.contains(self.get(state));
      }
      default -> {
        Supplier<Set<? extends Name>> self =
            VisitorState.memoize(
                state ->
                    suppressionClasses.stream()
                        .map(Class::getName)
                        .map(state::getName)
                        .collect(toImmutableSet()));
        yield (annos, state) -> !Collections.disjoint(self.get(state), annos);
      }
    };
  }

  /** Helper to create a Description for the common case where there is a fix. */
  @CheckReturnValue
  public Description describeMatch(ErrorPronePosition position, Fix fix) {
    return buildDescription(position).addFix(fix).build();
  }

  /** Helper to create a Description for the common case where there is a fix. */
  @CheckReturnValue
  public Description describeMatch(Tree node, Fix fix) {
    return describeMatch(ErrorPronePosition.from(node), fix);
  }

  /** Helper to create a Description for the common case where there is a fix. */
  @CheckReturnValue
  public Description describeMatch(JCTree node, Fix fix) {
    return describeMatch(ErrorPronePosition.from(node), fix);
  }

  /** Helper to create a Description for the common case where there is a fix. */
  @CheckReturnValue
  public Description describeMatch(DiagnosticPosition position, Fix fix) {
    return describeMatch(ErrorPronePosition.from(position), fix);
  }

  /** Helper to create a Description for the common case where there is no fix. */
  @CheckReturnValue
  public Description describeMatch(ErrorPronePosition position) {
    return buildDescription(position).build();
  }

  /** Helper to create a Description for the common case where there is no fix. */
  @CheckReturnValue
  public Description describeMatch(Tree node) {
    return describeMatch(ErrorPronePosition.from(node));
  }

  /** Helper to create a Description for the common case where there is no fix. */
  @CheckReturnValue
  public Description describeMatch(JCTree node) {
    return describeMatch(ErrorPronePosition.from(node));
  }

  /** Helper to create a Description for the common case where there is no fix. */
  @CheckReturnValue
  public Description describeMatch(DiagnosticPosition position) {
    return describeMatch(ErrorPronePosition.from(position));
  }

  /**
   * Returns a Description builder, which allows you to customize the diagnostic with a custom
   * message or multiple fixes.
   */
  @CheckReturnValue
  public Description.Builder buildDescription(ErrorPronePosition position) {
    return Description.builder(position, canonicalName(), linkUrl(), message());
  }

  /**
   * Returns a Description builder, which allows you to customize the diagnostic with a custom
   * message or multiple fixes.
   */
  @CheckReturnValue
  public Description.Builder buildDescription(Tree node) {
    return buildDescription(ErrorPronePosition.from(node));
  }

  /**
   * Returns a Description builder, which allows you to customize the diagnostic with a custom
   * message or multiple fixes.
   */
  @CheckReturnValue
  public Description.Builder buildDescription(DiagnosticPosition position) {
    return buildDescription(ErrorPronePosition.from(position));
  }

  /**
   * Returns a Description builder, which allows you to customize the diagnostic with a custom
   * message or multiple fixes.
   */
  // This overload exists purely to disambiguate for JCTree.
  @CheckReturnValue
  public Description.Builder buildDescription(JCTree tree) {
    return buildDescription(ErrorPronePosition.from(tree));
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

  /**
   * Returns true if the given tree is annotated with a {@code @SuppressWarnings} that disables this
   * bug checker.
   */
  public boolean isSuppressed(Tree tree, VisitorState state) {
    Symbol sym = getDeclaredSymbol(tree);
    /*
     * TODO(cpovirk): At least for @SuppressWarnings, should our suppression checks look for
     * annotations only on the kinds of trees that are covered by SuppressibleTreePathScanner? Or,
     * now that @SuppressWarnings has been changed to be applicable to all declaration locations,
     * should we generalize SuppressibleTreePathScanner to look on all those locations?
     */
    return sym != null && isSuppressed(sym, state);
  }

  /**
   * Returns true if the given symbol is annotated with a {@code @SuppressWarnings} or other
   * annotation that disables this bug checker.
   */
  /*
   * TODO(cpovirk): Would we consider deleting this overload (or at least making it `private`)? Its
   * callers appear to all have access to a Tree, and callers might accidentally pass
   * getSymbol(tree) instead of getDeclaredSymbol(tree), resulting in over-suppression. Fortunately,
   * the Tree probably provides all that we need, at least for looking for @SuppressWarnings. It
   * does *not* provide all that we need for looking for any @Inherited suppression annotations (if
   * such annotations are something that we (a) support and (b) want to support), but we can always
   * call getDeclaredSymbol inside the implementation where necessary.
   */
  public boolean isSuppressed(Symbol sym, VisitorState state) {
    ErrorProneOptions errorProneOptions = state.errorProneOptions();
    boolean suppressedInGeneratedCode =
        errorProneOptions.disableWarningsInGeneratedCode()
            && state.severityMap().get(canonicalName()) != SeverityLevel.ERROR;
    SuppressionInfo.SuppressedState suppressedState =
        SuppressionInfo.EMPTY
            .withExtendedSuppressions(sym, state, customSuppressionAnnotationNames.get(state))
            .suppressedState(BugChecker.this, suppressedInGeneratedCode, state);
    return suppressedState == SuppressionInfo.SuppressedState.SUPPRESSED;
  }

  private final Supplier<? extends Set<? extends Name>> customSuppressionAnnotationNames =
      VisitorState.memoize(
          state ->
              customSuppressionAnnotations().stream()
                  .map(a -> state.getName(a.getName()))
                  .collect(toImmutableSet()));

  /** Computes a RangeSet of code regions which are suppressed by this bug checker. */
  public ImmutableRangeSet<Integer> suppressedRegions(VisitorState state) {
    TreeRangeSet<Integer> suppressedRegions = TreeRangeSet.create();
    new TreeScanner<Void, Void>() {
      @Override
      public Void scan(Tree tree, Void unused) {
        if (getModifiers(tree) != null && isSuppressed(tree, state)) {
          suppressedRegions.add(Range.closed(getStartPosition(tree), state.getEndPosition(tree)));
        } else {
          super.scan(tree, null);
        }
        return null;
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return ImmutableRangeSet.copyOf(suppressedRegions);
  }

  public interface AnnotatedTypeTreeMatcher extends Suppressible {
    Description matchAnnotatedType(AnnotatedTypeTree tree, VisitorState state);
  }

  public interface AnnotationTreeMatcher extends Suppressible {
    Description matchAnnotation(AnnotationTree tree, VisitorState state);
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

  public interface BindingPatternTreeMatcher extends Suppressible {
    Description matchBindingPattern(BindingPatternTree tree, VisitorState state);
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

  public interface ConstantCaseLabelTreeMatcher extends Suppressible {
    Description matchConstantCaseLabel(ConstantCaseLabelTree tree, VisitorState state);
  }

  public interface ContinueTreeMatcher extends Suppressible {
    Description matchContinue(ContinueTree tree, VisitorState state);
  }

  public interface DeconstructionPatternTreeMatcher extends Suppressible {
    Description matchDeconstructionPattern(DeconstructionPatternTree tree, VisitorState state);
  }

  public interface DefaultCaseLabelTreeMatcher extends Suppressible {
    Description matchDefaultCaseLabel(DefaultCaseLabelTree tree, VisitorState state);
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

  public interface ExportsTreeMatcher extends Suppressible {
    Description matchExports(ExportsTree tree, VisitorState state);
  }

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

  public interface ModuleTreeMatcher extends Suppressible {
    Description matchModule(ModuleTree tree, VisitorState state);
  }

  public interface NewArrayTreeMatcher extends Suppressible {
    Description matchNewArray(NewArrayTree tree, VisitorState state);
  }

  public interface NewClassTreeMatcher extends Suppressible {
    Description matchNewClass(NewClassTree tree, VisitorState state);
  }

  // Intentionally skip OtherTreeMatcher. It seems to be used only for let expressions, which are
  // generated by javac to implement autoboxing. We are only interested in source-level constructs.

  public interface OpensTreeMatcher extends Suppressible {
    Description matchOpens(OpensTree tree, VisitorState state);
  }

  public interface PackageTreeMatcher extends Suppressible {
    Description matchPackage(PackageTree tree, VisitorState state);
  }

  public interface ParameterizedTypeTreeMatcher extends Suppressible {
    Description matchParameterizedType(ParameterizedTypeTree tree, VisitorState state);
  }

  public interface ParenthesizedTreeMatcher extends Suppressible {
    Description matchParenthesized(ParenthesizedTree tree, VisitorState state);
  }

  public interface PatternCaseLabelTreeMatcher extends Suppressible {
    Description matchPatternCaseLabel(PatternCaseLabelTree tree, VisitorState state);
  }

  public interface PrimitiveTypeTreeMatcher extends Suppressible {
    Description matchPrimitiveType(PrimitiveTypeTree tree, VisitorState state);
  }

  public interface ProvidesTreeMatcher extends Suppressible {
    Description matchProvides(ProvidesTree tree, VisitorState state);
  }

  public interface RequiresTreeMatcher extends Suppressible {
    Description matchRequires(RequiresTree tree, VisitorState state);
  }

  public interface ReturnTreeMatcher extends Suppressible {
    Description matchReturn(ReturnTree tree, VisitorState state);
  }

  public interface SwitchExpressionTreeMatcher extends Suppressible {
    Description matchSwitchExpression(SwitchExpressionTree tree, VisitorState state);
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

  public interface UsesTreeMatcher extends Suppressible {
    Description matchUses(UsesTree tree, VisitorState state);
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

  public interface YieldTreeMatcher extends Suppressible {
    Description matchYield(YieldTree tree, VisitorState state);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof BugChecker that
        && this.canonicalName().equals(that.canonicalName())
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

  /**
   * A {@link TreePathScanner} which skips trees which are suppressed for this check.
   *
   * @param <R> the type returned by each scanner method
   * @param <P> the type of the parameter passed to each scanner method
   */
  protected class SuppressibleTreePathScanner<R, P> extends TreePathScanner<R, P> {

    protected final VisitorState state;

    public SuppressibleTreePathScanner(VisitorState state) {
      this.state = state;
    }

    @Override
    public R scan(Tree tree, P param) {
      return suppressed(tree) ? null : super.scan(tree, param);
    }

    @Override
    public R scan(TreePath treePath, P param) {
      return suppressed(treePath.getLeaf()) ? null : super.scan(treePath, param);
    }

    private boolean suppressed(Tree tree) {
      boolean ignoreSuppressions = state.errorProneOptions().isIgnoreSuppressionAnnotations();
      if (ignoreSuppressions) {
        return false;
      }

      boolean isSuppressible =
          tree instanceof ClassTree || tree instanceof MethodTree || tree instanceof VariableTree;
      return isSuppressible && isSuppressed(tree, state);
    }
  }
}
