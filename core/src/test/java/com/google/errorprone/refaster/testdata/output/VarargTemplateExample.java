/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster.testdata;

/**
 * Test case for {@code VarargTemplate}.
 * 
 * @author juanch@google.com (Juan Chen)
 */
public class VarargTemplateExample {

  public String foo0() {
    return String.format("first: %s, second: %s");
  }

  public String foo0_empty() {
    return String.format("first: %s, second: %s");
  }

  public String foo1() {
    return String.format("first: %s, second: %s", "first");
  }

  public String foo2() {
    return String.format("first: %s, second: %s", "first", "second");
  }

  public String foo3() {
    return String.format("first: %s, second: %s", "first", "second", "third");
  }
}
