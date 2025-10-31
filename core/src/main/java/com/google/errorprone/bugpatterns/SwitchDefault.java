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
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSwitchDefault;
import static com.google.errorprone.util.ASTHelpers.isSwitchDefault;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.Reachability;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.SwitchTree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "The default case of a switch should appear at the end of the last statement group",
    tags = BugPattern.StandardTags.STYLE,
    severity = SUGGESTION)
public class SwitchDefault extends BugChecker implements SwitchTreeMatcher {

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    Optional<? extends CaseTree> maybeDefault = getSwitchDefault(tree);
    if (!maybeDefault.isPresent()) {
      return NO_MATCH;
    }
    // Collect all case trees in the statement group containing the default
    List<CaseTree> defaultStatementGroup = new ArrayList<>();
    Iterator<? extends CaseTree> it = tree.getCases().iterator();
    while (it.hasNext()) {
      CaseTree caseTree = it.next();
      defaultStatementGroup.add(caseTree);
      if (isSwitchDefault(caseTree)) {
        while (it.hasNext() && isNullOrEmpty(caseTree.getStatements())) {
          caseTree = it.next();
          defaultStatementGroup.add(caseTree);
        }
        break;
      }
      if (!isNullOrEmpty(caseTree.getStatements())) {
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
      if (!Reachability.canFallThrough(getLast(defaultStatementGroup))) {
        int start = getStartPosition(defaultStatementGroup.get(0));
        int end = state.getEndPosition(getLast(defaultStatementGroup));
        String replacement;
        String source = state.getSourceCode().toString();

        // If the default case isn't the last case in its statement group, move it to the end.
        if (idx != defaultStatementGroup.size() - 1) {
          int caseEnd = getStartPosition(getLast(defaultStatementGroup).getStatements().get(0));
          int cutStart = getStartPosition(defaultTree);
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
        if (isSwitchDefault(last) || Reachability.canFallThrough(last)) {
          replacement = "break;\n" + replacement;
        }
        fix.replace(start, end, "").postfixWith(getLast(tree.getCases()), replacement);
      }
    } else if (idx != defaultStatementGroup.size() - 1) {
      // If the default case isn't the last case in its statement group, move it to the end.
      fix.delete(defaultTree);
      CaseTree lastCase = getLast(defaultStatementGroup);
      if (!isNullOrEmpty(lastCase.getStatements())) {
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

  private static <T> boolean isNullOrEmpty(@Nullable List<T> elementList) {
    return elementList == null || elementList.isEmpty();
  }
}
