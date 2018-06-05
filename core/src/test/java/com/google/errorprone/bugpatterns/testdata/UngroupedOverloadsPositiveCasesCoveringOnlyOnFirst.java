/*
 * Copyright 2017 The Error Prone Authors.
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
public class UngroupedOverloadsPositiveCasesCoveringOnlyOnFirst {

  // BUG: Diagnostic contains: Overloads of 'foo' are not grouped together
  public void foo(int x) {
    System.out.println(x);
  }

  // BUG: Diagnostic contains: Overloads of 'bar' are not grouped together
  public void bar() {
    foo();
  }

  public void baz() {
    bar();
  }

  public void bar(int x) {
    foo(x);
  }

  // BUG: Diagnostic contains: Overloads of 'quux' are not grouped together
  private void quux() {
    norf();
  }

  private void norf() {
    quux();
  }

  public void quux(int x) {
    bar(x);
  }

  public void foo() {
    foo(42);
  }
}
