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

import java.util.HashMap;
import java.util.Map;

/**
 * Negative test cases for {@link ShouldHaveEvenArgs} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class ShouldHaveEvenArgsNegativeCases {

  private static final Map<String, String> map = new HashMap<String, String>();

  public void testWithNoArgs() {
    assertThat(map).containsExactly();
  }

  public void testWithMinimalArgs() {
    assertThat(map).containsExactly("hello", "there");
  }

  public void testWithEvenArgs() {
    assertThat(map).containsExactly("hello", "there", "hello", "there");
  }

  public void testWithVarargs(Object... args) {
    assertThat(map).containsExactly("hello", args);
    assertThat(map).containsExactly("hello", "world", args);
  }

  public void testWithArray() {
    String[] arg = {"hello", "there"};
    assertThat(map).containsExactly("yolo", arg);

    String key = "hello";
    Object[] value = new Object[] {};
    Object[][] args = new Object[][] {};

    assertThat(map).containsExactly(key, value);
    assertThat(map).containsExactly(key, value, (Object[]) args);
    assertThat(map).containsExactly(key, value, key, value, key, value);
  }
}
