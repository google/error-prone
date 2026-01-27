/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.SwitchUtils.KINDS_RETURN_OR_THROW;
import static com.google.errorprone.bugpatterns.SwitchUtils.REMOVE_DEFAULT_CASE_SHORT_DESCRIPTION;
import static com.google.errorprone.bugpatterns.SwitchUtils.analyzeCaseForNullAndDefault;
import static com.google.errorprone.bugpatterns.SwitchUtils.findCombinableVariableTree;
import static com.google.errorprone.bugpatterns.SwitchUtils.getPrecedingStatementsInBlock;
import static com.google.errorprone.bugpatterns.SwitchUtils.getStatements;
import static com.google.errorprone.bugpatterns.SwitchUtils.hasContinueOutOfTree;
import static com.google.errorprone.bugpatterns.SwitchUtils.isCompatibleWithFirstAssignment;
import static com.google.errorprone.bugpatterns.SwitchUtils.isSwitchCoveringAllEnumValues;
import static com.google.errorprone.bugpatterns.SwitchUtils.printCaseExpressionsOrPatternAndGuard;
import static com.google.errorprone.bugpatterns.SwitchUtils.renderComments;
import static com.google.errorprone.bugpatterns.SwitchUtils.renderNullDefaultKindPrefix;
import static com.google.errorprone.bugpatterns.SwitchUtils.renderVariableTreeAnnotations;
import static com.google.errorprone.bugpatterns.SwitchUtils.renderVariableTreeComments;
import static com.google.errorprone.bugpatterns.SwitchUtils.renderVariableTreeFlags;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.hasImplicitType;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.bugpatterns.SwitchUtils.CaseQualifications;
import com.google.errorprone.bugpatterns.SwitchUtils.NullDefaultKind;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneComment;
import com.google.errorprone.util.Reachability;
import com.google.errorprone.util.SourceVersion;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.YieldTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;

