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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.auto.value.AutoValue;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Streams;
import com.google.errorprone.util.Commented;
import com.google.errorprone.util.Comments;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Helper methods for checking if a commented argument matches a formal parameter and for generating
 * comments in the right format.
 *
 * <p>We look for a <i>NamedParameterComment</i>: this is the last block comment before the argument
 * which ends with an equals sign.
 */
public final class NamedParameterComment {

  public static final Pattern PARAMETER_COMMENT_PATTERN =
      Pattern.compile("\\s*([\\w\\d_]+)\\s*=\\s*");

  private static final String PARAMETER_COMMENT_MARKER = "=";

  /** Encodes the kind of match we found between a comment and the name of the parameter. */
  enum MatchType {
    /** The NamedParameterComment exactly matches the parameter name. */
    EXACT_MATCH,

    /** The NamedParameterComment doesn't match the parameter name. */
    BAD_MATCH,

    /**
     * There is no NamedParameterComment but a word in one of the other comments equals the
     * parameter name.
     */
    APPROXIMATE_MATCH,

    /** There is no NamedParameterComment and no approximate matches */
    NOT_ANNOTATED,
  }

  @AutoValue
  abstract static class MatchedComment {

    abstract Comment comment();

    abstract MatchType matchType();

    static MatchedComment create(Comment comment, MatchType matchType) {
      return new AutoValue_NamedParameterComment_MatchedComment(comment, matchType);
    }

    static MatchedComment notAnnotated() {
      return new AutoValue_NamedParameterComment_MatchedComment(
          new Comment() {
            @Override
            public String getText() {
              throw new IllegalArgumentException(
                  "Attempt to call getText on comment when in NOT_ANNOTATED state");
            }

            @Override
            public int getSourcePos(int i) {
              throw new IllegalArgumentException(
                  "Attempt to call getText on comment when in NOT_ANNOTATED state");
            }

            @Override
            public CommentStyle getStyle() {
              throw new IllegalArgumentException(
                  "Attempt to call getText on comment when in NOT_ANNOTATED state");
            }

            @Override
            public boolean isDeprecated() {
              throw new IllegalArgumentException(
                  "Attempt to call getText on comment when in NOT_ANNOTATED state");
            }
          },
          MatchType.NOT_ANNOTATED);
    }
  }

  private static boolean isApproximateMatchingComment(Comment comment, String formal) {
    switch (comment.getStyle()) {
      case BLOCK:
      case LINE:
        // sometimes people use comments around arguments for higher level structuring - such as
        // dividing two separate blocks of arguments. In these cases we want to avoid concluding
        // that its a match. Therefore we also check to make sure that the comment is not really
        // long and that it doesn't contain acsii-art style markup.
        String commentText = Comments.getTextFromComment(comment);
        boolean textMatches =
            Arrays.asList(commentText.split("[^a-zA-Z0-9_]+", -1)).contains(formal);
        boolean tooLong = commentText.length() > formal.length() + 5 && commentText.length() > 50;
        boolean tooMuchMarkup = CharMatcher.anyOf("-*!@<>").countIn(commentText) > 5;
        return textMatches && !tooLong && !tooMuchMarkup;
      default:
        return false;
    }
  }

  /**
   * Determine the kind of match we have between the comments on this argument and the formal
   * parameter name.
   */
  static MatchedComment match(Commented<ExpressionTree> actual, String formal) {
    Optional<Comment> lastBlockComment =
        Streams.findLast(
            actual.beforeComments().stream().filter(c -> c.getStyle() == CommentStyle.BLOCK));

    if (lastBlockComment.isPresent()) {
      Matcher m =
          PARAMETER_COMMENT_PATTERN.matcher(Comments.getTextFromComment(lastBlockComment.get()));
      if (m.matches()) {
        return MatchedComment.create(
            lastBlockComment.get(),
            m.group(1).equals(formal) ? MatchType.EXACT_MATCH : MatchType.BAD_MATCH);
      }
    }

    Optional<Comment> approximateMatchComment =
        Stream.concat(actual.beforeComments().stream(), actual.afterComments().stream())
            .filter(comment -> isApproximateMatchingComment(comment, formal))
            .findFirst();

    if (approximateMatchComment.isPresent()) {
      // Report EXACT_MATCH for comments that don't use the recommended style (e.g. `/*foo*/`
      // instead of `/* foo= */`), but which match the formal parameter name exactly, since it's
      // a style nit rather than a possible correctness issue.
      // TODO(cushon): revisit this if we standardize on the recommended comment style.
      String text =
          CharMatcher.anyOf("=:")
              .trimTrailingFrom(Comments.getTextFromComment(approximateMatchComment.get()).trim());
      return MatchedComment.create(
          approximateMatchComment.get(),
          text.equals(formal) ? MatchType.EXACT_MATCH : MatchType.APPROXIMATE_MATCH);
    }

    return MatchedComment.notAnnotated();
  }

  /**
   * Generate comment text which {@code exactMatch} would consider to match the formal parameter
   * name.
   */
  static String toCommentText(String formal) {
    return String.format("/* %s%s */", formal, PARAMETER_COMMENT_MARKER);
  }

  // Include:
  // * enclosing instance parameters, as javac doesn't account for parameters when associating
  //   names (see b/64954766).
  // * synthetic constructor parameters, e.g. in anonymous classes (see b/65065109)
  private static final Pattern SYNTHETIC_PARAMETER_NAME = Pattern.compile("(arg|this\\$|x)[0-9]+");

  /**
   * Returns true if the method has synthetic parameter names, indicating the real names are not
   * available.
   */
  public static boolean containsSyntheticParameterName(MethodSymbol sym) {
    return sym.getParameters().stream()
        .anyMatch(p -> SYNTHETIC_PARAMETER_NAME.matcher(p.getSimpleName()).matches());
  }

  private NamedParameterComment() {}
}
