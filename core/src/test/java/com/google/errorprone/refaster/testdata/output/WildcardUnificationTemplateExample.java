/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.errorprone.refaster.testdata;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;

/**
 * Test data for {@code WildcardUnificationTemplate}.
 * 
 * @author kak@google.com (Kurt Kluever)
 */
public class WildcardUnificationTemplateExample {
  public void example() {
    ImmutableList<String> actual = ImmutableList.of("kurt", "kluever");
    ImmutableList<String> expected = ImmutableList.of("kluever", "kurt");
    assertThat(actual).containsExactlyElementsIn(expected);
    
  }
}
