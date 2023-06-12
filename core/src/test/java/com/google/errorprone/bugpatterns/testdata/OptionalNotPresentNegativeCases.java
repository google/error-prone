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

import java.util.Optional;
import java.util.function.Predicate;

/** Includes true-negative cases and false-positive cases. */
public class OptionalNotPresentNegativeCases {

  // Test this doesn't trigger NullPointerException
  private final Predicate<Optional<?>> asField = o -> !o.isPresent();

  // False-positive
  public String getWhenTestedSafe_referenceEquality(Optional<String> optional) {
    if (!optional.isPresent()) {
      if (optional == Optional.of("OK")) { // always false
        // BUG: Diagnostic contains: Optional
        return optional.get();
      }
    }
    return "";
  }

  // False-positive
  public String getWhenTestedSafe_equals(Optional<String> optional) {
    if (!optional.isPresent()) {
      if (optional.equals(Optional.of("OK"))) { // always false
        // BUG: Diagnostic contains: Optional
        return optional.get();
      }
    }
    return "";
  }

  public String getWhenPresent_blockReassigned(Optional<String> optional) {
    if (!optional.isPresent()) {
      optional = Optional.of("value");
      return optional.get();
    }
    return "";
  }

  public String getWhenPresent_localReassigned(Optional<String> optional) {
    if (!optional.isPresent()) {
      optional = Optional.of("value");
    }
    return optional.get();
  }

  public String getWhenPresent_nestedCheck(Optional<String> optional) {
    if (!optional.isPresent() || true) {
      return optional.isPresent() ? optional.get() : "";
    }
    return "";
  }
}
