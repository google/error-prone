/*
 * Copyright 2022 The Error Prone Authors.
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
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSwitchDefault;
import static com.sun.source.tree.Tree.Kind.BLOCK;
import static com.sun.source.tree.Tree.Kind.BREAK;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.RETURN;
import static com.sun.source.tree.Tree.Kind.THROW;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneComment;
import com.google.errorprone.util.Reachability;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.Pretty;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;

/** Checks for statement switches that can be expressed as an equivalent expression switch. */
@BugPattern(
    severity = WARNING,
    summary = "This statement switch can be converted to an equivalent expression switch")
public final class StatementSwitchToExpressionSwitch extends BugChecker
    implements SwitchTreeMatcher {
  // Braces are not required if there is exactly one statement on the right hand of the arrow, and
  // it's either an ExpressionStatement or a Throw.  Refer to JLS 14 §14.11.1
  private static final ImmutableSet<Kind> KINDS_CONVERTIBLE_WITHOUT_BRACES =
      ImmutableSet.of(THROW, EXPRESSION_STATEMENT);
  private static final ImmutableSet<Kind> KINDS_RETURN_OR_THROW = ImmutableSet.of(THROW, RETURN);
  private static final Pattern FALL_THROUGH_PATTERN =
      Pattern.compile("\\bfalls?.?(through|out)\\b", Pattern.CASE_INSENSITIVE);
  // Default (negative) result for assignment switch conversion analysis. Note that the value is
  // immutable.
  private static final AssignmentSwitchAnalysisResult DEFAULT_ASSIGNMENT_SWITCH_ANALYSIS_RESULT =
      AssignmentSwitchAnalysisResult.of(
          /* canConvertToAssignmentSwitch= */ false,
          /* assignmentTargetOptional= */ Optional.empty(),
          /* assignmentKindOptional= */ Optional.empty(),
          /* assignmentSourceCodeOptional= */ Optional.empty());
  // Default (negative) result for overall analysis. Note that the value is immutable.
  private static final AnalysisResult DEFAULT_ANALYSIS_RESULT =
      AnalysisResult.of(
          /* canConvertDirectlyToExpressionSwitch= */ false,
          /* canConvertToReturnSwitch= */ false,
          DEFAULT_ASSIGNMENT_SWITCH_ANALYSIS_RESULT,
          /* groupedWithNextCase= */ ImmutableList.of());
  private static final String EQUALS_STRING = "=";

  // Tri-state to represent the fall-thru control flow of a particular case of a particular
  // statement switch
  private static enum CaseFallThru {
    DEFINITELY_DOES_NOT_FALL_THRU,
    MAYBE_FALLS_THRU,
    DEFINITELY_DOES_FALL_THRU
  };

  // Tri-state to represent whether cases within a single switch statement meet an (unspecified)
  // qualification predicate
  static enum CaseQualifications {
    NO_CASES_ASSESSED,
    ALL_CASES_QUALIFY,
    SOME_OR_ALL_CASES_DONT_QUALIFY
  }

  private final boolean enableDirectConversion;
  private final boolean enableReturnSwitchConversion;
  private final boolean enableAssignmentSwitchConversion;

  @Inject
  StatementSwitchToExpressionSwitch(ErrorProneFlags flags) {
    this.enableDirectConversion =
        true
            || flags
                .getBoolean("StatementSwitchToExpressionSwitch:EnableDirectConversion")
                .orElse(false);
    this.enableReturnSwitchConversion =
        flags
            .getBoolean("StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion")
            .orElse(false);
    this.enableAssignmentSwitchConversion =
        flags
            .getBoolean("StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion")
            .orElse(false);
  }

  @Override
  public Description matchSwitch(SwitchTree switchTree, VisitorState state) {
    // if (!SourceVersion.supportsSwitchExpressions(state.context)) {
    //  return NO_MATCH;
    // }

    AnalysisResult analysisResult = analyzeSwitchTree(switchTree, state);

    List<SuggestedFix> suggestedFixes = new ArrayList<>();
    if (enableReturnSwitchConversion && analysisResult.canConvertToReturnSwitch()) {
      suggestedFixes.add(convertToReturnSwitch(switchTree, state, analysisResult));
    }
    if (enableAssignmentSwitchConversion
        && analysisResult.assignmentSwitchAnalysisResult().canConvertToAssignmentSwitch()) {
      suggestedFixes.add(convertToAssignmentSwitch(switchTree, state, analysisResult));
    }
    if (enableDirectConversion && analysisResult.canConvertDirectlyToExpressionSwitch()) {
      suggestedFixes.add(convertDirectlyToExpressionSwitch(switchTree, state, analysisResult));
    }

    return suggestedFixes.isEmpty()
        ? NO_MATCH
        : buildDescription(switchTree).addAllFixes(suggestedFixes).build();
  }

  /**
   * Analyzes a {@code SwitchTree}, and determines any possible findings and suggested fixes related
   * to expression switches that can be made. Does not report any findings or suggested fixes up to
   * the Error Prone framework.
   */
  private static AnalysisResult analyzeSwitchTree(SwitchTree switchTree, VisitorState state) {
    // Don't convert switch within switch
    if (ASTHelpers.findEnclosingNode(state.getPath(), SwitchTree.class) != null) {
      return DEFAULT_ANALYSIS_RESULT;
    }

    List<? extends CaseTree> cases = switchTree.getCases();
    // A given case is said to have definite control flow if we are sure it always or never falls
    // thru at the end of its statement block
    boolean allCasesHaveDefiniteControlFlow = true;
    // A case is said to be grouped with the next one if we are sure it can appear together with the
    // next case on the left hand side of the arrow when converted to an expression switch.  For
    // example "case A,B -> ..."
    List<Boolean> groupedWithNextCase = new ArrayList<>(Collections.nCopies(cases.size(), false));

    // Set of all enum values (names) explicitly listed in a case tree
    Set<String> handledEnumValues = new HashSet<>();
    // Does each case consist solely of returning a (non-void) expression?
    CaseQualifications returnSwitchCaseQualifications = CaseQualifications.NO_CASES_ASSESSED;
    // Does each case consist solely of a throw or the same symbol assigned in the same way?
    AssignmentSwitchAnalysisState assignmentSwitchAnalysisState =
        AssignmentSwitchAnalysisState.of(
            /* assignmentSwitchCaseQualifications= */ CaseQualifications.NO_CASES_ASSESSED,
            /* assignmentTargetOptional= */ Optional.empty(),
            /* assignmentKindOptional= */ Optional.empty(),
            /* assignmentTreeOptional= */ Optional.empty());

    boolean hasDefaultCase = false;
    // One-pass scan through each case in switch
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);
      boolean isDefaultCase = caseTree.getExpressions().isEmpty();
      hasDefaultCase |= isDefaultCase;
      // Accumulate enum values included in this case
      handledEnumValues.addAll(
          caseTree.getExpressions().stream()
              .filter(IdentifierTree.class::isInstance)
              .map(expressionTree -> ((IdentifierTree) expressionTree).getName().toString())
              .collect(toImmutableSet()));
      boolean isLastCaseInSwitch = caseIndex == cases.size() - 1;

      List<? extends StatementTree> statements = getStatements(caseTree);
      CaseFallThru caseFallThru = CaseFallThru.MAYBE_FALLS_THRU;
      if (statements == null) {
        // This case must be of kind CaseTree.CaseKind.RULE, and thus this is already an expression
        // switch; no need to continue analysis.
        return DEFAULT_ANALYSIS_RESULT;
      } else if (statements.isEmpty()) {
        // If the code for this case is just an empty block, then it must fall thru
        caseFallThru = CaseFallThru.DEFINITELY_DOES_FALL_THRU;
        // Can group with the next case (unless this is the last case)
        groupedWithNextCase.set(caseIndex, !isLastCaseInSwitch);
      } else {
        groupedWithNextCase.set(caseIndex, false);
        // Code for this case is non-empty
        if (areStatementsConvertibleToExpressionSwitch(statements, isLastCaseInSwitch)) {
          caseFallThru = CaseFallThru.DEFINITELY_DOES_NOT_FALL_THRU;
        }
      }
      if (isDefaultCase) {
        // The "default" case has distinct semantics; don't allow anything to fall into or out of
        // default case.  Exception: allowed to fall out of default case if it's the last case
        boolean fallsIntoDefaultCase = (caseIndex > 0) && groupedWithNextCase.get(caseIndex - 1);
        if (isLastCaseInSwitch) {
          allCasesHaveDefiniteControlFlow &= !fallsIntoDefaultCase;
        } else {
          allCasesHaveDefiniteControlFlow &=
              !fallsIntoDefaultCase
                  && caseFallThru.equals(CaseFallThru.DEFINITELY_DOES_NOT_FALL_THRU);
        }
      } else {
        // Cases other than default
        allCasesHaveDefiniteControlFlow &= !caseFallThru.equals(CaseFallThru.MAYBE_FALLS_THRU);
      }

      // Analyze for return switch and assignment switch conversion
      returnSwitchCaseQualifications =
          analyzeCaseForReturnSwitch(
              returnSwitchCaseQualifications, statements, isLastCaseInSwitch);
      assignmentSwitchAnalysisState =
          analyzeCaseForAssignmentSwitch(
              assignmentSwitchAnalysisState, statements, isLastCaseInSwitch);
    }

    boolean exhaustive =
        isSwitchExhaustive(
            hasDefaultCase, handledEnumValues, ASTHelpers.getType(switchTree.getExpression()));

    boolean canConvertToReturnSwitch =
        // All restrictions for direct conversion apply
        allCasesHaveDefiniteControlFlow
            // Does each case consist solely of returning a (non-void) expression?
            && returnSwitchCaseQualifications.equals(CaseQualifications.ALL_CASES_QUALIFY)
            // The switch must be exhaustive (at compile time)
            && exhaustive;
    boolean canConvertToAssignmentSwitch =
        // All restrictions for direct conversion apply
        allCasesHaveDefiniteControlFlow
            // Does each case consist solely of a throw or the same symbol assigned in the same way?
            && assignmentSwitchAnalysisState
                .assignmentSwitchCaseQualifications()
                .equals(CaseQualifications.ALL_CASES_QUALIFY)
            // The switch must be exhaustive (at compile time)
            && exhaustive;

    return AnalysisResult.of(
        /* canConvertDirectlyToExpressionSwitch= */ allCasesHaveDefiniteControlFlow,
        canConvertToReturnSwitch,
        AssignmentSwitchAnalysisResult.of(
            canConvertToAssignmentSwitch,
            assignmentSwitchAnalysisState.assignmentTargetOptional(),
            assignmentSwitchAnalysisState.assignmentExpressionKindOptional(),
            assignmentSwitchAnalysisState
                .assignmentTreeOptional()
                .map(StatementSwitchToExpressionSwitch::renderJavaSourceOfAssignment)),
        ImmutableList.copyOf(groupedWithNextCase));
  }

  /**
   * Renders the Java source code for a [compound] assignment operator. The parameter must be either
   * an {@code AssignmentTree} or a {@code CompoundAssignmentTree}.
   */
  private static String renderJavaSourceOfAssignment(ExpressionTree tree) {
    // Simple assignment tree?
    if (tree instanceof JCAssign) {
      return EQUALS_STRING;
    }

    // Invariant: must be a compound assignment tree
    JCAssignOp jcAssignOp = (JCAssignOp) tree;
    Pretty pretty = new Pretty(new StringWriter(), /* sourceOutput= */ true);
    return pretty.operatorName(jcAssignOp.getTag().noAssignOp()) + EQUALS_STRING;
  }

  /**
   * Analyze a single {@code case} of a single {@code switch} statement to determine whether it is
   * convertible to a return switch. The supplied {@code previousCaseQualifications} is updated and
   * returned based on this analysis.
   */
  private static CaseQualifications analyzeCaseForReturnSwitch(
      CaseQualifications previousCaseQualifications,
      List<? extends StatementTree> statements,
      boolean isLastCaseInSwitch) {

    if (statements.isEmpty() && !isLastCaseInSwitch) {
      // This case can be grouped with the next and further analyzed there
      return previousCaseQualifications;
    }

    // Statement blocks on the RHS are not currently supported, except for trivial blocks of
    // statements expressions followed by a return or throw
    // TODO: handle more complex statement blocks that can be converted using 'yield'
    if (statements.isEmpty()) {
      return CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY;
    }
    StatementTree lastStatement = getLast(statements);
    if (!statements.subList(0, statements.size() - 1).stream()
            .allMatch(statement -> statement.getKind().equals(EXPRESSION_STATEMENT))
        || !KINDS_RETURN_OR_THROW.contains(lastStatement.getKind())) {
      return CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY;
    }

    // For this analysis, cases that don't return something can be disregarded
    if (!lastStatement.getKind().equals(RETURN)) {
      return previousCaseQualifications;
    }

    if (!previousCaseQualifications.equals(CaseQualifications.NO_CASES_ASSESSED)) {
      // There is no need to inspect the type compatibility of the return values, because if they
      // were incompatible then the compilation would have failed before reaching this point
      return previousCaseQualifications;
    }

    // This is the first value-returning case that we are examining
    Type returnType = ASTHelpers.getType(((ReturnTree) lastStatement).getExpression());
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
      List<? extends StatementTree> statements,
      boolean isLastCaseInSwitch) {

    CaseQualifications caseQualifications =
        previousAssignmentSwitchAnalysisState.assignmentSwitchCaseQualifications();
    Optional<Kind> assignmentExpressionKindOptional =
        previousAssignmentSwitchAnalysisState.assignmentExpressionKindOptional();
    Optional<ExpressionTree> assignmentTargetOptional =
        previousAssignmentSwitchAnalysisState.assignmentTargetOptional();
    Optional<ExpressionTree> assignmentTreeOptional =
        previousAssignmentSwitchAnalysisState.assignmentTreeOptional();

    if (statements.isEmpty()) {
      return isLastCaseInSwitch
          // An empty last case cannot be an assignment
          ? AssignmentSwitchAnalysisState.of(
              CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY,
              assignmentTargetOptional,
              assignmentExpressionKindOptional,
              assignmentTreeOptional)
          // This case can be grouped with the next and further analyzed there
          : previousAssignmentSwitchAnalysisState;
    }

    // Invariant: statements is never empty
    StatementTree firstStatement = statements.get(0);
    Kind firstStatementKind = firstStatement.getKind();

    // Can convert one statement or two with the second being an unconditional break
    boolean expressionOrExpressionBreak =
        (statements.size() == 1 && KINDS_CONVERTIBLE_WITHOUT_BRACES.contains(firstStatementKind))
            || (KINDS_CONVERTIBLE_WITHOUT_BRACES.contains(firstStatementKind)
                // If the second statement is a break, then there must be exactly two statements
                && statements.get(1).getKind().equals(BREAK)
                && ((BreakTree) statements.get(1)).getLabel() == null);
    if (!expressionOrExpressionBreak) {
      // Conversion of this block is not supported
      return AssignmentSwitchAnalysisState.of(
          CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY,
          assignmentTargetOptional,
          assignmentExpressionKindOptional,
          assignmentTreeOptional);
    }

    if (!firstStatement.getKind().equals(EXPRESSION_STATEMENT)) {
      // Throws don't affect the assignment analysis
      return previousAssignmentSwitchAnalysisState;
    }

    ExpressionTree expression = ((ExpressionStatementTree) firstStatement).getExpression();
    Optional<ExpressionTree> caseAssignmentTargetOptional = Optional.empty();
    Optional<Tree.Kind> caseAssignmentKindOptional = Optional.empty();
    Optional<ExpressionTree> caseAssignmentTreeOptional = Optional.empty();

    // The assignment could be a normal assignment ("=") or a compound assignment (e.g. "+=")
    if (expression instanceof CompoundAssignmentTree compoundAssignmentTree) {
      caseAssignmentTargetOptional = Optional.of(compoundAssignmentTree.getVariable());
      caseAssignmentKindOptional = Optional.of(compoundAssignmentTree.getKind());
      caseAssignmentTreeOptional = Optional.of(expression);
    } else if (expression instanceof AssignmentTree assignmentTree) {
      caseAssignmentTargetOptional = Optional.of(assignmentTree.getVariable());
      caseAssignmentKindOptional = Optional.of(Tree.Kind.ASSIGNMENT);
      caseAssignmentTreeOptional = Optional.of(expression);
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
    return AssignmentSwitchAnalysisState.of(
        caseQualifications,
        /* assignmentTargetOptional= */ assignmentTargetOptional.isEmpty()
            ? caseAssignmentTargetOptional
            : assignmentTargetOptional,
        /* assignmentKindOptional= */ assignmentExpressionKindOptional.isEmpty()
            ? caseAssignmentKindOptional
            : assignmentExpressionKindOptional,
        /* assignmentTreeOptional= */ assignmentTreeOptional.isEmpty()
            ? caseAssignmentTreeOptional
            : assignmentTreeOptional);
  }

  /**
   * In a switch with multiple assignments, determine whether a subsequent assignment target is
   * compatible with the first assignment target.
   */
  private static boolean isCompatibleWithFirstAssignment(
      Optional<ExpressionTree> assignmentTargetOptional,
      Optional<ExpressionTree> caseAssignmentTargetOptional) {

    if (assignmentTargetOptional.isEmpty() || caseAssignmentTargetOptional.isEmpty()) {
      return false;
    }

    Symbol assignmentTargetSymbol = getSymbol(assignmentTargetOptional.get());
    // For non-symbol assignment targets, multiple assignments are not currently supported
    if (assignmentTargetSymbol == null) {
      return false;
    }

    Symbol caseAssignmentTargetSymbol = getSymbol(caseAssignmentTargetOptional.get());
    return Objects.equals(assignmentTargetSymbol, caseAssignmentTargetSymbol);
  }

  /**
   * Determines whether the supplied case's {@code statements} are capable of being mapped to an
   * equivalent expression switch case (without repeating code), returning {@code true} if so.
   * Detection is based on an ad-hoc algorithm that is not guaranteed to detect every convertible
   * instance (whether a given block can fall-thru is an undecidable proposition in general).
   */
  private static boolean areStatementsConvertibleToExpressionSwitch(
      List<? extends StatementTree> statements, boolean isLastCaseInSwitch) {
    // For the last case, we can always convert (fall through has no effect)
    if (isLastCaseInSwitch) {
      return true;
    }
    // Always falls-thru; can combine with next case (if present)
    if (statements.isEmpty()) {
      return true;
    }
    // We only look at whether the block can fall-thru; javac would have already reported an
    // error if the last statement in the block weren't reachable (in the JLS sense).  Since we know
    // it is reachable, whether the block can complete normally is independent of any preceding
    // statements.  If the last statement cannot complete normally, then the block cannot, and thus
    // the block cannot fall-thru
    return !Reachability.canCompleteNormally(getLast(statements));
  }

  /**
   * Transforms the supplied statement switch into an expression switch directly. In this
   * conversion, each nontrivial statement block is mapped one-to-one to a new {@code Expression} or
   * {@code StatementBlock} on the right-hand side. Comments are preserved where possible.
   */
  private static SuggestedFix convertDirectlyToExpressionSwitch(
      SwitchTree switchTree, VisitorState state, AnalysisResult analysisResult) {

    List<? extends CaseTree> cases = switchTree.getCases();
    ImmutableList<ErrorProneComment> allSwitchComments =
        state.getTokensForNode(switchTree).stream()
            .flatMap(errorProneToken -> errorProneToken.comments().stream())
            .collect(toImmutableList());

    StringBuilder replacementCodeBuilder = new StringBuilder();
    replacementCodeBuilder
        .append("switch ")
        .append(state.getSourceForNode(switchTree.getExpression()))
        .append(" {");

    StringBuilder groupedCaseCommentsAccumulator = null;
    boolean firstCaseInGroup = true;
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);
      boolean isDefaultCase = isSwitchDefault(caseTree);

      // For readability, filter out trailing unlabelled break statement because these become a
      // No-Op when used inside expression switches
      ImmutableList<StatementTree> filteredStatements = filterOutRedundantBreak(caseTree);
      String transformedBlockSource = transformBlock(caseTree, state, filteredStatements);

      if (firstCaseInGroup) {
        groupedCaseCommentsAccumulator =
            new StringBuilder(
                caseIndex == 0
                    ? extractCommentsBeforeFirstCase(switchTree, allSwitchComments).orElse("")
                    : "");

        replacementCodeBuilder.append("\n  ");
        if (!isDefaultCase) {
          replacementCodeBuilder.append("case ");
        }
      }
      replacementCodeBuilder.append(
          isDefaultCase ? "default" : printCaseExpressions(caseTree, state));

      Optional<String> commentsAfterCaseOptional =
          extractCommentsAfterCase(switchTree, allSwitchComments, state, caseIndex);
      if (analysisResult.groupedWithNextCase().get(caseIndex)) {
        firstCaseInGroup = false;
        replacementCodeBuilder.append(", ");
        // Capture comments from this case so they can be added to the group's transformed case
        if (!transformedBlockSource.trim().isEmpty()) {
          String commentsToAppend = removeFallThruLines(transformedBlockSource);
          if (groupedCaseCommentsAccumulator.length() > 0) {
            groupedCaseCommentsAccumulator.append("\n");
          }
          groupedCaseCommentsAccumulator.append(commentsToAppend);
        }

        if (commentsAfterCaseOptional.isPresent()) {
          if (groupedCaseCommentsAccumulator.length() > 0) {
            groupedCaseCommentsAccumulator.append("\n");
          }
          groupedCaseCommentsAccumulator.append(commentsAfterCaseOptional.get());
        }

        // Add additional cases to the list on the lhs of the arrow
        continue;
      } else {
        // Extract comments (if any) preceding break that was removed as redundant
        Optional<String> commentsBeforeRemovedBreak =
            extractCommentsBeforeRemovedBreak(caseTree, state, filteredStatements);

        // Join together all comments and code, separating with newlines
        transformedBlockSource =
            Joiner.on("\n")
                .skipNulls()
                .join(
                    // This case is the last case in its group, so insert any comments from prior
                    // grouped cases first
                    groupedCaseCommentsAccumulator.length() == 0
                        ? null
                        : groupedCaseCommentsAccumulator.toString(),
                    transformedBlockSource.isEmpty() ? null : transformedBlockSource.trim(),
                    commentsBeforeRemovedBreak.orElse(null),
                    commentsAfterCaseOptional.orElse(null));
      }

      replacementCodeBuilder.append(" -> ");

      if (filteredStatements.isEmpty()) {
        // Transformed block has no code
        String trimmedTransformedBlockSource = transformedBlockSource.trim();
        // If block is just space or a single "break;" with no explanatory comments, then remove
        // it to eliminate redundancy and improve readability
        if (trimmedTransformedBlockSource.isEmpty()
            || trimmedTransformedBlockSource.equals("break;")) {
          replacementCodeBuilder.append("{}");
        } else {
          replacementCodeBuilder.append("{\n").append(transformedBlockSource).append("\n}");
        }
      } else {
        // Transformed block has code
        // To improve readability, don't use braces on the rhs if not needed
        if (shouldTransformCaseWithoutBraces(filteredStatements)) {
          // No braces needed
          replacementCodeBuilder.append("\n").append(transformedBlockSource);
        } else {
          // Use braces on the rhs
          replacementCodeBuilder.append("{\n").append(transformedBlockSource).append("\n}");
        }
      }

      firstCaseInGroup = true;
    } // case loop

    // Close the switch statement
    replacementCodeBuilder.append("\n}");

    return SuggestedFix.builder().replace(switchTree, replacementCodeBuilder.toString()).build();
  }

  /**
   * Transforms the supplied statement switch into a {@code return switch ...} style of expression
   * switch. In this conversion, each nontrivial statement block is mapped one-to-one to a new
   * expression on the right-hand side of the arrow. Comments are preserved where possible.
   * Precondition: the {@code AnalysisResult} for the {@code SwitchTree} must have deduced that this
   * conversion is possible.
   */
  private static SuggestedFix convertToReturnSwitch(
      SwitchTree switchTree, VisitorState state, AnalysisResult analysisResult) {

    List<StatementTree> statementsToDelete = new ArrayList<>();
    List<? extends CaseTree> cases = switchTree.getCases();
    ImmutableList<ErrorProneComment> allSwitchComments =
        state.getTokensForNode(switchTree).stream()
            .flatMap(errorProneToken -> errorProneToken.comments().stream())
            .collect(toImmutableList());
    StringBuilder replacementCodeBuilder = new StringBuilder();
    replacementCodeBuilder
        .append("return switch ")
        .append(state.getSourceForNode(switchTree.getExpression()))
        .append(" {");

    StringBuilder groupedCaseCommentsAccumulator = null;
    boolean firstCaseInGroup = true;
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);
      boolean isDefaultCase = isSwitchDefault(caseTree);

      String transformedBlockSource =
          transformReturnOrThrowBlock(caseTree, state, getStatements(caseTree));

      if (firstCaseInGroup) {
        groupedCaseCommentsAccumulator =
            new StringBuilder(
                caseIndex == 0
                    ? extractCommentsBeforeFirstCase(switchTree, allSwitchComments).orElse("")
                    : "");

        replacementCodeBuilder.append("\n  ");
        if (!isDefaultCase) {
          replacementCodeBuilder.append("case ");
        }
      }
      replacementCodeBuilder.append(
          isDefaultCase ? "default" : printCaseExpressions(caseTree, state));

      Optional<String> commentsAfterCaseOptional =
          extractCommentsAfterCase(switchTree, allSwitchComments, state, caseIndex);
      if (analysisResult.groupedWithNextCase().get(caseIndex)) {
        firstCaseInGroup = false;
        replacementCodeBuilder.append(", ");
        // Capture comments from this case so they can be added to the group's transformed case
        if (!transformedBlockSource.trim().isEmpty()) {
          String commentsToAppend = removeFallThruLines(transformedBlockSource);
          if (groupedCaseCommentsAccumulator.length() > 0) {
            groupedCaseCommentsAccumulator.append("\n");
          }
          groupedCaseCommentsAccumulator.append(commentsToAppend);
        }

        if (commentsAfterCaseOptional.isPresent()) {
          if (groupedCaseCommentsAccumulator.length() > 0) {
            groupedCaseCommentsAccumulator.append("\n");
          }
          groupedCaseCommentsAccumulator.append(commentsAfterCaseOptional.get());
        }

        // Add additional cases to the list on the lhs of the arrow
        continue;
      } else {
        // Join together all comments and code, separating with newlines
        transformedBlockSource =
            Joiner.on("\n")
                .skipNulls()
                .join(
                    // This case is the last case in its group, so insert any comments from prior
                    // grouped cases first
                    groupedCaseCommentsAccumulator.length() == 0
                        ? null
                        : groupedCaseCommentsAccumulator.toString(),
                    transformedBlockSource.isEmpty() ? null : transformedBlockSource,
                    commentsAfterCaseOptional.orElse(null));
      }
      replacementCodeBuilder.append(" -> ");
      // No braces needed
      replacementCodeBuilder.append("\n").append(transformedBlockSource);

      firstCaseInGroup = true;
    } // case loop

    // Close the switch statement
    replacementCodeBuilder.append("\n};");

    // Statements in the same block following the switch are currently reachable but will become
    // unreachable, which would lead to a compile-time error. Therefore, suggest that they be
    // removed.
    statementsToDelete.addAll(followingStatementsInBlock(switchTree, state));

    SuggestedFix.Builder suggestedFixBuilder =
        SuggestedFix.builder().replace(switchTree, replacementCodeBuilder.toString());
    // Delete trailing statements, leaving comments where feasible
    statementsToDelete.forEach(deleteMe -> suggestedFixBuilder.replace(deleteMe, ""));
    return suggestedFixBuilder.build();
  }

  /**
   * Retrieves a list of all statements (if any) following the supplied {@code SwitchTree} in its
   * lowest-ancestor statement block (if any).
   */
  private static List<StatementTree> followingStatementsInBlock(
      SwitchTree switchTree, VisitorState state) {
    List<StatementTree> followingStatements = new ArrayList<>();

    // NOMUTANTS--for performance/early return only; correctness unchanged
    if (!Matchers.nextStatement(Matchers.<StatementTree>anything()).matches(switchTree, state)) {
      // No lowest-ancestor block or no following statements
      return followingStatements;
    }

    // Fetch the lowest ancestor statement block
    TreePath pathToEnclosing = state.findPathToEnclosing(BlockTree.class);
    // NOMUTANTS--should early return above
    if (pathToEnclosing != null) {
      Tree enclosing = pathToEnclosing.getLeaf();
      if (enclosing instanceof BlockTree blockTree) {
        // Path from root -> switchTree
        TreePath rootToSwitchPath = TreePath.getPath(pathToEnclosing, switchTree);

        for (int i = findBlockStatementIndex(rootToSwitchPath, blockTree) + 1;
            (i >= 0) && (i < blockTree.getStatements().size());
            i++) {
          followingStatements.add(blockTree.getStatements().get(i));
        }
      }
    }
    return followingStatements;
  }

  /**
   * Search through the provided {@code BlockTree} to find which statement in that block tree lies
   * along the supplied {@code TreePath}. Returns the index (zero-based) of the matching statement
   * in the block tree, or -1 if not found.
   */
  private static int findBlockStatementIndex(TreePath treePath, BlockTree blockTree) {
    for (int i = 0; i < blockTree.getStatements().size(); i++) {
      StatementTree thisStatement = blockTree.getStatements().get(i);
      // Look for thisStatement along the path from the root to the switch tree
      TreePath pathFromRootToThisStatement = TreePath.getPath(treePath, thisStatement);
      if (pathFromRootToThisStatement != null) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Transforms the supplied statement switch into an assignment switch style of expression switch.
   * In this conversion, each nontrivial statement block is mapped one-to-one to a new expression on
   * the right-hand side of the arrow. Comments are preserved where possible. Precondition: the
   * {@code AnalysisResult} for the {@code SwitchTree} must have deduced that this conversion is
   * possible.
   */
  private static SuggestedFix convertToAssignmentSwitch(
      SwitchTree switchTree, VisitorState state, AnalysisResult analysisResult) {

    List<? extends CaseTree> cases = switchTree.getCases();
    ImmutableList<ErrorProneComment> allSwitchComments =
        state.getTokensForNode(switchTree).stream()
            .flatMap(errorProneToken -> errorProneToken.comments().stream())
            .collect(toImmutableList());

    StringBuilder replacementCodeBuilder =
        new StringBuilder(
                state.getSourceForNode(
                    analysisResult
                        .assignmentSwitchAnalysisResult()
                        .assignmentTargetOptional()
                        .get()))
            .append(" ")
            // Invariant: always present when a finding exists
            .append(
                analysisResult
                    .assignmentSwitchAnalysisResult()
                    .assignmentSourceCodeOptional()
                    .get())
            .append(" ")
            .append("switch ")
            .append(state.getSourceForNode(switchTree.getExpression()))
            .append(" {");

    StringBuilder groupedCaseCommentsAccumulator = null;
    boolean firstCaseInGroup = true;
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);
      boolean isDefaultCase = isSwitchDefault(caseTree);
      ImmutableList<StatementTree> filteredStatements = filterOutRedundantBreak(caseTree);

      String transformedBlockSource =
          transformAssignOrThrowBlock(caseTree, state, filteredStatements);

      if (firstCaseInGroup) {
        groupedCaseCommentsAccumulator =
            new StringBuilder(
                caseIndex == 0
                    ? extractCommentsBeforeFirstCase(switchTree, allSwitchComments).orElse("")
                    : "");

        replacementCodeBuilder.append("\n  ");
        if (!isDefaultCase) {
          replacementCodeBuilder.append("case ");
        }
      }
      replacementCodeBuilder.append(
          isDefaultCase ? "default" : printCaseExpressions(caseTree, state));

      Optional<String> commentsAfterCaseOptional =
          extractCommentsAfterCase(switchTree, allSwitchComments, state, caseIndex);
      if (analysisResult.groupedWithNextCase().get(caseIndex)) {
        firstCaseInGroup = false;
        replacementCodeBuilder.append(", ");
        // Capture comments from this case so they can be added to the group's transformed case
        if (!transformedBlockSource.trim().isEmpty()) {
          String commentsToAppend = removeFallThruLines(transformedBlockSource);
          if (groupedCaseCommentsAccumulator.length() > 0) {
            groupedCaseCommentsAccumulator.append("\n");
          }
          groupedCaseCommentsAccumulator.append(commentsToAppend);
        }

        if (commentsAfterCaseOptional.isPresent()) {
          if (groupedCaseCommentsAccumulator.length() > 0) {
            groupedCaseCommentsAccumulator.append("\n");
          }
          groupedCaseCommentsAccumulator.append(commentsAfterCaseOptional.get());
        }
        // Add additional cases to the list on the lhs of the arrow
        continue;
      } else {
        // Extract comments (if any) preceding break that was removed as redundant
        Optional<String> commentsBeforeRemovedBreak =
            extractCommentsBeforeRemovedBreak(caseTree, state, filteredStatements);

        // Join together all comments and code, separating with newlines
        transformedBlockSource =
            Joiner.on("\n")
                .skipNulls()
                .join(
                    // This case is the last case in its group, so insert any comments from prior
                    // grouped cases first
                    groupedCaseCommentsAccumulator.length() == 0
                        ? null
                        : groupedCaseCommentsAccumulator.toString(),
                    transformedBlockSource.isEmpty() ? null : transformedBlockSource,
                    commentsBeforeRemovedBreak.orElse(null),
                    commentsAfterCaseOptional.orElse(null));
      }

      replacementCodeBuilder.append(" -> ");

      // No braces needed
      replacementCodeBuilder.append("\n").append(transformedBlockSource);

      firstCaseInGroup = true;
    } // case loop

    // Close the switch statement
    replacementCodeBuilder.append("\n};");

    return SuggestedFix.builder().replace(switchTree, replacementCodeBuilder.toString()).build();
  }

  /**
   * Extracts comments after the last filtered statement but before a removed trailing break
   * statement, if present.
   */
  private static Optional<String> extractCommentsBeforeRemovedBreak(
      CaseTree caseTree, VisitorState state, ImmutableList<StatementTree> filteredStatements) {

    // Was a trailing break removed and some expressions remain?
    if (!filteredStatements.isEmpty()
        && getStatements(caseTree).size() > filteredStatements.size()) {
      // Extract any comments after what is now the last statement and before the removed
      // break
      String commentsAfterNewLastStatement =
          state
              .getSourceCode()
              .subSequence(
                  state.getEndPosition(getLast(filteredStatements)),
                  getStartPosition(getStatements(caseTree).get(getStatements(caseTree).size() - 1)))
              .toString()
              .trim();
      if (!commentsAfterNewLastStatement.isEmpty()) {
        return Optional.of(commentsAfterNewLastStatement);
      }
    }
    return Optional.empty();
  }

  /**
   * If the block for this {@code CaseTree} ends with a {@code break} statement that would be
   * redundant after transformation, then filter out the relevant {@code break} statement.
   */
  private static ImmutableList<StatementTree> filterOutRedundantBreak(CaseTree caseTree) {
    boolean caseEndsWithUnlabelledBreak =
        Streams.findLast(getStatements(caseTree).stream())
            .filter(statement -> statement.getKind().equals(BREAK))
            .filter(breakTree -> ((BreakTree) breakTree).getLabel() == null)
            .isPresent();
    return caseEndsWithUnlabelledBreak
        ? getStatements(caseTree).stream()
            .limit(getStatements(caseTree).size() - 1)
            .collect(toImmutableList())
        : ImmutableList.copyOf(getStatements(caseTree));
  }

  /**
   * Returns the statements of a {@link CaseTree}. If the only statement is a block statement,
   * return the block's statements instead.
   */
  private static List<? extends StatementTree> getStatements(CaseTree caseTree) {
    List<? extends StatementTree> statements = caseTree.getStatements();
    if (statements == null || statements.size() != 1) {
      return statements;
    }
    StatementTree onlyStatement = getOnlyElement(statements);
    if (!onlyStatement.getKind().equals(BLOCK)) {
      return statements;
    }
    return ((BlockTree) onlyStatement).getStatements();
  }

  /** Transforms code for this case into the code under an expression switch. */
  private static String transformBlock(
      CaseTree caseTree, VisitorState state, ImmutableList<StatementTree> filteredStatements) {

    StringBuilder transformedBlockBuilder = new StringBuilder();
    int codeBlockStart = extractLhsComments(caseTree, state, transformedBlockBuilder);
    if (!filteredStatements.isEmpty()) {
      int codeBlockEnd = state.getEndPosition(getLast(filteredStatements));
      transformedBlockBuilder.append(state.getSourceCode(), codeBlockStart, codeBlockEnd);
    }

    return transformedBlockBuilder.toString();
  }

  /**
   * Extracts comments to the left side of the colon for the provided {@code CaseTree} into the
   * {@code StringBuilder}. Note that any whitespace between distinct comments is not necessarily
   * preserved exactly.
   */
  private static int extractLhsComments(
      CaseTree caseTree, VisitorState state, StringBuilder stringBuilder) {

    int lhsStart = getStartPosition(caseTree);
    int lhsEnd =
        getStatements(caseTree).isEmpty()
            ? state.getEndPosition(caseTree)
            : getStartPosition(getStatements(caseTree).get(0));

    // Accumulate comments into transformed block
    state.getOffsetTokens(lhsStart, lhsEnd).stream()
        .flatMap(errorProneToken -> errorProneToken.comments().stream())
        .forEach(comment -> stringBuilder.append(comment.getText()).append("\n"));

    return lhsEnd;
  }

  /**
   * Extracts any comments appearing within the switch tree before the first case. Comments are
   * merged into a single string separated by newlines. Precondition: the switch tree has at least
   * one case.
   */
  private static Optional<String> extractCommentsBeforeFirstCase(
      SwitchTree switchTree, ImmutableList<ErrorProneComment> allSwitchComments) {
    // Indexing relative to the start position of the switch statement
    int switchStart = getStartPosition(switchTree);
    int firstCaseStartIndex = getStartPosition(switchTree.getCases().get(0)) - switchStart;

    return filterAndRenderComments(
        allSwitchComments, comment -> comment.getPos() < firstCaseStartIndex);
  }

  /**
   * Extracts any comments appearing after the specified {@code caseIndex} but before the subsequent
   * case or end of the {@code switchTree}. Comments are merged into a single string separated by
   * newlines.
   */
  private static Optional<String> extractCommentsAfterCase(
      SwitchTree switchTree,
      ImmutableList<ErrorProneComment> allSwitchComments,
      VisitorState state,
      int caseIndex) {

    // Indexing relative to the start position of the switch statement
    int switchStart = getStartPosition(switchTree);
    // Invariant: caseEndIndex >= 0
    int caseEndIndex = state.getEndPosition(switchTree.getCases().get(caseIndex)) - switchStart;
    // Invariant: nextCaseStartIndex >= caseEndIndex
    int nextCaseStartIndex =
        caseIndex == switchTree.getCases().size() - 1
            ? state.getEndPosition(switchTree) - switchStart
            : getStartPosition(switchTree.getCases().get(caseIndex + 1)) - switchStart;

    return filterAndRenderComments(
        allSwitchComments,
        comment -> comment.getPos() >= caseEndIndex && comment.getPos() < nextCaseStartIndex);
  }

  /**
   * Filters comments according to the supplied predicate ({@code commentFilter}), removes
   * fall-through and empty comments, and renders them into a single optional string. If no comments
   * remain, returns {@code Optional.empty()}.
   */
  private static Optional<String> filterAndRenderComments(
      ImmutableList<ErrorProneComment> comments, Predicate<ErrorProneComment> commentFilter) {

    String rendered =
        comments.stream()
            .filter(commentFilter)
            .map(ErrorProneComment::getText)
            // Remove "fall thru" comments
            .map(commentText -> removeFallThruLines(commentText))
            // Remove empty comments
            .filter(commentText -> !commentText.isEmpty())
            .collect(joining("\n"));

    return rendered.isEmpty() ? Optional.empty() : Optional.of(rendered);
  }

  /**
   * Finds the position in source corresponding to the end of the code block of the supplied {@code
   * caseIndex} within all {@code cases}.
   */
  private static int getBlockEnd(VisitorState state, CaseTree caseTree) {

    List<? extends StatementTree> statements = caseTree.getStatements();
    if (statements == null || statements.size() != 1) {
      return state.getEndPosition(caseTree);
    }

    // Invariant: statements.size() == 1
    StatementTree onlyStatement = getOnlyElement(statements);
    if (!onlyStatement.getKind().equals(BLOCK)) {
      return state.getEndPosition(caseTree);
    }

    // The RHS of the case has a single enclosing block { ... }
    List<? extends StatementTree> blockStatements = ((BlockTree) onlyStatement).getStatements();
    return blockStatements.isEmpty()
        ? state.getEndPosition(caseTree)
        : state.getEndPosition(blockStatements.get(blockStatements.size() - 1));
  }

  /**
   * Determines whether the supplied {@code case}'s logic should be expressed on the right of the
   * arrow symbol without braces, incorporating both language and readability considerations.
   */
  private static boolean shouldTransformCaseWithoutBraces(
      ImmutableList<StatementTree> statementTrees) {

    if (statementTrees.isEmpty()) {
      // Instead, express as "-> {}"
      return false;
    }

    if (statementTrees.size() > 1) {
      // Instead, express as a code block "-> { ... }"
      return false;
    }

    StatementTree onlyStatementTree = statementTrees.get(0);
    return KINDS_CONVERTIBLE_WITHOUT_BRACES.contains(onlyStatementTree.getKind());
  }

  /**
   * Removes any comment lines containing language similar to "fall thru". Intermediate line
   * delimiters are also changed to newline.
   */
  private static String removeFallThruLines(String comments) {
    StringBuilder output = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new CharArrayReader(comments.toCharArray()))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (!FALL_THROUGH_PATTERN.matcher(line).find()) {
          output.append(line).append("\n");
        }
      }
      // Remove trailing \n, if present
      return output.length() > 0 ? output.substring(0, output.length() - 1) : "";
    } catch (IOException e) {
      return comments;
    }
  }

  /** Prints source for all expressions in a given {@code case}, separated by commas. */
  private static String printCaseExpressions(CaseTree caseTree, VisitorState state) {
    return caseTree.getExpressions().stream().map(state::getSourceForNode).collect(joining(", "));
  }

  /**
   * Ad-hoc algorithm to search for a surjective map from (non-null) values of a {@code switch}'s
   * expression to a {@code CaseTree}. Note that this algorithm does not compute whether such a map
   * exists, but rather only whether it can find such a map.
   */
  private static boolean isSwitchExhaustive(
      boolean hasDefaultCase, Set<String> handledEnumValues, Type switchType) {
    if (hasDefaultCase) {
      // Anything not included in a case can be mapped to the default CaseTree
      return true;
    }

    // Handles switching on enum (map is bijective)
    if (switchType.asElement().getKind() != ElementKind.ENUM) {
      // Give up on search
      return false;
    }
    return handledEnumValues.containsAll(ASTHelpers.enumValues(switchType.asElement()));
  }

  /**
   * Transforms a return or throw block into an expression statement suitable for use on the
   * right-hand-side of the arrow of a return switch. For example, {@code return x+1;} would be
   * transformed to {@code x+1;}.
   */
  private static String transformReturnOrThrowBlock(
      CaseTree caseTree, VisitorState state, List<? extends StatementTree> statements) {

    StringBuilder transformedBlockBuilder = new StringBuilder();
    int codeBlockEnd = state.getEndPosition(caseTree);
    if (statements.size() > 1) {
      transformedBlockBuilder.append("{\n");
      int codeBlockStart = extractLhsComments(caseTree, state, transformedBlockBuilder);
      int offset = transformedBlockBuilder.length();
      transformedBlockBuilder.append(state.getSourceCode(), codeBlockStart, codeBlockEnd);
      transformedBlockBuilder.append("\n}");
      ReturnTree returnTree = (ReturnTree) getLast(statements);
      int start = getStartPosition(returnTree);
      transformedBlockBuilder.replace(
          offset + start - codeBlockStart,
          offset + start - codeBlockStart + "return".length(),
          "yield");
    } else if (statements.size() == 1 && statements.get(0).getKind().equals(RETURN)) {
      // For "return x;", we want to take source starting after the "return"
      int unused = extractLhsComments(caseTree, state, transformedBlockBuilder);
      ReturnTree returnTree = (ReturnTree) statements.get(0);
      int codeBlockStart = getStartPosition(returnTree.getExpression());
      codeBlockEnd = state.getEndPosition(Streams.findLast(statements.stream()).get());
      transformedBlockBuilder.append(state.getSourceCode(), codeBlockStart, codeBlockEnd);
    } else {
      int codeBlockStart = extractLhsComments(caseTree, state, transformedBlockBuilder);
      transformedBlockBuilder.append(state.getSourceCode(), codeBlockStart, codeBlockEnd);
    }

    return transformedBlockBuilder.toString();
  }

  /**
   * Transforms a assignment or throw into an expression statement suitable for use on the
   * right-hand-side of the arrow of an assignment switch. For example, {@code x >>= 2;} would be
   * transformed to {@code 2;}. Note that this method does not return the assignment operator (e.g.
   * {@code >>=}).
   */
  private static String transformAssignOrThrowBlock(
      CaseTree caseTree, VisitorState state, List<? extends StatementTree> statements) {

    StringBuilder transformedBlockBuilder = new StringBuilder();
    int codeBlockStart;
    int codeBlockEnd =
        statements.isEmpty()
            ? getBlockEnd(state, caseTree)
            : state.getEndPosition(Streams.findLast(statements.stream()).get());

    if (!statements.isEmpty() && statements.get(0).getKind().equals(EXPRESSION_STATEMENT)) {
      // For "x = foo", we want to take source starting after the "x ="
      int unused = extractLhsComments(caseTree, state, transformedBlockBuilder);
      ExpressionTree expression = ((ExpressionStatementTree) statements.get(0)).getExpression();
      Optional<ExpressionTree> rhs = Optional.empty();
      if (expression instanceof CompoundAssignmentTree compoundAssignmentTree) {
        rhs = Optional.of(compoundAssignmentTree.getExpression());
      } else if (expression instanceof AssignmentTree assignmentTree) {
        rhs = Optional.of(assignmentTree.getExpression());
      }
      codeBlockStart = getStartPosition(rhs.get());
    } else {
      codeBlockStart = extractLhsComments(caseTree, state, transformedBlockBuilder);
    }
    transformedBlockBuilder.append(state.getSourceCode(), codeBlockStart, codeBlockEnd);

    return transformedBlockBuilder.toString();
  }

  @AutoValue
  abstract static class AnalysisResult {
    // Whether the statement switch can be directly converted to an expression switch
    abstract boolean canConvertDirectlyToExpressionSwitch();

    // Whether the statement switch can be converted to a return switch
    abstract boolean canConvertToReturnSwitch();

    // Results of the analysis for conversion to an assignment switch
    abstract AssignmentSwitchAnalysisResult assignmentSwitchAnalysisResult();

    // List of whether each case tree can be grouped with its successor in transformed source code
    abstract ImmutableList<Boolean> groupedWithNextCase();

    static AnalysisResult of(
        boolean canConvertDirectlyToExpressionSwitch,
        boolean canConvertToReturnSwitch,
        AssignmentSwitchAnalysisResult assignmentSwitchAnalysisResult,
        ImmutableList<Boolean> groupedWithNextCase) {
      return new AutoValue_StatementSwitchToExpressionSwitch_AnalysisResult(
          canConvertDirectlyToExpressionSwitch,
          canConvertToReturnSwitch,
          assignmentSwitchAnalysisResult,
          groupedWithNextCase);
    }
  }

  @AutoValue
  abstract static class AssignmentSwitchAnalysisResult {
    // Whether the statement switch can be converted to an assignment switch
    abstract boolean canConvertToAssignmentSwitch();

    // Target of the assignment switch, if any
    abstract Optional<ExpressionTree> assignmentTargetOptional();

    // Kind of assignment made by the assignment switch, if any
    abstract Optional<Tree.Kind> assignmentKindOptional();

    // Java source code of the assignment switch's operator, e.g. "+="
    abstract Optional<String> assignmentSourceCodeOptional();

    static AssignmentSwitchAnalysisResult of(
        boolean canConvertToAssignmentSwitch,
        Optional<ExpressionTree> assignmentTargetOptional,
        Optional<Tree.Kind> assignmentKindOptional,
        Optional<String> assignmentSourceCodeOptional) {
      return new AutoValue_StatementSwitchToExpressionSwitch_AssignmentSwitchAnalysisResult(
          canConvertToAssignmentSwitch,
          assignmentTargetOptional,
          assignmentKindOptional,
          assignmentSourceCodeOptional);
    }
  }

  @AutoValue
  abstract static class AssignmentSwitchAnalysisState {
    // Overall qualification of the switch statement for conversion to an assignment switch
    abstract CaseQualifications assignmentSwitchCaseQualifications();

    // Target of the first assignment seen, if any
    abstract Optional<ExpressionTree> assignmentTargetOptional();

    // Kind of the first assignment seen, if any
    abstract Optional<Tree.Kind> assignmentExpressionKindOptional();

    // ExpressionTree of the first assignment seen, if any
    abstract Optional<ExpressionTree> assignmentTreeOptional();

    static AssignmentSwitchAnalysisState of(
        CaseQualifications assignmentSwitchCaseQualifications,
        Optional<ExpressionTree> assignmentTargetOptional,
        Optional<Tree.Kind> assignmentKindOptional,
        Optional<ExpressionTree> assignmentTreeOptional) {
      return new AutoValue_StatementSwitchToExpressionSwitch_AssignmentSwitchAnalysisState(
          assignmentSwitchCaseQualifications,
          assignmentTargetOptional,
          assignmentKindOptional,
          assignmentTreeOptional);
    }
  }
}
