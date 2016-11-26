/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Description.Builder;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.SwitchTree;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.ElementKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "MissingCasesInEnumSwitch",
  summary = "The Google Java Style Guide requires switch statements to have an explicit default",
  category = JDK,
  severity = WARNING
)
public class MissingCasesInEnumSwitch extends BugChecker implements SwitchTreeMatcher {

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    TypeSymbol switchType = ((JCSwitch) tree).getExpression().type.tsym;

    if (switchType.getKind() != ElementKind.ENUM) {
      return Description.NO_MATCH;
    }

    if (hasDefaultCase(tree)) {
      return Description.NO_MATCH;
    }

    Set<String> unhandled =
        Sets.difference(ASTHelpers.enumValues(switchType), collectEnumSwitchCases(tree));
    if (unhandled.isEmpty()) {
      return Description.NO_MATCH;
    }

    Description.Builder description = buildDescription(tree).setMessage(buildMessage(unhandled));
    buildFixes(tree, state, unhandled, description);
    return description.build();
  }

  /** Adds suggested fixes. */
  private void buildFixes(
      SwitchTree tree, VisitorState state, Set<String> unhandled, Builder description) {

    int idx = state.getEndPosition(tree) - 1; // preserve closing '}'

    StringBuilder sb = new StringBuilder();
    for (String label : unhandled) {
      sb.append(String.format("case %s: ", label));
    }
    sb.append("break;\n");
    description.addFix(SuggestedFix.replace(idx, idx, sb.toString()));

    description.addFix(
        SuggestedFix.replace(
            idx,
            idx,
            String.format(
                "default: throw new AssertionError(\"unexpected case: \" + %s);\n",
                state.getSourceForNode(TreeInfo.skipParens((JCTree) tree.getExpression())))));

    description.addFix(SuggestedFix.replace(idx, idx, "default: break;\n"));
  }

  /**
   * Build the diagnostic message.
   *
   * <p>Examples:
   * <ul>
   * <li>Non-exhaustive switch, expected cases for: FOO
   * <li>Non-exhaustive switch, expected cases for: FOO, BAR, BAZ, and 42 others.
   * </ul>
   */
  private String buildMessage(Set<String> unhandled) {
    final int maxCasesToPrint = 5;

    StringBuilder message = new StringBuilder("Non-exhaustive switch, expected cases for: ");

    boolean tooManyCasesToPrint = unhandled.size() > maxCasesToPrint;
    int numberToShow = tooManyCasesToPrint
        ? 3 // if there are too many to print, only show three examples.
        : unhandled.size();

    Iterator<String> it = unhandled.iterator();
    for (int i = 0; i < numberToShow; ++i) {
      if (i != 0) {
        message.append(", ");
      }
      message.append(it.next());
    }

    if (tooManyCasesToPrint) {
      message.append(String.format(", and %d others", unhandled.size() - numberToShow));
    }
    return message.toString();
  }

  /** Return the enum values handled by the given switch statement's cases. */
  private static LinkedHashSet<String> collectEnumSwitchCases(SwitchTree tree) {
    LinkedHashSet<String> cases = new LinkedHashSet<>();
    for (CaseTree caseTree : tree.getCases()) {
      ExpressionTree pat = caseTree.getExpression();
      if (pat instanceof IdentifierTree) {
        cases.add(((IdentifierTree) pat).getName().toString());
      }
    }
    return cases;
  }

  /** Return true if the switch has a 'default' case. */
  private static boolean hasDefaultCase(SwitchTree tree) {
    for (CaseTree caseTree : tree.getCases()) {
      if (isDefaultCase(caseTree)) {
        return true;
      }
    }
    return false;
  }

  /** The 'default' case is represented in the AST as a CaseTree with a null expression. */
  private static boolean isDefaultCase(CaseTree tree) {
    return tree.getExpression() == null;
  }
}
