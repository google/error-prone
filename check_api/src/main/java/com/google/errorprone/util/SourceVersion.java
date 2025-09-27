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

import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.util.Context;

/** JDK source version utilities. */
public final class SourceVersion {
  /** Returns true if the compiler source version level supports switch expressions. */
  public static boolean supportsSwitchExpressions(Context context) {
    return sourceIsAtLeast(context, 14);
  }

  /** Returns true if the compiler source version level supports text blocks. */
  public static boolean supportsTextBlocks(Context context) {
    return sourceIsAtLeast(context, 15);
  }

  /** Returns whether the compiler supports pattern-matching instanceofs. */
  public static boolean supportsPatternMatchingInstanceof(Context context) {
    return sourceIsAtLeast(context, 17);
  }

  /** Returns true if the compiler source version level supports static inner classes. */
  public static boolean supportsStaticInnerClass(Context context) {
    return sourceIsAtLeast(context, 16);
  }

  /** Returns true if the compiler source version level supports pattern-matching switches. */
  public static boolean supportsPatternMatchingSwitch(Context context) {
    return sourceIsAtLeast(context, 21);
  }

  /** Returns true if the compiler source version level supports instance main methods. */
  public static boolean supportsInstanceMainMethods(Context context) {
    return sourceIsAtLeast(context, 25);
  }

  private static boolean sourceIsAtLeast(Context context, int version) {
    Source lowerBound = Source.lookup(Integer.toString(version));
    return lowerBound != null && Source.instance(context).compareTo(lowerBound) >= 0;
  }

  private SourceVersion() {}
}
