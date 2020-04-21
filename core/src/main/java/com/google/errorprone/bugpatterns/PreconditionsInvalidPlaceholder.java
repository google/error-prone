/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.regex.Pattern;

/** @author Louis Wasserman */
@BugPattern(
    name = "PreconditionsInvalidPlaceholder",
    summary = "Preconditions only accepts the %s placeholder in error message strings",
    severity = ERROR,
    tags = StandardTags.LIKELY_ERROR)
public class PreconditionsInvalidPlaceholder extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> PRECONDITIONS_CHECK =
      allOf(
          anyOf(
              staticMethod().onClass("com.google.common.base.Preconditions"),
              staticMethod().onClass("com.google.common.base.Verify")),
          PreconditionsInvalidPlaceholder::secondParameterIsString);

  private static boolean secondParameterIsString(ExpressionTree tree, VisitorState state) {
    Symbol symbol = getSymbol(tree);
    if (!(symbol instanceof MethodSymbol)) {
      return false;
    }
    MethodSymbol methodSymbol = (MethodSymbol) symbol;
    return methodSymbol.getParameters().size() >= 2
        && isSubtype(methodSymbol.getParameters().get(1).type, state.getSymtab().stringType, state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (PRECONDITIONS_CHECK.matches(tree, state)
        && tree.getArguments().get(1) instanceof LiteralTree) {
      LiteralTree formatStringTree = (LiteralTree) tree.getArguments().get(1);
      if (formatStringTree.getValue() instanceof String) {
        String formatString = (String) formatStringTree.getValue();
        int expectedArgs = expectedArguments(formatString);
        if (expectedArgs < tree.getArguments().size() - 2
            && BAD_PLACEHOLDER_REGEX.matcher(formatString).find()) {
          return describe(tree, state);
        }
      }
    }
    return Description.NO_MATCH;
  }

  /**
   * Matches most {@code java.util.Formatter} and {@code java.text.MessageFormat} format
   * placeholders, other than %s itself.
   *
   * <p>This does not need to be completely exhaustive, since it is only used to suggest fixes.
   */
  private static final Pattern BAD_PLACEHOLDER_REGEX =
      Pattern.compile("\\$s|%(?:\\d+\\$)??[dbBhHScCoxXeEfgGaAtTn]|\\{\\d+}");

  public Description describe(MethodInvocationTree tree, VisitorState state) {
    LiteralTree formatTree = (LiteralTree) tree.getArguments().get(1);

    String fixedFormatString =
        BAD_PLACEHOLDER_REGEX.matcher(state.getSourceForNode(formatTree)).replaceAll("%s");
    if (expectedArguments(fixedFormatString) == tree.getArguments().size() - 2) {
      return describeMatch(formatTree, SuggestedFix.replace(formatTree, fixedFormatString));
    }
    int missing = tree.getArguments().size() - 2 - expectedArguments(fixedFormatString);
    StringBuilder builder = new StringBuilder(fixedFormatString);
    builder.deleteCharAt(builder.length() - 1);
    builder.append(" [%s");
    for (int i = 1; i < missing; i++) {
      builder.append(", %s");
    }
    builder.append("]\"");
    return describeMatch(tree, SuggestedFix.replace(formatTree, builder.toString()));
  }

  private static int expectedArguments(String formatString) {
    int count = 0;
    for (int i = formatString.indexOf("%s"); i != -1; i = formatString.indexOf("%s", i + 1)) {
      count++;
    }
    return count;
  }
}
