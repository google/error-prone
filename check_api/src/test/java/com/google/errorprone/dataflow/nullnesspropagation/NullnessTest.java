/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.BOTTOM;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NONNULL;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NULL;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NULLABLE;
import static org.junit.Assert.assertEquals;

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
    assertEquals(NULLABLE, NULLABLE.leastUpperBound(NULLABLE));
    assertEquals(NULLABLE, NULLABLE.leastUpperBound(NULL));
    assertEquals(NULLABLE, NULLABLE.leastUpperBound(NONNULL));
    assertEquals(NULLABLE, NULLABLE.leastUpperBound(BOTTOM));

    assertEquals(NULLABLE, NULL.leastUpperBound(NULLABLE));
    assertEquals(NULL, NULL.leastUpperBound(NULL));
    assertEquals(NULLABLE, NULL.leastUpperBound(NONNULL));
    assertEquals(NULL, NULL.leastUpperBound(BOTTOM));

    assertEquals(NULLABLE, NONNULL.leastUpperBound(NULLABLE));
    assertEquals(NULLABLE, NONNULL.leastUpperBound(NULL));
    assertEquals(NONNULL, NONNULL.leastUpperBound(NONNULL));
    assertEquals(NONNULL, NONNULL.leastUpperBound(BOTTOM));

    assertEquals(NULLABLE, BOTTOM.leastUpperBound(NULLABLE));
    assertEquals(NULL, BOTTOM.leastUpperBound(NULL));
    assertEquals(NONNULL, BOTTOM.leastUpperBound(NONNULL));
    assertEquals(BOTTOM, BOTTOM.leastUpperBound(BOTTOM));
  }

  @Test
  public void testGreatestLowerBound() {
    assertEquals(NULLABLE, NULLABLE.greatestLowerBound(NULLABLE));
    assertEquals(NULL, NULLABLE.greatestLowerBound(NULL));
    assertEquals(NONNULL, NULLABLE.greatestLowerBound(NONNULL));
    assertEquals(BOTTOM, NULLABLE.greatestLowerBound(BOTTOM));

    assertEquals(NULL, NULL.greatestLowerBound(NULLABLE));
    assertEquals(NULL, NULL.greatestLowerBound(NULL));
    assertEquals(BOTTOM, NULL.greatestLowerBound(NONNULL));
    assertEquals(BOTTOM, NULL.greatestLowerBound(BOTTOM));

    assertEquals(NONNULL, NONNULL.greatestLowerBound(NULLABLE));
    assertEquals(BOTTOM, NONNULL.greatestLowerBound(NULL));
    assertEquals(NONNULL, NONNULL.greatestLowerBound(NONNULL));
    assertEquals(BOTTOM, NONNULL.greatestLowerBound(BOTTOM));

    assertEquals(BOTTOM, BOTTOM.greatestLowerBound(NULLABLE));
    assertEquals(BOTTOM, BOTTOM.greatestLowerBound(NULL));
    assertEquals(BOTTOM, BOTTOM.greatestLowerBound(NONNULL));
    assertEquals(BOTTOM, BOTTOM.greatestLowerBound(BOTTOM));
  }

  @Test
  public void testDeducedValueWhenNotEqual() {
    assertEquals(NULLABLE, NULLABLE.deducedValueWhenNotEqual());
    assertEquals(NONNULL, NULL.deducedValueWhenNotEqual());
    assertEquals(NULLABLE, NONNULL.deducedValueWhenNotEqual());
    assertEquals(BOTTOM, BOTTOM.deducedValueWhenNotEqual());
  }
}
