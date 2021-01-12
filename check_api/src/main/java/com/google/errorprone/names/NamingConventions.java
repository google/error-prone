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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Utility functions for dealing with Java naming conventions */
public class NamingConventions {

  private static final Pattern ONLY_UNDERSCORES = Pattern.compile("^_+$");

  private static final String UNDERSCORE = "_";
  private static final String CASE_TRANSITION = "(?<=[a-z0-9])(?=[A-Z])";
  private static final String TRAILING_DIGITS = "(?<![0-9_])(?=[0-9]+$)";

  private static final Splitter TERM_SPLITTER =
      Splitter.onPattern(String.format("%s|%s|%s", UNDERSCORE, CASE_TRANSITION, TRAILING_DIGITS))
          .omitEmptyStrings();

  /**
   * Split a Java name into terms based on either Camel Case or Underscores. We also split digits at
   * the end of the name into a separate term so as to treat PERSON1 and PERSON_1 as the same thing.
   *
   * @param identifierName to split
   * @return a list of the terms in the name, in order and converted to lowercase
   */
  public static ImmutableList<String> splitToLowercaseTerms(String identifierName) {
    if (ONLY_UNDERSCORES.matcher(identifierName).matches()) {
      // Degenerate case of names which contain only underscore
      return ImmutableList.of(identifierName);
    }
    return TERM_SPLITTER
        .splitToStream(identifierName)
        .map(String::toLowerCase)
        .collect(toImmutableList());
  }

  public static String convertToLowerUnderscore(String identifierName) {
    return splitToLowercaseTerms(identifierName).stream().collect(Collectors.joining("_"));
  }
}
