/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

/** Similarity metrics for the ArgumentParameter* checks. */
public class ArgumentParameterSimilarityMetrics {

  private static final Pattern ONLY_UNDERSCORES = Pattern.compile("^_+$");
  private static final Pattern UNDERSCORES_OR_CASE_TRANSITIONS =
      Pattern.compile("_|(?<=[a-z0-9])(?=[A-Z])");

  /**
   * The similarity metric from Section 2.1 of Liu et al., "Nomen est Omen: Exploring and Exploiting
   * Similarities between Argument and Parameter Names," ICSE 2016.
   *
   * <p>The formula is |argTerms intersect paramTerms| / (|argTerms| + |paramTerms|) * 2.
   */
  public static double computeNormalizedTermIntersection(String arg, String param) {
    // TODO(ciera): consider also using edit distance on individual words.
    Set<String> argSplit = splitStringTerms(arg);
    Set<String> paramSplit = splitStringTerms(param);
    // TODO(andrewrice): Handle the substring cases so that lenList matches listLength
    double commonTerms = Sets.intersection(argSplit, paramSplit).size() * 2;
    double totalTerms = argSplit.size() + paramSplit.size();
    return commonTerms / totalTerms;
  }

  /**
   * Divides a string into a set of terms by splitting on underscores and transitions from lower to
   * upper case.
   */
  @VisibleForTesting
  static Set<String> splitStringTerms(String name) {
    if (ONLY_UNDERSCORES.matcher(name).matches()) {
      // Degenerate case of names which contain only underscore
      return ImmutableSet.of(name);
    }
    return Arrays.stream(UNDERSCORES_OR_CASE_TRANSITIONS.split(name))
        .map(String::toLowerCase)
        .collect(toImmutableSet());
  }

  private ArgumentParameterSimilarityMetrics() {}
}
