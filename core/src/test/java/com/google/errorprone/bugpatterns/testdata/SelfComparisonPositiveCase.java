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
 * Positive test case for {@link SelfComparison} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class SelfComparisonPositiveCase implements Comparable<Object> {

  public int test1() {
    SelfComparisonPositiveCase obj = new SelfComparisonPositiveCase();
    // BUG: Diagnostic contains: An object is compared to itself
    return obj.compareTo(obj);
  }

  private SelfComparisonPositiveCase obj = new SelfComparisonPositiveCase();

  public int test2() {
    // BUG: Diagnostic contains: An object is compared to itself
    return obj.compareTo(this.obj);
  }

  public int test3() {
    // BUG: Diagnostic contains: An object is compared to itself
    return this.obj.compareTo(obj);
  }

  public int test4() {
    // BUG: Diagnostic contains: An object is compared to itself
    return this.obj.compareTo(this.obj);
  }

  public int test5() {
    // BUG: Diagnostic contains: An object is compared to itself
    return compareTo(this);
  }

  @Override
  public int compareTo(Object o) {
    return 0;
  }

  public static class ComparisonTest implements Comparable<ComparisonTest> {
    private String testField;

    @Override
    public int compareTo(ComparisonTest s) {
      return testField.compareTo(s.testField);
    }

    public int test1() {
      ComparisonTest obj = new ComparisonTest();
      // BUG: Diagnostic contains: An object is compared to itself
      return obj.compareTo(obj);
    }

    private ComparisonTest obj = new ComparisonTest();

    public int test2() {
      // BUG: Diagnostic contains: An object is compared to itself
      return obj.compareTo(this.obj);
    }

    public int test3() {
      // BUG: Diagnostic contains: An object is compared to itself
      return this.obj.compareTo(obj);
    }

    public int test4() {
      // BUG: Diagnostic contains: An object is compared to itself
      return this.obj.compareTo(this.obj);
    }

    public int test5() {
      // BUG: Diagnostic contains: An object is compared to itself
      return compareTo(this);
    }
  }
}
