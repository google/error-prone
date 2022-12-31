/*
 * Copyright 2023 The Error Prone Authors.
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

import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.util.Context;

/**
 * JDK target version utilities.
 *
 * @see RuntimeVersion
 */
public final class TargetVersion {
  /** Returns true if the compiler targets JRE 11 or newer. */
  public static boolean isAtLeast11(Context context) {
    return majorVersion(context) >= 55;
  }

  /** Returns true if the compiler targets JRE 12 or newer. */
  public static boolean isAtLeast12(Context context) {
    return majorVersion(context) >= 56;
  }

  /** Returns true if the compiler targets JRE 13 or newer. */
  public static boolean isAtLeast13(Context context) {
    return majorVersion(context) >= 57;
  }

  /** Returns true if the compiler targets JRE 14 or newer. */
  public static boolean isAtLeast14(Context context) {
    return majorVersion(context) >= 58;
  }

  /** Returns true if the compiler targets JRE 15 or newer. */
  public static boolean isAtLeast15(Context context) {
    return majorVersion(context) >= 59;
  }

  /** Returns true if the compiler targets JRE 16 or newer. */
  public static boolean isAtLeast16(Context context) {
    return majorVersion(context) >= 60;
  }

  /** Returns true if the compiler targets JRE 17 or newer. */
  public static boolean isAtLeast17(Context context) {
    return majorVersion(context) >= 61;
  }

  /** Returns true if the compiler targets JRE 18 or newer. */
  public static boolean isAtLeast18(Context context) {
    return majorVersion(context) >= 62;
  }

  /** Returns true if the compiler targets JRE 19 or newer. */
  public static boolean isAtLeast19(Context context) {
    return majorVersion(context) >= 63;
  }

  /** Returns true if the compiler targets JRE 20 or newer. */
  public static boolean isAtLeast20(Context context) {
    return majorVersion(context) >= 64;
  }

  /** Returns true if the compiler targets JRE 21 or newer. */
  public static boolean isAtLeast21(Context context) {
    return majorVersion(context) >= 65;
  }

  private static int majorVersion(Context context) {
    return Target.instance(context).majorVersion;
  }

  private TargetVersion() {}
}
