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

/**
 * Tests for self assignment
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class SelfAssignmentPositiveCases1 {
  private int a;

  public void test1(int b) {
    // BUG: Diagnostic contains: this.a = b
    this.a = a;
  }

  public void test2(int b) {
    // BUG: Diagnostic contains: remove this line
    a = this.a;
  }

  public void test3() {
    int a = 0;
    // BUG: Diagnostic contains: this.a = a
    a = a;
  }

  public void test4() {
    // BUG: Diagnostic contains: remove this line
    this.a = this.a;
  }

  public void test5() {
    // BUG: Diagnostic contains: this.a = a
    if ((a = a) != 10) {
      System.out.println("foo");
    }
  }

  // Check that WrappedTreeMap handles folded strings; tested by EndPosTest.
  // See https://code.google.com/p/error-prone/issues/detail?id=209
  public String foldableString() {
    return "foo" + "bar";
  }
}
