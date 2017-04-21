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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.common.collect.Streams;
import com.google.errorprone.util.Commented;
import com.google.errorprone.util.Comments;
import com.sun.source.tree.ExpressionTree;
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
final class NamedParameterComment {

  private static final Pattern PARAMETER_COMMENT_PATTERN = Pattern.compile("([\\w\\d_]+)\\s*=\\s*");

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

  /**
   * Determine the kind of match we have between the comments on this argument and the formal
   * parameter name.
   */
  static MatchType match(Commented<ExpressionTree> actual, String formal) {
    Optional<Comment> lastBlockComment =
        Streams.findLast(
            actual.beforeComments().stream().filter(c -> c.getStyle() == CommentStyle.BLOCK));

    if (lastBlockComment.isPresent()) {
      Matcher m =
          PARAMETER_COMMENT_PATTERN.matcher(Comments.getTextFromComment(lastBlockComment.get()));
      if (m.matches()) {
        return m.group(1).equals(formal) ? MatchType.EXACT_MATCH : MatchType.BAD_MATCH;
      }
    }

    if (Stream.concat(actual.beforeComments().stream(), actual.afterComments().stream())
        .map(Comments::getTextFromComment)
        .anyMatch(comment -> Arrays.asList(comment.split("[^a-zA-Z0-9_]+")).contains(formal))) {
      return MatchType.APPROXIMATE_MATCH;
    }

    return MatchType.NOT_ANNOTATED;
  }

  /**
   * Generate comment text which @{code exactMatch} would consider to match the formal parameter
   * name.
   */
  static String toCommentText(String formal) {
    return String.format("/*%s%s*/", formal, PARAMETER_COMMENT_MARKER);
  }

  private NamedParameterComment() {}
}
