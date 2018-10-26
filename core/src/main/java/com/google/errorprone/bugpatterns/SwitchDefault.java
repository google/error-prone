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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.Reachability;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.SwitchTree;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "SwitchDefault",
    summary = "The default case of a switch should appear at the end of the last statement group",
    severity = SUGGESTION,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class SwitchDefault extends BugChecker implements SwitchTreeMatcher {

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    Optional<? extends CaseTree> maybeDefault =
        tree.getCases().stream().filter(c -> c.getExpression() == null).findAny();
    if (!maybeDefault.isPresent()) {
      return NO_MATCH;
    }
    // Collect all case trees in the statement group containing the default
    List<CaseTree> defaultStatementGroup = new ArrayList<>();
    Iterator<? extends CaseTree> it = tree.getCases().iterator();
    while (it.hasNext()) {
      CaseTree caseTree = it.next();
      defaultStatementGroup.add(caseTree);
      if (caseTree.getExpression() == null) {
        while (it.hasNext() && caseTree.getStatements().isEmpty()) {
          caseTree = it.next();
          defaultStatementGroup.add(caseTree);
        }
        break;
      }
      if (!caseTree.getStatements().isEmpty()) {
        defaultStatementGroup.clear();
      }
    }
    // Find the position of the default case within the statement group
    int idx = defaultStatementGroup.indexOf(maybeDefault.get());
    SuggestedFix.Builder fix = SuggestedFix.builder();
    CaseTree defaultTree = defaultStatementGroup.get(idx);
    if (it.hasNext()) {
      // If there are trailing cases after the default statement group, move the default to the end.
      // Only emit a fix if the default doesn't fall through.
      if (!Reachability.canCompleteNormally(getLast(defaultStatementGroup))) {
        int start = ((JCTree) defaultStatementGroup.get(0)).getStartPosition();
        int end = state.getEndPosition(getLast(defaultStatementGroup));
        String replacement;
        String source = state.getSourceCode().toString();

        // If the default case isn't the last case in its statement group, move it to the end.
        if (idx != defaultStatementGroup.size() - 1) {
          int caseEnd =
              ((JCTree) getLast(defaultStatementGroup).getStatements().get(0)).getStartPosition();
          int cutStart = ((JCTree) defaultTree).getStartPosition();
          int cutEnd = state.getEndPosition(defaultTree);
          replacement =
              source.substring(start, cutStart)
                  + source.substring(cutEnd, caseEnd)
                  + "\n"
                  + source.substring(cutStart, cutEnd)
                  + source.substring(caseEnd, end);
        } else {
          replacement = source.substring(start, end);
        }
        // If the last statement group falls out of the switch, add a `break;` before moving
        // the default to the end.
        CaseTree last = getLast(tree.getCases());
        if (last.getExpression() == null || Reachability.canCompleteNormally(last)) {
          replacement = "break;\n" + replacement;
        }
        fix.replace(start, end, "").postfixWith(getLast(tree.getCases()), replacement);
      }
    } else if (idx != defaultStatementGroup.size() - 1) {
      // If the default case isn't the last case in its statement group, move it to the end.
      fix.delete(defaultTree);
      CaseTree lastCase = getLast(defaultStatementGroup);
      if (!lastCase.getStatements().isEmpty()) {
        fix.prefixWith(lastCase.getStatements().get(0), state.getSourceForNode(defaultTree));
      } else {
        fix.postfixWith(lastCase, state.getSourceForNode(defaultTree));
      }
    } else {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(defaultStatementGroup.get(0));
    if (!fix.isEmpty()) {
      description.addFix(fix.build());
    }
    return description.build();
  }
}
