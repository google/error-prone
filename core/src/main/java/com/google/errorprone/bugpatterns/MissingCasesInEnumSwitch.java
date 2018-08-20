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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.SwitchTree;
import com.sun.tools.javac.code.Type;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "MissingCasesInEnumSwitch",
    summary = "Switches on enum types should either handle all values, or have a default case.",
    category = JDK,
    severity = WARNING)
public class MissingCasesInEnumSwitch extends BugChecker implements SwitchTreeMatcher {

  public static final int MAX_CASES_TO_PRINT = 5;

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    Type switchType = ASTHelpers.getType(tree.getExpression());
    if (switchType.asElement().getKind() != ElementKind.ENUM) {
      return Description.NO_MATCH;
    }
    // default case is present
    if (tree.getCases().stream().anyMatch(c -> c.getExpression() == null)) {
      return Description.NO_MATCH;
    }
    ImmutableSet<String> handled =
        tree.getCases().stream()
            .map(CaseTree::getExpression)
            .filter(IdentifierTree.class::isInstance)
            .map(e -> ((IdentifierTree) e).getName().toString())
            .collect(toImmutableSet());
    Set<String> unhandled = Sets.difference(ASTHelpers.enumValues(switchType.asElement()), handled);
    if (unhandled.isEmpty()) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree).setMessage(buildMessage(unhandled)).build();
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
  private String buildMessage(Set<String> unhandled) {
    StringBuilder message =
        new StringBuilder(
            "Non-exhaustive switch; either add a default or handle the remaining cases: ");
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
