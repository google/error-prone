/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

/**
 * Negative test cases for {@link SelfEquals} check.
 *
 * @author alexeagle@google.com (Alex Eagle)
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class SelfEqualsNegativeCases {
  private String field;

  @Override
  public int hashCode() {
    return field != null ? field.hashCode() : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SelfEqualsNegativeCases)) {
      return false;
    }

    SelfEqualsNegativeCases other = (SelfEqualsNegativeCases) o;
    return field.equals(other.field);
  }

  public boolean test() {
    return Boolean.TRUE.toString().equals(Boolean.FALSE.toString());
  }

  public void testAssertThatEq(SelfEqualsNegativeCases obj) {
    assertThat(obj).isEqualTo(obj);
  }

  public void testAssertThatNeq(SelfEqualsNegativeCases obj) {
    assertThat(obj).isNotEqualTo(obj);
  }
}
