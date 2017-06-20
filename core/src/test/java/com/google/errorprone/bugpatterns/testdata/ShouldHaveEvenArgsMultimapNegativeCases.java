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

package com.google.errorprone.bugpatterns.testdata;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * Negative test cases for {@link ShouldHaveEvenArgs} check.
 *
 * @author monnoroch@google.com (Max Strakhov)
 */
public class ShouldHaveEvenArgsMultimapNegativeCases {

  private static final Multimap<String, String> multimap = ImmutableMultimap.of();

  public void testWithMinimalArgs() {
    assertThat(multimap).containsExactly("hello", "there");
  }

  public void testWithEvenArgs() {
    assertThat(multimap).containsExactly("hello", "there", "hello", "there");
  }

  public void testWithVarargs(Object... args) {
    assertThat(multimap).containsExactly("hello", args);
    assertThat(multimap).containsExactly("hello", "world", args);
  }

  public void testWithArray() {
    String[] arg = {"hello", "there"};
    assertThat(multimap).containsExactly("yolo", arg);

    String key = "hello";
    Object[] value = new Object[] {};
    Object[][] args = new Object[][] {};

    assertThat(multimap).containsExactly(key, value);
    assertThat(multimap).containsExactly(key, value, (Object[]) args);
    assertThat(multimap).containsExactly(key, value, key, value, key, value);
  }
}
