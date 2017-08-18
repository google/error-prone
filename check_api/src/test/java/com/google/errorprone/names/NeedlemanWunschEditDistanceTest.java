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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for NeedlemanWunschEditDistance */
@RunWith(JUnit4.class)
public class NeedlemanWunschEditDistanceTest {

  @Test
  public void needlemanWunschEditDistance_returnsZero_withIdenticalNames() {
    String identifier = "foo";

    double distance =
        NeedlemanWunschEditDistance.getEditDistance(
            identifier, identifier, /* caseSensitive= */ false, 1, 1, 10);

    assertThat(distance).isEqualTo(0.0);
  }

  @Test
  public void needlemanWunschEditDistance_matchesLevenschtein_withHugeGapCost() {
    String identifier = "fooBar";
    String otherIdentifier = "bazQux";

    double levenschtein = LevenshteinEditDistance.getEditDistance(identifier, otherIdentifier);
    double needlemanWunsch =
        NeedlemanWunschEditDistance.getEditDistance(
            identifier, otherIdentifier, /* caseSensitive= */ false, 1, 1000, 1000);

    assertThat(needlemanWunsch).isEqualTo(levenschtein);
  }

  @Test
  public void needlemanWunschEditDistanceWorstCase_matchesLevenschtein_withHugeGapCost() {
    String identifier = "fooBar";
    String otherIdentifier = "bazQux";

    double levenschtein =
        LevenshteinEditDistance.getWorstCaseEditDistance(
            identifier.length(), otherIdentifier.length());
    double needlemanWunsch =
        NeedlemanWunschEditDistance.getWorstCaseEditDistance(
            identifier.length(), otherIdentifier.length(), 1, 1000, 1000);

    assertThat(needlemanWunsch).isEqualTo(levenschtein);
  }
}
