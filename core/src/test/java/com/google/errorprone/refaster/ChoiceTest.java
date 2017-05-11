/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@code Choice}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class ChoiceTest {
  @Test
  public void testNone() {
    assertThat(Choice.none().first()).isAbsent();
    assertThat(Choice.none().condition(true)).isSameAs(Choice.none());
    assertThat(Choice.none().condition(Predicates.alwaysTrue())).isSameAs(Choice.none());
    assertThat(Choice.none().thenChoose(Functions.constant(Choice.of("foo"))))
        .isSameAs(Choice.none());
  }

  @Test
  public void testThenOption() {
    assertThat(
            Choice.from(ImmutableList.of(1, 2, 3))
                .thenOption(
                    Functions.forMap(
                        ImmutableMap.of(2, Optional.of("foo")), Optional.<String>absent()))
                .asIterable())
        .containsExactly("foo");
  }

  @Test
  public void testThenChoose() {
    assertThat(
            Choice.from(ImmutableList.of(1, 2, 3))
                .thenChoose(
                    Functions.forMap(ImmutableMap.of(2, Choice.of("foo")), Choice.<String>none()))
                .asIterable())
        .containsExactly("foo");
  }

  @Test
  public void testOr() {
    assertThat(Choice.of(2).or(Choice.from(ImmutableList.of(1, 3))).asIterable())
        .containsExactly(2, 1, 3)
        .inOrder();
  }
}
