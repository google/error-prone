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
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSwitchDefault;
import static com.sun.source.tree.Tree.Kind.RETURN;
import static com.sun.source.tree.Tree.Kind.THROW;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.CompileTimeConstantExpressionMatcher;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneComment;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.Pretty;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import org.jspecify.annotations.Nullable;

/** Utility methods for refactoring switches. */
public final class SwitchUtils {
  static final Matcher<ExpressionTree> COMPILE_TIME_CONSTANT_MATCHER =
      CompileTimeConstantExpressionMatcher.instance();
  static final ImmutableSet<Kind> KINDS_RETURN_OR_THROW = ImmutableSet.of(THROW, RETURN);

  static final String EQUALS_STRING = "=";
  static final String REMOVE_DEFAULT_CASE_SHORT_DESCRIPTION =
      "Remove default case because all enum values handled";

  /**
   * Tri-state of whether the if-chain is valid, invalid, or possibly valid for conversion to a
   * switch.
   */
  enum Validity {
    MAYBE_VALID,
    INVALID,
    VALID
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

  /**
   * Renders the Java source code for a [compound] assignment operator. The parameter must be either
   * an {@code AssignmentTree} or a {@code CompoundAssignmentTree}.
   */
  static String renderJavaSourceOfAssignment(ExpressionTree tree) {
    // Simple assignment tree?
    if (tree instanceof JCAssign) {
      return EQUALS_STRING;
    }

    // Invariant: must be a compound assignment tree
    JCAssignOp jcAssignOp = (JCAssignOp) tree;
    Pretty pretty = new Pretty(new StringWriter(), /* sourceOutput= */ true);
    return pretty.operatorName(jcAssignOp.getTag().noAssignOp()) + EQUALS_STRING;
  }

  /** Render the supplied comments, separated by newlines. */
  static String renderComments(ImmutableList<ErrorProneComment> comments) {
    return comments.stream()
        .map(ErrorProneComment::getText)
        .filter(commentText -> !commentText.isEmpty())
        .collect(joining("\n"));
  }

  /** Retrieves a list of all statements (if any) preceding the current path, if any. */
  static ImmutableList<StatementTree> getPrecedingStatementsInBlock(VisitorState state) {
    TreePath path = state.getPath();
    if (!(path.getParentPath().getLeaf() instanceof BlockTree blockTree)) {
      return ImmutableList.of();
    }
    var statements = blockTree.getStatements();
    return ImmutableList.copyOf(statements.subList(0, statements.indexOf(path.getLeaf())));
  }

  static Optional<VariableTree> findCombinableVariableTree(
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
        && !(COMPILE_TIME_CONSTANT_MATCHER.matches(variableTree.getInitializer(), state)
            // Safe to elide dead store of an enum value
            || isEnumValue(variableTree.getInitializer(), state))) {
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

  static boolean isEnumValue(ExpressionTree expression, VisitorState state) {
    Type type = ASTHelpers.getType(expression);
    if (type == null) {
      return false;
    }
    return type.asElement().getKind() == ElementKind.ENUM;
  }

  /**
   * Determines whether local variable {@code symbol} has no reads within the scope of the {@code
   * VisitorState}. (Writes to the variable are ignored.)
   */
  static boolean noReadsOfVariable(VarSymbol symbol, VisitorState state) {
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

      void handle(Tree tree) {
        var symbol = getSymbol(tree);
        if (symbol instanceof VarSymbol varSymbol) {
          referencedLocalVariables.add(varSymbol);
        }
      }
    }.scan(state.getPath(), null);

    return !referencedLocalVariables.contains(symbol);
  }

