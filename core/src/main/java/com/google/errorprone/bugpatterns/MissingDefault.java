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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSwitchDefault;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Reachability;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.ElementKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "The Google Java Style Guide requires that each switch statement includes a default"
            + " statement group, even if it contains no code. (This requirement is lifted for any"
            + " switch statement that covers all values of an enum.)",
    severity = WARNING)
public class MissingDefault extends BugChecker implements SwitchTreeMatcher {
  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    Type switchType = ASTHelpers.getType(tree.getExpression());
    if (switchType.asElement().getKind() == ElementKind.ENUM) {
      // enum switches can omit the default if they're exhaustive, which is enforced separately
      // by MissingCasesInEnumSwitch
      return NO_MATCH;
    }
    Optional<? extends CaseTree> maybeDefault = getSwitchDefault(tree);
    if (!maybeDefault.isPresent()) {
      if (isExhaustive(tree)) {
        return NO_MATCH;
      }
      Description.Builder description = buildDescription(tree);
      if (!tree.getCases().isEmpty()) {
        // Inserting the default after the last case is easier than finding the closing brace
        // for the switch statement. Hopefully we don't often see switches with zero cases.
        CaseTree lastCase = getLast(tree.getCases());
        String replacement;
        List<? extends StatementTree> statements = lastCase.getStatements();
        if (tree.getCases().stream()
            .noneMatch(c -> c.getCaseKind() == CaseTree.CaseKind.STATEMENT)) {
          replacement = "\ndefault -> {}\n";
        } else if (statements == null
            || statements.isEmpty()
            || Reachability.canCompleteNormally(Iterables.getLast(statements))) {
          replacement = "\nbreak;\ndefault: // fall out\n";
        } else {
          replacement = "\ndefault: // fall out\n";
        }
        description.addFix(SuggestedFix.postfixWith(getLast(tree.getCases()), replacement));
      }
      return description.build();
    }
    CaseTree defaultCase = maybeDefault.get();
    List<? extends StatementTree> statements = defaultCase.getStatements();
    if (statements == null || !statements.isEmpty()) {
      return NO_MATCH;
    }
    // If `default` case is empty, and last in switch, add `// fall out` comment
    // TODO(epmjohnston): Maybe move comment logic to https://errorprone.info/bugpattern/FallThrough
    int idx = tree.getCases().indexOf(defaultCase);
    if (idx != tree.getCases().size() - 1) {
      return NO_MATCH;
    }
    if (state
        .getOffsetTokens(state.getEndPosition(defaultCase), state.getEndPosition(tree))
        .stream()
        .anyMatch(t -> !t.comments().isEmpty())) {
      return NO_MATCH;
    }
    return buildDescription(defaultCase)
        .setMessage("Default case should be documented with a comment")
        .addFix(SuggestedFix.postfixWith(defaultCase, " // fall out"))
        .build();
  }

  private static final Field IS_EXHAUSTIVE = getIsExhaustive();

  private static Field getIsExhaustive() {
    try {
      return JCTree.JCSwitch.class.getField("isExhaustive");
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  private static boolean isExhaustive(SwitchTree tree) {
    try {
      return IS_EXHAUSTIVE != null && IS_EXHAUSTIVE.getBoolean(tree);
    } catch (IllegalAccessException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }
}
