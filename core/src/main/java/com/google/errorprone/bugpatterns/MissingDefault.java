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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneTokens;
import com.google.errorprone.util.Reachability;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.SwitchTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import java.util.Optional;
import javax.lang.model.element.ElementKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "MissingDefault",
  category = JDK,
  summary =
      "The Google Java Style Guide requires that each switch statement includes a default statement"
          + " group, even if it contains no code. (This requirement is lifted for any switch"
          + " statement that covers all values of an enum.)",
  severity = WARNING,
  tags = StandardTags.STYLE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class MissingDefault extends BugChecker implements SwitchTreeMatcher {
  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    Type switchType = ASTHelpers.getType(tree.getExpression());
    if (switchType.asElement().getKind() == ElementKind.ENUM) {
      // enum switches can omit the default if they're exhaustive, which is enforced separately
      // by MissingCasesInEnumSwitch
      return NO_MATCH;
    }
    Optional<? extends CaseTree> maybeDefault =
        tree.getCases().stream().filter(c -> c.getExpression() == null).findFirst();
    if (!maybeDefault.isPresent()) {
      Description.Builder description = buildDescription(tree);
      if (!tree.getCases().isEmpty()) {
        // Inserting the default after the last case is easier than finding the closing brace
        // for the switch statement. Hopefully we don't often see switches with zero cases.
        CaseTree lastCase = getLast(tree.getCases());
        String replacement;
        if (lastCase.getStatements().isEmpty()
            || Reachability.canCompleteNormally(Iterables.getLast(lastCase.getStatements()))) {
          replacement = "\nbreak;\ndefault: // fall out\n";
        } else {
          replacement = "\ndefault: // fall out\n";
        }
        description.addFix(SuggestedFix.postfixWith(getLast(tree.getCases()), replacement));
      }
      return description.build();
    }
    CaseTree defaultCase = maybeDefault.get();
    if (!defaultCase.getStatements().isEmpty()) {
      return NO_MATCH;
    }
    int idx = tree.getCases().indexOf(defaultCase);
    // The default case may appear before a non-default case, in which case the documentation
    // should say "fall through" instead of "fall out".
    boolean isLast = idx == tree.getCases().size() - 1;
    int end =
        isLast
            ? state.getEndPosition(tree)
            : ((JCTree) tree.getCases().get(idx + 1)).getStartPosition();
    if (ErrorProneTokens.getTokens(
            state.getSourceCode().subSequence(state.getEndPosition(defaultCase), end).toString(),
            state.context)
        .stream()
        .anyMatch(t -> !t.comments().isEmpty())) {
      return NO_MATCH;
    }
    return buildDescription(defaultCase)
        .setMessage("Default case should be documented with a comment")
        .addFix(SuggestedFix.postfixWith(defaultCase, isLast ? " // fall out" : " // fall through"))
        .build();
  }
}
