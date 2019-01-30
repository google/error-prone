/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.time;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.errorprone.bugpatterns.time.TimeUnitMismatch.unitSuggestedByName;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cpovirk@google.com (Chris Povirk) */
@RunWith(JUnit4.class)
public class TimeUnitMismatchTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(TimeUnitMismatch.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("TimeUnitMismatchPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("TimeUnitMismatchNegativeCases.java").doTest();
  }

  @Test
  public void testUnitSuggestedByName() {
    assertSeconds("sleepSec", "deadlineSeconds", "secondsTimeout", "msToS");
    assertUnknown(
        "second",
        "getSecond",
        "SECOND",
        "secondDeadline",
        "twoSeconds",
        "THIRTY_SECONDS",
        "fromSeconds",
        "x",
        "millisMicros");
    assertMillis(
        "millis",
        "MILLIS",
        "someMillis",
        "timeoutMilli",
        "valueInMills",
        "mSec",
        "deadlineMSec",
        "milliSecond",
        "milliSeconds",
        "FOO_MILLI_SECONDS",
        "dateMS",
        "dateMs",
        "dateMsec",
        "msRemaining");
    assertMicros("micro", "us");
    assertNanos("nano", "secondsPart");
  }

  private static void assertUnknown(String... names) {
    for (String name : names) {
      assertWithMessage("unit for %s", name).that(unitSuggestedByName(name)).isNull();
    }
  }

  private static void assertSeconds(String... names) {
    for (String name : names) {
      assertWithMessage("unit for %s", name).that(unitSuggestedByName(name)).isEqualTo(SECONDS);
    }
  }

  private static void assertMillis(String... names) {
    for (String name : names) {
      assertWithMessage("unit for %s", name)
          .that(unitSuggestedByName(name))
          .isEqualTo(MILLISECONDS);
    }
  }

  private static void assertMicros(String... names) {
    for (String name : names) {
      assertWithMessage("unit for %s", name)
          .that(unitSuggestedByName(name))
          .isEqualTo(MICROSECONDS);
    }
  }

  private static void assertNanos(String... names) {
    for (String name : names) {
      assertWithMessage("unit for %s", name).that(unitSuggestedByName(name)).isEqualTo(NANOSECONDS);
    }
  }
}
