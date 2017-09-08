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
import java.util.function.Predicate;

/** Created by mariasam on 8/3/17. */
public class OptionalNotPresentNegativeCases {

  // Test this doesn't trigger NullPointerException
  private final Predicate<Optional<?>> asField = o -> !o.isPresent();

  public void testBasic(Optional<String> optional) {
    if (optional.get() != null) {
      String str = optional.get();
    }
  }

  public void testNested(Optional<String> optional) {
    if (7 == 7) {
      String str = optional.get();
      if (!optional.isPresent()) {
        System.out.println("test");
      }
    }
  }

  public void testOptional(Optional<String> optional) {
    if (!optional.isPresent()) {
      optional = Optional.of("value");
      String str = optional.get();
    }
  }

  public void afterIf(Optional<String> optional) {
    if (!optional.isPresent()) {
      optional = Optional.of("value");
    }
    String str = optional.get();
  }

  public void checkMultipleInIf(Optional<String> optional) {
    if (!optional.isPresent() || 7 == 7) {
      String str = optional.isPresent() ? optional.get() : "";
    }
  }

  public void checkInIf(Optional<String> optional) {
    if (!optional.isPresent()) {
      fillUpOptional(optional);
      String str = optional.get();
    }
  }

  private void fillUpOptional(Optional<String> optional) {
    optional = Optional.of("");
  }
}
