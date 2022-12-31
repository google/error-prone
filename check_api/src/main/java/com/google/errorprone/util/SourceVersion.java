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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.util.Context;
import java.util.Arrays;

/**
 * JDK source version utilities.
 *
 * @see RuntimeVersion
 */
public final class SourceVersion {
  private static final ImmutableMap<String, Feature> KNOWN_FEATURES =
      Maps.uniqueIndex(Arrays.asList(Feature.values()), Enum::name);

  /** Returns true if the compiler source version level supports switch expressions. */
  public static boolean supportsSwitchExpressions(Context context) {
    return supportsFeature("SWITCH_EXPRESSION", context);
  }

  /** Returns true if the compiler source version level supports text blocks. */
  public static boolean supportsTextBlocks(Context context) {
    return supportsFeature("TEXT_BLOCKS", context);
  }

  /**
   * Returns true if the compiler source version level supports the {@link Feature} indicated by the
   * specified string.
   *
   * @apiNote For features explicitly recognized by this class, prefer calling the associated method
   *     instead.
   */
  public static boolean supportsFeature(String featureString, Context context) {
    Feature feature = KNOWN_FEATURES.get(featureString);
    return feature != null && feature.allowedInSource(Source.instance(context));
  }

  private SourceVersion() {}
}
