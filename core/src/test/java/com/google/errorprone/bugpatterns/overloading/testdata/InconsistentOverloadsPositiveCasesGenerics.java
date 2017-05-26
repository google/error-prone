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

import java.util.List;

public final class InconsistentOverloadsPositiveCasesGenerics {

  // BUG: Diagnostic contains: foo(List<Integer> numbers, List<List<Integer>> nestedNumbers)
  public void foo(List<List<Integer>> nestedNumbers, List<Integer> numbers) {}

  public void foo(List<Integer> numbers) {}

  // BUG: Diagnostic contains: foo(Iterable<Integer> numbers, String description)
  public void foo(String description, Iterable<Integer> numbers) {}

  public void bar(int x) {}

  // BUG: Diagnostic contains: bar(int x, List<? extends java.util.ArrayList<String>> strings)
  public void bar(List<? extends java.util.ArrayList<String>> strings, int x) {}
}
