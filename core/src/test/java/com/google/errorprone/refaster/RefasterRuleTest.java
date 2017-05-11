/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@code RefasterRule}. */
@RunWith(JUnit4.class)
public final class RefasterRuleTest {
  @Test
  public void fromSecondLevel() {
    assertThat(
            RefasterRule.fromSecondLevel(
                "com.google.devtools.javatools.refactory.refaster.cleanups.MergeNestedIf"))
        .isEqualTo("MergeNestedIf");
    assertThat(
            RefasterRule.fromSecondLevel(
                "com.google.devtools.javatools.refactory.refaster.cleanups.HashingShortcuts.HashEntireByteArray"))
        .isEqualTo("HashEntireByteArray");
    assertThat(
            RefasterRule.fromSecondLevel(
                "com.google.devtools.javatools.refactory.refaster.cleanups.PrimitiveComparisons.Compare.Ints"))
        .isEqualTo("Compare_Ints");
  }
}
