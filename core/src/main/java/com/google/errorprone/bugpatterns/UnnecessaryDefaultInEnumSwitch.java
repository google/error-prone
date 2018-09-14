/*
 * Copyright 2017 The Error Prone Authors.
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
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.Reachability.canCompleteNormally;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ElementKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "UnnecessaryDefaultInEnumSwitch",
    summary =
        "Switch handles all enum values: an explicit default case is unnecessary and defeats error"
            + " checking for non-exhaustive switches.",
    category = JDK,
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class UnnecessaryDefaultInEnumSwitch extends BugChecker implements SwitchTreeMatcher {

  private static final String DESCRIPTION_MOVED_DEFAULT =
      "Switch handles all enum values: move code from the default case to execute after the "
          + "switch statement to enable checking for non-exhaustive switches. "
          + "That is, prefer: `switch (...) { ... } throw new AssertionError();` to "
          + "`switch (...) { ... default: throw new AssertionError(); }`";

  private static final String DESCRIPTION_REMOVED_DEFAULT =
      "Switch handles all enum values: the default case can be omitted to enable enforcement "
          + "at compile-time that the switch statement is exhaustive.";

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {

    TypeSymbol switchType = ((JCSwitch) tree).getExpression().type.tsym;
    if (switchType.getKind() != ElementKind.ENUM) {
      return NO_MATCH;
    }
    CaseTree caseBeforeDefault = null;
    CaseTree defaultCase = null;
    for (CaseTree caseTree : tree.getCases()) {
      if (caseTree.getExpression() == null) {
        defaultCase = caseTree;
        break;
      } else {
        caseBeforeDefault = caseTree;
      }
    }
    if (caseBeforeDefault == null || defaultCase == null) {
      return NO_MATCH;
    }
    Set<String> handledCases =
        tree.getCases().stream()
            .map(CaseTree::getExpression)
            .filter(IdentifierTree.class::isInstance)
            .map(p -> ((IdentifierTree) p).getName().toString())
            .collect(toImmutableSet());
    boolean unrecognized = false;
    SetView<String> setDifference =
        Sets.difference(ASTHelpers.enumValues(switchType), handledCases);
    if (!setDifference.isEmpty()) {
      if (setDifference.contains("UNRECOGNIZED") && setDifference.size() == 1) {
        // if a switch handles all values of an proto-generated enum except for 'UNRECOGNIZED',
        // we explicitly handle 'UNRECOGNIZED' and remove the default.
        unrecognized = true;
      } else {
        return NO_MATCH;
      }
    }
    List<? extends StatementTree> defaultStatements = defaultCase.getStatements();
    if (trivialDefault(defaultStatements)) {
      // deleting `default:` or `default: break;` is a no-op
      SuggestedFix fix =
          SuggestedFix.replace(
              defaultCase, unrecognized ? "case UNRECOGNIZED: \n // continue below" : "");
      return buildDescription(defaultCase)
          .setMessage(DESCRIPTION_REMOVED_DEFAULT)
          .addFix(fix)
          .build();
    }
    String defaultSource =
        state
            .getSourceCode()
            .subSequence(
                ((JCTree) defaultStatements.get(0)).getStartPosition(),
                state.getEndPosition(getLast(defaultStatements)))
            .toString();
    String initialComments = comments(state, defaultCase, defaultStatements);
    if (!canCompleteNormally(tree)) {
      // if the switch statement cannot complete normally, then deleting the default
      // and moving its statements to after the switch statement is a no-op
      SuggestedFix fix =
          SuggestedFix.builder()
              .replace(defaultCase, unrecognized ? "case UNRECOGNIZED: \n break;" : "")
              .postfixWith(tree, initialComments + defaultSource)
              .build();
      return buildDescription(defaultCase)
          .setMessage(DESCRIPTION_MOVED_DEFAULT)
          .addFix(fix)
          .build();
    }
    // The switch is already exhaustive, we want to delete the default.
    // There are a few modes we need to handle:
    // 1) switch (..) {
    //      case FOO:
    //      default: doWork();
    //    }
    //    In this mode, we need to lift the statements from 'default' into FOO, otherwise
    //    we change the code.  This mode also captures any variation of statements in FOO
    //    where any of them would fall-through (e.g, if (!bar)  { break; } ) -- if bar is
    //    true then we'd fall through.
    //
    //  2) switch (..) {
    //       case FOO: break;
    //       default: doDefault();
    //     }
    //     In this mode, we can safely delete 'default'.
    //
    //  3) var x;
    //     switch (..) {
    //       case FOO: x = 1; break;
    //       default: x = 2;
    //     }
    //     doWork(x);
    //     In this mode, we can't delete 'default' because javac analysis requires that 'x'
    //     must be set before using it.

    //  To solve this, we take the approach of:
    //  Try deleting the code entirely.  If it fails to compile, we've broken (3) -> no match.
    //  Try lifting the code to the prior case statement.  If it fails to compile, we had (2)
    //  and the code is unreachable -- so use (2) as the strategy.  Otherwise, use (1).
    if (!SuggestedFixes.compilesWithFix(SuggestedFix.delete(defaultCase), state)) {
      return NO_MATCH; // case (3)
    }
    if (!canCompleteNormally(caseBeforeDefault)) {
      // case (2) -- If the case before the default can't complete normally,
      // it's OK to to delete the default.
      return buildDescription(defaultCase)
          .setMessage(DESCRIPTION_REMOVED_DEFAULT)
          .addFix(
              unrecognized
                  ? SuggestedFix.builder()
                      .replace(defaultCase, "case UNRECOGNIZED:" + initialComments + defaultSource)
                      .build()
                  : SuggestedFix.delete(defaultCase))
          .build();
    }
    // case (1) -- If it can complete, we need to merge the default into it.
    SuggestedFix.Builder fix = SuggestedFix.builder().delete(defaultCase);
    if (unrecognized) {
      fix.prefixWith(defaultCase, "case UNRECOGNIZED:")
          .postfixWith(defaultCase, initialComments + defaultSource);
    } else {
      fix.postfixWith(caseBeforeDefault, initialComments + defaultSource);
    }
    return buildDescription(defaultCase)
        .setMessage(DESCRIPTION_REMOVED_DEFAULT)
        .addFix(fix.build())
        .build();
  }

  /** Returns true if the default is empty, or contains only a break statement. */
  private boolean trivialDefault(List<? extends StatementTree> defaultStatements) {
    if (defaultStatements.isEmpty()) {
      return true;
    }
    return (defaultStatements.size() == 1
        && getOnlyElement(defaultStatements).getKind() == Tree.Kind.BREAK);
  }

  /** Returns the comments between the "default:" case and the first statement within it, if any. */
  private String comments(
      VisitorState state, CaseTree defaultCase, List<? extends StatementTree> defaultStatements) {
    // If there are no statements, then there can be no comments that we strip,
    // because comments are attached to the statements, not the "default:" case.
    if (defaultStatements.isEmpty()) {
      return "";
    }
    // To extract the comments, we have to get the source code from
    // "default:" to the first statement, and then strip off "default:", because
    // we have no way of identifying the end position of just the "default:" statement.
    int defaultStart = ((JCTree) defaultCase).getStartPosition();
    int statementStart = ((JCTree) defaultStatements.get(0)).getStartPosition();
    String defaultAndComments =
        state.getSourceCode().subSequence(defaultStart, statementStart).toString();
    String comments =
        defaultAndComments
            .substring(defaultAndComments.indexOf("default:") + "default:".length())
            .trim();
    if (!comments.isEmpty()) {
      comments = "\n" + comments + "\n";
    }
    return comments;
  }
}
