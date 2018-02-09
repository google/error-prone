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
public class UngroupedOverloadsPositiveCasesInterleaved {

  private int foo;

  // BUG: Diagnostic contains: Overloads of 'bar' are not grouped together
  public void bar(int x, String z, int y) {
    System.out.println(String.format("z: %s, x: %d, y: %d", z, x, y));
  }

  public UngroupedOverloadsPositiveCasesInterleaved(int foo) {
    this.foo = foo;
  }

  // BUG: Diagnostic contains: Overloads of 'bar' are not grouped together
  public void bar(int x) {
    bar(foo, x);
  }

  // BUG: Diagnostic contains: Overloads of 'baz' are not grouped together
  public void baz(String x) {
    baz(x, FOO);
  }

  // BUG: Diagnostic contains: Overloads of 'bar' are not grouped together
  public void bar(int x, int y) {
    bar(y, FOO, x);
  }

  public static final String FOO = "foo";

  // BUG: Diagnostic contains: Overloads of 'baz' are not grouped together
  public void baz(String x, String y) {
    bar(foo, x + y, foo);
  }

  public void foo(int x) {}

  public void foo() {
    foo(foo);
  }
}