/** Checks for refactorings that can be applied to simplify arrow switches. */
@BugPattern(severity = WARNING, summary = "This switch can be refactored to be more readable")
public final class RefactorSwitch extends BugChecker
    implements SwitchTreeMatcher, SwitchExpressionTreeMatcher {

  /** Default (negative) result for overall analysis. Note that the value is immutable. */
  private static final AnalysisResult DEFAULT_ANALYSIS_RESULT =
      new AnalysisResult(ImmutableList.of());

  private final boolean enableAssignmentSwitch;
  private final boolean enableReturnSwitch;
  private final boolean enableSimplifySwitch;

  @Inject
  RefactorSwitch(ErrorProneFlags flags) {
    enableAssignmentSwitch =
        flags.getBoolean("RefactorSwitch:EnableAssignmentSwitch").orElse(false);
    enableReturnSwitch = flags.getBoolean("RefactorSwitch:EnableReturnSwitch").orElse(false);
    enableSimplifySwitch = flags.getBoolean("RefactorSwitch:EnableSimplifySwitch").orElse(false);
  }

  @Override
  public Description matchSwitch(SwitchTree switchTree, VisitorState visitorState) {
    // NOMUTANTS -- This is a performance optimization
    if (!enableReturnSwitch && !enableAssignmentSwitch && !enableSimplifySwitch) {
      return NO_MATCH;
    }

    if (!SourceVersion.supportsPatternMatchingSwitch(visitorState.context)) {
      return NO_MATCH;
    }

    AnalysisResult analysisResult = analyzeSwitchTree(switchTree, visitorState);

    ImmutableList<SuggestedFix> suggestedFixes =
        analysisResult.convertibleFindings().stream()
            .map(convertible -> convertible.convert(switchTree, visitorState))
            .collect(toImmutableList());

    return suggestedFixes.isEmpty()
        ? NO_MATCH
        : buildDescription(switchTree).addAllFixes(suggestedFixes).build();
  }

  @Override
  public Description matchSwitchExpression(
      SwitchExpressionTree switchExpressionTree, VisitorState visitorState) {
    if (!enableSimplifySwitch) {
      return NO_MATCH;
    }

    AnalysisResult analysisResult = analyzeSwitchExpressionTree(switchExpressionTree, visitorState);

    ImmutableList<SuggestedFix> suggestedFixes =
        analysisResult.convertibleFindings().stream()
            .map(convertible -> convertible.convert(switchExpressionTree, visitorState))
            .collect(toImmutableList());

    return suggestedFixes.isEmpty()
        ? NO_MATCH
        : buildDescription(switchExpressionTree).addAllFixes(suggestedFixes).build();
  }

  /**
   * Analyzes a {@code SwitchTree}, and determines any possible findings and suggested fixes related
   * to expression switches that can be made. Does not report any findings or suggested fixes up to
   * the Error Prone framework.
   */
  private AnalysisResult analyzeSwitchTree(SwitchTree switchTree, VisitorState state) {

    // Don't convert switch within switch because findings may overlap
    if (ASTHelpers.findEnclosingNode(state.getPath(), SwitchTree.class) != null) {
      return DEFAULT_ANALYSIS_RESULT;
    }

    if (ASTHelpers.findEnclosingNode(state.getPath(), SwitchExpressionTree.class) != null) {
      return DEFAULT_ANALYSIS_RESULT;
    }

    List<? extends CaseTree> cases = switchTree.getCases();

    // Does each case consist solely of returning a (non-void) expression?
    CaseQualifications returnSwitchCaseQualifications = CaseQualifications.NO_CASES_ASSESSED;
    // Does each case consist solely of a throw or the same symbol assigned in the same way?
    Optional<ExpressionTree> assignmentTargetOptional = Optional.empty();
    Optional<Tree.Kind> assignmentKindOptional = Optional.empty();

    AssignmentSwitchAnalysisState assignmentSwitchAnalysisState =
        new AssignmentSwitchAnalysisState(
            CaseQualifications.NO_CASES_ASSESSED,
            assignmentTargetOptional,
            assignmentKindOptional,
            Optional.empty());

    boolean canSimplify = false;

    // Set of all enum values (names) explicitly listed in a case tree
    Set<String> handledEnumValues = new HashSet<>();

    // One-pass scan through each case in switch
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);
      ImmutableList<StatementTree> statements = getStatements(caseTree);

      // All cases must be of kind CaseTree.CaseKind.RULE
      if (caseTree.getCaseKind() != CaseTree.CaseKind.RULE) {
        return DEFAULT_ANALYSIS_RESULT;
      }

      // Accumulate enum values included in this case
      handledEnumValues.addAll(
          caseTree.getExpressions().stream()
              .map(ASTHelpers::getSymbol)
              .filter(x -> x != null)
              .map(symbol -> symbol.getSimpleName().toString())
              .collect(toImmutableSet()));

      returnSwitchCaseQualifications =
          analyzeCaseForReturnSwitch(returnSwitchCaseQualifications, statements);
      assignmentSwitchAnalysisState =
          analyzeCaseForAssignmentSwitch(assignmentSwitchAnalysisState, statements);
      canSimplify = canSimplify || analyzeCaseForSimplify(statements, caseTree);
    }

    ImmutableList<Range<Integer>> caseRhsSourceCodeRanges =
        cases.stream()
            .map(
                caseTree ->
                    Range.closedOpen(
                        getStartPosition(caseTree.getBody()),
                        state.getEndPosition(caseTree.getBody())))
            .collect(toImmutableList());
    // Has at least once case with a pattern
    boolean hasPattern =
        cases.stream()
            .flatMap(caseTree -> caseTree.getLabels().stream())
            .anyMatch(caseLabelTree -> caseLabelTree instanceof PatternCaseLabelTree);
    boolean hasDefault = cases.stream().anyMatch(ASTHelpers::isSwitchDefault);
    boolean coversAllEnumValues =
        isSwitchCoveringAllEnumValues(
            handledEnumValues, ASTHelpers.getType(switchTree.getExpression()));
    boolean anyCaseCanCompleteNormally =
        cases.stream()
            .anyMatch(
                caseTree -> Reachability.canCompleteNormally((StatementTree) caseTree.getBody()));

    boolean canRemoveDefault = hasDefault && coversAllEnumValues;

    // Continuing out of a switch statement is possible, but continuing out of a switch expression
    // is not
    boolean hasContinueOut = hasContinueOutOfTree(switchTree, state);

    boolean canConvertToAssignmentSwitch =
        // The switch must be known to be exhaustive at compile time
        (hasDefault || hasPattern || coversAllEnumValues)
            && assignmentSwitchAnalysisState
                .assignmentSwitchCaseQualifications()
                .equals(CaseQualifications.ALL_CASES_QUALIFY)
            && !hasContinueOut;

    // If the switch cannot complete normally, this is sufficient to ensure every case cannot.
    // Alternatively, if the switch *can* complete normally and covers all enum values, with each
    // case unable to complete normally, then we will also propose conversion to a return switch
    // (safe unless the runtime enum values were to differ)
    boolean canConvertToReturnSwitch =
        (!Reachability.canCompleteNormally(switchTree)
                || (!anyCaseCanCompleteNormally && coversAllEnumValues))
            && returnSwitchCaseQualifications.equals(CaseQualifications.ALL_CASES_QUALIFY)
            && !hasContinueOut;

    ImmutableList<StatementTree> precedingStatements = getPrecedingStatementsInBlock(state);
    Optional<ExpressionTree> assignmentTarget =
        assignmentSwitchAnalysisState.assignmentTargetOptional();

    // If present, the variable tree that can be combined with the switch block
    Optional<VariableTree> combinableVariableTree =
        canConvertToAssignmentSwitch
            ? assignmentTarget.flatMap(
                target -> findCombinableVariableTree(target, precedingStatements, state))
            : Optional.empty();

    List<Convertible> convertibleFindings = new ArrayList<>();
    if (enableReturnSwitch && canConvertToReturnSwitch) {
      convertibleFindings.add(
          new ReturnSwitchAnalysisResult(
              canConvertToReturnSwitch, /* canRemoveDefault= */ false, caseRhsSourceCodeRanges));
      if (canRemoveDefault) {
        convertibleFindings.add(
            new ReturnSwitchAnalysisResult(
                canConvertToReturnSwitch, /* canRemoveDefault= */ true, caseRhsSourceCodeRanges));
      }
    }
    if (enableAssignmentSwitch && canConvertToAssignmentSwitch) {
      convertibleFindings.add(
          new AssignmentSwitchAnalysisResult(
              canConvertToAssignmentSwitch,
              combinableVariableTree,
              assignmentSwitchAnalysisState.assignmentTargetOptional(),
              assignmentSwitchAnalysisState.assignmentExpressionKindOptional(),
              /* canRemoveDefault= */ false,
              assignmentSwitchAnalysisState
                  .assignmentTreeOptional()
                  .map(SwitchUtils::renderJavaSourceOfAssignment)));

      if (canRemoveDefault) {
        convertibleFindings.add(
            new AssignmentSwitchAnalysisResult(
                canConvertToAssignmentSwitch,
                combinableVariableTree,
                assignmentSwitchAnalysisState.assignmentTargetOptional(),
                assignmentSwitchAnalysisState.assignmentExpressionKindOptional(),
                /* canRemoveDefault= */ true,
                assignmentSwitchAnalysisState
                    .assignmentTreeOptional()
                    .map(SwitchUtils::renderJavaSourceOfAssignment)));
      }
    }
    if (enableSimplifySwitch && canSimplify) {
      convertibleFindings.add(
          new SimplifyAnalysisResult(
              canSimplify, /* canRemoveDefault= */ false, caseRhsSourceCodeRanges));
      if (canRemoveDefault) {
        convertibleFindings.add(
            new SimplifyAnalysisResult(
                canSimplify, /* canRemoveDefault= */ true, caseRhsSourceCodeRanges));
      }
    }

    return new AnalysisResult(ImmutableList.copyOf(convertibleFindings));
  }

  /**
   * Analyzes a {@code SwitchExpressionTree}, and determines any possible findings and suggested
   * fixes related to expression switches that can be made. Does not report any findings or
   * suggested fixes up to the Error Prone framework.
   */
  private AnalysisResult analyzeSwitchExpressionTree(
      SwitchExpressionTree switchTree, VisitorState state) {
    // Nothing to be done for an empty switch
    if (switchTree.getCases().isEmpty()) {
      return DEFAULT_ANALYSIS_RESULT;
    }

    // Don't convert switch within switch because findings may overlap
    if (ASTHelpers.findEnclosingNode(state.getPath(), SwitchTree.class) != null) {
      return DEFAULT_ANALYSIS_RESULT;
    }

    if (ASTHelpers.findEnclosingNode(state.getPath(), SwitchExpressionTree.class) != null) {
      return DEFAULT_ANALYSIS_RESULT;
    }
    List<? extends CaseTree> cases = switchTree.getCases();

    boolean canSimplify = false;

    // Set of all enum values (names) explicitly listed in a case tree
    Set<String> handledEnumValues = new HashSet<>();

    // One-pass scan through each case in switch
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);

      // All cases must be of kind CaseTree.CaseKind.RULE
      if (caseTree.getCaseKind() != CaseTree.CaseKind.RULE) {
        return DEFAULT_ANALYSIS_RESULT;
      }
      ImmutableList<StatementTree> statements = getStatements(caseTree);

      // Accumulate enum values included in this case
      handledEnumValues.addAll(
          caseTree.getExpressions().stream()
              .map(ASTHelpers::getSymbol)
              .filter(x -> x != null)
              .map(symbol -> symbol.getSimpleName().toString())
              .collect(toImmutableSet()));

      canSimplify = canSimplify || analyzeCaseForSimplify(statements, caseTree);
    }

    ImmutableList<Range<Integer>> caseRhsSourceCodeRanges =
        cases.stream()
            .map(
                caseTree ->
                    Range.closedOpen(
                        getStartPosition(caseTree.getBody()),
                        state.getEndPosition(caseTree.getBody())))
            .collect(toImmutableList());

    boolean hasDefault = cases.stream().anyMatch(ASTHelpers::isSwitchDefault);
    boolean coversAllEnumValues =
        isSwitchCoveringAllEnumValues(
            handledEnumValues, ASTHelpers.getType(switchTree.getExpression()));
    boolean canRemoveDefault = hasDefault && coversAllEnumValues;

    List<Convertible> convertibleFindings = new ArrayList<>();
    if (enableSimplifySwitch && canSimplify) {
      convertibleFindings.add(
          new SimplifyAnalysisResult(
              canSimplify, /* canRemoveDefault= */ false, caseRhsSourceCodeRanges));
      if (canRemoveDefault) {
        convertibleFindings.add(
            new SimplifyAnalysisResult(
                canSimplify, /* canRemoveDefault= */ true, caseRhsSourceCodeRanges));
      }
    }

    return new AnalysisResult(ImmutableList.copyOf(convertibleFindings));
  }

  /** Analyzes a single case for simplifications. */
  private static boolean analyzeCaseForSimplify(
      ImmutableList<StatementTree> statements, CaseTree caseTree) {
    // Remove redundant braces
    if (caseTree.getBody() instanceof BlockTree blockTree
        && blockTree.getStatements().size() == 1
        && blockTree.getStatements().get(0) instanceof BlockTree) {
      return true;
    }

    // Convert `-> {yield x;}` to `-> x`
    if (statements.size() == 1 && statements.get(0) instanceof YieldTree) {
      return true;
    }

    return false;
  }

  /**
   * Returns a map from return trees to the scope that they are returning from. Special case: when
   * returning from the scope of the switch statement itself, the {@code switchTree} itself is used
   * as a sentinel value (no attempt is made to identify the specific enclosing scope).
   */
  private static Map<Tree, Tree> analyzeReturnControlFlow(
      SwitchTree switchTree, VisitorState state) {
    ArrayDeque<Tree> returnScope = new ArrayDeque<>();
    returnScope.push(switchTree);
    HashMap<Tree, Tree> result = new HashMap<>();
    // One can only return from a method or a lambda expression
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMethod(MethodTree methodTree, Void unused) {
        returnScope.push(methodTree);
        super.visitMethod(methodTree, null);
        returnScope.pop();
        return null;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void unused) {
        returnScope.push(lambdaExpressionTree);
        super.visitLambdaExpression(lambdaExpressionTree, null);
        returnScope.pop();
        return null;
      }

      @Override
      public Void visitReturn(ReturnTree node, Void unused) {
        result.put(node, returnScope.peek());
        return null;
      }
    }.scan(state.getPath(), null);
    return result;
  }

  /**
   * Analyze a single {@code case} of a single {@code switch} statement to determine whether it is
   * convertible to a return switch. The supplied {@code previousCaseQualifications} is updated and
   * returned based on this analysis.
   */
  private static CaseQualifications analyzeCaseForReturnSwitch(
      CaseQualifications previousCaseQualifications, ImmutableList<StatementTree> statements) {
    // An empty RHS can't yield a value
    if (statements.isEmpty()) {
      return CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY;
    }
    StatementTree lastStatement = getLast(statements);
    if (!KINDS_RETURN_OR_THROW.contains(lastStatement.getKind())) {
      return CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY;
    }

    // For this analysis, cases that don't return something can be disregarded
    if (!(lastStatement instanceof ReturnTree returnTree)) {
      return previousCaseQualifications;
    }

    // NOMUTANTS -- This is a performance optimization, not a correctness check
    if (!previousCaseQualifications.equals(CaseQualifications.NO_CASES_ASSESSED)) {
      // There is no need to inspect the type compatibility of the return values, because if they
      // were incompatible then the compilation would have failed before reaching this point
      return previousCaseQualifications;
    }

    // This is the first value-returning case that we are examining
    Type returnType = ASTHelpers.getType(returnTree.getExpression());
    return returnType == null
        // Return of void does not qualify
        ? CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY
        : CaseQualifications.ALL_CASES_QUALIFY;
  }

  /**
   * Analyze a single {@code case} of a single {@code switch} statement to determine whether it is
   * convertible to an assignment switch. The supplied {@code previousAssignmentSwitchAnalysisState}
   * is updated and returned based on this analysis.
   */
  private static AssignmentSwitchAnalysisState analyzeCaseForAssignmentSwitch(
      AssignmentSwitchAnalysisState previousAssignmentSwitchAnalysisState,
      List<StatementTree> statements) {

    CaseQualifications caseQualifications =
        previousAssignmentSwitchAnalysisState.assignmentSwitchCaseQualifications();
    Optional<Kind> assignmentExpressionKindOptional =
        previousAssignmentSwitchAnalysisState.assignmentExpressionKindOptional();
    Optional<ExpressionTree> assignmentTargetOptional =
        previousAssignmentSwitchAnalysisState.assignmentTargetOptional();
    Optional<ExpressionTree> assignmentTreeOptional =
        previousAssignmentSwitchAnalysisState.assignmentTreeOptional();

    // An empty RHS can't assign anything; we don't support >1 statement in the RHS
    if (statements.isEmpty() || statements.size() > 1) {
      return new AssignmentSwitchAnalysisState(
          CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY,
          assignmentTargetOptional,
          assignmentExpressionKindOptional,
          assignmentTreeOptional);
    }

    // Invariant: exactly one statement on RHS
    StatementTree firstStatement = statements.get(0);
    // Must be an assignment or a throw
    if (firstStatement instanceof ThrowTree) {
      return previousAssignmentSwitchAnalysisState;
    }
    if (!(firstStatement instanceof ExpressionStatementTree expressionStatementTree)) {
      // Definitely not an assignment
      return new AssignmentSwitchAnalysisState(
          CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY,
          assignmentTargetOptional,
          assignmentExpressionKindOptional,
          assignmentTreeOptional);
    }

    ExpressionTree expression = expressionStatementTree.getExpression();
    Optional<ExpressionTree> caseAssignmentTargetOptional = Optional.empty();
    Optional<Tree.Kind> caseAssignmentKindOptional = Optional.empty();
    Optional<ExpressionTree> caseAssignmentTreeOptional = Optional.empty();

    // The assignment could be a normal assignment ("=") or a compound assignment (e.g. "+=")
    switch (expression) {
      case CompoundAssignmentTree compoundAssignmentTree -> {
        caseAssignmentTargetOptional = Optional.of(compoundAssignmentTree.getVariable());
        caseAssignmentKindOptional = Optional.of(compoundAssignmentTree.getKind());
        caseAssignmentTreeOptional = Optional.of(expression);
      }
      case AssignmentTree assignmentTree -> {
        caseAssignmentTargetOptional = Optional.of(assignmentTree.getVariable());
        caseAssignmentKindOptional = Optional.of(Tree.Kind.ASSIGNMENT);
        caseAssignmentTreeOptional = Optional.of(expression);
      }
      default -> {
        // Something else that is not an assignment
        return new AssignmentSwitchAnalysisState(
            CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY,
            assignmentTargetOptional,
            assignmentExpressionKindOptional,
            assignmentTreeOptional);
      }
    }

    boolean compatibleOperator =
        // First assignment seen?
        (assignmentExpressionKindOptional.isEmpty() && caseAssignmentKindOptional.isPresent())
            // Not first assignment, but compatible with the first?
            || (assignmentExpressionKindOptional.isPresent()
                && caseAssignmentKindOptional.isPresent()
                && assignmentExpressionKindOptional.get().equals(caseAssignmentKindOptional.get()));
    boolean compatibleReference =
        // First assignment seen?
        (assignmentTargetOptional.isEmpty() && caseAssignmentTargetOptional.isPresent())
            // Not first assignment, but assigning to same symbol as the first assignment?
            || isCompatibleWithFirstAssignment(
                assignmentTargetOptional, caseAssignmentTargetOptional);

    if (compatibleOperator && compatibleReference) {
      caseQualifications =
          caseQualifications.equals(CaseQualifications.NO_CASES_ASSESSED)
              ? CaseQualifications.ALL_CASES_QUALIFY
              : caseQualifications;
    } else {
      caseQualifications = CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY;
    }

    // Save the assignment target/kind in the state, but never overwrite existing target/kind
    return new AssignmentSwitchAnalysisState(
        caseQualifications,
        assignmentTargetOptional.isEmpty()
            ? caseAssignmentTargetOptional
            : assignmentTargetOptional,
        assignmentExpressionKindOptional.isEmpty()
            ? caseAssignmentKindOptional
            : assignmentExpressionKindOptional,
        assignmentTreeOptional.isEmpty() ? caseAssignmentTreeOptional : assignmentTreeOptional);
  }

  /**
   * Converts a switch that {@code return}s on each case, and is exhaustive, into a {@code return
   * switch}.
   */
  private static SuggestedFix convertToReturnSwitch(
      SwitchTree switchTree,
      VisitorState state,
      ReturnSwitchAnalysisResult returnSwitchAnalysisResult,
      boolean removeDefault) {

    Map<Tree, Tree> returnToScope = analyzeReturnControlFlow(switchTree, state);

    SuggestedFix.Builder suggestedFixBuilder = SuggestedFix.builder();
    List<? extends CaseTree> cases = switchTree.getCases();

    suggestedFixBuilder.prefixWith(switchTree, "return ");
    // We need to add a semicolon as in `return switch ... ;`
    suggestedFixBuilder.postfixWith(switchTree, ";");

    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);

      NullDefaultKind nullDefaultKind = analyzeCaseForNullAndDefault(caseTree);
      if (removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_DEFAULT)) {
        // Delete removed default (and its code) entirely
        suggestedFixBuilder.delete(caseTree);
        continue;
      }

      transformCaseForReturnSwitch(
          switchTree,
          caseTree,
          state,
          returnToScope,
          returnSwitchAnalysisResult.caseRhsSourceCodeRanges().get(caseIndex),
          nullDefaultKind,
          removeDefault,
          suggestedFixBuilder);
    }

    // The transformed code can cause other existing code to become dead code.  So, we must analyze
    // and delete such dead code, otherwise the suggested autofix could fail to compile.

    // The `return switch ...` will always return or throw
    Tree cannotCompleteNormallyTree = switchTree;
    // Search up the AST for enclosing statement blocks, marking any newly-dead code for deletion
    // along the way
    Tree prev = state.getPath().getLeaf();
    for (Tree tree : state.getPath().getParentPath()) {
      if (tree instanceof BlockTree blockTree) {
        var statements = blockTree.getStatements();
        int indexInBlock = statements.indexOf(prev);
        // A single mock of the immediate child statement block (or switch) is sufficient to
        // analyze reachability here; deeper-nested statements are not relevant.
        boolean nextStatementReachable =
            Reachability.canCompleteNormally(
                statements.get(indexInBlock), ImmutableMap.of(cannotCompleteNormallyTree, false));
        // If we continue to the ancestor statement block, it will be because the end of this
        // statement block is not reachable
        cannotCompleteNormallyTree = blockTree;
        if (nextStatementReachable) {
          break;
        }

        // If a next statement in this block exists, then it is not reachable.
        if (indexInBlock < statements.size() - 1) {
          String deletedRegion =
              state
                  .getSourceCode()
                  .subSequence(
                      state.getEndPosition(statements.get(indexInBlock)),
                      state.getEndPosition(blockTree))
                  .toString();
          // If the region we would delete looks interesting, bail out and just delete the orphaned
          // statements.
          if (deletedRegion.contains("LINT.")) {
            statements
                .subList(indexInBlock + 1, statements.size())
                .forEach(suggestedFixBuilder::delete);
          } else {
            // If the region doesn't seem to contain interesting comments, delete it along with
            // comments: those comments are often just of the form "Unreachable code".
            suggestedFixBuilder.replace(
                state.getEndPosition(statements.get(indexInBlock)),
                state.getEndPosition(blockTree),
                "}");
          }
        }
      }
      prev = tree;
    }

    if (removeDefault) {
      suggestedFixBuilder.setShortDescription(REMOVE_DEFAULT_CASE_SHORT_DESCRIPTION);
    }
    return suggestedFixBuilder.build();
  }

  /**
   * Converts a single `yield` statement to a single expression within the {@code
   * suggestedFixBuilder}, for example `case null -> {yield 0;}` would become `case null -> 0;`.
   * Precondition: it must have been validated that this transformation is syntactically valid
   * before invoking this method.
   */
  private static void simplifySingleYieldStatement(
      YieldTree yieldStatement,
      VisitorState state,
      CaseTree caseTree,
      SuggestedFix.Builder suggestedFixBuilder,
      ImmutableList<ErrorProneComment> allComments,
      boolean renderWholeCase,
      NullDefaultKind nullDefaultKind,
      boolean removeDefault) {
    Tree value = yieldStatement.getValue();
    StringBuilder suffixBuilder = new StringBuilder();
    suffixBuilder.append(";");
    StringBuilder replacementBuilder = new StringBuilder();
    // Extract any comments that would be orphaned by the simplification
    StringBuilder commentBuilder = new StringBuilder();
    Range<Integer> caseRhsSourceCodeRange =
        Range.closedOpen(getStartPosition(value), state.getEndPosition(value));
    ImmutableList<ErrorProneComment> orphanedComments =
        computeOrphanedComments(
            allComments,
            renderWholeCase
                ? caseRhsSourceCodeRange
                : Range.closedOpen(getStartPosition(value), state.getEndPosition(value)),
            caseRhsSourceCodeRange);
    String renderedOrphans = renderComments(orphanedComments);
    if (!renderedOrphans.isEmpty()) {
      commentBuilder.append("\n").append(renderedOrphans).append("\n");
    }

    Tree replacementTarget;
    if (renderWholeCase) {
      // Render both LHS and RHS
      replacementBuilder.append(renderNullDefaultKindPrefix(nullDefaultKind, removeDefault));
      if (nullDefaultKind.equals(NullDefaultKind.KIND_NEITHER)) {
        replacementBuilder.append(printCaseExpressionsOrPatternAndGuard(caseTree, state));
      }
      replacementBuilder.append(" -> ");
      replacementTarget = caseTree;
    } else {
      // Render only the RHS
      replacementTarget = caseTree.getBody();
    }
    suggestedFixBuilder.replace(
        replacementTarget,
        replacementBuilder.toString()
            + commentBuilder
            + state.getSourceForNode(value)
            + suffixBuilder);
  }

  /**
   * Transforms the RHS block of a case for a return switch. This involves replacing relevant
   * `return`s with `yield`s, removing any redundant braces, and adjusting comments if needed.
   */
  private static void transformCaseForReturnSwitch(
      SwitchTree switchTree,
      CaseTree caseTree,
      VisitorState state,
      Map<Tree, Tree> returnToScope,
      Range<Integer> caseRhsSourceCodeRange,
      NullDefaultKind nullDefaultKind,
      boolean removeDefault,
      SuggestedFix.Builder suggestedFixBuilder) {
    ImmutableList<StatementTree> statements = getStatements(caseTree);

    Optional<StatementTree> singleStatement = Optional.empty();
    if (statements.size() == 1) {
      StatementTree at = statements.get(0);
      singleStatement = Optional.of(at);
    }
    if (singleStatement.isPresent()) {
      ImmutableList<ErrorProneComment> allComments =
          state.getTokensForNode(caseTree.getBody()).stream()
              .flatMap(errorProneToken -> errorProneToken.comments().stream())
              .collect(toImmutableList());
      // Can the RHS be made into an expression? e.g. `case null -> {return 0;}` becomes
      // `case null -> 0;`
      if (singleStatement.get() instanceof ReturnTree rt) {
        Range<Integer> printedRange =
            Range.closedOpen(
                getStartPosition(rt.getExpression()), state.getEndPosition(rt.getExpression()));

        StringBuilder replacementBuilder = new StringBuilder();
        String renderedOrphans =
            renderComments(
                computeOrphanedComments(allComments, caseRhsSourceCodeRange, printedRange));
        if (!renderedOrphans.isEmpty()) {
          replacementBuilder.append("\n").append(renderedOrphans).append("\n");
        }
        replacementBuilder.append(state.getSourceForNode(rt.getExpression())).append(";");
        if (removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_NULL_AND_DEFAULT)) {
          // LHS must be rendered to remove the "default"
          replacementBuilder.insert(
              0, renderNullDefaultKindPrefix(nullDefaultKind, removeDefault) + " -> ");
          suggestedFixBuilder.replace(caseTree, replacementBuilder.toString());
        } else {
          suggestedFixBuilder.replace(caseTree.getBody(), replacementBuilder.toString());
        }
      } else if (singleStatement.get() instanceof ThrowTree tt) {
        // RHS is just a single throw
        Range<Integer> printedRange =
            Range.closedOpen(getStartPosition(tt), state.getEndPosition(tt));

        StringBuilder replacementBuilder = new StringBuilder();
        String renderedOrphans =
            renderComments(
                computeOrphanedComments(allComments, caseRhsSourceCodeRange, printedRange));
        if (!renderedOrphans.isEmpty()) {
          replacementBuilder.append("\n").append(renderedOrphans).append("\n");
        }
        replacementBuilder.append(state.getSourceForNode(tt));
        if (removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_NULL_AND_DEFAULT)) {
          // LHS must be rendered to remove the "default"
          replacementBuilder.insert(
              0, renderNullDefaultKindPrefix(nullDefaultKind, removeDefault) + " -> ");
          suggestedFixBuilder.replace(caseTree, replacementBuilder.toString());
        } else {
          suggestedFixBuilder.replace(caseTree.getBody(), replacementBuilder.toString());
        }
      }
      return;
    }

    // Invariant: singleStatement is empty; zero or multiple statements on RHS
    // Can redundant braces be removed from the RHS?
    Tree at = caseTree.getBody();
    while (at instanceof BlockTree bt
        && bt.getStatements().size() == 1
        && bt.getStatements().get(0) instanceof BlockTree) {
      // Strip out this brace and descend into the inner block (which must exist)
      suggestedFixBuilder.replace(
          ASTHelpers.getStartPosition(bt), ASTHelpers.getStartPosition(bt) + 1, "");
      suggestedFixBuilder.replace(state.getEndPosition(bt) - 1, state.getEndPosition(bt), "");
      at = bt.getStatements().get(0);
    }

    // Transform relevant `return`s to `yield`s
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitReturn(ReturnTree returnTree, Void unused) {
        if (returnToScope.get(returnTree) == switchTree) {
          StringBuilder yieldStatementBuilder = new StringBuilder();
          yieldStatementBuilder
              .append("yield ")
              .append(state.getSourceForNode(returnTree.getExpression()))
              .append(";");
          suggestedFixBuilder.replace(returnTree, yieldStatementBuilder.toString());
        }
        return super.visitReturn(returnTree, null);
      }
    }.scan(TreePath.getPath(state.getPath(), caseTree), null);
    // LHS must be re-rendered to remove the "default"
    if (removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_NULL_AND_DEFAULT)) {
      suggestedFixBuilder.replace(
          getStartPosition(caseTree),
          getStartPosition(caseTree.getBody()),
          renderNullDefaultKindPrefix(nullDefaultKind, removeDefault) + " -> ");
    }
  }

  /**
   * Transforms the supplied switch into an assignment switch (e.g. {@code x = switch ... ;}).
   * Comments are preserved where possible. Precondition: the {@code AnalysisResult} must have
   * deduced that this conversion is possible.
   */
  private static SuggestedFix convertToAssignmentSwitch(
      SwitchTree switchTree,
      VisitorState state,
      AssignmentSwitchAnalysisResult assignmentSwitchAnalysisResult,
      boolean removeDefault) {
    SuggestedFix.Builder suggestedFixBuilder = SuggestedFix.builder();
    StringBuilder lhsBuilder = new StringBuilder();

    assignmentSwitchAnalysisResult
        .precedingVariableDeclaration()
        .ifPresent(
            variableTree -> {
              suggestedFixBuilder.delete(variableTree);

              lhsBuilder.append(
                  Streams.concat(
                          renderVariableTreeComments(variableTree, state).stream(),
                          renderVariableTreeAnnotations(variableTree, state).stream(),
                          Stream.of(renderVariableTreeFlags(variableTree)))
                      .collect(joining("\n")));

              // Local variables declared with "var" must unfortunately be handled as a special case
              // because getSourceForNode() returns null for the source code of a "var" declaration.
              String sourceForType =
                  hasImplicitType(variableTree, state)
                      ? "var"
                      : state.getSourceForNode(variableTree.getType());

              lhsBuilder.append(sourceForType).append(" ");
            });

    lhsBuilder
        .append(
            state.getSourceForNode(assignmentSwitchAnalysisResult.assignmentTargetOptional().get()))
        .append(" ")
        // Invariant: always present when a finding exists
        .append(assignmentSwitchAnalysisResult.assignmentSourceCodeOptional().get())
        .append(" ");
    suggestedFixBuilder.prefixWith(switchTree, lhsBuilder.toString());

    List<? extends CaseTree> cases = switchTree.getCases();
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);

      NullDefaultKind nullDefaultKind = analyzeCaseForNullAndDefault(caseTree);
      if (removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_DEFAULT)) {
        // Delete removed default (and its code) entirely
        suggestedFixBuilder.replace(caseTree, "");
        continue;
      }

      // Invariant: exactly one statement on RHS
      ImmutableList<StatementTree> statements = getStatements(caseTree);
      StatementTree statement = statements.get(0);
      ImmutableList<ErrorProneComment> allComments =
          state.getTokensForNode(caseTree.getBody()).stream()
              .flatMap(errorProneToken -> errorProneToken.comments().stream())
              .collect(toImmutableList());
      Range<Integer> caseRhsSourceCodeRange =
          Range.closedOpen(getStartPosition(statement), state.getEndPosition(statement));

      Optional<Tree> treeOptional = Optional.empty();
      boolean addSemicolonSuffix = false;
      // Always throw or valid assignment/compound assignment because checked in analysis, thus
      // treeOptional is always re-assigned
      if (statement instanceof ThrowTree throwTree) {
        treeOptional = Optional.of(throwTree);
      } else if (statement instanceof ExpressionStatementTree expressionStatementTree) {
        if (expressionStatementTree.getExpression()
            instanceof CompoundAssignmentTree compoundAssignmentTree) {
          treeOptional = Optional.of(compoundAssignmentTree.getExpression());
          addSemicolonSuffix = true;
        } else if (expressionStatementTree.getExpression()
            instanceof AssignmentTree assignmentTree) {
          treeOptional = Optional.of(assignmentTree.getExpression());
          addSemicolonSuffix = true;
        }
      }
      StringBuilder replacementBuilder = new StringBuilder();

      // Invariant: treeOptional always present
      treeOptional.ifPresent(
          tree -> {
            ImmutableList<ErrorProneComment> orphanedComments =
                computeOrphanedComments(
                    allComments,
                    Range.closedOpen(getStartPosition(tree), state.getEndPosition(tree)),
                    caseRhsSourceCodeRange);
            String renderedOrphans = renderComments(orphanedComments);
            if (!renderedOrphans.isEmpty()) {
              replacementBuilder.append("\n").append(renderedOrphans).append("\n");
            }
            replacementBuilder.append(state.getSourceForNode(tree));
          });

      if (addSemicolonSuffix) {
        replacementBuilder.append(";");
      }

      if (removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_NULL_AND_DEFAULT)) {
        // RHS & LHS must be re-rendered to remove the "default"
        replacementBuilder.insert(
            0, renderNullDefaultKindPrefix(nullDefaultKind, removeDefault) + " -> ");
        suggestedFixBuilder.replace(caseTree, replacementBuilder.toString());
      } else {
        // Just re-render the RHS
        suggestedFixBuilder.replace(caseTree.getBody(), replacementBuilder.toString());
      }
    } // case loop

    // Add a semicolon after the switch, since it's now a switch expression
    suggestedFixBuilder.postfixWith(switchTree, ";");

    if (removeDefault) {
      suggestedFixBuilder.setShortDescription(REMOVE_DEFAULT_CASE_SHORT_DESCRIPTION);
    }
    return suggestedFixBuilder.build();
  }

  /**
   * Converts a switch statement to a simplified form. Where possible, a single suggested fix will
   * contain multiple simplifications.
   */
  private static SuggestedFix convertToSimplifiedCommon(
      List<? extends CaseTree> cases, VisitorState state, boolean removeDefault) {

    SuggestedFix.Builder suggestedFixBuilder = SuggestedFix.builder();
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);

      NullDefaultKind nullDefaultKind = analyzeCaseForNullAndDefault(caseTree);
      if (removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_DEFAULT)) {
        // Delete removed default (and its code) entirely
        suggestedFixBuilder.delete(caseTree);
        continue;
      }
      // The entire case must be re-rendered to remove a "default"
      boolean renderWholeCase =
          removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_NULL_AND_DEFAULT);

      ImmutableList<StatementTree> statements = getStatements(caseTree);
      Optional<YieldTree> singleYieldStatement = Optional.empty();
      if (statements.size() == 1 && statements.get(0) instanceof YieldTree yieldTree) {
        singleYieldStatement = Optional.of(yieldTree);
      }

      ImmutableList<ErrorProneComment> allComments =
          state.getTokensForNode(caseTree.getBody()).stream()
              .flatMap(errorProneToken -> errorProneToken.comments().stream())
              .collect(toImmutableList());

      if (singleYieldStatement.isPresent()) {
        simplifySingleYieldStatement(
            singleYieldStatement.get(),
            state,
            caseTree,
            suggestedFixBuilder,
            allComments,
            renderWholeCase,
            nullDefaultKind,
            removeDefault);
      } else if (renderWholeCase) {
        StringBuilder commentBuilder = new StringBuilder();
        Range<Integer> caseRhsSourceCodeRange =
            Range.closedOpen(
                getStartPosition(caseTree.getBody()), state.getEndPosition(caseTree.getBody()));
        ImmutableList<ErrorProneComment> orphanedComments =
            computeOrphanedComments(allComments, caseRhsSourceCodeRange, caseRhsSourceCodeRange);
        String renderedOrphans = renderComments(orphanedComments);
        if (!renderedOrphans.isEmpty()) {
          commentBuilder.append("\n").append(renderedOrphans).append("\n");
        }

        // Look for brace simplification on the RHS
        Tree at = caseTree.getBody();
        while (at instanceof BlockTree bt
            && bt.getStatements().size() == 1
            && bt.getStatements().get(0) instanceof BlockTree) {
          // Strip out this brace and descend into the inner block (which must exist)
          at = bt.getStatements().get(0);
        }

        // Render both LHS and RHS
        StringBuilder lhs = new StringBuilder();
        lhs.append(renderNullDefaultKindPrefix(nullDefaultKind, removeDefault));
        if (nullDefaultKind.equals(NullDefaultKind.KIND_NEITHER)) {
          lhs.append(printCaseExpressionsOrPatternAndGuard(caseTree, state));
        }
        suggestedFixBuilder.replace(
            caseTree, lhs + " -> " + commentBuilder + state.getSourceForNode(at));
      } else {
        // Not a single yield statement, and we don't need to re-render the whole case
        // Can redundant braces be removed from the RHS?
        Tree at = caseTree.getBody();
        while (at instanceof BlockTree bt
            && bt.getStatements().size() == 1
            && bt.getStatements().get(0) instanceof BlockTree) {
          // Strip out this brace and descend into the inner block (which must exist)
          suggestedFixBuilder.replace(
              ASTHelpers.getStartPosition(bt), ASTHelpers.getStartPosition(bt) + 1, "");
          suggestedFixBuilder.replace(state.getEndPosition(bt) - 1, state.getEndPosition(bt), "");
          at = bt.getStatements().get(0);
        }

        // Braces are not required if there is exactly one statement on the right hand of the arrow,
        // and it's either an ExpressionStatement or a Throw.  Refer to JLS 14 §14.11.1
        if (at instanceof BlockTree blockTree && blockTree.getStatements().size() == 1) {
          StatementTree statement = blockTree.getStatements().get(0);
          if (statement instanceof ThrowTree || statement instanceof ExpressionStatementTree) {
            suggestedFixBuilder.replace(
                ASTHelpers.getStartPosition(blockTree),
                ASTHelpers.getStartPosition(blockTree) + 1,
                "");
            suggestedFixBuilder.replace(
                state.getEndPosition(blockTree) - 1, state.getEndPosition(blockTree), "");
          }
        }
      }
    }

    if (removeDefault) {
      suggestedFixBuilder.setShortDescription(REMOVE_DEFAULT_CASE_SHORT_DESCRIPTION);
    }
    return suggestedFixBuilder.build();
  }

  /** Returns a range that encloses the given comment, offset by the given start position. */
  private static Range<Integer> buildCommentRange(ErrorProneComment comment, int startPosition) {
    return Range.closedOpen(comment.getPos() + startPosition, comment.getEndPos() + startPosition);
  }

  /**
   * Returns the comments that are within the case's RHS source code range but outside the printed
   * range for the RHS.
   */
  private static ImmutableList<ErrorProneComment> computeOrphanedComments(
      ImmutableList<ErrorProneComment> allComments,
      Range<Integer> caseRhsSourceCodeRange,
      Range<Integer> printedRange) {
    return allComments.stream()
        // Within this case's source code
        .filter(
            comment ->
                caseRhsSourceCodeRange.encloses(
                    buildCommentRange(comment, caseRhsSourceCodeRange.lowerEndpoint())))
        // But outside the printed range for the RHS
        .filter(
            comment ->
                !buildCommentRange(comment, caseRhsSourceCodeRange.lowerEndpoint())
                    .isConnected(printedRange))
        .collect(toImmutableList());
  }

  record AnalysisResult(
      // A list of conversions that can be performed on the switch
      ImmutableList<Convertible> convertibleFindings) {}

  record AssignmentSwitchAnalysisState(
      // Current qualification for conversion based on cases examined so far
      CaseQualifications assignmentSwitchCaseQualifications,
      // What is being assigned to (if any)
      Optional<ExpressionTree> assignmentTargetOptional,
      // The kind of assignment being performed (if any)
      Optional<Tree.Kind> assignmentExpressionKindOptional,
      // The tree of the assignment being performed (if any)
      Optional<ExpressionTree> assignmentTreeOptional) {}

  record AssignmentSwitchAnalysisResult(
      // The switch can be converted to an assignment switch
      boolean canConvertToAssignmentSwitch,
      // The variable declaration that preceded the switch (if any)
      Optional<VariableTree> precedingVariableDeclaration,
      // What is being assigned to (if any)
      Optional<ExpressionTree> assignmentTargetOptional,
      // The kind of assignment being performed (if any)
      Optional<Tree.Kind> assignmentKindOptional,
      // Whether the default case can be removed
      boolean canRemoveDefault,
      // The range of source code covered by each case
      Optional<String> assignmentSourceCodeOptional)
      implements Convertible {
    @Override
    public SuggestedFix convert(SwitchTree switchTree, VisitorState state) {
      return convertToAssignmentSwitch(switchTree, state, this, canRemoveDefault);
    }
  }

  record ReturnSwitchAnalysisResult(
      // The switch can be converted to a return switch
      boolean canConvertToReturnSwitch,
      // Whether the default case can be removed
      boolean canRemoveDefault,
      // Range of source code covered by each case
      ImmutableList<Range<Integer>> caseRhsSourceCodeRanges)
      implements Convertible {
    @Override
    public SuggestedFix convert(SwitchTree switchTree, VisitorState state) {
      return convertToReturnSwitch(switchTree, state, this, canRemoveDefault);
    }
  }

  record SimplifyAnalysisResult(
      // At least one case can be simplified
      boolean canSimplify,
      // Whether the default case can be removed
      boolean canRemoveDefault,
      // Range of source code covered by each case
      ImmutableList<Range<Integer>> caseRhsSourceCodeRanges)
      implements Convertible {
    @Override
    public SuggestedFix convert(SwitchTree switchTree, VisitorState state) {
      return convertToSimplifiedCommon(switchTree.getCases(), state, canRemoveDefault);
    }

    @Override
    public SuggestedFix convert(SwitchExpressionTree switchExpressionTree, VisitorState state) {
      return convertToSimplifiedCommon(switchExpressionTree.getCases(), state, canRemoveDefault);
    }
  }

  private interface Convertible {
    default SuggestedFix convert(SwitchTree switchTree, VisitorState state) {
      throw new UnsupportedOperationException("Not supported for SwitchTree");
    }

    default SuggestedFix convert(SwitchExpressionTree switchTree, VisitorState state) {
      throw new UnsupportedOperationException("Not supported for SwitchExpressionTree");
    }
  }
}
