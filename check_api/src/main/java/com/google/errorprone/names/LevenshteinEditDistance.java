/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

/**
 * A utility class for finding the Levenshtein edit distance between strings. The edit distance
 * between two strings is the number of deletions, insertions, and substitutions required to
 * transform the source to the target. See <a
 * href="https://en.wikipedia.org/wiki/Levenshtein_distance">
 * https://en.wikipedia.org/wiki/Levenshtein_distance</a>.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class LevenshteinEditDistance {

  private LevenshteinEditDistance() {
    /* disallow instantiation */
  }

  /**
   * Returns the edit distance between two strings.
   *
   * @param source The source string.
   * @param target The target distance.
   * @return The edit distance between the source and target string.
   * @see #getEditDistance(String, String, boolean)
   */
  public static int getEditDistance(String source, String target) {
    return getEditDistance(source, target, true);
  }

  /**
   * Returns the edit distance between two strings. The algorithm used to calculate this distance
   * has space requirements of len(source)*len(target).
   *
   * @param source The source string.
   * @param target The target string
   * @param caseSensitive If true, case is used in comparisons and 'a' != 'A'.
   * @return The edit distance between the source and target strings.
   * @see #getEditDistance(String, String)
   */
  public static int getEditDistance(String source, String target, boolean caseSensitive) {

    // Levenshtein distance algorithm

    int sourceLength = isEmptyOrWhitespace(source) ? 0 : source.length();
    int targetLength = isEmptyOrWhitespace(target) ? 0 : target.length();

    if (sourceLength == 0) {
      return targetLength;
    }

    if (targetLength == 0) {
      return sourceLength;
    }

    if (!caseSensitive) {
      source = source.toLowerCase();
      target = target.toLowerCase();
    }

    int[][] levMatrix = new int[sourceLength + 1][targetLength + 1];

    for (int i = 0; i <= sourceLength; i++) {
      levMatrix[i][0] = i;
    }

    for (int i = 0; i <= targetLength; i++) {
      levMatrix[0][i] = i;
    }

    for (int i = 1; i <= sourceLength; i++) {

      char sourceI = source.charAt(i - 1);
      for (int j = 1; j <= targetLength; j++) {
        char targetJ = target.charAt(j - 1);

        int cost = 0;
        if (sourceI != targetJ) {
          cost = 1;
        }

        levMatrix[i][j] =
            Math.min(
                cost + levMatrix[i - 1][j - 1],
                Math.min(levMatrix[i - 1][j] + 1, levMatrix[i][j - 1] + 1));
      }
    }

    return levMatrix[sourceLength][targetLength];
  }

  /**
   * Returns a normalized edit distance between 0 and 1. This is useful if you are comparing or
   * aggregating distances of different pairs of strings
   */
  public static double getNormalizedEditDistance(
      String source, String target, boolean caseSensitive) {

    if (isEmptyOrWhitespace(source) && isEmptyOrWhitespace(target)) {
      return 0.0;
    }

    return (double) getEditDistance(source, target, caseSensitive)
        / (double) getWorstCaseEditDistance(source.length(), target.length());
  }

  /** Calculate the worst case distance between two strings with the given lengths */
  public static int getWorstCaseEditDistance(int sourceLength, int targetLength) {
    return Math.max(sourceLength, targetLength);
  }

  /**
   * Determines if a string is empty or consists only of whitespace
   *
   * @param source The string to check
   * @return True if the string is empty or contains only whitespace, false otherwise
   */
  private static boolean isEmptyOrWhitespace(String source) {
    return source == null || source.matches("\\s*");
  }
}
