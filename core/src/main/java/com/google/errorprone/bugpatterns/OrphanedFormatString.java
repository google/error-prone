/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import edu.umd.cs.findbugs.formatStringChecker.Formatter;
import edu.umd.cs.findbugs.formatStringChecker.MissingFormatArgumentException;
import java.util.regex.Pattern;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "OrphanedFormatString",
    summary = "String literal contains format specifiers, but is not passed to a format method",
    severity = WARNING)
public class OrphanedFormatString extends BugChecker implements LiteralTreeMatcher {

  private static final Matcher<Tree> MATCHER =
      toType(
          ExpressionTree.class,
          anyOf(
              constructor().forClass("java.lang.StringBuilder"),
              allOf(
                  constructor()
                      .forClass(TypePredicates.isDescendantOf((s) -> s.getSymtab().throwableType)),
                  (tree, state) -> {
                    Symbol sym = ASTHelpers.getSymbol(tree);
                    return sym instanceof MethodSymbol && !((MethodSymbol) sym).isVarArgs();
                  }),
              instanceMethod()
                  .onDescendantOf("java.io.PrintStream")
                  .withNameMatching(Pattern.compile("print|println")),
              instanceMethod()
                  .onDescendantOf("java.io.PrintWriter")
                  .withNameMatching(Pattern.compile("print|println")),
              instanceMethod().onExactClass("java.lang.StringBuilder").named("append")));


  @Override
  public Description matchLiteral(LiteralTree tree, VisitorState state) {
    Object value = tree.getValue();
    if (!(value instanceof String)) {
      return NO_MATCH;
    }
    if (!missingFormatArgs((String) value)) {
      return NO_MATCH;
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!MATCHER.matches(parent, state)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  /** Returns true for strings that contain format specifiers. */
  private static boolean missingFormatArgs(String value) {
    try {
      Formatter.check(value);
    } catch (MissingFormatArgumentException e) {
      return true;
    } catch (Exception ignored) {
      // we don't care about other errors (it isn't supposed to be a format string)
    }
    return false;
  }
}
