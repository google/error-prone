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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFix.Builder;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "ExpectedExceptionChecker",
  category = JUNIT,
  summary = "Calls to ExpectedException#expect should always be followed by exactly one statement.",
  severity = WARNING
)
public class ExpectedExceptionChecker extends BugChecker implements MethodTreeMatcher {

  static final Matcher<StatementTree> MATCHER =
      expressionStatement(
          instanceMethod()
              .onExactClass("org.junit.rules.ExpectedException")
              .withNameMatching(Pattern.compile("expect.*")));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getBody() == null) {
      return NO_MATCH;
    }

    PeekingIterator<? extends StatementTree> it =
        Iterators.peekingIterator(tree.getBody().getStatements().iterator());
    while (it.hasNext() && !MATCHER.matches(it.peek(), state)) {
      it.next();
    }
    List<MethodInvocationTree> expectations = new ArrayList<>();
    while (it.hasNext() && MATCHER.matches(it.peek(), state)) {
      expectations.add(
          (MethodInvocationTree) ((ExpressionStatementTree) it.next()).getExpression());
    }
    List<StatementTree> suffix = new ArrayList<>();
    Iterators.addAll(suffix, it);
    if (suffix.size() <= 1) {
      // for now, allow ExpectedException as long as it's testing that exactly one statement throws
      return NO_MATCH;
    }

    String exceptionClass = "Throwable";
    // additional assertions to perform on the captured exception (if any)
    List<String> newAsserts = new ArrayList<>();

    Builder fix = SuggestedFix.builder();
    for (MethodInvocationTree expectation : expectations) {
      MethodSymbol symbol = ASTHelpers.getSymbol(expectation);
      Symtab symtab = state.getSymtab();
      List<? extends ExpressionTree> args = expectation.getArguments();
      switch (symbol.getSimpleName().toString()) {
        case "expect":
          if (isSubtype(getOnlyElement(symbol.getParameters()).asType(), symtab.classType, state)) {
            // expect(Class<?>)
            exceptionClass = state.getSourceForNode(getReceiver(getOnlyElement(args)));
          } else {
            // expect(Matcher)
            fix.addStaticImport("org.hamcrest.MatcherAssert.assertThat");
            newAsserts.add(
                String.format(
                    "assertThat(thrown, %s);", state.getSourceForNode(getOnlyElement(args))));
          }
          break;
        case "expectCause":
          fix.addStaticImport("org.hamcrest.MatcherAssert.assertThat");
          newAsserts.add(
              String.format(
                  "assertThat(thrown.getCause(), %s);",
                  state.getSourceForNode(getOnlyElement(args))));
          break;
        case "expectMessage":
          if (isSubtype(
              getOnlyElement(symbol.getParameters()).asType(), symtab.stringType, state)) {
            // expectedMessage(String)
            fix.addStaticImport("com.google.common.truth.Truth.assertThat");
            newAsserts.add(
                String.format(
                    "assertThat(thrown).hasMessageThat().contains(%s);",
                    state.getSourceForNode(getOnlyElement(args))));
          } else {
            // expectedMessage(Matcher)
            fix.addStaticImport("org.hamcrest.MatcherAssert.assertThat");
            newAsserts.add(
                String.format(
                    "assertThat(thrown.getMessage(), %s);",
                    state.getSourceForNode(getOnlyElement(args))));
          }
          break;
        default:
          throw new AssertionError("unknown expect method: " + symbol.getSimpleName());
      }
    }
    // remove all interactions with the ExpectedException rule
    fix.replace(
        ((JCTree) expectations.get(0)).getStartPosition(),
        ((JCTree) suffix.get(0)).getStartPosition(),
        "");
    SuggestedFix baseFix = fix.build();
    List<Fix> fixes = allFixes(suffix, exceptionClass, newAsserts, baseFix);
    Description.Builder description = buildDescription(tree);
    if (!fixes.isEmpty()) {
      description.addAllFixes(fixes);
    } else {
      description.addFix(
          fixStatement(
              SuggestedFix.builder().merge(baseFix), getLast(suffix), exceptionClass, newAsserts));
    }
    return description.build();
  }

  // provide fixes to wrap each of the trailing statements in a lambda
  // skip statements that look like assertions
  private static List<Fix> allFixes(
      List<StatementTree> suffix,
      String exceptionClass,
      List<String> newAsserts,
      SuggestedFix baseFix) {
    return Lists.reverse(suffix)
        .stream()
        .filter(t -> !JUnitMatchers.containsTestMethod(t))
        .map(
            t -> fixStatement(SuggestedFix.builder().merge(baseFix), t, exceptionClass, newAsserts))
        .collect(toImmutableList());
  }

  private static Fix fixStatement(
      SuggestedFix.Builder fix,
      StatementTree expectThrows,
      String exceptionClass,
      List<String> newAsserts) {
    StringBuilder fixPrefix = new StringBuilder();
    if (newAsserts.isEmpty()) {
      fix.addStaticImport("org.junit.Assert.assertThrows");
      fixPrefix.append("assertThrows");
    } else {
      fix.addStaticImport("org.junit.Assert.expectThrows");
      fixPrefix.append(String.format("%s thrown = expectThrows", exceptionClass));
    }
    fixPrefix.append(String.format("(%s.class, () -> ", exceptionClass));

    if (expectThrows.getKind() != Kind.EXPRESSION_STATEMENT) {
      fixPrefix.append("{");
    }
    fix.prefixWith(expectThrows, fixPrefix.toString());

    if (expectThrows.getKind() == Kind.EXPRESSION_STATEMENT) {
      fix.postfixWith(((ExpressionStatementTree) expectThrows).getExpression(), ")");
      fix.postfixWith(expectThrows, Joiner.on('\n').join(newAsserts));
    } else {
      fix.postfixWith(expectThrows, "});\n" + Joiner.on('\n').join(newAsserts));
    }
    return fix.build();
  }
}
