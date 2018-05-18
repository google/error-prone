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

import com.google.common.collect.ImmutableList;

/**
 * Negative test cases for TruthAssertExpected check.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
final class TruthAssertExpectedNegativeCases {
  private static final Object expected = new Object();
  private static final Object actual = new Object();
  private static final Object foo = new Object();
  private static final long CONSTANT = 1L;

  private enum Enum {
    A,
    B;
  }

  private void simple() {
    assertThat(foo).isEqualTo(expected);

    assertThat(expected.hashCode()).isEqualTo(expected.hashCode());
    assertThat(hashCode()).isEqualTo(foo);
  }

  private void expectedExceptions() {
    Exception expectedException = new Exception("Oh no.");
    assertThat(expectedException).hasMessageThat().isEqualTo("Oh no.");
    assertThat(expectedException.getClass()).isEqualTo(hashCode());
  }

  private void staticFactoryMethod() {
    assertThat(expected).isEqualTo(Long.valueOf(10L));
    assertThat(expected).isEqualTo(ImmutableList.of(1));
  }

  private void constantValues() {
    assertThat(expected).isEqualTo(Enum.A);
    assertThat(expected).isEqualTo(10L);
    assertThat(expected).isEqualTo(CONSTANT);
  }
}
