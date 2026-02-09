/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.SwitchUtils.COMPILE_TIME_CONSTANT_MATCHER;
import static com.google.errorprone.bugpatterns.SwitchUtils.isEnumValue;
import static com.google.errorprone.bugpatterns.SwitchUtils.renderComments;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.sameVariable;
import static com.sun.source.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static com.sun.source.tree.Tree.Kind.THROW;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IfTreeMatcher;
import com.google.errorprone.bugpatterns.SwitchUtils.Validity;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneComment;
import com.google.errorprone.util.Reachability;
import com.google.errorprone.util.SourceVersion;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BindingPatternTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.YieldTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import org.jspecify.annotations.Nullable;

/** Checks for chains of if statements that may be converted to a switch. */
@BugPattern(severity = WARNING, summary = "This if-chain may be converted into a switch")
public final class IfChainToSwitch extends BugChecker implements IfTreeMatcher {
  // Braces are not required if there is exactly one statement on the right hand of the arrow, and
  // it's either an ExpressionStatement or a Throw.  Refer to JLS 14 §14.11.1
  private static final ImmutableSet<Kind> KINDS_CONVERTIBLE_WITHOUT_BRACES =
      ImmutableSet.of(THROW, EXPRESSION_STATEMENT);

  private final boolean enableMain;
  private final boolean enableSafe;
  private final int maxChainLength;
  private final ConstantExpressions constantExpressions;

  @Inject
  IfChainToSwitch(ErrorProneFlags flags, ConstantExpressions constantExpressions) {
    enableMain = flags.getBoolean("IfChainToSwitch:EnableMain").orElse(false);
    enableSafe = flags.getBoolean("IfChainToSwitch:EnableSafe").orElse(false);
    maxChainLength = flags.getInteger("IfChainToSwitch:MaxChainLength").orElse(50);
    this.constantExpressions = constantExpressions;
  }

  @Override
  public Description matchIf(IfTree ifTree, VisitorState visitorState) {
    if (!enableMain) {
      return NO_MATCH;
    }

    if (!SourceVersion.supportsPatternMatchingSwitch(visitorState.context)) {
      return NO_MATCH;
    }

    return analyzeIfTree(ifTree, visitorState);
  }

  /** Analyzes the tree rooted at {@code IfTree} for conversion to a switch. */
  private Description analyzeIfTree(IfTree ifTree, VisitorState state) {
    // Match only at start of chain; no if-within-if
    for (Tree tree : state.getPath().getParentPath()) {
      if (tree instanceof IfTree) {
        return NO_MATCH;
      }
    }

    Range<Integer> ifTreeSourceRange = computeIfTreeSourceRange(ifTree, state);
    IfChainAnalysisState ifChainAnalysisState =
        new IfChainAnalysisState(
            /* subjectOptional= */ Optional.empty(),
            /* depth= */ 1,
            Validity.MAYBE_VALID,
            /* at= */ ifTree,
            /* allConditionalBlocksReturnOrThrow= */ true,
            /* handledEnumValues= */ ImmutableSet.of());
    List<CaseIr> cases = new ArrayList<>();

    // Walk down the if-chain, performing quick analysis at each "if" to see whether it's
    // potentially convertible
    for (int iteration = 0;
        ifChainAnalysisState.validity().equals(Validity.MAYBE_VALID);
        iteration++) {

      ifChainAnalysisState =
          analyzeIfStatement(
              ifChainAnalysisState,
              ifChainAnalysisState.at().getCondition(),
              ifChainAnalysisState.at().getThenStatement(),
              ifChainAnalysisState.at().getElseStatement(),
              cases,
              state,
              ifTreeSourceRange);

      // If the if-chain is long, then the worst-case runtime of the deep analysis can grow.
      // For runtime performance reasons, limit the maximum length of the if-chain
      if (iteration > maxChainLength) {
        return NO_MATCH;
      }
    }

    if (!ifChainAnalysisState.validity().equals(Validity.VALID)
        // Exclude short if-chains, since they may be more readable as-is
        || ifChainAnalysisState.depth() < 3) {
      return NO_MATCH;
    }

    List<SuggestedFix> suggestedFixes =
        deepAnalysisOfIfChain(cases, ifChainAnalysisState, ifTree, state, ifTreeSourceRange);
    return suggestedFixes.isEmpty()
        ? NO_MATCH
        : buildDescription(ifTree).addAllFixes(suggestedFixes).build();
  }

  /** Compute the position range of the source code for the {@code IfTree} */
  private static Range<Integer> computeIfTreeSourceRange(IfTree ifTree, VisitorState state) {
    ImmutableList<StatementTree> subsequentIfStatements = getSubsequentStatementsInBlock(state);
    if (subsequentIfStatements.isEmpty()) {
      return Range.closedOpen(getStartPosition(ifTree), state.getEndPosition(ifTree));
    }
    return Range.closedOpen(
        getStartPosition(ifTree), state.getEndPosition(subsequentIfStatements.get(0)));
  }

  /**
   * Within the context of the right-hand-side of the arrow of a `case`, strips any redundant
   * surrounding braces. The returned value is either the input {@code StatementTree}, or a subtree
   * of the input that excludes the unnecessary braces.
   */
  private static StatementTree stripUnnecessaryBracesForArrowRhs(StatementTree statementTree) {

    StatementTree at = statementTree;

    while (at instanceof BlockTree blockTree && (blockTree.getStatements().size() == 1)) {
      StatementTree firstStatement = blockTree.getStatements().get(0);
      Kind firstStatementKind = firstStatement.getKind();

      if (KINDS_CONVERTIBLE_WITHOUT_BRACES.contains(firstStatementKind)) {
        // Remove braces
        at = firstStatement;
        break;
      }

      if (firstStatement instanceof BlockTree) {
        // Descend
        at = firstStatement;
        continue;
      }

      // We're neither over another block nor a statement that can be rendered without braces.
      // Thus, these braces are necessary.
      break;
    }
    return at;
  }

  /**
   * Determines whether the given statement tree needs to be wrapped in braces when used on the
   * right hand side of the arrow of a `case`.
   */
  public static boolean needsBracesForArrowRhs(StatementTree statementTree) {
    Kind statementTreeKind = statementTree.getKind();

    if (statementTree instanceof BlockTree) {
      // Already wrapped in braces
      return false;
    }

    if (KINDS_CONVERTIBLE_WITHOUT_BRACES.contains(statementTreeKind)) {
      return false;
    }

    return true;
  }

