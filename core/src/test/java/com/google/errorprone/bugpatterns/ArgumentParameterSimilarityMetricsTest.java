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

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ArgumentParameterSimilarityMetrics}. */
@RunWith(JUnit4.class)
public class ArgumentParameterSimilarityMetricsTest {

  @Test
  public void paperSimilarityMetric_sameBiggerThanDifferent() throws Exception {
    assertThat(ArgumentParameterSimilarityMetrics.computeNormalizedTermIntersection("foo", "foo"))
        .isGreaterThan(
            ArgumentParameterSimilarityMetrics.computeNormalizedTermIntersection("foo", "bar"));
  }

  @Test
  public void paperSimilarityMetric_partialSmallerBiggerThanDifferent() throws Exception {
    assertThat(
            ArgumentParameterSimilarityMetrics.computeNormalizedTermIntersection("foo", "barFoo"))
        .isGreaterThan(
            ArgumentParameterSimilarityMetrics.computeNormalizedTermIntersection("foo", "bar"));
  }

  @Test
  public void paperSimilarityMetric_partialLargerBiggerThanDifferent() throws Exception {
    assertThat(
            ArgumentParameterSimilarityMetrics.computeNormalizedTermIntersection("fooBar", "bar"))
        .isGreaterThan(
            ArgumentParameterSimilarityMetrics.computeNormalizedTermIntersection("foo", "bar"));
  }

  @Test
  public void paperSimilarityMetric_partialRepeatedBiggerThanDifferent() throws Exception {
    assertThat(
            ArgumentParameterSimilarityMetrics.computeNormalizedTermIntersection(
                "fooBarFoo", "foo"))
        .isGreaterThan(
            ArgumentParameterSimilarityMetrics.computeNormalizedTermIntersection("foo", "bar"));
  }

  @Test
  public void paperSimilarityMetric_partialSameBiggerThanDifferent() throws Exception {
    assertThat(
            ArgumentParameterSimilarityMetrics.computeNormalizedTermIntersection(
                "fooBar", "barBaz"))
        .isGreaterThan(
            ArgumentParameterSimilarityMetrics.computeNormalizedTermIntersection("foo", "bar"));
  }

  @Test
  public void splitStringTerms_lower() throws Exception {
    Set<String> terms = ArgumentParameterSimilarityMetrics.splitStringTerms("foo");
    assertThat(terms).containsExactly("foo");
  }

  @Test
  public void splitStringTerms_camel() throws Exception {
    Set<String> terms = ArgumentParameterSimilarityMetrics.splitStringTerms("fooBar");
    assertThat(terms).containsExactly("foo", "bar");
  }

  @Test
  public void splitStringTerms_upper() throws Exception {
    Set<String> terms = ArgumentParameterSimilarityMetrics.splitStringTerms("FOO_BAR");
    assertThat(terms).containsExactly("foo", "bar");
  }

  @Test
  public void splitStringTerms_repeated() throws Exception {
    Set<String> terms = ArgumentParameterSimilarityMetrics.splitStringTerms("fooBarFoo");
    assertThat(terms).containsExactly("foo", "bar");
  }

  @Test
  public void splitStringTerms_single() throws Exception {
    Set<String> terms = ArgumentParameterSimilarityMetrics.splitStringTerms("fooBarID");
    assertThat(terms).containsExactly("foo", "bar", "id");
  }

  @Test
  public void splitStringTerms_mixed() throws Exception {
    Set<String> terms = ArgumentParameterSimilarityMetrics.splitStringTerms("foo_barBaz");
    assertThat(terms).containsExactly("foo", "bar", "baz");
  }
}
