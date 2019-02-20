/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.BOTTOM;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NONNULL;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NULL;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NULLABLE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link Nullness}.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class NullnessTest {
  @Test
  public void testLeastUpperBound() {
    assertThat(NULLABLE.leastUpperBound(NULLABLE)).isEqualTo(NULLABLE);
    assertThat(NULLABLE.leastUpperBound(NULL)).isEqualTo(NULLABLE);
    assertThat(NULLABLE.leastUpperBound(NONNULL)).isEqualTo(NULLABLE);
    assertThat(NULLABLE.leastUpperBound(BOTTOM)).isEqualTo(NULLABLE);

    assertThat(NULL.leastUpperBound(NULLABLE)).isEqualTo(NULLABLE);
    assertThat(NULL.leastUpperBound(NULL)).isEqualTo(NULL);
    assertThat(NULL.leastUpperBound(NONNULL)).isEqualTo(NULLABLE);
    assertThat(NULL.leastUpperBound(BOTTOM)).isEqualTo(NULL);

    assertThat(NONNULL.leastUpperBound(NULLABLE)).isEqualTo(NULLABLE);
    assertThat(NONNULL.leastUpperBound(NULL)).isEqualTo(NULLABLE);
    assertThat(NONNULL.leastUpperBound(NONNULL)).isEqualTo(NONNULL);
    assertThat(NONNULL.leastUpperBound(BOTTOM)).isEqualTo(NONNULL);

    assertThat(BOTTOM.leastUpperBound(NULLABLE)).isEqualTo(NULLABLE);
    assertThat(BOTTOM.leastUpperBound(NULL)).isEqualTo(NULL);
    assertThat(BOTTOM.leastUpperBound(NONNULL)).isEqualTo(NONNULL);
    assertThat(BOTTOM.leastUpperBound(BOTTOM)).isEqualTo(BOTTOM);
  }

  @Test
  public void testGreatestLowerBound() {
    assertThat(NULLABLE.greatestLowerBound(NULLABLE)).isEqualTo(NULLABLE);
    assertThat(NULLABLE.greatestLowerBound(NULL)).isEqualTo(NULL);
    assertThat(NULLABLE.greatestLowerBound(NONNULL)).isEqualTo(NONNULL);
    assertThat(NULLABLE.greatestLowerBound(BOTTOM)).isEqualTo(BOTTOM);

    assertThat(NULL.greatestLowerBound(NULLABLE)).isEqualTo(NULL);
    assertThat(NULL.greatestLowerBound(NULL)).isEqualTo(NULL);
    assertThat(NULL.greatestLowerBound(NONNULL)).isEqualTo(BOTTOM);
    assertThat(NULL.greatestLowerBound(BOTTOM)).isEqualTo(BOTTOM);

    assertThat(NONNULL.greatestLowerBound(NULLABLE)).isEqualTo(NONNULL);
    assertThat(NONNULL.greatestLowerBound(NULL)).isEqualTo(BOTTOM);
    assertThat(NONNULL.greatestLowerBound(NONNULL)).isEqualTo(NONNULL);
    assertThat(NONNULL.greatestLowerBound(BOTTOM)).isEqualTo(BOTTOM);

    assertThat(BOTTOM.greatestLowerBound(NULLABLE)).isEqualTo(BOTTOM);
    assertThat(BOTTOM.greatestLowerBound(NULL)).isEqualTo(BOTTOM);
    assertThat(BOTTOM.greatestLowerBound(NONNULL)).isEqualTo(BOTTOM);
    assertThat(BOTTOM.greatestLowerBound(BOTTOM)).isEqualTo(BOTTOM);
  }

  @Test
  public void testDeducedValueWhenNotEqual() {
    assertThat(NULLABLE.deducedValueWhenNotEqual()).isEqualTo(NULLABLE);
    assertThat(NULL.deducedValueWhenNotEqual()).isEqualTo(NONNULL);
    assertThat(NONNULL.deducedValueWhenNotEqual()).isEqualTo(NULLABLE);
    assertThat(BOTTOM.deducedValueWhenNotEqual()).isEqualTo(BOTTOM);
  }
}
