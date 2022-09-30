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

/** Includes true-positive and false-negative cases. */
public class OptionalNotPresentPositiveCases {

  // False-negative
  public String getWhenUnknown(Optional<String> optional) {
    return optional.get();
  }

  // False-negative
  public String getWhenUnknown_testNull(Optional<String> optional) {
    if (optional.get() != null) {
      return optional.get();
    }
    return "";
  }

  // False-negative
  public String getWhenAbsent_testAndNestUnrelated(Optional<String> optional) {
    if (true) {
      String str = optional.get();
      if (!optional.isPresent()) {
        return "";
      }
      return str;
    }
    return "";
  }

  public String getWhenAbsent(Optional<String> testStr) {
    if (!testStr.isPresent()) {
      // BUG: Diagnostic contains: Optional
      return testStr.get();
    }
    return "";
  }

  public String getWhenAbsent_multipleStatements(Optional<String> optional) {
    if (!optional.isPresent()) {
      String test = "test";
      // BUG: Diagnostic contains: Optional
      return test + optional.get();
    }
    return "";
  }

  // False-negative
  public String getWhenAbsent_nestedCheck(Optional<String> optional) {
    if (!optional.isPresent() || true) {
      return !optional.isPresent() ? optional.get() : "";
    }
    return "";
  }

  public String getWhenAbsent_compoundIf_false(Optional<String> optional) {
    if (!optional.isPresent() && true) {
      // BUG: Diagnostic contains: Optional
      return optional.get();
    }
    return "";
  }

  // False-negative
  public String getWhenAbsent_compoundIf_true(Optional<String> optional) {
    if (!optional.isPresent() || true) {
      return optional.get();
    }
    return "";
  }

  public String getWhenAbsent_elseClause(Optional<String> optional) {
    if (optional.isPresent()) {
      return optional.get();
    } else {
      // BUG: Diagnostic contains: Optional
      return optional.get();
    }
  }

  // False-negative
  public String getWhenAbsent_localReassigned(Optional<String> optional) {
    if (!optional.isPresent()) {
      optional = Optional.empty();
    }
    return optional.get();
  }

  // False-negative
  public String getWhenAbsent_methodScoped(Optional<String> optional) {
    if (optional.isPresent()) {
      return "";
    }
    return optional.get();
  }
}
