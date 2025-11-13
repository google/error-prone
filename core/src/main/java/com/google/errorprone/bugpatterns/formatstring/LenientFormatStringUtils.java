/*
 * Copyright 2025 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.formatstring;

import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static java.util.regex.Pattern.compile;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;

/** Utilities relating to lenient format strings. */
public final class LenientFormatStringUtils {

  /**
   * Returns the index of the lenient format string parameter in {@code tree}, or {@code -1} if
   * there is none.
   */
  public static int getLenientFormatStringPosition(ExpressionTree tree, VisitorState state) {
    for (LenientFormatMethod method : LENIENT_FORMATTING_METHODS) {
      if (method.matcher().matches(tree, state)) {
        return method.formatStringPosition;
      }
    }
    return -1;
  }

  private static final ImmutableList<LenientFormatMethod> LENIENT_FORMATTING_METHODS =
      ImmutableList.of(
          new LenientFormatMethod(
              staticMethod()
                  .onClass("com.google.common.base.Preconditions")
                  .withNameMatching(compile("^check(?!ElementIndex|PositionIndex).*")),
              1),
          new LenientFormatMethod(
              staticMethod()
                  .onClass("com.google.common.base.Verify")
                  .withNameMatching(compile("^verify.*")),
              1),
          new LenientFormatMethod(
              staticMethod().onClass("com.google.common.base.Strings").named("lenientFormat"), 0),
          new LenientFormatMethod(
              staticMethod().onClass("com.google.common.truth.Truth").named("assertWithMessage"),
              0),
          new LenientFormatMethod(
              instanceMethod().onDescendantOf("com.google.common.truth.Subject").named("check"), 0),
          new LenientFormatMethod(
              instanceMethod()
                  .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
                  .named("withMessage"),
              0));

  /**
   * @param formatStringPosition position of the format string; we assume every argument afterwards
   *     is a format argument.
   */
  private record LenientFormatMethod(Matcher<ExpressionTree> matcher, int formatStringPosition) {}

  private LenientFormatStringUtils() {}
}
