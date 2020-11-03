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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Streams.forEachPair;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.argumentselectiondefects.NamedParameterComment;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Comments;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.util.Position;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "ParameterName",
    summary =
        "Detects `/* name= */`-style comments on actual parameters where the name doesn't match the"
            + " formal parameter",
    severity = WARNING)
public class ParameterName extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private final ImmutableList<String> exemptPackages;

  public ParameterName() {
    this(ErrorProneFlags.empty());
  }

  public ParameterName(ErrorProneFlags errorProneFlags) {
    this.exemptPackages =
        errorProneFlags
            .getList("ParameterName:exemptPackagePrefixes")
            .orElse(ImmutableList.of())
            .stream()
            // add a trailing '.' so that e.g. com.foo matches as a prefix of com.foo.bar, but not
            // com.foobar
            .map(p -> p.endsWith(".") ? p : p + ".")
            .collect(toImmutableList());
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    checkArguments(tree, tree.getArguments(), state);
    return NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    checkArguments(tree, tree.getArguments(), state);
    return NO_MATCH;
  }

  private void checkArguments(
      Tree tree, List<? extends ExpressionTree> arguments, VisitorState state) {
    if (arguments.isEmpty()) {
      return;
    }
    MethodSymbol sym = (MethodSymbol) ASTHelpers.getSymbol(tree);
    if (NamedParameterComment.containsSyntheticParameterName(sym)) {
      return;
    }
    int start = getStartPosition(tree);
    int end = state.getEndPosition(getLast(arguments));
    if (start == Position.NOPOS || end == Position.NOPOS) {
      // best effort work-around for https://github.com/google/error-prone/issues/780
      return;
    }
    String source = state.getSourceCode().subSequence(start, end).toString();
    if (!source.contains("/*")) {
      // fast path if the arguments don't contain anything that looks like a comment
      return;
    }
    String enclosingClass = ASTHelpers.enclosingClass(sym).toString();
    if (exemptPackages.stream().anyMatch(enclosingClass::startsWith)) {
      return;
    }
    Deque<ErrorProneToken> tokens =
        new ArrayDeque<>(ErrorProneTokens.getTokens(source, start, state.context));
    forEachPair(
        sym.getParameters().stream(),
        arguments.stream(),
        (p, a) -> {
          if (advanceTokens(tokens, a, state)) {
            checkArgument(p, a, tokens.removeFirst(), state);
          }
        });

    // handle any varargs arguments after the first
    int numParams = sym.getParameters().size();
    int numArgs = arguments.size();
    if (numParams < numArgs) {
      for (ExpressionTree arg : arguments.subList(numParams, numArgs)) {
        if (advanceTokens(tokens, arg, state)) {
          checkComment(arg, tokens.removeFirst(), state);
        }
      }
    }
  }

  private static boolean advanceTokens(
      Deque<ErrorProneToken> tokens, ExpressionTree actual, VisitorState state) {
    while (!tokens.isEmpty() && tokens.peekFirst().pos() < getStartPosition(actual)) {
      tokens.removeFirst();
    }
    if (tokens.isEmpty()) {
      return false;
    }
    Range<Integer> argRange =
        Range.closedOpen(getStartPosition(actual), state.getEndPosition(actual));
    if (!argRange.contains(tokens.peekFirst().pos())) {
      return false;
    }
    return true;
  }

  @AutoValue
  abstract static class FixInfo {
    abstract boolean isFormatCorrect();

    abstract boolean isNameCorrect();

    abstract Comment comment();

    abstract String name();

    static FixInfo create(
        boolean isFormatCorrect, boolean isNameCorrect, Comment comment, String name) {
      return new AutoValue_ParameterName_FixInfo(isFormatCorrect, isNameCorrect, comment, name);
    }
  }

  private void checkArgument(
      VarSymbol formal, ExpressionTree actual, ErrorProneToken token, VisitorState state) {
    List<FixInfo> matches = new ArrayList<>();
    for (Comment comment : token.comments()) {
      Matcher m =
          NamedParameterComment.PARAMETER_COMMENT_PATTERN.matcher(
              Comments.getTextFromComment(comment));
      if (!m.matches()) {
        continue;
      }

      boolean isFormatCorrect = isVarargs(formal) ^ Strings.isNullOrEmpty(m.group(2));
      String name = m.group(1);
      boolean isNameCorrect = formal.getSimpleName().contentEquals(name);

      // If there are multiple parameter name comments, bail if any one of them is an exact match.
      if (isNameCorrect && isFormatCorrect) {
        matches.clear();
        break;
      }

      matches.add(FixInfo.create(isFormatCorrect, isNameCorrect, comment, name));
    }

    String fixTemplate = isVarargs(formal) ? "/* %s...= */" : "/* %s= */";
    for (FixInfo match : matches) {
      SuggestedFix rewriteCommentFix =
          rewriteComment(match.comment(), String.format(fixTemplate, formal.getSimpleName()));
      SuggestedFix rewriteToRegularCommentFix =
          rewriteComment(match.comment(), String.format("/* %s */", match.name()));

      Description description;
      if (match.isFormatCorrect() && !match.isNameCorrect()) {
        description =
            buildDescription(actual)
                .setMessage(
                    String.format(
                        "`%s` does not match formal parameter name `%s`; either fix the name or"
                            + " use a regular comment",
                        match.comment().getText(), formal.getSimpleName()))
                .addFix(rewriteCommentFix)
                .addFix(rewriteToRegularCommentFix)
                .build();
      } else if (!match.isFormatCorrect() && match.isNameCorrect()) {
        description =
            buildDescription(actual)
                .setMessage(
                    String.format(
                        "parameter name comment `%s` uses incorrect format",
                        match.comment().getText()))
                .addFix(rewriteCommentFix)
                .build();
      } else if (!match.isFormatCorrect() && !match.isNameCorrect()) {
        description =
            buildDescription(actual)
                .setMessage(
                    String.format(
                        "`%s` does not match formal parameter name `%s` and uses incorrect "
                            + "format; either fix the format or use a regular comment",
                        match.comment().getText(), formal.getSimpleName()))
                .addFix(rewriteCommentFix)
                .addFix(rewriteToRegularCommentFix)
                .build();
      } else {
        throw new AssertionError(
            "Unexpected match with both isNameCorrect and isFormatCorrect true: " + match);
      }
      state.reportMatch(description);
    }
  }

  private static SuggestedFix rewriteComment(Comment comment, String format) {
    int replacementStartPos = comment.getSourcePos(0);
    int replacementEndPos = comment.getSourcePos(comment.getText().length() - 1) + 1;
    return SuggestedFix.replace(replacementStartPos, replacementEndPos, format);
  }

  // complains on parameter name comments on varargs past the first one
  private void checkComment(ExpressionTree arg, ErrorProneToken token, VisitorState state) {
    for (Comment comment : token.comments()) {
      Matcher m =
          NamedParameterComment.PARAMETER_COMMENT_PATTERN.matcher(
              Comments.getTextFromComment(comment));
      if (m.matches()) {
        SuggestedFix rewriteCommentFix =
            rewriteComment(comment, String.format("/* %s%s */", m.group(1), m.group(2)));
        state.reportMatch(
            buildDescription(arg)
                .addFix(rewriteCommentFix)
                .setMessage("parameter name comment only allowed on first varargs argument")
                .build());
      }
    }
  }

  private static boolean isVarargs(VarSymbol sym) {
    Preconditions.checkArgument(
        sym.owner instanceof MethodSymbol, "sym must be a parameter to a method");
    MethodSymbol method = (MethodSymbol) sym.owner;
    return method.isVarArgs() && (method.getParameters().last() == sym);
  }
}