  /**
   * Renders Java source code for a {@code switch} statement, as specified by the supplied internal
   * representations. Context is extracted from the {@code VisitorState} and {@code
   * SuggestedFix.Builder} where needed.
   */
  private static String prettyPrint(
      List<CaseIr> cases,
      ExpressionTree subject,
      VisitorState state,
      SuggestedFix.Builder suggestedFixBuilder,
      Range<Integer> ifTreeSourceRange,
      ImmutableList<ErrorProneComment> allComments) {

    StringBuilder sb = new StringBuilder();
    sb.append("switch (").append(state.getSourceForNode(subject)).append(") {\n");
    for (CaseIr caseIr : cases) {
      if (caseIr.hasCaseNull() && caseIr.hasDefault()) {
        sb.append("case null, default");
      } else if (caseIr.hasCaseNull()) {
        sb.append("case null");
      } else if (caseIr.hasDefault()) {
        sb.append("default");
      } else if (caseIr.expressionsOptional().isPresent()) {
        sb.append("case ");
        sb.append(
            caseIr.expressionsOptional().get().stream()
                .map(state::getSourceForNode)
                .collect(joining(",")));
        sb.append(" ");
      } else if (caseIr.instanceOfOptional().isPresent()) {
        InstanceOfIr instanceOfIr = caseIr.instanceOfOptional().get();
        sb.append("case ");
        if (instanceOfIr.patternVariable().isPresent()) {
          sb.append(
              printRawTypesAsWildcards(
                  getType(instanceOfIr.patternVariable().get()), state, suggestedFixBuilder));

          Symbol sym = ASTHelpers.getSymbol(instanceOfIr.patternVariable().get());
          sb.append(" ").append(sym.getSimpleName()).append(" ");
        } else if (instanceOfIr.expression().isPresent()) {
          sb.append(
              printRawTypesAsWildcards(getType(instanceOfIr.type()), state, suggestedFixBuilder));
          // It's possible that "unused" could conflict with an existing local variable name;
          // support for unnamed variables gets around this issue, but requires later Java versions
          sb.append(" unused ");
        }
        if (caseIr.guardOptional().isPresent()) {
          sb.append("when ").append(state.getSourceForNode(caseIr.guardOptional().get()));
        }
      }

      sb.append(" -> ");

      int ifTreeStart = ifTreeSourceRange.lowerEndpoint();

      if (caseIr.arrowRhsOptional().isEmpty()) {
        // Empty braces on RHS; block is synthesized, so no comments to render
        sb.append("{}");
      } else {
        StatementTree stripped = stripUnnecessaryBracesForArrowRhs(caseIr.arrowRhsOptional().get());
        Range<Integer> printedRange =
            Range.closedOpen(getStartPosition(stripped), state.getEndPosition(stripped));
        ImmutableList<ErrorProneComment> orphanedComments =
            allComments.stream()
                // Within this case's source code
                .filter(
                    comment ->
                        caseIr
                            .caseSourceCodeRange()
                            .encloses(buildCommentRange(comment, ifTreeStart)))
                // But outside the printed range for the RHS
                .filter(
                    comment -> !buildCommentRange(comment, ifTreeStart).isConnected(printedRange))
                .collect(toImmutableList());

        boolean needsBraces = needsBracesForArrowRhs(stripped);
        if (needsBraces) {
          sb.append("{");
        }

        String renderedOrphans = renderComments(orphanedComments);
        if (!renderedOrphans.isEmpty()) {
          sb.append("\n").append(renderedOrphans).append("\n");
        }

        sb.append(state.getSourceForNode(stripped));
        if (needsBraces) {
          sb.append("}");
        }
      }
      sb.append("\n");
    }
    sb.append("}");

    return sb.toString();
  }

  /**
   * Returns a range that encloses the given comment, offset by the given start position of the if
   * tree.
   */
  private static Range<Integer> buildCommentRange(ErrorProneComment comment, int ifTreeStart) {
    return Range.closedOpen(comment.getPos() + ifTreeStart, comment.getEndPos() + ifTreeStart);
  }

  /**
   * Renders Java source code representation of the supplied {@code Type} that is suitable for use
   * in fixes, where any raw types are replaced with wildcard types. For example, `List` becomes
   * `List<?>`.
   */
  private static String printRawTypesAsWildcards(
      Type type, VisitorState state, SuggestedFix.Builder suggestedFixBuilder) {
    StringBuilder sb = new StringBuilder();
    List<TypeVariableSymbol> typeParameters = type.tsym.getTypeParameters();
    List<Type> typeArguments = type.getTypeArguments();
    Types types = state.getTypes();

    // Unwrap array-of's
    if (types.isArray(type)) {
      Type at = type;
      StringBuilder suffix = new StringBuilder();
      while (types.isArray(at)) {
        suffix.append("[]");
        at = types.elemtype(at);
      }
      // Primitive types are always in context and don't need qualification
      sb.append(
          at.isPrimitive()
              ? SuggestedFixes.prettyType(at, state)
              : SuggestedFixes.qualifyType(state, suggestedFixBuilder, at.tsym));
      sb.append(suffix);
    } else {
      sb.append(SuggestedFixes.qualifyType(state, suggestedFixBuilder, type.tsym));
    }

    if (!typeParameters.isEmpty()) {
      if (typeArguments.isEmpty()) {
        sb.append("<");
        sb.repeat("?,", max(0, typeParameters.size() - 1))
            .repeat("?", min(1, typeParameters.size()));
        sb.append(">");
      } else {
        sb.append("<");
        sb.append(
            typeArguments.stream()
                .map(t -> SuggestedFixes.prettyType(t, state))
                .collect(joining(",")));
        sb.append(">");
      }
    }

    return sb.toString();
  }

  /**
   * Determines whether a {@code switch} having the given {@code subject} and {@code cases} would
   * implicitly throw in the event that the {@code subject} is {@code null} at runtime. Here,
   * implicitly throwing means that an exception would be thrown, and further that the {@code throw}
   * would not be caused by logic in any of the supplied {@code cases}. (If the subject cannot be
   * assigned {@code null}, returns {@code false}.)
   */
  private static boolean switchOnNullWouldImplicitlyThrow(
      ExpressionTree subject, List<CaseIr> cases) {
    return !getType(subject).isPrimitive()
        // If there is an explicit `case null` already, then there can't be an implicit throw caused
        // by null
        && cases.stream().noneMatch(CaseIr::hasCaseNull);
  }

