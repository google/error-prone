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

package com.google.errorprone.bugpatterns.testdata;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Negative test cases for FloatingPointAssertionWithinEpsilon check.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
final class FloatingPointAssertionWithinEpsilonNegativeCases {

  private static final float TOLERANCE = 1e-5f;
  private static final double TOLERANCE2 = 1e-10f;
  private static final float VALUE = 1;

  public void testFloat() {
    String test = Boolean.TRUE.toString();
    assertThat(1.0f).isWithin(1e-5f).of(1.0f);
    assertThat(1f).isWithin(TOLERANCE).of(VALUE);
    assertThat(1f).isWithin(1).of(1);

    assertThat(1f).isNotWithin(0).of(2f);

    assertThat(1f).isNotWithin(.5f).of(2f);

    assertEquals(1f, 1f, TOLERANCE);
  }

  public void testDouble() {
    String test = Boolean.TRUE.toString();
    assertThat(1.0).isWithin(1e-10).of(1.0);
    assertThat(1.0).isWithin(TOLERANCE2).of(1f);
    assertThat(1.0).isWithin(TOLERANCE2).of(1);

    assertEquals(1.0, 1.0, TOLERANCE);
  }

  public void testZeroCases() {
    assertThat(1.0).isWithin(0.0).of(1.0);
    assertThat(1f).isWithin(0f).of(1f);
    assertThat(1f).isWithin(0).of(1f);

    assertEquals(1f, 1f, 0f);
  }
}
