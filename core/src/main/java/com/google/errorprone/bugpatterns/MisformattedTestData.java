/*
 * Copyright 2024 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ErrorProneTokens.getTokens;
import static com.google.errorprone.util.SourceVersion.supportsTextBlocks;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Splitter;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.parser.Tokens.TokenKind;

/** See the summary. */
@BugPattern(
    severity = WARNING,
    summary = "This test data will be more readable if correctly formatted.")
public final class MisformattedTestData extends BugChecker implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    // We're not only matching text blocks below, just taking the fact that it's a single literal
    // argument containing source code as a sign that it should be formatted in a text block.
    if (!supportsTextBlocks(state.context)) {
      return NO_MATCH;
    }
    if (!ADD_SOURCE_CALL.matches(tree, state)) {
      return NO_MATCH;
    }
    if (tree.getArguments().size() != 2) {
      return NO_MATCH;
    }
    var sourceTree = tree.getArguments().get(1);
    if (!(sourceTree instanceof LiteralTree literalTree)) {
      return NO_MATCH;
    }
    var sourceValue = literalTree.getValue();
    if (!(sourceValue instanceof String string)) {
      return NO_MATCH;
    }

    Formatter formatter = new Formatter();
    String formattedSource;
    try {
      formattedSource = formatter.formatSource(string);
    } catch (FormatterException exception) {
      return NO_MATCH;
    }
    if (formattedSource.trim().equals(string.trim())) {
      return NO_MATCH;
    }
    // This is a bit crude: but tokenize between the comma and the 2nd argument in order to work out
    // an appropriate indent level for the text block. This is assuming that the source has already
    // been formatted so that the arguments are nicely indented.
    int startPos = state.getEndPosition(tree.getArguments().get(0));
    int endPos = getStartPosition(tree.getArguments().get(1));
    var tokens =
        getTokens(
            state.getSourceCode().subSequence(startPos, endPos).toString(),
            startPos,
            state.context);
    var afterCommaPos =
        tokens.reverse().stream()
            .filter(t -> t.kind().equals(TokenKind.COMMA))
            .findFirst()
            .orElseThrow()
            .endPos();
    var betweenArguments = state.getSourceCode().subSequence(afterCommaPos, endPos).toString();
    var spaces =
        betweenArguments.contains("\n")
            ? betweenArguments.substring(betweenArguments.indexOf('\n') + 1)
            : "";
    return describeMatch(
        sourceTree,
        SuggestedFix.replace(
            sourceTree,
            "\"\"\"\n"
                + LINE_SPLITTER
                    .splitToStream(escape(formattedSource))
                    .map(line -> line.isEmpty() ? "" : spaces + line)
                    .collect(joining("\n"))
                + spaces
                + "\"\"\""));
  }

  // TODO(ghm): Consider generalising this via an annotation.
  private static final Matcher<ExpressionTree> ADD_SOURCE_CALL =
      anyOf(
          instanceMethod()
              .onExactClass("com.google.errorprone.CompilationTestHelper")
              .named("addSourceLines"),
          instanceMethod()
              .onExactClass("com.google.errorprone.BugCheckerRefactoringTestHelper")
              .named("addInputLines"),
          instanceMethod()
              .onExactClass("com.google.errorprone.BugCheckerRefactoringTestHelper.ExpectOutput")
              .named("addOutputLines"));

  private static final Splitter LINE_SPLITTER = Splitter.on('\n');

  private static String escape(String line) {
    return ESCAPER.escape(line).replace("\"\"\"", "\\\"\"\"");
  }

  private static final Escaper ESCAPER = Escapers.builder().addEscape('\\', "\\\\").build();
}
