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
 * Positive test cases for FloatingPointAssertionWithinEpsilon check.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
final class FloatingPointAssertionWithinEpsilonPositiveCases {

  private static final float TOLERANCE = 1e-10f;
  private static final double TOLERANCE2 = 1e-20f;
  private static final float VALUE = 1;

  public void testFloat() {
    // BUG: Diagnostic contains: 6.0e-08
    assertThat(1.0f).isWithin(1e-20f).of(1.0f);
    // BUG: Diagnostic contains: 6.0e-08
    assertThat(1f).isWithin(TOLERANCE).of(VALUE);
    // BUG: Diagnostic contains: 1.0e+03
    assertThat(1e10f).isWithin(1).of(1e10f);

    // BUG: Diagnostic contains: 1.2e-07
    assertThat(1f).isNotWithin(1e-10f).of(2);

    // BUG: Diagnostic contains: 6.0e-08
    assertEquals(1f, 1f, TOLERANCE);
    // BUG: Diagnostic contains: 6.0e-08
    assertEquals("equal!", 1f, 1f, TOLERANCE);
  }

  public void testDouble() {
    // BUG: Diagnostic contains: 1.1e-16
    assertThat(1.0).isWithin(1e-20).of(1.0);
    // BUG: Diagnostic contains: 1.1e-16
    assertThat(1.0).isWithin(TOLERANCE2).of(1.0f);
    // BUG: Diagnostic contains: 1.1e-16
    assertThat(1.0).isWithin(TOLERANCE2).of(1);
    // BUG: Diagnostic contains: 1.6e+04
    assertThat(1e20).isWithin(1).of(1e20);

    // BUG: Diagnostic contains: 1.4e-17
    assertThat(0.1).isNotWithin(TOLERANCE2).of(0.1f);

    // BUG: Diagnostic contains: 1.1e-16
    assertEquals(1.0, 1.0, TOLERANCE2);
    // BUG: Diagnostic contains: 1.1e-16
    assertEquals("equal!", 1.0, 1.0, TOLERANCE2);
  }
}
