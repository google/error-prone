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

package com.google.errorprone.util;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Utilities for checks that work with regexes. */
public class Regexes {

  /**
   * A map of regex constructs to escape characters we can translate to a literal. Note that '\b'
   * doesn't appear here because '\b' in a regex means wordbreak and '\b' a as literal means
   * backspace.
   */
  private static final ImmutableMap<Character, Character> REGEXCHAR_TO_LITERALCHAR =
      new ImmutableMap.Builder<Character, Character>()
          .put('t', '\t')
          .put('n', '\n')
          .put('f', '\f')
          .put('r', '\r')
          .build();

  private static final CharMatcher UNESCAPED_CONSTRUCT = CharMatcher.anyOf("[].^$?*+{}()|");

  /**
   * If the given regexes matches exactly one string, returns that string. Otherwise returns {@code
   * null}. This can be used to identify arguments to e.g. {@code String.replaceAll} that don't need
   * to be regexes.
   */
  public static Optional<String> convertRegexToLiteral(String s) {
    try {
      Pattern.compile(s);
    } catch (PatternSyntaxException e) {
      /* The string is a malformed regular expression which will throw an error at runtime. We will
       * preserve this behavior by not rewriting it.
       */
      return Optional.empty();
    }

    boolean inQuote = false;
    StringBuilder result = new StringBuilder();
    int length = s.length();
    for (int i = 0; i < length; ++i) {
      char current = s.charAt(i);
      if (!inQuote && UNESCAPED_CONSTRUCT.matches(current)) {
        /* If we see an unescaped regular expression control character then we can't match this as a
         * string-literal so give up
         */
        return Optional.empty();
      } else if (current == '\\') {

        /* There should be a character following the backslash. No need to check for string length
         * since we have already ascertained we have a well formed regex */
        char escaped = s.charAt(++i);

        if (escaped == 'Q') {
          inQuote = true;
        } else if (escaped == 'E') {
          inQuote = false;
        } else {
          /* If not starting or ending a quotation (\Q...\E) backslashes can only be used to write
           * escaped constructs or to quote characters that would otherwise be interpreted as
           * unescaped constructs.
           *
           * If they are escaping a construct we can write as a literal string (i.e. one of \t \n
           * \f \r or \\) then we convert to a literal character.
           *
           * If they are escaping an unescaped construct we convert to the relevant character
           *
           * Everything else we can't represent in a literal string
           */
          Character controlChar = REGEXCHAR_TO_LITERALCHAR.get(escaped);
          if (controlChar != null) {
            result.append(controlChar);
          } else if (escaped == '\\') {
            result.append('\\');
          } else if (UNESCAPED_CONSTRUCT.matches(escaped)) {
            result.append(escaped);
          } else {
            return Optional.empty();
          }
        }
      } else {
        /* Otherwise we have a literal character to match so keep going */
        result.append(current);
      }
    }
    return Optional.of(result.toString());
  }
}
