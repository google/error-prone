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

/**
 * JDK source version utilities.
 *
 * @see RuntimeVersion
 */
public final class SourceVersion {
  /** Returns true if the compiler source version level supports switch expressions. */
  public static boolean supportsSwitchExpressions(Context context) {
    return sourceIsAtLeast(context, 14);
  }

  /** Returns true if the compiler source version level supports text blocks. */
  public static boolean supportsTextBlocks(Context context) {
    return sourceIsAtLeast(context, 15);
  }

  /** Returns true if the compiler source version level supports effectively final. */
  public static boolean supportsEffectivelyFinal(Context context) {
    return sourceIsAtLeast(context, 8);
  }

  private static boolean sourceIsAtLeast(Context context, int version) {
    Source lowerBound = Source.lookup(Integer.toString(version));
    return lowerBound != null && Source.instance(context).compareTo(lowerBound) >= 0;
  }

  private SourceVersion() {}
}