  /**
   * Analyzes the supplied case IRs for a switch statement for issues related default/unconditional
   * cases. If deemed necessary, this method injects a `default` and/or `case null` into the
   * supplied case IRs. If the supplied case IRs cannot be used to form a syntactically valid switch
   * statement, returns `Optional.empty()`. Precondition: the list of supplied cases must not
   * contain any dominance violations.
   */
  private Optional<List<CaseIr>> maybeFixDefaultNullAndUnconditional(
      List<CaseIr> cases,
      ExpressionTree subject,
      StatementTree ifTree,
      VisitorState state,
      Optional<SuggestedFix.Builder> suggestedFixBuilder,
      int numberPulledUp,
      Set<String> handledEnumValues,
      Type switchType,
      Range<Integer> ifTreeSourceRange) {

    // Make a mutable copy of the cases, so that we can inject new cases as needed.
    cases = new ArrayList<>(cases);

    boolean hasDefault = cases.stream().anyMatch(CaseIr::hasDefault);
    boolean hasCaseNull = cases.stream().anyMatch(CaseIr::hasCaseNull);
    // NOMUTANTS -- this is a performance optimization
    boolean recheckDominanceNeeded = false;

    boolean switchOnNullWouldImplicitlyThrow = switchOnNullWouldImplicitlyThrow(subject, cases);

    // Has an unconditional case, meaning that any non-null value of the subject will be matched
    long unconditionalCount =
        cases.stream()
            .filter(
                caseIr ->
                    caseIr.instanceOfOptional().isPresent()
                        && caseIr.guardOptional().isEmpty()
                        && isSubtype(
                            getType(subject),
                            getType(caseIr.instanceOfOptional().get().type()),
                            state))
            .count();

    boolean hasUnconditional = unconditionalCount > 0;
    boolean hasMultipleUnconditional = unconditionalCount > 1;
    // Has at least once case with a pattern
    boolean hasPattern = cases.stream().anyMatch(x -> x.instanceOfOptional().isPresent());

    boolean allEnumValuesPresent =
        isEnumValue(subject, state)
            && handledEnumValues.containsAll(ASTHelpers.enumValues(switchType.asElement()));

    if (hasDefault && hasUnconditional) {
      // The explicit default cases conflicts with the unconditional case
      return Optional.empty();
    }

    if (hasMultipleUnconditional) {
      // Multiple unconditional cases conflicts with each other
      return Optional.empty();
    }

    if (hasPattern && !(hasDefault || hasUnconditional)) {
      // If there's a pattern, then the switch must be exhaustive, but it's not
      return Optional.empty();
    }

    // Although not required by Java itself, Error Prone will generate a finding if there is no
    // default for the switch.  We conform to that convention here, too.
    if (!hasPattern && !hasDefault && !allEnumValuesPresent) {
      int previousCaseEndPosition =
          cases.stream()
              .map(CaseIr::caseSourceCodeRange)
              .mapToInt(Range::upperEndpoint)
              .max()
              .orElse(ifTreeSourceRange.lowerEndpoint());
      cases.add(
          new CaseIr(
              /* hasCaseNull= */ !hasCaseNull && (enableSafe && switchOnNullWouldImplicitlyThrow),
              /* hasDefault= */ true,
              /* instanceOfOptional= */ Optional.empty(),
              /* guardOptional= */ Optional.empty(),
              /* expressionsOptional= */ Optional.empty(),
              /* arrowRhsOptional= */ Optional.empty(),
              /* caseSourceCodeRange= */ Range.closedOpen(
                  previousCaseEndPosition, previousCaseEndPosition)));
      recheckDominanceNeeded = true;
    } else if (enableSafe && !hasCaseNull && switchOnNullWouldImplicitlyThrow) {
      if (hasDefault) {
        // Upgrade existing `default` to `case null, default`.
        cases =
            cases.stream()
                .map(
                    caseIr -> {
                      if (caseIr.hasDefault()) {
                        return new CaseIr(
                            /* hasCaseNull= */ true,
                            /* hasDefault= */ true,
                            /* instanceOfOptional= */ caseIr.instanceOfOptional(),
                            /* guardOptional= */ caseIr.guardOptional(),
                            /* expressionsOptional= */ caseIr.expressionsOptional(),
                            /* arrowRhsOptional= */ caseIr.arrowRhsOptional(),
                            /* caseSourceCodeRange= */ caseIr.caseSourceCodeRange());
                      }
                      return caseIr;
                    })
                .collect(toImmutableList());
        recheckDominanceNeeded = true;
      } else {
        // Inject new `case null -> {}` for safe mode, to avoid implicit throw
        cases.add(
            new CaseIr(
                /* hasCaseNull= */ true,
                /* hasDefault= */ false,
                /* instanceOfOptional= */ Optional.empty(),
                /* guardOptional= */ Optional.empty(),
                /* expressionsOptional= */ Optional.empty(),
                /* arrowRhsOptional= */ Optional.empty(),
                /* caseSourceCodeRange= */ Range.closedOpen(
                    ifTreeSourceRange.lowerEndpoint(), ifTreeSourceRange.lowerEndpoint())));
        recheckDominanceNeeded = true;
      }
    }

    // Given any possible changes, is the code after the switch reachable?
    if (suggestedFixBuilder.isPresent()) {
      long emptyRhsBlockCount =
          cases.stream().filter(caseIr -> caseIr.arrowRhsOptional().isEmpty()).count();
      long canCompleteNormallyBlockCount =
          cases.stream()
              .filter(
                  caseIr ->
                      caseIr.arrowRhsOptional().isPresent()
                          && Reachability.canCompleteNormally(
                              caseIr.arrowRhsOptional().get(), ImmutableMap.of()))
              .count();

      if (emptyRhsBlockCount + canCompleteNormallyBlockCount == 0) {
        // All cases cannot complete normally, so we need to do reachability analysis
        Tree cannotCompleteNormallyTree = ifTree;
        // Search up the AST for enclosing statement blocks, marking any newly-dead code for
        // deletion along the way
        Tree prev = state.getPath().getLeaf();
        for (Tree tree : state.getPath().getParentPath()) {
          if (tree instanceof BlockTree blockTree) {
            var statements = blockTree.getStatements();
            int indexInBlock = statements.indexOf(prev);
            // A single mock of the immediate child statement block (or switch) is sufficient to
            // analyze reachability here; deeper-nested statements are not relevant.
            boolean nextStatementReachable =
                Reachability.canCompleteNormally(
                    statements.get(indexInBlock),
                    ImmutableMap.of(cannotCompleteNormallyTree, false));
            // If we continue to the ancestor statement block, it will be because the end of this
            // statement block is not reachable
            cannotCompleteNormallyTree = blockTree;
            if (nextStatementReachable) {
              break;
            }
            // If a next statement in this block exists, then it is not reachable.
            if (indexInBlock + numberPulledUp < statements.size() - 1) {
              String deletedRegion =
                  state
                      .getSourceCode()
                      .subSequence(
                          state.getEndPosition(statements.get(indexInBlock + numberPulledUp)),
                          state.getEndPosition(blockTree))
                      .toString();
              // If the region we would delete looks interesting, bail out and just delete the
              // orphaned statements.
              if (deletedRegion.contains("LINT.")) {
                statements
                    .subList(indexInBlock + 1, statements.size())
                    .forEach(suggestedFixBuilder.get()::delete);
              } else {
                // If the region doesn't seem to contain interesting comments, delete it along with
                // comments: those comments are often just of the form "Unreachable code".
                suggestedFixBuilder
                    .get()
                    .replace(
                        state.getEndPosition(statements.get(indexInBlock)),
                        state.getEndPosition(blockTree),
                        "}");
              }
            }
          }
          // Only applies in lowest block
          numberPulledUp = 0;

          prev = tree;
        }
      }
    }

    return recheckDominanceNeeded
        // If there is a dominance violation, then we must have caused it by adding a case, so
        // fixing it merely puts the new case in the correct position
        ? maybeFixDominance(cases, state, subject, /* canReorderCases= */ true)
        : Optional.of(cases);
  }

  /**
   * Analyzes the supplied case IRs for a switch statement. If deemed likely possible, this method
   * pulls up the statement subsequent to the if statement into the switch under a default case, and
   * removes it in the {@code SuggestedFix.Builder}. If deemed not likely possible, returns the
   * original case IRs. Note that this method does not fully validate the resulting case IRs, but
   * rather only partially validates them with respect to pull-up.
   */
  private Optional<List<CaseIr>> maybePullUp(
      List<CaseIr> cases,
      VisitorState state,
      IfChainAnalysisState ifChainAnalysisState,
      Optional<SuggestedFix.Builder> suggestedFixBuilder,
      Range<Integer> ifTreeRange) {

    if (ifChainAnalysisState.subjectOptional().isEmpty()) {
      // Nothing to analyze
      return Optional.empty();
    }

    List<CaseIr> casesCopy = new ArrayList<>(cases);

    if (ifChainAnalysisState.at().getElseStatement() == null
        && ifChainAnalysisState.allConditionalBlocksReturnOrThrow()) {
      ImmutableList<StatementTree> subsequentIfStatements = getSubsequentStatementsInBlock(state);
      // If there are, say, ten subsequent statements, pulling up one and leaving the other nine
      // wouldn't necessarily be a readability win.
      int subsequentStatementsLimit = 1;
      if (subsequentIfStatements.size() <= subsequentStatementsLimit) {
        for (StatementTree statement : subsequentIfStatements) {
          if (hasBreakOrYieldInTree(statement)) {
            // Statements containing break or yield cannot be pulled up
            break;
          }
          int startPos =
              casesCopy.isEmpty()
                  ? ifTreeRange.lowerEndpoint()
                  : casesCopy.getLast().caseSourceCodeRange().upperEndpoint();
          int endPos = state.getEndPosition(statement);
          casesCopy.add(
              new CaseIr(
                  /* hasCaseNull= */ enableSafe
                      && switchOnNullWouldImplicitlyThrow(
                          ifChainAnalysisState.subjectOptional().get(), cases),
                  /* hasDefault= */ true,
                  /* instanceOfOptional= */ Optional.empty(),
                  /* guardOptional= */ Optional.empty(),
                  /* expressionsOptional= */ Optional.empty(),
                  /* arrowRhsOptional= */ Optional.of(statement),
                  /* caseSourceCodeRange= */ Range.closedOpen(startPos, endPos)));
          if (suggestedFixBuilder.isPresent()) {
            suggestedFixBuilder.get().delete(statement);
          }
        }
      }
    }
    return Optional.of(casesCopy);
  }

