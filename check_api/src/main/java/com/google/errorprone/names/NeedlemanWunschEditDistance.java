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

package com.google.errorprone.names;

/**
 * The Needleman-Wunsch algorithm for finding least-cost string edit distances between pairs of
 * strings. Like Levenshtein, but this version allows for a sequence of adjacent
 * deletions/insertions to cost less than the total cost of each individual deletion/insertion, so
 * that, for example editing {@code Christopher} into {@code Chris} (dropping 6 characters) is not 6
 * times as expensive as editing {@code Christopher} into {@code Christophe}.
 *
 * <p>See http://en.wikipedia.org/wiki/Needleman-Wunsch_algorithm
 *
 * @author alanw@google.com (Alan Wendt)
 */
public final class NeedlemanWunschEditDistance {

  private NeedlemanWunschEditDistance() {
    /* disallow instantiation */
  }

  /**
   * Returns the edit distance between two strings. Levenshtein charges the same cost for each
   * insertion or deletion. This algorithm is slightly more general in that it charges a sequence of
   * adjacent insertions/deletions an up-front cost plus an incremental cost per insert/delete
   * operation. The idea is that Christopher -&gt; Chris should be less than 6 times as expensive as
   * Christopher -&gt; Christophe. The algorithm used to calculate this distance takes time and
   * space proportional to the product of {@code source.length()} and {@code target.length()} to
   * build the 3 arrays.
   *
   * @param source source string.
   * @param target target string
   * @param caseSensitive if true, case is used in comparisons and 'a' != 'A'.
   * @param changeCost cost of changing one character
   * @param openGapCost cost to open a gap to insert or delete some characters.
   * @param continueGapCost marginal cost to insert or delete next character.
   * @return edit distance between the source and target strings.
   */
  public static int getEditDistance(
      String source,
      String target,
      boolean caseSensitive,
      int changeCost,
      int openGapCost,
      int continueGapCost) {

    if (!caseSensitive) {
      source = source.toLowerCase();
      target = target.toLowerCase();
    }

    int sourceLength = source.length();
    int targetLength = target.length();

    if (sourceLength == 0) {
      return scriptCost(openGapCost, continueGapCost, targetLength);
    }

    if (targetLength == 0) {
      return scriptCost(openGapCost, continueGapCost, sourceLength);
    }

    // mMatrix[i][j] = Cost of aligning source.substring(0,i) with
    // target.substring(0,j), using an edit script ending with
    // matched characters.
    int[][] mMatrix = new int[sourceLength + 1][targetLength + 1];

    // Cost of an alignment that ends with a bunch of deletions.
    // dMatrix[i][j] = best found cost of changing the first i chars
    // of source into the first j chars of target, ending with one
    // or more deletes of source characters.
    int[][] dMatrix = new int[sourceLength + 1][targetLength + 1];

    // Cost of an alignment that ends with one or more insertions.
    int[][] iMatrix = new int[sourceLength + 1][targetLength + 1];

    mMatrix[0][0] = dMatrix[0][0] = iMatrix[0][0] = 0;

    // Any edit script that changes i chars of source into zero
    // chars of target will only involve deletions.  So only the
    // d&m Matrix entries are relevant, because dMatrix[i][0] gives
    // the cost of changing an i-length string into a 0-length string,
    // using an edit script ending in deletions.
    for (int i = 1; i <= sourceLength; i++) {
      mMatrix[i][0] = dMatrix[i][0] = scriptCost(openGapCost, continueGapCost, i);

      // Make the iMatrix entries impossibly expensive, so they'll be
      // ignored as inputs to min().  Use a big cost but not
      // max int because that will overflow if anything's added to it.
      iMatrix[i][0] = Integer.MAX_VALUE / 2;
    }

    for (int j = 1; j <= targetLength; j++) {

      // Only the i&m Matrix entries are relevant here, because they represent
      // the cost of changing a 0-length string into a j-length string, using
      // an edit script ending in insertions.
      mMatrix[0][j] = iMatrix[0][j] = scriptCost(openGapCost, continueGapCost, j);

      // Make the dMatrix entries impossibly expensive, so they'll be
      // ignored as inputs to min().  Use a big cost but not
      // max int because that will overflow if anything's added to it.
      dMatrix[0][j] = Integer.MAX_VALUE / 2;
    }

    for (int i = 1; i <= sourceLength; i++) {

      char sourceI = source.charAt(i - 1);
      for (int j = 1; j <= targetLength; j++) {
        char targetJ = target.charAt(j - 1);

        int cost = (sourceI == targetJ) ? 0 : changeCost;

        // Cost of changing i chars of source into j chars of target,
        // using an edit script ending in matched characters.
        mMatrix[i][j] =
            cost
                + Math.min(
                    mMatrix[i - 1][j - 1], Math.min(iMatrix[i - 1][j - 1], dMatrix[i - 1][j - 1]));

        // Cost of an edit script ending in a deletion.
        dMatrix[i][j] =
            Math.min(
                mMatrix[i - 1][j] + openGapCost + continueGapCost,
                dMatrix[i - 1][j] + continueGapCost);

        // Cost of an edit script ending in an insertion.
        iMatrix[i][j] =
            Math.min(
                mMatrix[i][j - 1] + openGapCost + continueGapCost,
                iMatrix[i][j - 1] + continueGapCost);
      }
    }

    // Return the minimum cost.
    int costOfEditScriptEndingWithMatch = mMatrix[sourceLength][targetLength];
    int costOfEditScriptEndingWithDelete = dMatrix[sourceLength][targetLength];
    int costOfEditScriptEndingWithInsert = iMatrix[sourceLength][targetLength];
    return Math.min(
        costOfEditScriptEndingWithMatch,
        Math.min(costOfEditScriptEndingWithDelete, costOfEditScriptEndingWithInsert));
  }

  /** Return the worst case edit distance between strings of this length */
  public static int getWorstCaseEditDistance(
      int sourceLength, int targetLength, int changeCost, int openGapCost, int continueGapCost) {

    int maxLen = Math.max(sourceLength, targetLength);
    int minLen = Math.min(sourceLength, targetLength);

    // Compute maximum cost of changing one string into another.  If the
    // lengths differ, you'll need maxLen - minLen insertions or deletions.
    int totChangeCost =
        scriptCost(openGapCost, continueGapCost, maxLen - minLen) + minLen * changeCost;

    // Another possibility is to just delete the entire source and insert the
    // target, and not do any changes.
    int blowAwayCost =
        scriptCost(openGapCost, continueGapCost, sourceLength)
            + scriptCost(openGapCost, continueGapCost, targetLength);

    return Math.min(totChangeCost, blowAwayCost);
  }

  /**
   * Returns a normalized edit distance between 0 and 1. This is useful if you are comparing or
   * aggregating distances of different pairs of strings
   */
  public static double getNormalizedEditDistance(
      String source,
      String target,
      boolean caseSensitive,
      int changeCost,
      int openGapCost,
      int continueGapCost) {

    if (source.isEmpty() && target.isEmpty()) {
      return 0.0;
    }

    return (double)
            getEditDistance(source, target, caseSensitive, changeCost, openGapCost, continueGapCost)
        / (double)
            getWorstCaseEditDistance(
                source.length(), target.length(), changeCost, openGapCost, continueGapCost);
  }

  /**
   * Return the cost of a script consisting of a contiguous sequence of insertions or a contiguous
   * sequence of deletions.
   */
  private static int scriptCost(int openGapCost, int continueGapCost, int scriptLength) {
    return (scriptLength == 0) ? 0 : openGapCost + scriptLength * continueGapCost;
  }
}
