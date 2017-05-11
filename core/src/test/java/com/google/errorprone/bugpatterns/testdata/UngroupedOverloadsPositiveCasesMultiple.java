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
// BUG: Diagnostic contains: Overloaded methods ("bar", "norf", "quux") of this class are not
// grouped together
public class UngroupedOverloadsPositiveCasesMultiple {

  private int foo;

  public void bar(int x, String z, int y) {
    System.out.println(String.format("z: %s, x: %d, y: %d", z, x, y));
  }

  private UngroupedOverloadsPositiveCasesMultiple(int foo) {
    this.foo = foo;
  }

  public void bar(int x) {
    bar(foo, x);
  }

  public void baz(String x) {
    bar(42, x, 42);
  }

  public void bar(int x, int y) {
    bar(y, FOO, x);
  }

  public static final String FOO = "foo";

  public void bar(int x, int y, int z) {
    bar(x, String.valueOf(y), z);
  }

  public int quux() {
    return quux(quux);
  }

  public int quux = 42;

  public int quux(int x) {
    return x + quux;
  }

  private static class Quux {}

  public int quux(int x, int y) {
    return quux(x + y);
  }

  public int norf(int x) {
    return quux(x, x);
  }

  public int norf(int x, int y) {
    return norf(x + y);
  }

  public void foo() {
    System.out.println("foo");
  }

  public void norf(int x, int y, int w) {
    norf(x + w, y + w);
  }
}