  /**
   * Analyzes the supplied case IRs for duplicate constants (either primitives or enum values). If
   * any duplicates are found, returns {@code Optional.empty()}.
   */
  private static Optional<List<CaseIr>> maybeDetectDuplicateConstants(List<CaseIr> cases) {

    Set<Object> seenConstants = new HashSet<>();

    for (CaseIr caseIr : cases) {
      if (caseIr.expressionsOptional().isPresent()) {
        for (ExpressionTree expression : caseIr.expressionsOptional().get()) {
          // Check for compile-time constants
          Object constant = constValue(expression);
          if (constant != null) {
            if (seenConstants.contains(constant)) {
              return Optional.empty();
            }
            seenConstants.add(constant);
          }

          // Check for enums
          var sym = ASTHelpers.getSymbol(expression);
          if (sym != null && sym.getKind() == ElementKind.ENUM_CONSTANT) {
            if (seenConstants.contains(sym)) {
              return Optional.empty();
            }
            seenConstants.add(sym);
          }
        }
      }
    }

    return Optional.of(cases);
  }

  /**
   * Analyzes the supplied case IRs for dominance violations. If a dominance violation is detected
   * and {@code canReorderCases} is {@code true}, attempts to fix the problem by reordering cases so
   * that no violation is present. If a dominance violation exists and cannot be corrected by this
   * algorithm, returns {@code Optional.empty()}.
   */
  private static Optional<List<CaseIr>> maybeFixDominance(
      List<CaseIr> cases, VisitorState state, ExpressionTree subject, boolean canReorderCases) {

    List<CaseIr> casesCopy = new ArrayList<>(cases);
    List<CaseIr> casesToInsert = new ArrayList<>();

    for (int insert = 0; insert < casesCopy.size(); insert++) {
      // Happy path: try to insert at the end, in its original code order (i.e. the order in which
      // they were encountered in the if-chain)
      casesToInsert.add(casesCopy.get(insert));

      // Violation found when trying to insert @insert?
      if (hasDominanceViolation(casesToInsert, state, subject)) {
        if (!canReorderCases) {
          // Dominance violation exists and not allowed to fix
          return Optional.empty();
        }
        casesToInsert.remove(casesToInsert.size() - 1);

        // Slow path: try to clean up by moving insertion around.  The idea is a bit like an
        // insertion sort, although not entirely, since dominance is not transitive, thus it does
        // not constitute a partial order
        List<CaseIr> temp = new ArrayList<>();
        boolean cleanedUp = false;
        for (int insertPos = casesToInsert.size() - 1; insertPos >= 0; insertPos--) {
          temp.clear();
          int readIndex = 0;

          for (int i = 0; i < insertPos; i++) {
            temp.add(casesToInsert.get(readIndex++));
          }
          temp.add(casesCopy.get(insert));
          for (; readIndex < casesToInsert.size(); readIndex++) {
            temp.add(casesToInsert.get(readIndex));
          }
          if (!hasDominanceViolation(temp, state, subject)) {
            cleanedUp = true;
            // Successfully cleaned up by inserting @insertPos
            break;
          }
        }

        if (cleanedUp) {
          casesToInsert = temp;
        } else {
          // Cleanup failed
          return Optional.empty();
        }
      }
    }

    return Optional.of(casesToInsert);
  }

