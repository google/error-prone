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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Streams.zip;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasExplicitSource;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.SourceVersion.supportsTextBlocks;
import static java.util.stream.Collectors.joining;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.google.errorprone.util.SourceCodeEscapers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.parser.Tokens;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "This string literal can be written more clearly as a text block",
    severity = WARNING)
public class StringConcatToTextBlock extends BugChecker
    implements LiteralTreeMatcher, MethodInvocationTreeMatcher {

  public static final String DELIMITER = "\"\"\"";

  @Override
  public Description matchLiteral(LiteralTree tree, VisitorState state) {
    if (!supportsTextBlocks(state.context)) {
      return NO_MATCH;
    }
    // javac constant folds string concat during parsing, so we don't need to handle binary
    // expressions
    // see -XDallowStringFolding=false
    if (!tree.getKind().equals(Tree.Kind.STRING_LITERAL)) {
      return NO_MATCH;
    }
    if (state.getPath().getParentPath().getLeaf() instanceof BinaryTree parent) {
      if (parent.getKind().equals(Tree.Kind.PLUS)
          && isSameType(getType(parent), state.getSymtab().stringType, state)) {
        // the readability benefit of text blocks is less if they're inside another string
        // concat expression
        return NO_MATCH;
      }
    }
    if (!hasExplicitSource(tree, state)) {
      return NO_MATCH;
    }
    ImmutableList<ErrorProneToken> tokens = state.getTokensForNode(tree);
    ImmutableList<String> strings =
        tokens.stream()
            .filter(t -> t.kind().equals(Tokens.TokenKind.STRINGLITERAL))
            .map(t -> t.stringVal())
            .collect(toImmutableList());
    boolean trailingNewline = getLast(strings).endsWith("\n");
    // Only migrate if there are enough lines to make it worthwhile. Escaping the trailing newline
    // slightly reduces the readability benefit of migrating, so require an extra line to make it
    // worth it.
    int thresholdToMigrate = trailingNewline ? 2 : 3;
    if (strings.size() < thresholdToMigrate) {
      return NO_MATCH;
    }
    // only migrate if each piece of the string concat ends in an escaped newline. It would be
    // possible to migrate more eagerly, and perhaps use escaped newlines to reflow the string, or
    // recognize APIs that don't care about extra newlines.
    if (!strings.stream().limit(strings.size() - 1).allMatch(s -> s.endsWith("\n"))) {
      return NO_MATCH;
    }
    // shared logic in 'match' assumes the strings are split at newline boundaries, so trim the
    // escaped newlines here
    strings =
        Streams.concat(
                strings.stream().limit(strings.size() - 1).map(s -> s.substring(0, s.length() - 1)),
                Stream.of(getLast(strings)))
            .collect(toImmutableList());
    return match(tree, strings, state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!supportsTextBlocks(state.context)) {
      return NO_MATCH;
    }
    if (JOINER_JOIN.matches(tree, state)) {
      return joiner(tree, state);
    }
    if (STRING_JOINER_TO_STRING.matches(tree, state)) {
      return stringJoiner(tree, state);
    }
    if (STRING_JOIN.matches(tree, state)) {
      return stringJoin(tree, state);
    }
    if (FOR_SOURCE_LINES.matches(tree, state)) {
      return forSourceLines(tree, state);
    }
    return NO_MATCH;
  }

  private Description match(Tree replace, ImmutableList<String> lines, VisitorState state) {
    return match(replace, getStartPosition(replace), state.getEndPosition(replace), lines, state);
  }

  private Description match(
      Tree tree,
      int replaceFrom,
      int replaceTo,
      ImmutableList<String> strings,
      VisitorState state) {
    if (strings.isEmpty()) {
      return NO_MATCH;
    }
    ImmutableList<ErrorProneToken> tokens =
        ErrorProneTokens.getTokens(
            state.getSourceCode().subSequence(replaceFrom, replaceTo).toString(), state.context);
    if (!tokens.stream()
        .flatMap(t -> t.comments().stream())
        .map(c -> c.getText())
        .allMatch(x -> x.isEmpty())) {
      // For long string literals with interspersed comments, moving all the comments to the
      // beginning could regress readability.
      // TODO: cushon - handle comments?
      return NO_MATCH;
    }
    // bail out if the contents contain a delimiter that would require escaping
    if (strings.stream().anyMatch(s -> s.contains(DELIMITER))) {
      return NO_MATCH;
    }
    boolean trailingNewline = getLast(strings).endsWith("\n");
    String joined = String.join("\n", strings);
    ImmutableList<String> outdentedStrings =
        zip(
                joined.stripIndent().lines(),
                joined.lines(),
                (s, orig) -> s + " ".repeat(orig.length() - SPACE.trimTrailingFrom(orig).length()))
            .collect(toImmutableList());
    LineMap lineMap = state.getPath().getCompilationUnit().getLineMap();
    if (lineMap == null) {
      return NO_MATCH;
    }
    String indent = " ".repeat((int) lineMap.getColumnNumber(replaceFrom) - 1);
    String suffix = trailingNewline ? "" : "\\";
    String replacement =
        outdentedStrings.stream()
            .map(line -> line.isEmpty() ? line : (indent + line))
            .map(SourceCodeEscapers.getJavaTextBlockEscaper()::escape)
            .map(s -> s.endsWith(" ") ? (s.substring(0, s.length() - 1) + "\\s") : s)
            .collect(joining("\n", DELIMITER + "\n", suffix + "\n" + indent + DELIMITER));
    if (state.getSourceCode().subSequence(replaceFrom, replaceTo).toString().equals(replacement)) {
      return NO_MATCH;
    }
    SuggestedFix fix = SuggestedFix.replace(replaceFrom, replaceTo, replacement);
    return describeMatch(tree, fix);
  }

  private static final Matcher<ExpressionTree> JOINER_JOIN =
      instanceMethod().onExactClass("com.google.common.base.Joiner").named("join");
  private static final Matcher<ExpressionTree> JOINER_ON =
      staticMethod().onClass("com.google.common.base.Joiner").named("on");

  private static final Matcher<ExpressionTree> STRING_JOINER_TO_STRING =
      instanceMethod().onExactClass("java.util.StringJoiner").named("toString");
  private static final Matcher<ExpressionTree> STRING_JOINER_ADD =
      instanceMethod().onExactClass("java.util.StringJoiner").named("add");
  private static final Matcher<ExpressionTree> STRING_JOINER_CONSTRUCTOR =
      constructor().forClass("java.util.StringJoiner").withParameters("java.lang.CharSequence");

  private static final Matcher<ExpressionTree> STRING_JOIN =
      staticMethod().onClass("java.lang.String").named("join");

  private static final Matcher<ExpressionTree> FOR_SOURCE_LINES =
      staticMethod().onClass("com.google.testing.compile.JavaFileObjects").named("forSourceLines");

  private static final CharMatcher SPACE = CharMatcher.is(' ');

  private Description joiner(MethodInvocationTree tree, VisitorState state) {
    ImmutableList<String> strings = stringLiteralArguments(tree.getArguments());
    if (strings.isEmpty()) {
      return NO_MATCH;
    }
    if (!(getReceiver(tree) instanceof MethodInvocationTree receiver)) {
      return NO_MATCH;
    }
    if (!JOINER_ON.matches(receiver, state)) {
      return NO_MATCH;
    }
    if (!newlineLiteral(getOnlyElement(receiver.getArguments()))) {
      return NO_MATCH;
    }
    return match(tree, strings, state);
  }

  private Description stringJoiner(MethodInvocationTree tree, VisitorState state) {
    Deque<String> strings = new ArrayDeque<>();
    ExpressionTree current = tree;
    while (getReceiver(current) instanceof MethodInvocationTree receiver) {
      if (!STRING_JOINER_ADD.matches(receiver, state)) {
        break;
      }
      Optional<String> string = stringLiteral(getOnlyElement(receiver.getArguments()));
      if (string.isEmpty()) {
        return NO_MATCH;
      }
      strings.addFirst(string.get());
      current = receiver;
    }
    if (!(getReceiver(current) instanceof NewClassTree constructor
        && STRING_JOINER_CONSTRUCTOR.matches(constructor, state))) {
      return NO_MATCH;
    }
    if (!newlineLiteral(getOnlyElement(constructor.getArguments()))) {
      return NO_MATCH;
    }
    return match(tree, ImmutableList.copyOf(strings), state);
  }

  private Description stringJoin(MethodInvocationTree tree, VisitorState state) {
    ImmutableList<String> strings =
        stringLiteralArguments(tree.getArguments().subList(1, tree.getArguments().size()));
    if (!newlineLiteral(tree.getArguments().get(0))) {
      return NO_MATCH;
    }
    return match(tree, strings, state);
  }

  private Description forSourceLines(MethodInvocationTree tree, VisitorState state) {
    ImmutableList<String> strings =
        stringLiteralArguments(tree.getArguments().subList(1, tree.getArguments().size()));
    return match(
        tree.getArguments().get(1),
        getStartPosition(tree.getArguments().get(1)),
        state.getEndPosition(getLast(tree.getArguments())),
        strings,
        state);
  }

  private static boolean newlineLiteral(ExpressionTree expressionTree) {
    Object value = ASTHelpers.constValue(expressionTree);
    if (value == null) {
      return false;
    }
    return value.equals("\n") || value.equals('\n');
  }

  static ImmutableList<String> stringLiteralArguments(List<? extends ExpressionTree> arguments) {
    ImmutableList<String> strings =
        arguments.stream()
            .filter(a -> a.getKind().equals(Tree.Kind.STRING_LITERAL))
            .map(x -> (String) ((LiteralTree) x).getValue())
            .collect(toImmutableList());
    if (strings.size() != arguments.size()) {
      return ImmutableList.of();
    }
    return strings;
  }

  static Optional<String> stringLiteral(ExpressionTree tree) {
    return tree.getKind().equals(Tree.Kind.STRING_LITERAL)
        ? Optional.of((String) ((LiteralTree) tree).getValue())
        : Optional.empty();
  }
}
