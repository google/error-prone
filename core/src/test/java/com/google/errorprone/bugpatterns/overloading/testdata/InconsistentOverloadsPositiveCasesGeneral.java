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

package com.google.errorprone.bugpatterns.overloading.testdata;

public final class InconsistentOverloadsPositiveCasesGeneral {

  public void foo(Object object) {}

  // BUG: Diagnostic contains: foo(Object object, int i)
  public void foo(int i, Object object) {}

  // BUG: Diagnostic contains: foo(Object object, int i, String string)
  public void foo(String string, Object object, int i) {}

  // BUG: Diagnostic contains: bar(int i, int j, String x, String y, Object object)
  public void bar(Object object, String x, String y, int i, int j) {}

  public void bar(int i, int j) {}

  // BUG: Diagnostic contains: bar(int i, int j, String x, String y)
  public void bar(String x, String y, int i, int j) {}

  public void baz(int i, int j) {}

  public void baz(Object object) {}

  // BUG: Diagnostic contains: baz(int i, int j, String x, Object object)
  public void baz(String x, int i, int j, Object object) {}

  public void quux(int x, int y, String string) {}

  // BUG: Diagnostic contains: quux(int x, int y, Object object)
  public void quux(Object object, int y, int x) {}
}
