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
 * Expected refactoring output for FloatingPointAssertionWithinEpsilon bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
final class FloatingPointAssertionWithinEpsilonPositiveCases {

  private static final float TOLERANCE = 1e-10f;
  private static final double TOLERANCE2 = 1e-20f;
  private static final float VALUE = 1;

  public void testFloat() {
    assertThat(1.0f).isEqualTo(1.0f);
    assertThat(1f).isEqualTo(VALUE);
    assertThat(1e10f).isEqualTo(1e10f);
    assertThat(1f).isNotEqualTo(2f);
    assertEquals(1f, 1f, 0);
    assertEquals("equal!", 1f, 1f, 0);
  }

  public void testDouble() {
    assertThat(1.0).isEqualTo(1.0);
    assertThat(1.0).isEqualTo(1.0);
    assertThat(1.0).isEqualTo(1d);
    assertThat(1e20).isEqualTo(1e20);
    assertThat(0.1).isNotEqualTo((double) 0.1f);
    assertEquals(1.0, 1.0, 0);
    assertEquals("equal!", 1.0, 1.0, 0);
  }
}
