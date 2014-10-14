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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.SwitchTree;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.JCTree.JCSwitch;

import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.lang.model.element.ElementKind;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "MissingCasesInEnumSwitch",
    summary = "Enum switch statement is missing cases",
    explanation = "Enums on switches should either handle all possible values of the enum, or"
        + " have an explicit 'default' case.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class MissingCasesInEnumSwitch extends BugChecker
    implements SwitchTreeMatcher {

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    TypeSymbol switchType = ((JCSwitch) tree).getExpression().type.tsym;

    if (switchType.getKind() != ElementKind.ENUM) {
      return Description.NO_MATCH;
    }

    if (hasDefaultCase(tree)) {
      return Description.NO_MATCH;
    }

    LinkedHashSet<String> unhandled =
        setDifference(ASTHelpers.enumValues(switchType), collectEnumSwitchCases(tree));
    if (unhandled.isEmpty()) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree).setMessage(buildMessage(unhandled)).build();
  }

  /**
   * Build the diagnostic message.
   *
   * <p>Examples:
   * <ul>
   * <li>Non-exhaustive switch, expected cases for: FOO
   * <li>Non-exhaustive switch, expected cases for: FOO, BAR, BAZ, and 42 others. Did you mean to
   * include a 'default' case?
   * </ul>
   */
  private String buildMessage(LinkedHashSet<String> unhandled) {
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
      message.append(String.format(", and %d others. Did you mean to include a 'default' case?",
          unhandled.size() - numberToShow));
    } else {
      message.append(".");
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

  /** Return the difference of sets ax and bx. */
  private static <T> LinkedHashSet<T> setDifference(LinkedHashSet<T> ax, LinkedHashSet<T> bx) {
    LinkedHashSet<T> result = new LinkedHashSet<>(ax);
    result.removeAll(bx);
    return result;
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
