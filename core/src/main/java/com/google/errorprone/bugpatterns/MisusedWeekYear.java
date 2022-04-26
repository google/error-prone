/*
 * Copyright 2015 The Error Prone Authors.
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

/**
 * Ban use of YYYY in a SimpleDateFormat pattern, unless it is being used for a week date. Otherwise
 * the user almost certainly meant yyyy instead. See the summary in the {@link BugPattern} below for
 * more details.
 *
 * <p>This bug caused a Twitter outage in December 2014.
 */
@BugPattern(
    summary =
        "Use of \"YYYY\" (week year) in a date pattern without \"ww\" (week in year). "
            + "You probably meant to use \"yyyy\" (year) instead.",
    severity = ERROR)
public final class MisusedWeekYear extends MisusedDateFormat {
  @Override
  Optional<String> rewriteTo(String pattern) {
    boolean[] containsY = new boolean[1];
    boolean[] containsW = new boolean[1];
    parseDateFormat(
        pattern,
        new DateFormatConsumer() {
          @Override
          public void consumeLiteral(char literal) {}

          @Override
          public void consumeSpecial(char special) {
            if (special == 'Y') {
              containsY[0] = true;
            }
            if (special == 'w') {
              containsW[0] = true;
            }
          }
        });
    if (containsY[0] && !containsW[0]) {
      return Optional.of(replaceFormatChar(pattern, 'Y', 'y'));
    }
    return Optional.empty();
  }
}
