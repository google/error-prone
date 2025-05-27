/*
 * Copyright 2014 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.Switches.isDefaultCaseForSkew;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Switches on enum types should either handle all values, or have a default case.",
    severity = WARNING)
public class MissingCasesInEnumSwitch extends BugChecker
    implements SwitchTreeMatcher, SwitchExpressionTreeMatcher {
  public static final int MAX_CASES_TO_PRINT = 5;

  private final boolean checkSwitchExpressionsWithDefaultForSkew;

  @Inject
  MissingCasesInEnumSwitch(ErrorProneFlags flags) {
    this.checkSwitchExpressionsWithDefaultForSkew =
        flags
            .getBoolean("MissingCasesInEnumSwitch:CheckSwitchExpressionsWithDefaultForSkew")
            .orElse(true);
  }

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    ExpressionTree expression = tree.getExpression();
    List<? extends CaseTree> cases = tree.getCases();
    Type switchType = ASTHelpers.getType(expression);
    if (switchType.asElement().getKind() != ElementKind.ENUM) {
      return Description.NO_MATCH;
    }
    Optional<? extends CaseTree> defaultCase =
        cases.stream().filter(ASTHelpers::isSwitchDefault).findFirst();

    // Continue to perform the check only if:
    //  - there is no default case present or
    //  - the default case only exists for potential version skew.
    if (defaultCase.isPresent() && !isDefaultCaseForSkew(tree, defaultCase.get(), state)) {
      return Description.NO_MATCH;
    }

    return generateDescriptionForUnhandledCases(
        "Non-exhaustive switch; either add a default or handle the remaining cases",
        expression,
        switchType,
        cases);
  }

  @Override
  public Description matchSwitchExpression(SwitchExpressionTree tree, VisitorState state) {
    // Javac will check for exhaustiveness; the only reason we would check is if there's a default
    // only intended for version skew.
    if (!checkSwitchExpressionsWithDefaultForSkew) {
      return Description.NO_MATCH;
    }

    ExpressionTree expression = tree.getExpression();
    List<? extends CaseTree> cases = tree.getCases();
    Type switchType = ASTHelpers.getType(expression);
    if (switchType.asElement().getKind() != ElementKind.ENUM) {
      return Description.NO_MATCH;
    }
    Optional<? extends CaseTree> defaultCase =
        cases.stream().filter(ASTHelpers::isSwitchDefault).findFirst();

    // Javac will enforce that switch expressions are exhaustive so only continue if both:
    //  - there is a default case present
    //  - the default case only exists for potential version skew.
    if (defaultCase.isEmpty() || !isDefaultCaseForSkew(tree, defaultCase.get(), state)) {
      return Description.NO_MATCH;
    }

    return generateDescriptionForUnhandledCases(
        "Non-exhaustive switch; ensure all cases are handled in addition to the default case",
        expression,
        switchType,
        cases);
  }

  private Description generateDescriptionForUnhandledCases(
      String errorMessage, Tree switchNode, Type switchType, List<? extends CaseTree> cases) {
    Set<String> unhandled = getUnhandledEnumValues(switchType, cases);
    if (unhandled.isEmpty()) {
      return Description.NO_MATCH;
    }
    return buildDescription(switchNode).setMessage(buildMessage(errorMessage, unhandled)).build();
  }

  private static Set<String> getUnhandledEnumValues(
      Type switchType, List<? extends CaseTree> cases) {
    ImmutableSet<String> handled =
        cases.stream()
            .flatMap(c -> c.getExpressions().stream())
            .map(ASTHelpers::getSymbol)
            .filter(x -> x != null)
            .map(symbol -> symbol.getSimpleName().toString())
            .collect(toImmutableSet());
    return Sets.difference(ASTHelpers.enumValues(switchType.asElement()), handled);
  }

  /**
   * Build the diagnostic message.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>Non-exhaustive switch, expected cases for: FOO
   *   <li>Non-exhaustive switch, expected cases for: FOO, BAR, BAZ, and 42 others.
   * </ul>
   */
  private static String buildMessage(String errorMessage, Set<String> unhandled) {
    StringBuilder message = new StringBuilder(errorMessage);
    message.append(": ");
    int numberToShow =
        unhandled.size() > MAX_CASES_TO_PRINT
            ? 3 // if there are too many to print, only show three examples.
            : unhandled.size();
    message.append(unhandled.stream().limit(numberToShow).collect(Collectors.joining(", ")));
    if (numberToShow < unhandled.size()) {
      message.append(String.format(", and %d others", unhandled.size() - numberToShow));
    }
    return message.toString();
  }
}
