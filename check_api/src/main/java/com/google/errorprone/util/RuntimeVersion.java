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

import java.lang.reflect.Method;

/** JDK version string utilities. */
public class RuntimeVersion {

  private static final int MAJOR = getMajor();

  private static int getMajor() {
    try {
      Method versionMethod = Runtime.class.getMethod("version");
      Object version = versionMethod.invoke(null);
      return (int) version.getClass().getMethod("major").invoke(version);
    } catch (Exception e) {
      // continue below
    }

    int version = (int) Double.parseDouble(System.getProperty("java.class.version"));
    if (49 <= version && version <= 52) {
      return version - (49 - 5);
    }
    throw new IllegalStateException(
        "Unknown Java version: " + System.getProperty("java.specification.version"));
  }

  /** Returns true if the current runtime is JDK 8 or newer. */
  public static boolean isAtLeast8() {
    return MAJOR >= 8;
  }

  /** Returns true if the current runtime is JDK 9 or newer. */
  public static boolean isAtLeast9() {
    return MAJOR >= 9;
  }

  /** Returns true if the current runtime is JDK 10 or newer. */
  public static boolean isAtLeast10() {
    return MAJOR >= 10;
  }

  /** Returns true if the current runtime is JDK 10 or earlier. */
  public static boolean isAtMost10() {
    return MAJOR <= 10;
  }

  /** Returns true if the current runtime is JDK 11 or newer. */
  public static boolean isAtLeast11() {
    return MAJOR >= 11;
  }

  /** Returns true if the current runtime is JDK 12 or newer. */
  public static boolean isAtLeast12() {
    return MAJOR >= 12;
  }

  /** Returns true if the current runtime is JDK 13 or newer. */
  public static boolean isAtLeast13() {
    return MAJOR >= 13;
  }

  /** Returns true if the current runtime is JDK 14 or newer. */
  public static boolean isAtLeast14() {
    return MAJOR >= 14;
  }

  /** Returns true if the current runtime is JDK 15 or newer. */
  public static boolean isAtLeast15() {
    return MAJOR >= 15;
  }
}
