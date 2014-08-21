/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.dataflow.nullnesspropagation;

import org.checkerframework.dataflow.analysis.AbstractValue;

/**
 * The type system for nullness tracking and propagation
 *
 * @author deminguyen@google.com (Demi Nguyen)
 */
public enum NullnessValue implements AbstractValue<NullnessValue> {
  /**
   * Note: This includes null types, i.e., there is no distinction between values known to be null
   * and values that could be anything.
   */
  NULLABLE("Nullable"),
  NONNULL("Non-null");

  private final String displayName;
  
  NullnessValue(String displayName) {
    this.displayName = displayName;
  }

  public boolean isNullable() {
    return this == NULLABLE;
  }

  public boolean isNonNull() {
    return this == NONNULL;
  }

  @Override
  public NullnessValue leastUpperBound(NullnessValue other) {
    if (isNonNull() && other.isNonNull()) {
      return NONNULL;
    }
    return NULLABLE;
  }
  
  public NullnessValue greatestLowerBound(NullnessValue other) {
    if (isNullable() && other.isNullable()) {
      return NULLABLE;
    }
    return NONNULL;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
