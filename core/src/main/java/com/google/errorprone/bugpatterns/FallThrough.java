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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.Reachability;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.SwitchTree;
import com.sun.tools.javac.tree.JCTree;
import java.util.regex.Pattern;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "FallThrough",
  altNames = "fallthrough",
  category = JDK,
  summary = "Switch case may fall through",
  severity = ERROR
)
public class FallThrough extends BugChecker implements SwitchTreeMatcher {

  private static final Pattern FALL_THROUGH_PATTERN =
      Pattern.compile("\\bfalls?.?through\\b", Pattern.CASE_INSENSITIVE);

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    PeekingIterator<JCTree.JCCase> it =
        Iterators.peekingIterator(((JCTree.JCSwitch) tree).cases.iterator());
    while (it.hasNext()) {
      JCTree.JCCase caseTree = it.next();
      if (!it.hasNext()) {
        break;
      }
      JCTree.JCCase next = it.peek();
      if (caseTree.stats.isEmpty()) {
        continue;
      }
      // We only care whether the last statement completes; javac would have already
      // reported an error if that statement wasn't reachable, and the answer is
      // independent of any preceding statements.
      boolean completes = Reachability.canCompleteNormally(getLast(caseTree.stats));
      int caseEndPosition = caseEndPosition(state, caseTree);
      String comments =
          state
              .getSourceCode()
              .subSequence(caseEndPosition, next.getStartPosition())
              .toString()
              .trim();
      if (completes && !FALL_THROUGH_PATTERN.matcher(comments).find()) {
        state.reportMatch(describeMatch(next, SuggestedFix.prefixWith(next, "// fall through\n")));
      } else if (!completes && FALL_THROUGH_PATTERN.matcher(comments).find()) {
        SuggestedFix deleteFallThroughComment =
            SuggestedFix.replace(caseEndPosition, next.getStartPosition(), "\n");
        state.reportMatch(
            buildDescription(next)
                .addFix(deleteFallThroughComment)
                .setMessage("Switch case has 'fall through' comment, but does not fall through")
                .build());
      }
    }
    return NO_MATCH;
  }

  private static int caseEndPosition(VisitorState state, JCTree.JCCase caseTree) {
    // if the statement group is a single block statement, handle fall through comments at the
    // end of the block
    if (caseTree.stats.size() == 1) {
      JCTree.JCStatement only = getOnlyElement(caseTree.stats);
      if (only.hasTag(JCTree.Tag.BLOCK)) {
        return state.getEndPosition(getLast(((BlockTree) only).getStatements()));
      }
    }
    return state.getEndPosition(caseTree);
  }
}
