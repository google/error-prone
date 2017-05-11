/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import com.google.auto.value.AutoValue;

/**
 * Encapsulates information about a positive or negative test case that should be included in the
 * generated documentation.
 */
@AutoValue
public abstract class ExampleInfo {

  public enum ExampleKind {
    POSITIVE,
    NEGATIVE
  }

  /** Whether this is a positive or negative example. */
  public abstract ExampleKind type();

  /** The fully-qualified name of the checker that this example belongs to. */
  public abstract String checkerClass();

  /**
   * The name of the example, e.g. "ArrayEqualsPositiveCases.java" or
   * "testFlagsSimpleCovariantEqualsMethod".
   */
  public abstract String name();

  /** The example code. */
  public abstract String code();

  public static ExampleInfo create(
      ExampleKind type, String checkerClass, String name, String code) {
    return new AutoValue_ExampleInfo(type, checkerClass, name, code);
  }
}
