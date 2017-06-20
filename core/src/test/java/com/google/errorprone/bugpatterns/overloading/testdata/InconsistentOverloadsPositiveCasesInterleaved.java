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

public final class InconsistentOverloadsPositiveCasesInterleaved {

  // BUG: Diagnostic contains: baz(int x, String string, int y)
  public void baz(int y, int x, String string) {}

  // BUG: Diagnostic contains: foo(int x, int y, int z, String string)
  public void foo(int x, int z, int y, String string) {}

  public void foo(int x, int y) {}

  public void bar(String string, Object object) {}

  // BUG: Diagnostic contains: baz(int x, String string)
  public void baz(String string, int x) {}

  // BUG: Diagnostic contains: foo(int x, int y, int z)
  public void foo(int z, int x, int y) {}

  // BUG: Diagnostic contains: bar(String string, Object object, int x, int y)
  public void bar(int x, int y, String string, Object object) {}

  public void baz(int x) {}
}
