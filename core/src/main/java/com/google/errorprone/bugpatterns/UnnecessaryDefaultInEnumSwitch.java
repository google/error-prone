/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
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
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ElementKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "UnnecessaryDefaultInEnumSwitch",
  summary =
      "Switch handles all enum values; an explicit default case is unnecessary and defeats error"
          + " checking for non-exhaustive switches.",
  category = JDK,
  severity = WARNING
)
public class UnnecessaryDefaultInEnumSwitch extends BugChecker implements SwitchTreeMatcher {

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    TypeSymbol switchType = ((JCSwitch) tree).getExpression().type.tsym;
    if (switchType.getKind() != ElementKind.ENUM) {
      return NO_MATCH;
    }
    Optional<? extends CaseTree> maybeDefaultCase =
        tree.getCases().stream().filter(c -> c.getExpression() == null).findFirst();
    if (!maybeDefaultCase.isPresent()) {
      return NO_MATCH;
    }
    CaseTree defaultCase = maybeDefaultCase.get();
    Set<String> handledCases =
        tree.getCases()
            .stream()
            .map(CaseTree::getExpression)
            .filter(IdentifierTree.class::isInstance)
            .map(p -> ((IdentifierTree) p).getName().toString())
            .collect(toImmutableSet());
    if (!ASTHelpers.enumValues(switchType).equals(handledCases)) {
      return NO_MATCH;
    }
    Fix fix;
    List<? extends StatementTree> defaultStatements = defaultCase.getStatements();
    if (trivialDefault(defaultStatements)) {
      // deleting `default:` or `default: break;` is a no-op
      fix = SuggestedFix.delete(defaultCase);
    } else if (!canCompleteNormally(tree)) {
      // if the switch statement cannot complete normally, then deleting the default
      // and moving its statements to after the switch statement is a no-op
      String defaultSource =
          state
              .getSourceCode()
              .subSequence(
                  ((JCTree) defaultStatements.get(0)).getStartPosition(),
                  state.getEndPosition(getLast(defaultStatements)))
              .toString();
      String initialComments = comments(state, defaultCase, defaultStatements);
      fix =
          SuggestedFix.builder()
              .delete(defaultCase)
              .postfixWith(tree, initialComments + defaultSource)
              .build();
    } else {
      return NO_MATCH;
    }
    return describeMatch(defaultCase, fix);
  }

  /** Returns true if the default is empty, or contains only a break statement. */
  private boolean trivialDefault(List<? extends StatementTree> defaultStatements) {
    if (defaultStatements.isEmpty()) {
      return true;
    }
    if (defaultStatements.size() == 1
        && getOnlyElement(defaultStatements).getKind() == Tree.Kind.BREAK) {
      return true;
    }
    return false;
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
