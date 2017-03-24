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

/** @author hanuszczak@google.com (Łukasz Hanuszczak) */
public class UngroupedOverloadsPositiveCasesSingle {

  public void quux() {
    foo();
  }

  // BUG: Diagnostic contains: Overloaded versions of this method are not grouped together
  public void foo() {
    foo(42);
  }

  public void foo(int x) {
    foo(x, x);
  }

  public void bar() {
    bar(42);
  }

  public void bar(int x) {
    foo(x);
  }

  public void foo(int x, int y) {
    System.out.println(x + y);
  }

  public void norf() {}
}