  /**
   * Checks whether the supplied case IRs contain a dominance violation, returning {@code true} if
   * so.
   */
  private static boolean hasDominanceViolation(
      List<CaseIr> cases, VisitorState state, ExpressionTree subject) {
    List<CaseIr> casesCopy = new ArrayList<>(cases);

    for (int r = 1; r < casesCopy.size(); r++) {
      for (int l = 0; l < r; l++) {
        CaseIr caseR = casesCopy.get(r);
        CaseIr caseL = casesCopy.get(l);
        boolean violation = isDominatedBy(caseL, caseR, state, subject);
        if (violation) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Analyzes the supplied if statement (current level only, no recursion) for the purposes of
   * converting it to a switch statement. Returns the analysis state following the analysis of the
   * if statement at this level.
   */
  private IfChainAnalysisState analyzeIfStatement(
      IfChainAnalysisState ifChainAnalysisState,
      ExpressionTree condition,
      StatementTree conditionalBlock,
      StatementTree elseStatement,
      List<CaseIr> cases,
      VisitorState state,
      Range<Integer> ifTreeRange) {

    if (ifChainAnalysisState.validity().equals(Validity.INVALID)) {
      return ifChainAnalysisState;
    }

    boolean conditionalBlockCanCompleteNormally =
        conditionalBlock == null || Reachability.canCompleteNormally(conditionalBlock);

    // Is the predicate sensible?
    Set<String> handledEnumValues = new HashSet<>(ifChainAnalysisState.handledEnumValues());
    int caseStartPosition =
        cases.isEmpty()
            ? ifTreeRange.lowerEndpoint()
            : cases.getLast().caseSourceCodeRange().upperEndpoint();
    Optional<ExpressionTree> newSubjectOptional =
        validatePredicateForSubject(
            condition,
            ifChainAnalysisState.subjectOptional(),
            state,
            /* mustBeInstanceOf= */ false,
            cases,
            /* elseOptional= */ Optional.ofNullable(elseStatement),
            /* arrowRhsOptional= */ Optional.ofNullable(conditionalBlock),
            handledEnumValues,
            ifTreeRange,
            caseStartPosition);
    if (newSubjectOptional.isEmpty()) {
      // Not sensible, return invalid state
      return new IfChainAnalysisState(
          Optional.empty(),
          ifChainAnalysisState.depth() + 1,
          Validity.INVALID,
          ifChainAnalysisState.at(),
          ifChainAnalysisState.allConditionalBlocksReturnOrThrow()
              && !conditionalBlockCanCompleteNormally,
          ImmutableSet.copyOf(handledEnumValues));
    }

    if (!(elseStatement instanceof IfTree elseStatementIfTree)) {
      // We reached the final else in the chain (if non-null), or reached the end of the chain and
      // there's no else (if null)
      return new IfChainAnalysisState(
          ifChainAnalysisState.subjectOptional(),
          ifChainAnalysisState.depth(),
          Validity.VALID,
          ifChainAnalysisState.at(),
          ifChainAnalysisState.allConditionalBlocksReturnOrThrow()
              && !conditionalBlockCanCompleteNormally,
          ImmutableSet.copyOf(handledEnumValues));
    }

    // Invariant:The next level of the chain exists
    return new IfChainAnalysisState(
        newSubjectOptional,
        ifChainAnalysisState.depth() + 1,
        ifChainAnalysisState.validity(),
        elseStatementIfTree,
        ifChainAnalysisState.allConditionalBlocksReturnOrThrow()
            && !conditionalBlockCanCompleteNormally,
        ImmutableSet.copyOf(handledEnumValues));
  }

  /** Determines whether any yield or break statements are present in the tree. */
  private static boolean hasBreakOrYieldInTree(Tree tree) {
    Boolean result =
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean visitBreak(BreakTree breakTree, Void unused) {
            return true;
          }

          @Override
          public Boolean visitYield(YieldTree continueTree, Void unused) {
            return true;
          }

          @Override
          public Boolean reduce(@Nullable Boolean left, @Nullable Boolean right) {
            return Objects.equals(left, true) || Objects.equals(right, true);
          }
        }.scan(tree, null);

    return result != null && result;
  }

  /**
   * Validates whether the predicate of the if statement can be converted by this checker to a
   * switch, returning the expression being switched on if so. If it cannot be converted by this
   * checker, returns {@code Optional.empty()}. Note that this checker's conversion is best-effort,
   * so even if this method returns {@code Optional.empty()} the predicate may still be convertible
   * by some other (unsupported) means.
   */
  private Optional<ExpressionTree> validatePredicateForSubject(
      ExpressionTree predicate,
      Optional<ExpressionTree> subject,
      VisitorState state,
      boolean mustBeInstanceOf,
      List<CaseIr> cases,
      Optional<StatementTree> elseOptional,
      Optional<StatementTree> arrowRhsOptional,
      Set<String> handledEnumValues,
      Range<Integer> ifTreeRange,
      int caseStartPosition) {

    int caseEndPosition =
        elseOptional.isPresent()
            ? getStartPosition(elseOptional.get())
            : (arrowRhsOptional.isPresent()
                ? state.getEndPosition(arrowRhsOptional.get())
                : state.getEndPosition(predicate));

    boolean hasElse = elseOptional.isPresent();
    boolean hasElseIf = hasElse && (elseOptional.get() instanceof IfTree);

    // Strip any surrounding parentheses e.g. `if(((x == 1)))`
    ExpressionTree at = ASTHelpers.stripParentheses(predicate);

    InstanceOfTree instanceOfTree = null;
    if (at instanceof InstanceOfTree iot) {
      instanceOfTree = iot;
      at = iot.getExpression();
    }

    if (at instanceof BinaryTree binaryTree) {
      ExpressionTree lhs = binaryTree.getLeftOperand();
      ExpressionTree rhs = binaryTree.getRightOperand();
      boolean predicateIsEquality = binaryTree.getKind().equals(Kind.EQUAL_TO);
      boolean predicateIsConditionalAnd = binaryTree.getKind().equals(Kind.CONDITIONAL_AND);

      if (!mustBeInstanceOf && predicateIsEquality) {
        // Either lhs or rhs must be a compile-time constant.
        if (COMPILE_TIME_CONSTANT_MATCHER.matches(lhs, state)
            || COMPILE_TIME_CONSTANT_MATCHER.matches(rhs, state)) {
          return validateCompileTimeConstantForSubject(
              lhs,
              rhs,
              subject,
              state,
              cases,
              elseOptional,
              arrowRhsOptional,
              ifTreeRange,
              caseEndPosition,
              hasElse,
              hasElseIf);
        } else {
          // Predicate is a binary tree, but neither side is a constant.
          if (isEnumValue(lhs, state) || isEnumValue(rhs, state)) {
            return validateEnumPredicateForSubject(
                lhs,
                rhs,
                subject,
                state,
                cases,
                elseOptional,
                arrowRhsOptional,
                handledEnumValues,
                ifTreeRange,
                caseEndPosition,
                hasElse,
                hasElseIf);
          }

          return Optional.empty();
        }
      } else if (predicateIsConditionalAnd) {
        // Maybe the predicate is something like `a instanceof Foo && predicate`.  If so, recurse on
        // the left side, and attach the right side of the conditional and as a guard to the
        // resulting case.
        if (!mustBeInstanceOf && binaryTree.getKind().equals(Kind.CONDITIONAL_AND)) {
          int currentCasesSize = cases.size();
          var rv =
              validatePredicateForSubject(
                  binaryTree.getLeftOperand(),
                  subject,
                  state,
                  /* mustBeInstanceOf= */ true,
                  cases,
                  elseOptional,
                  arrowRhsOptional,
                  handledEnumValues,
                  ifTreeRange,
                  /* caseStartPosition= */ caseStartPosition);
          if (rv.isPresent()) {
            CaseIr oldLastCase = cases.get(currentCasesSize);
            // Update last case to attach the guard
            cases.set(
                currentCasesSize,
                new CaseIr(
                    /* hasCaseNull= */ oldLastCase.hasCaseNull(),
                    /* hasDefault= */ oldLastCase.hasDefault(),
                    /* instanceOfOptional= */ oldLastCase.instanceOfOptional(),
                    /* guardOptional= */ Optional.of(binaryTree.getRightOperand()),
                    /* expressionsOptional= */ oldLastCase.expressionsOptional(),
                    /* arrowRhsOptional= */ oldLastCase.arrowRhsOptional(),
                    /* caseSourceCodeRange= */ oldLastCase.caseSourceCodeRange()));
            return rv;
          }
        }
      }
    }

    if (instanceOfTree != null) {
      return validateInstanceofForSubject(
          at,
          instanceOfTree,
          subject,
          state,
          cases,
          elseOptional,
          arrowRhsOptional,
          ifTreeRange,
          caseEndPosition,
          hasElse,
          hasElseIf);
    }

    // Predicate not a supported style
    return Optional.empty();
  }

  /**
   * Determines whether the {@code subject} expression "matches" the given {@code expression}. If
   * {@code enableSafe} is true, then matching means that the subject must be referring to the same
   * variable or constant expression. If {@code enableSafe} is false, then we also allow expressions
   * that have potential side-effects.
   */
  private boolean subjectMatches(
      ExpressionTree subject, ExpressionTree expression, VisitorState state) {

    boolean sameVariable = sameVariable(subject, expression);

    return enableSafe
        ? sameVariable || constantExpressions.isSame(subject, expression, state)
        : sameVariable || expressionSourceMatches(subject, expression, state);
  }

  private Optional<ExpressionTree> validateInstanceofForSubject(
      ExpressionTree at,
      InstanceOfTree instanceOfTree,
      Optional<ExpressionTree> subject,
      VisitorState state,
      List<CaseIr> cases,
      Optional<StatementTree> elseOptional,
      Optional<StatementTree> arrowRhsOptional,
      Range<Integer> ifTreeRange,
      int caseEndPosition,
      boolean hasElse,
      boolean hasElseIf) {

    ExpressionTree expression = at;
    // Does this expression and the subject (if present) refer to the same thing?
    if (subject.isPresent() && !subjectMatches(subject.get(), expression, state)) {
      return Optional.empty();
    }

    if (instanceOfTree.getPattern() instanceof BindingPatternTree bpt) {
      boolean addDefault = hasElse && !hasElseIf;
      int previousCaseEndPosition =
          cases.isEmpty()
              ? ifTreeRange.lowerEndpoint()
              : cases.getLast().caseSourceCodeRange().upperEndpoint();
      cases.add(
          new CaseIr(
              /* hasCaseNull= */ false,
              /* hasDefault= */ false,
              /* instanceOfOptional= */ Optional.of(
                  new InstanceOfIr(
                      Optional.ofNullable(instanceOfTree.getExpression()),
                      Optional.ofNullable(bpt.getVariable()),
                      instanceOfTree.getType())),
              /* guardOptional= */ Optional.empty(),
              /* expressionsOptional= */ Optional.empty(),
              /* arrowRhsOptional= */ arrowRhsOptional,
              /* caseSourceCodeRange= */ Range.closedOpen(
                  previousCaseEndPosition, caseEndPosition)));

      if (addDefault) {
        previousCaseEndPosition =
            cases.isEmpty()
                ? ifTreeRange.lowerEndpoint()
                : cases.getLast().caseSourceCodeRange().upperEndpoint();
        cases.add(
            new CaseIr(
                /* hasCaseNull= */ false,
                /* hasDefault= */ true,
                /* instanceOfOptional= */ Optional.empty(),
                /* guardOptional= */ Optional.empty(),
                /* expressionsOptional= */ Optional.empty(),
                /* arrowRhsOptional= */ elseOptional,
                /* caseSourceCodeRange= */ Range.closedOpen(
                    caseEndPosition,
                    elseOptional.isPresent()
                        ? getStartPosition(elseOptional.get())
                        : caseEndPosition)));
      }
    } else if (instanceOfTree.getType() != null) {
      boolean addDefault = hasElse && !hasElseIf;
      int previousCaseEndPosition =
          cases.isEmpty()
              ? ifTreeRange.lowerEndpoint()
              : cases.getLast().caseSourceCodeRange().upperEndpoint();
      cases.add(
          new CaseIr(
              /* hasCaseNull= */ false,
              /* hasDefault= */ false,
              /* instanceOfOptional= */ Optional.of(
                  new InstanceOfIr(
                      Optional.ofNullable(instanceOfTree.getExpression()),
                      Optional.empty(),
                      instanceOfTree.getType())),
              /* guardOptional= */ Optional.empty(),
              /* expressionsOptional= */ Optional.empty(),
              /* arrowRhsOptional= */ arrowRhsOptional,
              /* caseSourceCodeRange= */ Range.closedOpen(
                  previousCaseEndPosition, caseEndPosition)));
      if (addDefault) {
        cases.add(
            new CaseIr(
                /* hasCaseNull= */ false,
                /* hasDefault= */ true,
                /* instanceOfOptional= */ Optional.empty(),
                /* guardOptional= */ Optional.empty(),
                /* expressionsOptional= */ Optional.empty(),
                /* arrowRhsOptional= */ elseOptional,
                /* caseSourceCodeRange= */ Range.closedOpen(
                    caseEndPosition,
                    elseOptional.isPresent()
                        ? state.getEndPosition(elseOptional.get())
                        : caseEndPosition)));
      }
    } else {
      // Neither a binding pattern tree nor a type (possibly a record); unsupported
      return Optional.empty();
    }

    return Optional.of(expression);
  }

  private Optional<ExpressionTree> validateCompileTimeConstantForSubject(
      ExpressionTree lhs,
      ExpressionTree rhs,
      Optional<ExpressionTree> subject,
      VisitorState state,
      List<CaseIr> cases,
      Optional<StatementTree> elseOptional,
      Optional<StatementTree> arrowRhsOptional,
      Range<Integer> ifTreeRange,
      int caseEndPosition,
      boolean hasElse,
      boolean hasElseIf) {
    boolean compileTimeConstantOnLhs = COMPILE_TIME_CONSTANT_MATCHER.matches(lhs, state);
    ExpressionTree testExpression = compileTimeConstantOnLhs ? rhs : lhs;
    ExpressionTree compileTimeConstant = compileTimeConstantOnLhs ? lhs : rhs;

    if (subject.isPresent() && !subjectMatches(subject.get(), testExpression, state)) {
      // Predicate not compatible with predicate of preceding if statement
      return Optional.empty();
    }

    // Don't support the use of Booleans as switch conditions
    if (isSubtype(getType(testExpression), Suppliers.JAVA_LANG_BOOLEAN_TYPE.get(state), state)) {
      return Optional.empty();
    }

    // Don't support the use of String as switch conditions
    if (isSubtype(getType(testExpression), state.getSymtab().stringType, state)) {
      return Optional.empty();
    }

    // Switching on primitive long requires later Java version (we don't currently support)
    if (state.getTypes().isSameType(getType(testExpression), state.getSymtab().longType)) {
      return Optional.empty();
    }

    boolean addDefault = hasElse && !hasElseIf;
    int previousCaseEndPosition =
        cases.isEmpty()
            ? ifTreeRange.lowerEndpoint()
            : cases.getLast().caseSourceCodeRange().upperEndpoint();
    cases.add(
        new CaseIr(
            /* hasCaseNull= */ compileTimeConstant.getKind() == Kind.NULL_LITERAL,
            /* hasDefault= */ false,
            /* instanceOfOptional= */ Optional.empty(),
            /* guardOptional= */ Optional.empty(),
            /* expressionsOptional= */ Optional.of(ImmutableList.of(compileTimeConstant)),
            /* arrowRhsOptional= */ arrowRhsOptional,
            /* caseSourceCodeRange= */ Range.openClosed(previousCaseEndPosition, caseEndPosition)));
    if (addDefault) {
      previousCaseEndPosition =
          cases.isEmpty()
              ? ifTreeRange.lowerEndpoint()
              : cases.getLast().caseSourceCodeRange().upperEndpoint();
      cases.add(
          new CaseIr(
              /* hasCaseNull= */ false,
              /* hasDefault= */ true,
              /* instanceOfOptional= */ Optional.empty(),
              /* guardOptional= */ Optional.empty(),
              /* expressionsOptional= */ Optional.empty(),
              /* arrowRhsOptional= */ elseOptional,
              /* caseSourceCodeRange= */ Range.openClosed(
                  previousCaseEndPosition,
                  elseOptional.isPresent()
                      ? getStartPosition(elseOptional.get())
                      : caseEndPosition)));
    }

    return Optional.of(testExpression);
  }

  private Optional<ExpressionTree> validateEnumPredicateForSubject(
      ExpressionTree lhs,
      ExpressionTree rhs,
      Optional<ExpressionTree> subject,
      VisitorState state,
      List<CaseIr> cases,
      Optional<StatementTree> elseOptional,
      Optional<StatementTree> arrowRhsOptional,
      Set<String> handledEnumValues,
      Range<Integer> ifTreeRange,
      int caseEndPosition,
      boolean hasElse,
      boolean hasElseIf) {
    boolean lhsIsEnumConstant = isEnumValue(lhs, state) && ASTHelpers.isEnumConstant(lhs);
    boolean rhsIsEnumConstant = isEnumValue(rhs, state) && ASTHelpers.isEnumConstant(rhs);

    if (lhsIsEnumConstant && rhsIsEnumConstant) {
      // Comparing enum const to enum const, cannot convert
      return Optional.empty();
    }

    if (!lhsIsEnumConstant && !rhsIsEnumConstant) {
      // Can't find an enum const, cannot convert
      return Optional.empty();
    }

    // Invariant: exactly one of lhs or rhs is an enum constant.
    ExpressionTree compileTimeConstant = lhsIsEnumConstant ? lhs : rhs;
    ExpressionTree testExpression = lhsIsEnumConstant ? rhs : lhs;

    if (subject.isPresent() && !subjectMatches(subject.get(), testExpression, state)) {
      return Optional.empty();
    }

    // Record this enum const as handled
    handledEnumValues.addAll(
        Stream.of(compileTimeConstant)
            .map(ASTHelpers::getSymbol)
            .filter(x -> x != null)
            .map(symbol -> symbol.getSimpleName().toString())
            .collect(toImmutableSet()));

    boolean addDefault = hasElse && !hasElseIf;
    int previousCaseEndPosition =
        cases.isEmpty()
            ? ifTreeRange.lowerEndpoint()
            : cases.getLast().caseSourceCodeRange().upperEndpoint();
    cases.add(
        new CaseIr(
            /* hasCaseNull= */ false,
            /* hasDefault= */ false,
            /* instanceOfOptional= */ Optional.empty(),
            /* guardOptional= */ Optional.empty(),
            /* expressionsOptional= */ Optional.of(ImmutableList.of(compileTimeConstant)),
            /* arrowRhsOptional= */ arrowRhsOptional,
            /* caseSourceCodeRange= */ Range.closedOpen(previousCaseEndPosition, caseEndPosition)));

    if (addDefault) {
      previousCaseEndPosition =
          cases.isEmpty()
              ? ifTreeRange.lowerEndpoint()
              : cases.getLast().caseSourceCodeRange().upperEndpoint();
      cases.add(
          new CaseIr(
              /* hasCaseNull= */ false,
              /* hasDefault= */ true,
              /* instanceOfOptional= */ Optional.empty(),
              /* guardOptional= */ Optional.empty(),
              /* expressionsOptional= */ Optional.empty(),
              /* arrowRhsOptional= */ elseOptional,
              /* caseSourceCodeRange= */ Range.closedOpen(
                  caseEndPosition,
                  elseOptional.isPresent()
                      ? state.getEndPosition(elseOptional.get())
                      : caseEndPosition)));
    }
    return Optional.of(testExpression);
  }

  /**
   * Determines whether the supplied expression and subject (if present) have the same Java source
   * code. Note that this is just a textual comparison, and does not consider code structure,
   * comments, etc.
   */
  private static boolean expressionSourceMatches(
      ExpressionTree subject, ExpressionTree expression, VisitorState state) {

    return state.getSourceForNode(subject).equals(state.getSourceForNode(expression));
  }

  /** Retrieves a list of all statements (if any) following the current path, if any. */
  private static ImmutableList<StatementTree> getSubsequentStatementsInBlock(VisitorState state) {
    TreePath path = state.getPath();
    if (!(path.getParentPath().getLeaf() instanceof BlockTree blockTree)) {
      return ImmutableList.of();
    }
    var statements = blockTree.getStatements();
    return ImmutableList.copyOf(
        statements.subList(statements.indexOf(path.getLeaf()) + 1, statements.size()));
  }

  /**
   * Unboxes the given type, if it is a reference type which can be unboxed. Returns {@code
   * Optional.empty()} if cannot be unboxed.
   */
  private static Optional<Type> unboxed(Tree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    if (type == null || !type.isReference()) {
      return Optional.empty();
    }
    Type unboxed = state.getTypes().unboxedType(type);
    if (unboxed == null
        || unboxed.getTag() == TypeTag.NONE
        // Don't match java.lang.Void.
        || unboxed.getTag() == TypeTag.VOID) {
      return Optional.empty();
    }
    return Optional.of(unboxed);
  }

  /**
   * Finds the intersection of two types, or {@code null} if there is no such intersection. This is
   * not quite the same thing as the "Intersection Types" defined JLS 21 § 4.9 (it is not a distinct
   * type; there is no {@code IntersectionTypeTree}) although they are similar in that the
   * (non-null) return value can be assigned to both types.
   */
  public static @Nullable Type intersectTypes(Type type1, Type type2, VisitorState state) {
    com.sun.tools.javac.code.Type lower = null;
    if (isSubtype(type1, type2, state)) {
      lower = type1;
    }

    if (isSubtype(type2, type1, state)) {
      lower = type2;
    }

    return lower;
  }

  /** Performs a detailed analysis of the if-chain, generating suggested fixes as needed. */
  private List<SuggestedFix> deepAnalysisOfIfChain(
      List<CaseIr> cases,
      IfChainAnalysisState finalIfChainAnalysisState,
      IfTree ifTree,
      VisitorState state,
      Range<Integer> ifTreeSourceRange) {

    // Wrapping break/yield in a switch can potentially change its semantics.  A deeper analysis of
    // whether semantics are preserved is not attempted here
    if (hasBreakOrYieldInTree(ifTree)) {
      return new ArrayList<>();
    }

    ExpressionTree subject = finalIfChainAnalysisState.subjectOptional().get();

    SuggestedFix.Builder suggestedFixBuilderWithPullupEnabled = SuggestedFix.builder();
    Optional<List<CaseIr>> casesWithPullupMaybeApplied =
        Optional.of(cases)
            .flatMap(
                caseList ->
                    maybePullUp(
                        caseList,
                        state,
                        finalIfChainAnalysisState,
                        Optional.of(suggestedFixBuilderWithPullupEnabled),
                        ifTreeSourceRange));

    int numberPulledUp =
        casesWithPullupMaybeApplied.isEmpty()
            ? 0
            : (casesWithPullupMaybeApplied.get().size() - cases.size());
    Type switchType = getType(finalIfChainAnalysisState.subjectOptional().get());

    Optional<List<CaseIr>> fixedCasesOptional =
        casesWithPullupMaybeApplied
            .flatMap(caseList -> maybeDetectDuplicateConstants(caseList))
            .flatMap(
                caseList ->
                    maybeFixDominance(caseList, state, subject, /* canReorderCases= */ !enableSafe))
            .flatMap(
                x ->
                    maybeFixDefaultNullAndUnconditional(
                        x,
                        subject,
                        ifTree,
                        state,
                        Optional.of(suggestedFixBuilderWithPullupEnabled),
                        numberPulledUp,
                        finalIfChainAnalysisState.handledEnumValues(),
                        switchType,
                        ifTreeSourceRange));

    SuggestedFix.Builder suggestedFixBuilderWithoutPullup = SuggestedFix.builder();
    boolean pullupDisabled = false;
    if (fixedCasesOptional.isEmpty()) {
      // Try again with pull-up disabled
      pullupDisabled = true;
      fixedCasesOptional =
          Optional.of(cases)
              .flatMap(caseList -> maybeDetectDuplicateConstants(caseList))
              .flatMap(
                  caseList ->
                      maybeFixDominance(
                          caseList, state, subject, /* canReorderCases= */ !enableSafe))
              .flatMap(
                  caseList ->
                      maybeFixDefaultNullAndUnconditional(
                          caseList,
                          subject,
                          ifTree,
                          state,
                          Optional.of(suggestedFixBuilderWithoutPullup),
                          /* numberPulledUp= */ 0,
                          finalIfChainAnalysisState.handledEnumValues(),
                          switchType,
                          ifTreeSourceRange))
              // Changing default/null can affect dominance
              .flatMap(
                  caseList ->
                      maybeFixDominance(
                          caseList, state, subject, /* canReorderCases= */ !enableSafe));
    }

    List<SuggestedFix> suggestedFixes = new ArrayList<>();
    maybeBuildAndAddSuggestedFix(
        fixedCasesOptional,
        pullupDisabled ? suggestedFixBuilderWithoutPullup : suggestedFixBuilderWithPullupEnabled,
        finalIfChainAnalysisState,
        ifTree,
        state,
        ifTreeSourceRange,
        suggestedFixes);
    return suggestedFixes;
  }

  /**
   * If a finding is available, build a {@code SuggestedFix} for it and add to the suggested fixes.
   */
  private static void maybeBuildAndAddSuggestedFix(
      Optional<List<CaseIr>> fixedCasesOptional,
      SuggestedFix.Builder suggestedFixBuilder,
      IfChainAnalysisState ifChainAnalysisState,
      IfTree ifTree,
      VisitorState state,
      Range<Integer> ifTreeSourceRange,
      List<SuggestedFix> suggestedFixes) {

    if (fixedCasesOptional.isPresent()) {
      List<CaseIr> fixedCases = fixedCasesOptional.get();
      ImmutableList<ErrorProneComment> allComments =
          state.getTokensForNode(ifTree).stream()
              .flatMap(errorProneToken -> errorProneToken.comments().stream())
              .collect(toImmutableList());

      suggestedFixBuilder.replace(
          ifTree,
          prettyPrint(
              fixedCases,
              ifChainAnalysisState.subjectOptional().get(),
              state,
              suggestedFixBuilder,
              ifTreeSourceRange,
              allComments));

      suggestedFixes.add(suggestedFixBuilder.build());
    }
  }

  /**
   * Compute whether the RHS is dominated by the LHS.
   *
   * <p>Domination refers to the notion of "is dominated" defined in e.g. JLS 21 § 14.11.1. Note
   * that this method does not support record types, which simplifies implementation.
   */
  public static boolean isDominatedBy(
      CaseIr lhs, CaseIr rhs, VisitorState state, ExpressionTree subject) {

    // Nothing dominates the default case
    if (rhs.hasDefault()) {
      return false;
    }

    // Only default dominates case null
    if (rhs.hasCaseNull()) {
      return lhs.hasDefault();
    }

    // Constants are dominated by patterns that can match them
    if (rhs.instanceOfOptional().isEmpty()) {
      for (ExpressionTree constantExpression : rhs.expressionsOptional().get()) {
        // A "constant expression" (e.g. JLS 21 § 15.29) can be a primitive type or a String.
        // However, constant expressions as used here refer to the usage as in e.g.
        // ConstantCaseLabelTree, which can include enum values.
        boolean isPrimitive = getType(constantExpression).isPrimitive();
        if (isPrimitive) {
          // Guarded patterns cannot dominate primitives
          if (lhs.guardOptional().isPresent()) {
            continue;
          }
          if (lhs.instanceOfOptional().isPresent()) {
            InstanceOfIr instanceOfIr = lhs.instanceOfOptional().get();
            if (instanceOfIr.type() != null) {
              Optional<Type> unboxedInstanceOfType = unboxed(instanceOfIr.type(), state);
              if (unboxedInstanceOfType.isPresent()) {
                if (isSubtype(getType(constantExpression), unboxedInstanceOfType.get(), state)) {
                  // RHS constant can be assigned to LHS unboxed instanceof's type
                  return true;
                }
              } else {
                // Cannot unbox LHS pattern, so RHS primitive constant should come before it
                return true;
              }
            } else if (instanceOfIr.patternVariable().isPresent()) {
              VariableTree patternVariable = instanceOfIr.patternVariable().get();
              Type patternType = getType(patternVariable);
              if (isSubtype(getType(constantExpression), patternType, state)) {
                // RHS constant can be assigned to LHS pattern
                return true;
              }
            }
            continue;
          } else {
            // LHS must be a constant; constants don't dominate other constants.
            continue;
          }
        }
        boolean isEnum = isEnumValue(constantExpression, state);
        if (isEnum) {
          if (lhs.guardOptional().isPresent()) {
            // Guarded patterns cannot dominate enum values
            continue;
          }

          if (lhs.instanceOfOptional().isPresent()) {
            InstanceOfIr instanceOfIr = lhs.instanceOfOptional().get();
            if (isSubtype(getType(constantExpression), getType(instanceOfIr.type()), state)) {
              // RHS enum value can be assigned to LHS instanceof's type
              return true;
            }
          } else {
            // LHS must be a constant
            continue;
          }
          continue;
        }

        // RHS must be a reference
        // The rhs-reference code would be needed to support e.g. String literals.  It is included
        // for completeness.
        if (lhs.guardOptional().isPresent()) {
          // Guarded patterns cannot dominate references
          continue;
        }

        if (lhs.instanceOfOptional().isPresent()) {
          InstanceOfIr instanceOfIr = lhs.instanceOfOptional().get();
          Type subjectType = getType(subject);
          if (instanceOfIr.type() != null) {
            var instanceOfType = instanceOfIr.type();
            if (isSubtype(
                intersectTypes(getType(constantExpression), subjectType, state),
                intersectTypes(getType(instanceOfType), subjectType, state),
                state)) {
              return true;
            }
          }
        } else {
          // LHS must be a constant
        }
      } // for loop
      return false;
    }

    // The RHS must be a pattern
    if (lhs.hasDefault() || lhs.hasCaseNull()) {
      // LHS has a default or case null, which dominates the RHS
      return true;
    }

    // RHS must be a pattern
    if (lhs.guardOptional().isPresent()) {
      // LHS has a guard, so cannot dominate RHS
      return false;
    }

    // Is LHS a pattern?
    if (lhs.instanceOfOptional().isPresent()) {
      Type lhsType =
          lhs.instanceOfOptional().get().type() != null
              ? getType(lhs.instanceOfOptional().get().type())
              : getType(lhs.instanceOfOptional().get().patternVariable().get().getType());
      var rhsInstanceOf = rhs.instanceOfOptional().get();
      Type rhsType =
          rhsInstanceOf.type() != null
              ? getType(rhsInstanceOf.type())
              : getType(rhsInstanceOf.patternVariable().get().getType());
      if (isSubtype(rhsType, lhsType, state)) {
        // The LHS type is a subtype of the RHS type, so the LHS dominates the RHS
        return true;
      }
    }

    // RHS is a pattern; LHS constant cannot dominate this pattern
    return false;
  }

  /**
   * This record is an intermediate representation of a single `x instanceof Y` or `x instanceof Y
   * y` expression.
   */
  record InstanceOfIr(
      // In the example above, the expression would be `y`.
      Optional<ExpressionTree> expression,
      // In the example above, the variable tree would be `Y y`.
      Optional<VariableTree> patternVariable,
      // In the example above, the type would be `Y`.
      Tree type) {

    InstanceOfIr {
      checkArgument(type != null);
    }
  }

  /**
   * This record is an intermediate representation of a single case in a switch statement that is
   * being synthesized. Its scope is roughly equivalent to a `CaseTree` in Java's AST, although does
   * not cover all of the same functionality.
   */
  record CaseIr(
      boolean hasCaseNull,
      boolean hasDefault,
      // The pattern, if any
      Optional<InstanceOfIr> instanceOfOptional,
      // The guard predicate, if any
      Optional<ExpressionTree> guardOptional,
      // Constants appearing before the arrow in the case
      Optional<List<ExpressionTree>> expressionsOptional,
      // Code appearing after the arrow in the case
      Optional<StatementTree> arrowRhsOptional,
      // Range of source code covered by the case
      Range<Integer> caseSourceCodeRange) {

    CaseIr {
      checkArgument(
          hasCaseNull
              || hasDefault
              || instanceOfOptional.isPresent()
              || (expressionsOptional.isPresent() && !expressionsOptional.get().isEmpty()),
          "CaseIr must have at least one of case null, default, instanceof, or expressions");
      checkArgument(
          !(hasDefault && (instanceOfOptional.isPresent() || expressionsOptional.isPresent())),
          "Default and instanceof/expressions cannot both be present");
    }
  }

  /** This record represents the current state of the analysis of an if-chain. */
  record IfChainAnalysisState(
      // The expression to be switched on (if known)
      Optional<ExpressionTree> subjectOptional,
      // Depth of the if statement being analyzed (relative to the start of the if-chain)
      int depth,
      // Whether the if-chain is eligible for conversion (so far)
      Validity validity,
      // Current if statement being analyzed in the if-chain
      IfTree at,
      // Do all RHS blocks complete abruptly (so far)?
      boolean allConditionalBlocksReturnOrThrow,
      // All enum values seen so far (in condition expressions)
      ImmutableSet<String> handledEnumValues) {}
}
