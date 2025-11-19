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
import static com.google.errorprone.util.ASTHelpers.hasImplicitType;
import static com.google.errorprone.util.ASTHelpers.isSwitchDefault;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.RETURN;
import static com.sun.source.tree.Tree.Kind.THROW;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.CompileTimeConstantExpressionMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneComment;
import com.google.errorprone.util.Reachability;
import com.google.errorprone.util.SourceVersion;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
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
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.IntersectionType;
import org.jspecify.annotations.Nullable;

/** Checks for statement switches that can be converted into a new-style arrow (`->`) switch. */
@BugPattern(
    severity = WARNING,
    summary = "This statement switch can be converted to a new-style arrow switch")
public final class StatementSwitchToExpressionSwitch extends BugChecker
    implements SwitchTreeMatcher {
  // Braces are not required if there is exactly one statement on the right hand of the arrow, and
  // it's either an ExpressionStatement or a Throw.  Refer to JLS 14 §14.11.1
  private static final ImmutableSet<Kind> KINDS_CONVERTIBLE_WITHOUT_BRACES =
      ImmutableSet.of(THROW, EXPRESSION_STATEMENT);
  private static final ImmutableSet<Kind> KINDS_RETURN_OR_THROW = ImmutableSet.of(THROW, RETURN);
  private static final Pattern FALL_THROUGH_PATTERN =
      Pattern.compile("\\bfalls?.?(through|out)\\b", Pattern.CASE_INSENSITIVE);

  /**
   * Default (negative) result for assignment switch conversion analysis. Note that the value is
   * immutable.
   */
  private static final AssignmentSwitchAnalysisResult DEFAULT_ASSIGNMENT_SWITCH_ANALYSIS_RESULT =
      new AssignmentSwitchAnalysisResult(
          /* canConvertToAssignmentSwitch= */ false,
          /* precedingVariableDeclaration= */ Optional.empty(),
          /* assignmentTargetOptional= */ Optional.empty(),
          /* assignmentKindOptional= */ Optional.empty(),
          /* assignmentSourceCodeOptional= */ Optional.empty());

  /** Default (negative) result for overall analysis. Note that the value is immutable. */
  private static final AnalysisResult DEFAULT_ANALYSIS_RESULT =
      new AnalysisResult(
          false,
          false,
          false,
          DEFAULT_ASSIGNMENT_SWITCH_ANALYSIS_RESULT,
          ImmutableList.of(),
          ImmutableBiMap.of());

  private static final String EQUALS_STRING = "=";
  private static final Matcher<ExpressionTree> COMPILE_TIME_CONSTANT_MATCHER =
      CompileTimeConstantExpressionMatcher.instance();
  private static final String REMOVE_DEFAULT_CASE_SHORT_DESCRIPTION =
      "Remove default case because all enum values handled";

  /**
   * Tri-state to represent the fall-thru control flow of a particular case of a particular
   * statement switch
   */
  private enum CaseFallThru {
    DEFINITELY_DOES_NOT_FALL_THRU,
    MAYBE_FALLS_THRU,
    DEFINITELY_DOES_FALL_THRU
  }

  /**
   * Tri-state to represent whether cases within a single switch statement meet an (unspecified)
   * qualification predicate
   */
  enum CaseQualifications {
    NO_CASES_ASSESSED,
    ALL_CASES_QUALIFY,
    SOME_OR_ALL_CASES_DONT_QUALIFY
  }

  /**
   * The kind of null/default cases included within a single CaseTree.
   *
   * <p>This enum is used to classify whether a CaseTree includes a null and/or default. Referencing
   * JLS 21 §14.11.1, the `SwitchLabel:` production has specific rules applicable to null/default
   * cases: `case null, [default]` and `default`. All other scenarios are lumped into KIND_NEITHER.
   */
  enum NullDefaultKind {
    KIND_NULL_AND_DEFAULT,
    KIND_DEFAULT,
    KIND_NULL,
    KIND_NEITHER
  }

  private final boolean enableDirectConversion;
  private final boolean enableReturnSwitchConversion;
  private final boolean enableAssignmentSwitchConversion;

  @Inject
  StatementSwitchToExpressionSwitch(ErrorProneFlags flags) {
    this.enableDirectConversion =
        flags.getBoolean("StatementSwitchToExpressionSwitch:EnableDirectConversion").orElse(true);
    this.enableReturnSwitchConversion =
        flags
            .getBoolean("StatementSwitchToExpressionSwitch:EnableReturnSwitchConversion")
            .orElse(true);
    this.enableAssignmentSwitchConversion =
        flags
            .getBoolean("StatementSwitchToExpressionSwitch:EnableAssignmentSwitchConversion")
            .orElse(true);
  }

  @Override
  public Description matchSwitch(SwitchTree switchTree, VisitorState state) {
    if (!SourceVersion.supportsSwitchExpressions(state.context)) {
      return NO_MATCH;
    }

    AnalysisResult analysisResult = analyzeSwitchTree(switchTree, state);

    List<SuggestedFix> suggestedFixes = new ArrayList<>();
    if (enableReturnSwitchConversion && analysisResult.canConvertToReturnSwitch()) {
      suggestedFixes.add(
          convertToReturnSwitch(switchTree, state, analysisResult, /* removeDefault= */ false));

      if (analysisResult.canRemoveDefault()) {
        suggestedFixes.add(
            convertToReturnSwitch(switchTree, state, analysisResult, /* removeDefault= */ true));
      }
    }
    if (enableAssignmentSwitchConversion
        && analysisResult.assignmentSwitchAnalysisResult().canConvertToAssignmentSwitch()) {
      suggestedFixes.add(
          convertToAssignmentSwitch(switchTree, state, analysisResult, /* removeDefault= */ false));

      if (analysisResult.canRemoveDefault()) {
        suggestedFixes.add(
            convertToAssignmentSwitch(
                switchTree, state, analysisResult, /* removeDefault= */ true));
      }
    }
    if (enableDirectConversion && analysisResult.canConvertDirectlyToExpressionSwitch()) {
      suggestedFixes.add(
          convertDirectlyToExpressionSwitch(
              switchTree, state, analysisResult, /* removeDefault= */ false));

      if (analysisResult.canRemoveDefault()) {
        suggestedFixes.add(
            convertDirectlyToExpressionSwitch(
                switchTree, state, analysisResult, /* removeDefault= */ true));
      }
    }

    return suggestedFixes.isEmpty()
        ? NO_MATCH
        : buildDescription(switchTree).addAllFixes(suggestedFixes).build();
  }

  /**
   * Extracts all variable symbols defined in the given list of statements. Note that this includes
   * only declarations in the top-level list, not those nested within any subtrees. Returns a
   * bidirectional mapping from variable symbol to its original variable declaration tree.
   */
  private static BiMap<VarSymbol, VariableTree> extractSymbolsDefinedInStatementBlock(
      List<? extends StatementTree> statements) {
    BiMap<VarSymbol, VariableTree> symbolsDefinedInStatementBlock = HashBiMap.create();
    if (statements == null) {
      return symbolsDefinedInStatementBlock;
    }
    for (StatementTree statement : statements) {
      if (statement instanceof VariableTree variableTree) {
        VarSymbol symbol = ASTHelpers.getSymbol(variableTree);
        if (symbol != null) {
          symbolsDefinedInStatementBlock.put(symbol, variableTree);
        }
      }
    }
    return symbolsDefinedInStatementBlock;
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
    BiMap<VarSymbol, VariableTree> symbolsDefinedInPreviousCases = HashBiMap.create();
    BiMap<VarSymbol, VariableTree> symbolsToHoist = HashBiMap.create();

    List<? extends CaseTree> cases = switchTree.getCases();
    // A given case is said to have definite control flow if we are sure it always or never falls
    // thru at the end of its statement block
    boolean allCasesHaveDefiniteControlFlow = true;
    // A case is said to be grouped with the next one if we are sure it can appear together with the
    // next case on the left hand side of the arrow when converted to an expression switch.  For
    // example "case A,B -> ..."
    List<Boolean> groupedWithNextCase = new ArrayList<>(Collections.nCopies(cases.size(), false));
    List<Boolean> isNullCase = new ArrayList<>(Collections.nCopies(cases.size(), false));

    // Set of all enum values (names) explicitly listed in a case tree
    Set<String> handledEnumValues = new HashSet<>();
    // Does each case consist solely of returning a (non-void) expression?
    CaseQualifications returnSwitchCaseQualifications = CaseQualifications.NO_CASES_ASSESSED;
    // Does each case consist solely of a throw or the same symbol assigned in the same way?
    Optional<ExpressionTree> assignmentTargetOptional = Optional.empty();
    Optional<Tree.Kind> assignmentKindOptional = Optional.empty();
    /* assignmentSwitchCaseQualifications= */
    /* assignmentTargetOptional= */
    /* assignmentKindOptional= */
    /* assignmentTreeOptional= */ AssignmentSwitchAnalysisState assignmentSwitchAnalysisState =
        new AssignmentSwitchAnalysisState(
            CaseQualifications.NO_CASES_ASSESSED,
            assignmentTargetOptional,
            assignmentKindOptional,
            Optional.empty());

    boolean hasDefaultCase = false;
    // One-pass scan through each case in switch
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);
      NullDefaultKind nullDefaultKind = analyzeCaseForNullAndDefault(caseTree);
      boolean isDefaultCase =
          nullDefaultKind.equals(NullDefaultKind.KIND_DEFAULT)
              || nullDefaultKind.equals(NullDefaultKind.KIND_NULL_AND_DEFAULT);
      isNullCase.set(
          caseIndex,
          nullDefaultKind.equals(NullDefaultKind.KIND_NULL)
              || nullDefaultKind.equals(NullDefaultKind.KIND_NULL_AND_DEFAULT));
      hasDefaultCase |= isDefaultCase;

      // Null case can never be grouped with a preceding case
      if (caseIndex > 0 && groupedWithNextCase.get(caseIndex - 1) && isNullCase.get(caseIndex)) {
        return DEFAULT_ANALYSIS_RESULT;
      }

      // Null case can never be grouped with a following case (except possibly default)
      if (caseIndex > 0
          && groupedWithNextCase.get(caseIndex - 1)
          && isNullCase.get(caseIndex - 1)
          && !isDefaultCase) {
        return DEFAULT_ANALYSIS_RESULT;
      }

      // Grouping null with default requires Java 21+
      if (caseIndex > 0
          && isNullCase.get(caseIndex - 1)
          && isDefaultCase
          && !SourceVersion.supportsPatternMatchingSwitch(state.context)) {
        return DEFAULT_ANALYSIS_RESULT;
      }

      // Accumulate enum values included in this case
      handledEnumValues.addAll(
          caseTree.getExpressions().stream()
              .map(ASTHelpers::getSymbol)
              .filter(x -> x != null)
              .map(symbol -> symbol.getSimpleName().toString())
              .collect(toImmutableSet()));
      boolean isLastCaseInSwitch = caseIndex == cases.size() - 1;

      ImmutableList<StatementTree> statements = getStatements(caseTree);
      BiMap<VarSymbol, VariableTree> symbolsDefinedInThisCase =
          extractSymbolsDefinedInStatementBlock(statements);
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
        // default case.  Exceptions: (1.) allowed to fall out of default case if it's the last case
        // and (2.) allowed to fall into the default case if the preceding case is null and grouped
        // with this one.
        boolean fallsIntoDefaultCase = (caseIndex > 0) && groupedWithNextCase.get(caseIndex - 1);
        boolean precedingCaseIsNull = (caseIndex > 0) && isNullCase.get(caseIndex - 1);
        if (isLastCaseInSwitch) {
          if (!precedingCaseIsNull) {
            allCasesHaveDefiniteControlFlow &= !fallsIntoDefaultCase;
          }
        } else {
          allCasesHaveDefiniteControlFlow &=
              (precedingCaseIsNull || !fallsIntoDefaultCase)
                  && caseFallThru.equals(CaseFallThru.DEFINITELY_DOES_NOT_FALL_THRU);
        }
      } else {
        // Cases other than default
        allCasesHaveDefiniteControlFlow &= !caseFallThru.equals(CaseFallThru.MAYBE_FALLS_THRU);
      }

      // Find any symbols referenced in this case that were defined in a previous case, and thus
      // should be hoisted out of the switch block
      ImmutableSet<VarSymbol> newSymbolsToHoist =
          symbolsDefinedInPreviousCases.keySet().stream()
              .filter(symbol -> hasReadsOrWritesOfVariableInTree(symbol, caseTree))
              .collect(toImmutableSet());

      // Ensure that hoisting does not conflict with other declared variables in switch scope
      boolean hasNamingConflict =
          newSymbolsToHoist.stream()
              // In principle, this search could be terminated after checking up through `caseTree`
              // (inclusive) because naming conflicts after that would have caused compile-time
              // errors.  For simplicity, the search is not restricted.
              .filter(symbol -> declaresAnotherVariableNamed(symbol, switchTree))
              .findAny()
              .isPresent();
      if (hasNamingConflict) {
        return DEFAULT_ANALYSIS_RESULT;
      }

      newSymbolsToHoist.forEach(
          symbol -> symbolsToHoist.put(symbol, symbolsDefinedInPreviousCases.get(symbol)));

      // Analyze for return switch and assignment switch conversion
      returnSwitchCaseQualifications =
          analyzeCaseForReturnSwitch(
              returnSwitchCaseQualifications, statements, isLastCaseInSwitch);
      assignmentSwitchAnalysisState =
          analyzeCaseForAssignmentSwitch(
              assignmentSwitchAnalysisState, statements, isLastCaseInSwitch);
      symbolsDefinedInPreviousCases.putAll(symbolsDefinedInThisCase);
    }

    boolean exhaustive =
        isSwitchExhaustive(
            hasDefaultCase, handledEnumValues, ASTHelpers.getType(switchTree.getExpression()));
    boolean canRemoveDefault =
        hasDefaultCase
            && isSwitchExhaustiveWithoutDefault(
                handledEnumValues, ASTHelpers.getType(switchTree.getExpression()));

    boolean canConvertToReturnSwitch =
        // All restrictions for direct conversion apply
        allCasesHaveDefiniteControlFlow
            // Hoisting is currently not supported with return switches
            && symbolsToHoist.isEmpty()
            // Does each case consist solely of returning a (non-void) expression?
            && returnSwitchCaseQualifications.equals(CaseQualifications.ALL_CASES_QUALIFY)
            // The switch must be exhaustive (at compile time)
            && exhaustive;
    boolean canConvertToAssignmentSwitch =
        // All restrictions for direct conversion apply
        allCasesHaveDefiniteControlFlow
            // Hoisting is currently not supported with assignment switches
            && symbolsToHoist.isEmpty()
            // Does each case consist solely of a throw or the same symbol assigned in the same way?
            && assignmentSwitchAnalysisState
                .assignmentSwitchCaseQualifications()
                .equals(CaseQualifications.ALL_CASES_QUALIFY)
            // The switch must be exhaustive (at compile time)
            && exhaustive;
    boolean canConvertDirectlyToExpressionSwitch =
        allCasesHaveDefiniteControlFlow
            // Hoisting currently not supported for arrays due to restrictions on using assignment
            // expressions to initialize them
            && symbolsToHoist.keySet().stream()
                .noneMatch(symbol -> state.getTypes().isArray(symbol.type))
            // Hoisting currently not supported for intersection types because the type is not
            // denotable as an explicit type (see JLS 21 § 14.4.1.)
            && symbolsToHoist.keySet().stream()
                .noneMatch(symbol -> symbol.type instanceof IntersectionType);

    ImmutableList<StatementTree> precedingStatements = getPrecedingStatementsInBlock(state);
    Optional<ExpressionTree> assignmentTarget =
        assignmentSwitchAnalysisState.assignmentTargetOptional();

    // If present, the variable tree that can be combined with the switch block
    Optional<VariableTree> combinableVariableTree =
        canConvertToAssignmentSwitch
            ? assignmentTarget.flatMap(
                target -> findCombinableVariableTree(target, precedingStatements, state))
            : Optional.empty();

    return new AnalysisResult(
        canConvertDirectlyToExpressionSwitch,
        canConvertToReturnSwitch,
        canRemoveDefault,
        new AssignmentSwitchAnalysisResult(
            canConvertToAssignmentSwitch,
            combinableVariableTree,
            assignmentSwitchAnalysisState.assignmentTargetOptional(),
            assignmentSwitchAnalysisState.assignmentExpressionKindOptional(),
            assignmentSwitchAnalysisState
                .assignmentTreeOptional()
                .map(StatementSwitchToExpressionSwitch::renderJavaSourceOfAssignment)),
        ImmutableList.copyOf(groupedWithNextCase),
        ImmutableBiMap.copyOf(symbolsToHoist));
  }

  private static Optional<VariableTree> findCombinableVariableTree(
      ExpressionTree assignmentTarget,
      ImmutableList<StatementTree> precedingStatements,
      VisitorState state) {
    // Don't try to combine when multiple variables are declared together
    if (precedingStatements.isEmpty()
        || !precedingTwoStatementsNotInSameVariableDeclaratorList(precedingStatements)) {
      return Optional.empty();
    }
    if (!(getLast(precedingStatements) instanceof VariableTree variableTree)) {
      return Optional.empty();
    }
    if (variableTree.getInitializer() != null
        && !COMPILE_TIME_CONSTANT_MATCHER.matches(variableTree.getInitializer(), state)) {
      return Optional.empty();
    }
    // If we are reading the initialized value in the switch block, we can't remove it
    if (!noReadsOfVariable(ASTHelpers.getSymbol(variableTree), state)) {
      return Optional.empty();
    }
    // The variable and the switch's assignment must be compatible
    if (!isVariableCompatibleWithAssignment(assignmentTarget, variableTree)) {
      return Optional.empty();
    }
    return Optional.of(variableTree);
  }

  /**
   * Determines whether local variable {@code symbol} has no reads within the scope of the {@code
   * VisitorState}. (Writes to the variable are ignored.)
   */
  private static boolean noReadsOfVariable(VarSymbol symbol, VisitorState state) {
    Set<VarSymbol> referencedLocalVariables = new HashSet<>();
    new TreePathScanner<Void, Void>() {

      @Override
      public Void visitAssignment(AssignmentTree tree, Void unused) {
        // Only looks at the right-hand side of the assignment
        return scan(tree.getExpression(), null);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelect, Void unused) {
        handle(memberSelect);
        return super.visitMemberSelect(memberSelect, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifier, Void unused) {
        handle(identifier);
        return super.visitIdentifier(identifier, null);
      }

      private void handle(Tree tree) {
        var symbol = getSymbol(tree);
        if (symbol instanceof VarSymbol varSymbol) {
          referencedLocalVariables.add(varSymbol);
        }
      }
    }.scan(state.getPath(), null);

    return !referencedLocalVariables.contains(symbol);
  }

  /**
   * Determines whether local variable {@code symbol} has reads or writes within the scope of the
   * supplied {@code tree}.
   */
  private static boolean hasReadsOrWritesOfVariableInTree(VarSymbol symbol, Tree tree) {
    Set<VarSymbol> referencedLocalVariables = new HashSet<>();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelect, Void unused) {
        handle(memberSelect);
        return super.visitMemberSelect(memberSelect, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifier, Void unused) {
        handle(identifier);
        return super.visitIdentifier(identifier, null);
      }

      private void handle(Tree tree) {
        var symbol = getSymbol(tree);
        if (symbol instanceof VarSymbol varSymbol) {
          referencedLocalVariables.add(varSymbol);
        }
      }
    }.scan(tree, null);
    return referencedLocalVariables.contains(symbol);
  }

  /**
   * Determines whether the switch statement has a case that declares a local variable with the same
   * name as the supplied {@code symbol}.
   */
  private static boolean declaresAnotherVariableNamed(VarSymbol symbol, SwitchTree switchTree) {
    return new TreeScanner<Boolean, Void>() {
      @Override
      public Boolean visitVariable(VariableTree variableTree, Void unused) {
        // If the variable is named the same as the symbol, but it's not the original declaration
        // of the symbol, then there's a name conflict.
        if (variableTree.getName().contentEquals(symbol.name.toString())) {
          VarSymbol thisVarSymbol = ASTHelpers.getSymbol(variableTree);
          if (!thisVarSymbol.equals(symbol)) {
            return true;
          }
        }
        return super.visitVariable(variableTree, null);
      }

      @Override
      public Boolean reduce(@Nullable Boolean left, @Nullable Boolean right) {
        return Objects.equals(left, true) || Objects.equals(right, true);
      }
    }.scan(switchTree, null);
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
   * Renders the Java source prefix needed for the supplied {@code nullDefaultKind}, incorporating
   * whether the `default` case should be removed.
   */
  private static String renderNullDefaultKindPrefix(
      NullDefaultKind nullDefaultKind, boolean removeDefault) {

    return switch (nullDefaultKind) {
      case KIND_NULL_AND_DEFAULT -> removeDefault ? "case null" : "case null, default";
      case KIND_NULL -> "case null";
      case KIND_DEFAULT -> removeDefault ? "" : "default";
      case KIND_NEITHER -> "case ";
    };
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
            .allMatch(statement -> statement instanceof ExpressionStatementTree)
        || !KINDS_RETURN_OR_THROW.contains(lastStatement.getKind())) {
      return CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY;
    }

    // For this analysis, cases that don't return something can be disregarded
    if (!(lastStatement instanceof ReturnTree returnTree)) {
      return previousCaseQualifications;
    }

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
          ? new AssignmentSwitchAnalysisState(
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
                && statements.get(1) instanceof BreakTree breakTree
                && breakTree.getLabel() == null);
    if (!expressionOrExpressionBreak) {
      // Conversion of this block is not supported
      return new AssignmentSwitchAnalysisState(
          CaseQualifications.SOME_OR_ALL_CASES_DONT_QUALIFY,
          assignmentTargetOptional,
          assignmentExpressionKindOptional,
          assignmentTreeOptional);
    }

    if (!(firstStatement instanceof ExpressionStatementTree expressionStatementTree)) {
      // Throws don't affect the assignment analysis
      return previousAssignmentSwitchAnalysisState;
    }

    ExpressionTree expression = expressionStatementTree.getExpression();
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
   * Determines whether a variable definition is compatible with an assignment target (e.g. of a
   * switch statement). Compatibility means that the assignment is being made to to the same
   * variable that is being defined.
   */
  private static boolean isVariableCompatibleWithAssignment(
      ExpressionTree assignmentTarget, VariableTree variableDefinition) {
    Symbol assignmentTargetSymbol = getSymbol(assignmentTarget);
    Symbol definedSymbol = ASTHelpers.getSymbol(variableDefinition);

    return Objects.equals(assignmentTargetSymbol, definedSymbol);
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
   * Renders all comments of the supplied {@code variableTree} into a list of Strings, in code
   * order.
   */
  private static ImmutableList<String> renderVariableTreeComments(
      VariableTree variableTree, VisitorState state) {
    return state.getTokensForNode(variableTree).stream()
        .flatMap(errorProneToken -> errorProneToken.comments().stream())
        .filter(comment -> !comment.getText().isEmpty())
        .map(ErrorProneComment::getText)
        .collect(toImmutableList());
  }

  /**
   * Renders all annotations of the supplied {@code variableTree} into a list of Strings, in code
   * order.
   */
  private static ImmutableList<String> renderVariableTreeAnnotations(
      VariableTree variableTree, VisitorState state) {
    return variableTree.getModifiers().getAnnotations().stream()
        .map(state::getSourceForNode)
        .collect(toImmutableList());
  }

  /**
   * Renders the flags of the supplied variable declaration, such as "final", into a single
   * space-separated String.
   */
  private static String renderVariableTreeFlags(VariableTree variableTree) {
    StringBuilder flagsBuilder = new StringBuilder();
    if (!variableTree.getModifiers().getFlags().isEmpty()) {
      flagsBuilder.append(
          variableTree.getModifiers().getFlags().stream()
              .map(flag -> flag + " ")
              .collect(joining("")));
    }
    return flagsBuilder.toString();
  }

  /**
   * Renders the variable declarations that need to be hoisted above the switch statement. Each
   * variable declaration is rendered on its own line, with comments preserved where possible.
   *
   * @return true if the generated switch statement needs to be wrapped in braces
   */
  private static boolean renderHoistedVariables(
      StringBuilder renderTo,
      AnalysisResult analysisResult,
      SwitchTree switchTree,
      VisitorState state) {

    boolean wrapInBraces = false;
    if (!analysisResult.symbolsToHoist().isEmpty()) {
      // If the switch statement is part of a "LabeledStatement", we wrap the generated code in
      // braces to transform it into into a "Statement" (a "LocalVariableDeclarationStatement" is
      // not a "Statement"). See e.g. JLS 21 §14.4.2, 14.7.

      // Fetch the lowest ancestor LabelledStatementTree (if any)
      TreePath pathToEnclosing = state.findPathToEnclosing(LabeledStatementTree.class);
      if (pathToEnclosing != null) {
        Tree enclosing = pathToEnclosing.getLeaf();
        // This cast should always succeed
        if (enclosing instanceof LabeledStatementTree lst) {
          // We only need to wrap in braces where the SwitchTree is the immediate child of the
          // LabelledStatementTree
          if (lst.getStatement().equals(switchTree)) {
            wrapInBraces = true;
          }
        }
      }
    }

    if (wrapInBraces) {
      renderTo.append("{\n");
    }

    for (VariableTree variableTree : analysisResult.symbolsToHoist().values()) {
      renderTo.append(
          Streams.concat(
                  renderVariableTreeComments(variableTree, state).stream(),
                  renderVariableTreeAnnotations(variableTree, state).stream(),
                  Stream.of(renderVariableTreeFlags(variableTree)))
              .collect(joining("\n")));

      VarSymbol varSymbol = analysisResult.symbolsToHoist().inverse().get(variableTree);
      String sourceForType =
          hasImplicitType(variableTree, state)
              // If the variable is declared with "var", then we need to transform to an explicit
              // type declaration because Java cannot infer the type of a var unless it has an
              // initializer; hoisting an uninitialized "var" doesn't work.
              ? SuggestedFixes.prettyType(varSymbol.type, state)
              : state.getSourceForNode(variableTree.getType());

      renderTo.append(sourceForType).append(" ").append(variableTree.getName()).append(";\n");
    }
    return wrapInBraces;
  }

  /**
   * Transforms the supplied statement switch into an expression switch directly. In this
   * conversion, each nontrivial statement block is mapped one-to-one to a new {@code Expression} or
   * {@code StatementBlock} on the right-hand side (the `default:` case is removed if {@code
   * removeDefault} is true). Comments are preserved where possible.
   */
  private static SuggestedFix convertDirectlyToExpressionSwitch(
      SwitchTree switchTree,
      VisitorState state,
      AnalysisResult analysisResult,
      boolean removeDefault) {

    List<? extends CaseTree> cases = switchTree.getCases();
    ImmutableList<ErrorProneComment> allSwitchComments =
        state.getTokensForNode(switchTree).stream()
            .flatMap(errorProneToken -> errorProneToken.comments().stream())
            .collect(toImmutableList());
    StringBuilder replacementCodeBuilder = new StringBuilder();

    // Render the variable declarations that need to be hoisted above the switch statement
    boolean insertClosingBrace =
        renderHoistedVariables(replacementCodeBuilder, analysisResult, switchTree, state);

    // Render the switch statement
    replacementCodeBuilder
        .append("switch ")
        .append(state.getSourceForNode(switchTree.getExpression()))
        .append(" {");

    StringBuilder groupedCaseCommentsAccumulator = null;
    boolean firstCaseInGroup = true;
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);
      NullDefaultKind nullDefaultKind = analyzeCaseForNullAndDefault(caseTree);

      if (removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_DEFAULT)) {
        // Skip removed default (and its code) entirely
        continue;
      }

      // For readability, filter out trailing unlabelled break statement because these become a
      // No-Op when used inside expression switches
      ImmutableList<StatementTree> filteredStatements = filterOutRedundantBreak(caseTree);
      String transformedBlockSource =
          transformBlock(caseTree, state, filteredStatements, analysisResult.symbolsToHoist());

      if (firstCaseInGroup) {
        groupedCaseCommentsAccumulator =
            new StringBuilder(
                caseIndex == 0
                    ? extractCommentsBeforeFirstCase(switchTree, allSwitchComments).orElse("")
                    : "");

        replacementCodeBuilder
            .append("\n  ")
            .append(renderNullDefaultKindPrefix(nullDefaultKind, removeDefault));
      } else {
        // Second or later case in our group
        if (nullDefaultKind.equals(NullDefaultKind.KIND_DEFAULT)) {
          replacementCodeBuilder.append("default");
        }
      }

      if (nullDefaultKind.equals(NullDefaultKind.KIND_NEITHER)) {
        replacementCodeBuilder.append(printCaseExpressionsOrPatternAndGuard(caseTree, state));
      }
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

    // Close the surrounding braces (if needed)
    if (insertClosingBrace) {
      replacementCodeBuilder.append("\n}");
    }

    SuggestedFix.Builder suggestedFixBuilder = SuggestedFix.builder();
    if (removeDefault) {
      suggestedFixBuilder.setShortDescription(REMOVE_DEFAULT_CASE_SHORT_DESCRIPTION);
    }
    suggestedFixBuilder.replace(switchTree, replacementCodeBuilder.toString());
    return suggestedFixBuilder.build();
  }

  /**
   * Transforms the supplied statement switch into a {@code return switch ...} style of expression
   * switch. In this conversion, each nontrivial statement block is mapped one-to-one to a new
   * expression on the right-hand side of the arrow. Comments are preserved where possible.
   * Precondition: the {@code AnalysisResult} for the {@code SwitchTree} must have deduced that this
   * conversion is possible.
   */
  private static SuggestedFix convertToReturnSwitch(
      SwitchTree switchTree,
      VisitorState state,
      AnalysisResult analysisResult,
      boolean removeDefault) {

    SuggestedFix.Builder suggestedFixBuilder = SuggestedFix.builder();
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
      NullDefaultKind nullDefaultKind = analyzeCaseForNullAndDefault(caseTree);
      if (removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_DEFAULT)) {
        // Skip removed default (and its code) entirely
        continue;
      }

      String transformedBlockSource =
          transformReturnOrThrowBlock(caseTree, state, getStatements(caseTree));

      if (firstCaseInGroup) {
        groupedCaseCommentsAccumulator =
            new StringBuilder(
                caseIndex == 0
                    ? extractCommentsBeforeFirstCase(switchTree, allSwitchComments).orElse("")
                    : "");

        replacementCodeBuilder
            .append("\n  ")
            .append(renderNullDefaultKindPrefix(nullDefaultKind, removeDefault));
      } else {
        // Second or later case in our group
        if (nullDefaultKind.equals(NullDefaultKind.KIND_DEFAULT)) {
          replacementCodeBuilder.append("default");
        }
      }

      if (nullDefaultKind.equals(NullDefaultKind.KIND_NEITHER)) {
        replacementCodeBuilder.append(printCaseExpressionsOrPatternAndGuard(caseTree, state));
      }

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
    suggestedFixBuilder.replace(switchTree, replacementCodeBuilder.toString());
    return suggestedFixBuilder.build();
  }

  /** Retrieves a list of all statements (if any) preceding the current path, if any. */
  private static ImmutableList<StatementTree> getPrecedingStatementsInBlock(VisitorState state) {
    TreePath path = state.getPath();
    if (!(path.getParentPath().getLeaf() instanceof BlockTree blockTree)) {
      return ImmutableList.of();
    }
    var statements = blockTree.getStatements();
    return ImmutableList.copyOf(statements.subList(0, statements.indexOf(path.getLeaf())));
  }

  /**
   * Determines whether the last two preceding statements are not variable declarations within the
   * same VariableDeclaratorList, for example {@code int x, y;}. VariableDeclaratorLists are defined
   * in e.g. JLS 21 § 14.4. Precondition: all preceding statements are taken from the same {@code
   * BlockTree}.
   */
  private static boolean precedingTwoStatementsNotInSameVariableDeclaratorList(
      List<StatementTree> precedingStatements) {

    if (precedingStatements.size() < 2) {
      return true;
    }

    StatementTree secondToLastStatement = precedingStatements.get(precedingStatements.size() - 2);
    StatementTree lastStatement = Iterables.getLast(precedingStatements);
    if (!(secondToLastStatement instanceof VariableTree variableTree1)
        || !(lastStatement instanceof VariableTree variableTree2)) {
      return true;
    }

    // Start positions will vary if the variable declarations are in the same
    // VariableDeclaratorList.
    return getStartPosition(variableTree1) != getStartPosition(variableTree2);
  }

  /**
   * Transforms the supplied statement switch into an assignment switch style of expression switch.
   * In this conversion, each nontrivial statement block is mapped one-to-one to a new expression on
   * the right-hand side of the arrow (if {@code removeDefault} is true, then the {@code default:}
   * block is skipped). Comments are preserved where possible. Precondition: the {@code
   * AnalysisResult} for the {@code SwitchTree} must have deduced that this conversion is possible.
   */
  private static SuggestedFix convertToAssignmentSwitch(
      SwitchTree switchTree,
      VisitorState state,
      AnalysisResult analysisResult,
      boolean removeDefault) {

    SuggestedFix.Builder suggestedFixBuilder = SuggestedFix.builder();
    StringBuilder replacementCodeBuilder = new StringBuilder();

    analysisResult
        .assignmentSwitchAnalysisResult()
        .precedingVariableDeclaration()
        .ifPresent(
            variableTree -> {
              suggestedFixBuilder.delete(variableTree);

              replacementCodeBuilder.append(
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

              replacementCodeBuilder.append(sourceForType).append(" ");
            });

    List<? extends CaseTree> cases = switchTree.getCases();
    ImmutableList<ErrorProneComment> allSwitchComments =
        state.getTokensForNode(switchTree).stream()
            .flatMap(errorProneToken -> errorProneToken.comments().stream())
            .collect(toImmutableList());

    replacementCodeBuilder
        .append(
            state.getSourceForNode(
                analysisResult.assignmentSwitchAnalysisResult().assignmentTargetOptional().get()))
        .append(" ")
        // Invariant: always present when a finding exists
        .append(
            analysisResult.assignmentSwitchAnalysisResult().assignmentSourceCodeOptional().get())
        .append(" ")
        .append("switch ")
        .append(state.getSourceForNode(switchTree.getExpression()))
        .append(" {");

    StringBuilder groupedCaseCommentsAccumulator = null;
    boolean firstCaseInGroup = true;
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);
      NullDefaultKind nullDefaultKind = analyzeCaseForNullAndDefault(caseTree);

      if (removeDefault && nullDefaultKind.equals(NullDefaultKind.KIND_DEFAULT)) {
        // Skip removed default (and its code) entirely
        continue;
      }
      ImmutableList<StatementTree> filteredStatements = filterOutRedundantBreak(caseTree);

      String transformedBlockSource =
          transformAssignOrThrowBlock(caseTree, state, filteredStatements);

      if (firstCaseInGroup) {
        groupedCaseCommentsAccumulator =
            new StringBuilder(
                caseIndex == 0
                    ? extractCommentsBeforeFirstCase(switchTree, allSwitchComments).orElse("")
                    : "");

        replacementCodeBuilder
            .append("\n  ")
            .append(renderNullDefaultKindPrefix(nullDefaultKind, removeDefault));
      } else {
        // Second or later case in our group
        if (nullDefaultKind.equals(NullDefaultKind.KIND_DEFAULT)) {
          replacementCodeBuilder.append("default");
        }
      }

      if (nullDefaultKind.equals(NullDefaultKind.KIND_NEITHER)) {
        replacementCodeBuilder.append(printCaseExpressionsOrPatternAndGuard(caseTree, state));
      }

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

    if (removeDefault) {
      suggestedFixBuilder.setShortDescription(REMOVE_DEFAULT_CASE_SHORT_DESCRIPTION);
    }
    suggestedFixBuilder.replace(switchTree, replacementCodeBuilder.toString());
    return suggestedFixBuilder.build();
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
                  getStartPosition(getLast(getStatements(caseTree))))
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
    var statements = getStatements(caseTree);
    boolean caseEndsWithUnlabelledBreak =
        !statements.isEmpty()
            && getLast(statements) instanceof BreakTree bt
            && bt.getLabel() == null;
    return caseEndsWithUnlabelledBreak ? statements.subList(0, statements.size() - 1) : statements;
  }

  /**
   * Returns the statements of a {@link CaseTree}. If the only statement is a block statement,
   * return the block's statements instead.
   */
  private static @Nullable ImmutableList<StatementTree> getStatements(CaseTree caseTree) {
    List<? extends StatementTree> statements = caseTree.getStatements();
    if (statements == null) {
      return null;
    }
    if (statements.size() != 1) {
      return ImmutableList.copyOf(statements);
    }
    return getOnlyElement(statements) instanceof BlockTree blockTree
        ? ImmutableList.copyOf(blockTree.getStatements())
        : ImmutableList.copyOf(statements);
  }

  /** Transforms code for this case into the code under an expression switch. */
  private static String transformBlock(
      CaseTree caseTree,
      VisitorState state,
      ImmutableList<StatementTree> filteredStatements,
      ImmutableBiMap<VarSymbol, VariableTree> symbolsToHoist) {

    StringBuilder transformedBlockBuilder = new StringBuilder();
    int codeBlockStart = extractLhsComments(caseTree, state, transformedBlockBuilder);
    int codeBlockEnd = codeBlockStart;
    if (!filteredStatements.isEmpty()) {
      // One pass-algorithm:
      // * For each statement, if it's a variable declaration and if it's a for a symbol that is
      // being hoisted, then emit accumulated statements (if any), and transform the variable
      // declaration into an assignment and also emit that.  Otherwise, just accumulate the
      // statement.
      // * Emit any remaining accumulated statements
      for (int i = 0; i < filteredStatements.size(); i++) {
        StatementTree statement = filteredStatements.get(i);
        if (statement instanceof VariableTree variableTree) {
          // Transform hoisted variable declaration
          if (symbolsToHoist.containsValue(variableTree)) {
            // Emit accumulated statements (if any)
            if (codeBlockEnd > codeBlockStart) {
              transformedBlockBuilder.append(state.getSourceCode(), codeBlockStart, codeBlockEnd);
            }
            codeBlockStart =
                (i < filteredStatements.size() - 1)
                    ? getStartPosition(filteredStatements.get(i + 1))
                    : state.getEndPosition(statement);

            // If the hoisted variable has an initializer, transform into an assignment
            // For example `String hoisted = "foo";` becomes `hoisted = "foo";`.
            if (variableTree.getInitializer() != null) {
              transformedBlockBuilder.append(variableTree.getName()).append(" = ");
              transformedBlockBuilder
                  .append(
                      state.getSourceCode(),
                      getStartPosition(variableTree.getInitializer()),
                      state.getEndPosition(variableTree.getInitializer()))
                  .append(";\n");
            }
          }
        }
        codeBlockEnd =
            (i < filteredStatements.size() - 1)
                ? getStartPosition(filteredStatements.get(i + 1))
                : state.getEndPosition(statement);
      } // For each filtered statement

      // Emit accumulated statements (if any)
      if (codeBlockEnd > codeBlockStart) {
        transformedBlockBuilder.append(state.getSourceCode(), codeBlockStart, codeBlockEnd);
      }
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
    ImmutableList<StatementTree> statements = getStatements(caseTree);
    int lhsEnd =
        statements.isEmpty() ? state.getEndPosition(caseTree) : getStartPosition(statements.get(0));

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
    if (!(onlyStatement instanceof BlockTree blockTree)) {
      return state.getEndPosition(caseTree);
    }

    // The RHS of the case has a single enclosing block { ... }
    List<? extends StatementTree> blockStatements = blockTree.getStatements();
    return blockStatements.isEmpty()
        ? state.getEndPosition(caseTree)
        : state.getEndPosition(blockStatements.getLast());
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

  private static boolean hasCasePattern(CaseTree caseTree) {
    return caseTree.getLabels().stream()
        .anyMatch(caseLabelTree -> caseLabelTree instanceof PatternCaseLabelTree);
  }

  /**
   * Prints source for all expressions in a given {@code case}, separated by commas, or the pattern
   * and guard (if present).
   */
  private static String printCaseExpressionsOrPatternAndGuard(
      CaseTree caseTree, VisitorState state) {
    if (!hasCasePattern(caseTree)) {
      return caseTree.getExpressions().stream().map(state::getSourceForNode).collect(joining(", "));
    }
    // Currently, `case`s can only have a single pattern, however the compiler's class structure
    // does not reflect this restriction.
    StringBuilder sb =
        new StringBuilder(
            caseTree.getLabels().stream().map(state::getSourceForNode).collect(joining(", ")));
    if (caseTree.getGuard() != null) {
      sb.append(" when ").append(state.getSourceForNode(caseTree.getGuard())).append(" ");
    }
    return sb.toString();
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

    return isSwitchExhaustiveWithoutDefault(handledEnumValues, switchType);
  }

  /**
   * Ad-hoc algorithm to search for a surjective map from (non-null) values of a {@code switch}'s
   * expression to a {@code CaseTree}, not including a {@code default} case (if present).
   */
  private static boolean isSwitchExhaustiveWithoutDefault(
      Set<String> handledEnumValues, Type switchType) {
    // Handles switching on enum only (map is bijective)
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
    int codeBlockEnd = getBlockEnd(state, caseTree);
    if (statements.size() > 1) {
      transformedBlockBuilder.append("{\n");
      int codeBlockStart = extractLhsComments(caseTree, state, transformedBlockBuilder);
      int offset = transformedBlockBuilder.length();
      transformedBlockBuilder.append(state.getSourceCode(), codeBlockStart, codeBlockEnd);
      transformedBlockBuilder.append("\n}");
      if (getLast(statements) instanceof ReturnTree returnTree) {
        int start = getStartPosition(returnTree);
        transformedBlockBuilder.replace(
            offset + start - codeBlockStart,
            offset + start - codeBlockStart + "return".length(),
            "yield");
      }
    } else if (statements.size() == 1 && statements.get(0) instanceof ReturnTree returnTree) {
      // For "return x;", we want to take source starting after the "return"
      int unused = extractLhsComments(caseTree, state, transformedBlockBuilder);
      int codeBlockStart = getStartPosition(returnTree.getExpression());
      transformedBlockBuilder.append(state.getSourceCode(), codeBlockStart, codeBlockEnd);
    } else {
      int codeBlockStart = extractLhsComments(caseTree, state, transformedBlockBuilder);
      transformedBlockBuilder.append(state.getSourceCode(), codeBlockStart, codeBlockEnd);
    }

    return transformedBlockBuilder.toString();
  }

  /**
   * Transforms an assignment or throw into an expression statement suitable for use on the
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

    if (!statements.isEmpty()
        && statements.get(0) instanceof ExpressionStatementTree expressionStatementTree) {
      // For "x = foo", we want to take source starting after the "x ="
      int unused = extractLhsComments(caseTree, state, transformedBlockBuilder);
      ExpressionTree expression = expressionStatementTree.getExpression();
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

  /**
   * Determines whether the supplied {@code caseTree} case contains `case null` and/or `default`.
   */
  private static NullDefaultKind analyzeCaseForNullAndDefault(CaseTree caseTree) {
    boolean hasDefault = isSwitchDefault(caseTree);
    boolean hasNull =
        caseTree.getExpressions().stream()
            .anyMatch(expression -> expression.getKind().equals(Tree.Kind.NULL_LITERAL));

    if (hasNull && hasDefault) {
      return NullDefaultKind.KIND_NULL_AND_DEFAULT;
    } else if (hasNull) {
      return NullDefaultKind.KIND_NULL;
    } else if (hasDefault) {
      return NullDefaultKind.KIND_DEFAULT;
    }

    return NullDefaultKind.KIND_NEITHER;
  }

  /**
   * @param canConvertDirectlyToExpressionSwitch Whether the statement switch can be directly
   *     converted to an expression switch
   * @param canConvertToReturnSwitch Whether the statement switch can be converted to a return
   *     switch
   * @param canRemoveDefault Whether the assignment switch is exhaustive even in the absence of the
   *     default case that exists in the original switch statement
   * @param assignmentSwitchAnalysisResult Results of the analysis for conversion to an assignment
   *     switch
   * @param groupedWithNextCase List of whether each case tree can be grouped with its successor in
   *     transformed source code
   * @param symbolsToHoist Bidirectional map from symbols to hoist to the top of the switch
   *     statement to their declaration trees
   */
  record AnalysisResult(
      boolean canConvertDirectlyToExpressionSwitch,
      boolean canConvertToReturnSwitch,
      boolean canRemoveDefault,
      AssignmentSwitchAnalysisResult assignmentSwitchAnalysisResult,
      ImmutableList<Boolean> groupedWithNextCase,
      ImmutableBiMap<VarSymbol, VariableTree> symbolsToHoist) {}

  /**
   * @param canConvertToAssignmentSwitch Whether the statement switch can be converted to an
   *     assignment switch
   * @param precedingVariableDeclaration The immediately preceding variable declaration if this
   *     switch can be combined with it.
   * @param assignmentTargetOptional Target of the assignment switch, if any
   * @param assignmentKindOptional Kind of assignment made by the assignment switch, if any
   * @param assignmentSourceCodeOptional Java source code of the assignment switch's operator, e.g.
   *     "+="
   */
  record AssignmentSwitchAnalysisResult(
      boolean canConvertToAssignmentSwitch,
      Optional<VariableTree> precedingVariableDeclaration,
      Optional<ExpressionTree> assignmentTargetOptional,
      Optional<Tree.Kind> assignmentKindOptional,
      Optional<String> assignmentSourceCodeOptional) {}

  /**
   * @param assignmentSwitchCaseQualifications Overall qualification of the switch statement for
   *     conversion to an assignment switch
   * @param assignmentTargetOptional Target of the first assignment seen, if any
   * @param assignmentExpressionKindOptional Kind of the first assignment seen, if any
   * @param assignmentTreeOptional ExpressionTree of the first assignment seen, if any
   */
  record AssignmentSwitchAnalysisState(
      CaseQualifications assignmentSwitchCaseQualifications,
      Optional<ExpressionTree> assignmentTargetOptional,
      Optional<Tree.Kind> assignmentExpressionKindOptional,
      Optional<ExpressionTree> assignmentTreeOptional) {}
}