  /**
   * Determines whether the last two preceding statements are not variable declarations within the
   * same VariableDeclaratorList, for example {@code int x, y;}. VariableDeclaratorLists are defined
   * in e.g. JLS 21 § 14.4. Precondition: all preceding statements are taken from the same {@code
   * BlockTree}.
   */
  static boolean precedingTwoStatementsNotInSameVariableDeclaratorList(
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
   * Determines whether a variable definition is compatible with an assignment target (e.g. of a
   * switch statement). Compatibility means that the assignment is being made to to the same
   * variable that is being defined.
   */
  static boolean isVariableCompatibleWithAssignment(
      ExpressionTree assignmentTarget, VariableTree variableDefinition) {
    Symbol assignmentTargetSymbol = getSymbol(assignmentTarget);
    Symbol definedSymbol = ASTHelpers.getSymbol(variableDefinition);

    return Objects.equals(assignmentTargetSymbol, definedSymbol);
  }

  /**
   * Returns the statements on the RHS of a {@link CaseTree}, if any exist, otherwise an empty list.
   * If the only statement is a block statement, return the block's inner statements instead,
   * unwrapping multiple blocks if needed.
   */
  static ImmutableList<StatementTree> getStatements(CaseTree caseTree) {
    Tree at = caseTree.getBody();

    while (at instanceof BlockTree bt) {
      if (bt.getStatements().size() == 1) {
        at = bt.getStatements().get(0);
      } else {
        break;
      }
    }

    return switch (at) {
      case BlockTree blockTree -> ImmutableList.copyOf(blockTree.getStatements());
      case StatementTree statementTree -> ImmutableList.of(statementTree);
      case null, default -> ImmutableList.of();
    };
  }

  /** Returns whether the switch statement covers all possible values of the enum. */
  static boolean isSwitchCoveringAllEnumValues(Set<String> handledEnumValues, Type switchType) {
    // Handles switching on enum only (map is bijective)
    if (switchType.asElement().getKind() != ElementKind.ENUM) {
      // Give up on search
      return false;
    }
    return handledEnumValues.containsAll(ASTHelpers.enumValues(switchType.asElement()));
  }

  private static Tree skipLabel(JCTree tree) {
    return tree.hasTag(JCTree.Tag.LABELLED) ? ((JCTree.JCLabeledStatement) tree).body : tree;
  }

  /**
   * Determines whether any `continue` statements that jump out of the {@code tree} are present
   * within the {@code tree}.
   */
  static boolean hasContinueOutOfTree(Tree tree, VisitorState state) {
    Boolean result =
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean visitContinue(ContinueTree continueTree, Void unused) {
            Tree continueTarget =
                skipLabel(requireNonNull(((JCTree.JCContinue) continueTree).target));
            // If the continue transfers control to somewhere above the switch, it is jumping out
            var pathIterator = state.getPath().iterator();
            for (Tree at = tree; pathIterator.hasNext(); at = pathIterator.next()) {
              if (at == continueTarget) {
                return true;
              }
            }
            return false;
          }

          @Override
          public Boolean reduce(@Nullable Boolean left, @Nullable Boolean right) {
            return Objects.equals(left, true) || Objects.equals(right, true);
          }
        }.scan(tree, null);

    return result != null && result;
  }

  /**
   * In a switch with multiple assignments, determine whether a subsequent assignment target is
   * compatible with the first assignment target.
   */
  static boolean isCompatibleWithFirstAssignment(
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
   * Renders all comments of the supplied {@code variableTree} into a list of Strings, in code
   * order.
   */
  static ImmutableList<String> renderVariableTreeComments(
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
  static ImmutableList<String> renderVariableTreeAnnotations(
      VariableTree variableTree, VisitorState state) {
    return variableTree.getModifiers().getAnnotations().stream()
        .map(state::getSourceForNode)
        .collect(toImmutableList());
  }

  /**
   * Renders the flags of the supplied variable declaration, such as "final", into a single
   * space-separated String.
   */
  static String renderVariableTreeFlags(VariableTree variableTree) {
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
   * Prints source for all expressions in a given {@code case}, separated by commas, or the pattern
   * and guard (if present).
   */
  static String printCaseExpressionsOrPatternAndGuard(CaseTree caseTree, VisitorState state) {
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

  static boolean hasCasePattern(CaseTree caseTree) {
    return caseTree.getLabels().stream()
        .anyMatch(caseLabelTree -> caseLabelTree instanceof PatternCaseLabelTree);
  }

  /**
   * Determines whether the supplied {@code caseTree} case contains `case null` and/or `default`.
   */
  static NullDefaultKind analyzeCaseForNullAndDefault(CaseTree caseTree) {
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
   * Renders the Java source prefix needed for the supplied {@code nullDefaultKind}, incorporating
   * whether the `default` case should be removed.
   */
  static String renderNullDefaultKindPrefix(
      NullDefaultKind nullDefaultKind, boolean removeDefault) {

    return switch (nullDefaultKind) {
      case KIND_NULL_AND_DEFAULT -> removeDefault ? "case null" : "case null, default";
      case KIND_NULL -> "case null";
      case KIND_DEFAULT -> removeDefault ? "" : "default";
      case KIND_NEITHER -> "case ";
    };
  }

  private SwitchUtils() {}
}
