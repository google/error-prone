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
class UngroupedOverloadsRefactoringMultiple {

  public void foo() {}

  public void foo(int x) {}

  private static class foo {}

  public void foo(int x, int y) {}

  public void bar() {}

  public static final String BAZ = "baz";

  public void foo(int x, int y, int z) {}

  public void quux() {}

  public void quux(int x) {}

  public static final int X = 0;
  public static final int Y = 1;

  public void quux(int x, int y) {}

  private int quux;

  public void norf() {}

  public void quux(int x, int y, int z) {}

  public void thud() {}
}
