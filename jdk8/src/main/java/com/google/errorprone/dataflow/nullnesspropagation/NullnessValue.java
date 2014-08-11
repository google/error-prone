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
 * TODO(user): add static factory methods to NullnessValue that return a cached copy of each
 * value instead of allocating new NullnessValues everywhere. Perhaps use the enum directly as the
 * value.
 *
 * @author deminguyen@google.com (Demi Nguyen)
 */
public class NullnessValue implements AbstractValue<NullnessValue> {

  /**
   * The nullness type of a value
   */
  protected final Type type;

  /**
   * Note: TOP includes null types, i.e. there is no distinction between values known to be null and
   * values that could be anything.
   */
  public enum Type {
    NULLABLE("Nullable"),
    NONNULL("Non-null");

    private String displayName;

    Type(String displayName) {
      this.displayName = displayName;
    }

    @Override
    public String toString() {
      return displayName;
    }
  }

  public NullnessValue(Type type) {
    this.type = type;
  }

  public boolean isNullable() {
    return type == Type.NULLABLE;
  }

  public boolean isNonNull() {
    return type == Type.NONNULL;
  }

  public NullnessValue copy() {
    return new NullnessValue(type);
  }

  @Override
  public NullnessValue leastUpperBound(NullnessValue other) {
    if (other.isNonNull() && this.isNonNull()) {
      return new NullnessValue(Type.NONNULL);
    }
    return new NullnessValue(Type.NULLABLE);
  }

  public NullnessValue greatestLowerBound(NullnessValue other) {
    if (other.isNullable() && this.isNullable()) {
      return new NullnessValue(Type.NULLABLE);
    }
    return new NullnessValue(Type.NONNULL);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof NullnessValue)) {
      return false;
    }
    NullnessValue other = (NullnessValue) obj;
    return type == other.type;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public String toString() {
    return type.toString();
  }
}
