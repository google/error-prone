/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/**
 * Negative test cases for {@link SelfComparison} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class SelfComparisonNegativeCases implements Comparable<Object> {
  private String field;

  @Override
  public int hashCode() {
    return field != null ? field.hashCode() : 0;
  }

  @Override
  public int compareTo(Object o) {
    if (!(o instanceof SelfComparisonNegativeCases)) {
      return -1;
    }

    SelfComparisonNegativeCases other = (SelfComparisonNegativeCases) o;
    return field.compareTo(other.field);
  }

  public int test() {
    return Boolean.TRUE.toString().compareTo(Boolean.FALSE.toString());
  }

  public static class CopmarisonTest implements Comparable<CopmarisonTest> {
    private String testField;

    @Override
    public int compareTo(CopmarisonTest obj) {
      return testField.compareTo(obj.testField);
    }
  }
}
