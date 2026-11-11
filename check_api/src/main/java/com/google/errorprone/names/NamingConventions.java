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

package com.google.errorprone.names;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import java.util.Locale;

/** Utility functions for dealing with Java naming conventions */
public final class NamingConventions {

  private static final CharMatcher UNDERSCORE = CharMatcher.is('_');
  private static final CharMatcher ASCII_DIGIT = CharMatcher.inRange('0', '9');
  private static final CharMatcher NON_ASCII_DIGIT = ASCII_DIGIT.negate();
  private static final CharMatcher LOWER_OR_DIGIT = CharMatcher.inRange('a', 'z').or(ASCII_DIGIT);

  /**
   * Split a Java name into terms based on either Camel Case or Underscores. We also split digits at
   * the end of the name into a separate term so as to treat PERSON1 and PERSON_1 as the same thing.
   *
   * @param identifierName to split
   * @return a list of the terms in the name, in order and converted to lowercase
   */
  public static ImmutableList<String> splitToLowercaseTerms(String identifierName) {
    if (!identifierName.isEmpty() && UNDERSCORE.matchesAllOf(identifierName)) {
      // Degenerate case of names which contain only underscore
      return ImmutableList.of(identifierName);
    }
    int trailingDigits = NON_ASCII_DIGIT.lastIndexIn(identifierName) + 1;
    ImmutableList.Builder<String> terms = ImmutableList.builder();
    int start = 0;
    for (int i = 0; i < identifierName.length(); i++) {
      char c = identifierName.charAt(i);
      if (c == '_') {
        addTerm(terms, identifierName, start, i);
        start = i + 1;
      } else if (i == trailingDigits
          || (i > 0
              && Ascii.isUpperCase(c)
              && LOWER_OR_DIGIT.matches(identifierName.charAt(i - 1)))) {
        addTerm(terms, identifierName, start, i);
        start = i;
      }
    }
    addTerm(terms, identifierName, start, identifierName.length());
    return terms.build();
  }

  private static void addTerm(
      ImmutableList.Builder<String> terms, String name, int start, int end) {
    if (start < end) {
      terms.add(name.substring(start, end).toLowerCase(Locale.ROOT));
    }
  }

  public static String convertToLowerUnderscore(String identifierName) {
    return String.join("_", splitToLowercaseTerms(identifierName));
  }

  private NamingConventions() {}
}
