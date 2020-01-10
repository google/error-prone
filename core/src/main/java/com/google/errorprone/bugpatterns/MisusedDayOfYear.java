/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import java.util.Optional;

/** Ban use of D (day-of-year) in a date format pattern that also contains M (month-of-year). */
@BugPattern(
    name = "MisusedDayOfYear",
    summary =
        "Use of 'DD' (day of year) in a date pattern with 'MM' (month of year) is not likely to be"
            + " intentional, as it would lead to dates like 'March 73rd'.",
    severity = ERROR)
public final class MisusedDayOfYear extends MisusedDateFormat {
  @Override
  Optional<String> rewriteTo(String pattern) {
    boolean[] containsD = new boolean[1];
    boolean[] containsM = new boolean[1];
    parseDateFormat(
        pattern,
        new DateFormatConsumer() {
          @Override
          public void consumeLiteral(char literal) {}

          @Override
          public void consumeSpecial(char special) {
            if (special == 'D') {
              containsD[0] = true;
            }
            if (special == 'M') {
              containsM[0] = true;
            }
          }
        });
    if (containsD[0] && containsM[0]) {
      return Optional.of(replaceFormatChar(pattern, 'D', 'd'));
    }
    return Optional.empty();
  }
}
