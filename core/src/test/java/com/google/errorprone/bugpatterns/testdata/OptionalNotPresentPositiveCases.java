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

import java.util.Optional;

/** @author mariasam@google.com on 8/3/17. */
public class OptionalNotPresentPositiveCases {

  public void test(Optional<String> testStr) {
    // BUG: Diagnostic contains: Optional
    if (!testStr.isPresent()) {
      String str = testStr.get();
    }
  }

  public void testMultipleStatements(Optional<String> optional) {
    // BUG: Diagnostic contains: Optional
    if (!optional.isPresent()) {
      String test = "test";
      String str = optional.get();
    }
  }

  public void testNestedIf(Optional<String> optional) {
    // BUG: Diagnostic contains: Optional
    if (!optional.isPresent()) {
      if (optional == Optional.of("")) {
        String str = optional.get();
      }
    }
  }

  public void testAnd(Optional<String> optional) {
    // BUG: Diagnostic contains: Optional
    if (!optional.isPresent() && 7 == 7) {
      String str = optional.get();
    }
  }
}
