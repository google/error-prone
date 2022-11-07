/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.util;

/** JDK version string utilities. */
public final class RuntimeVersion {

  private static final int FEATURE = Runtime.version().feature();

  /** Returns true if the current runtime is JDK 12 or newer. */
  public static boolean isAtLeast12() {
    return FEATURE >= 12;
  }

  /** Returns true if the current runtime is JDK 13 or newer. */
  public static boolean isAtLeast13() {
    return FEATURE >= 13;
  }

  /** Returns true if the current runtime is JDK 14 or newer. */
  public static boolean isAtLeast14() {
    return FEATURE >= 14;
  }

  /** Returns true if the current runtime is JDK 15 or newer. */
  public static boolean isAtLeast15() {
    return FEATURE >= 15;
  }

  /** Returns true if the current runtime is JDK 16 or newer. */
  public static boolean isAtLeast16() {
    return FEATURE >= 16;
  }

  /** Returns true if the current runtime is JDK 17 or newer. */
  public static boolean isAtLeast17() {
    return FEATURE >= 17;
  }

  /** Returns true if the current runtime is JDK 18 or newer. */
  public static boolean isAtLeast18() {
    return FEATURE >= 18;
  }

  /** Returns true if the current runtime is JDK 19 or newer. */
  public static boolean isAtLeast19() {
    return FEATURE >= 19;
  }

  /** Returns true if the current runtime is JDK 20 or newer. */
  public static boolean isAtLeast20() {
    return FEATURE >= 20;
  }

  /**
   * Returns the latest {@code --release} version.
   *
   * <p>Prefer the {@code isAtLeast} methods for assumption checks in tests.
   */
  public static int release() {
    return FEATURE;
  }

  private RuntimeVersion() {}
}
