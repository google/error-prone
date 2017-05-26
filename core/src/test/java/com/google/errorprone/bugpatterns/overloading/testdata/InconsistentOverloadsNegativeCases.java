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

public final class InconsistentOverloadsNegativeCases {

  public void foo(Object object) {}

  public void foo(Object object, int x, int y) {}

  public void foo(Object object, int x, int y, String string) {}

  public void bar(int x, int y, int z) {}

  public void bar(int x) {}

  public void bar(int x, int y) {}

  public void baz(String string) {}

  public void baz(int x, int y, String otherString) {}

  public void baz(int x, int y, String otherString, Object object) {}

  public void quux(int x, int y, int z) {}

  public void quux(int x, int y, String string) {}

  public void norf(int x, int y) {}

  public void norf(Object object, String string) {}
}
