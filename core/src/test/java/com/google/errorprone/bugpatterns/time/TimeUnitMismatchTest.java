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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author cpovirk@google.com (Chris Povirk)
 */
@RunWith(JUnit4.class)
public class TimeUnitMismatchTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(TimeUnitMismatch.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(TimeUnitMismatch.class, getClass());

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "TimeUnitMismatchPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.time.testdata;

            import static java.util.concurrent.TimeUnit.NANOSECONDS;

            import java.util.Optional;

            /**
             * @author cpovirk@google.com (Chris Povirk)
             */
            public class TimeUnitMismatchPositiveCases {
              int startMicros;
              int stopMillis;

              void fields() {
                // BUG: Diagnostic contains: expected microseconds but was milliseconds
                startMicros = stopMillis;

                // BUG: Diagnostic contains: If it instead means microseconds
                startMicros = stopMillis;

                // BUG: Diagnostic contains: MILLISECONDS.toMicros(stopMillis)
                startMicros = stopMillis;
              }

              void memberSelect() {
                // BUG: Diagnostic contains: expected microseconds but was milliseconds
                this.startMicros = this.stopMillis;
              }

              void locals() {
                int millis = 0;
                // BUG: Diagnostic contains: expected microseconds but was milliseconds
                startMicros = millis;
              }

              long getMicros() {
                return 0;
              }

              void returns() {
                // BUG: Diagnostic contains: expected nanoseconds but was microseconds
                long fooNano = getMicros();
              }

              void doSomething(double startSec, double endSec) {}

              void setMyMillis(int timeout) {}

              void args() {
                double ms = 0;
                double ns = 0;
                // BUG: Diagnostic contains: expected seconds but was milliseconds
                doSomething(ms, ns);
                // BUG: Diagnostic contains: expected seconds but was nanoseconds
                doSomething(ms, ns);

                // BUG: Diagnostic contains: expected milliseconds but was nanoseconds
                setMyMillis((int) ns);
              }

              void timeUnit() {
                int micros = 0;
                // BUG: Diagnostic contains: expected nanoseconds but was microseconds
                NANOSECONDS.toMillis(micros);
              }

              class Foo {
                Foo(long seconds) {}
              }

              void constructor() {
                int nanos = 0;
                // BUG: Diagnostic contains: expected seconds but was nanoseconds
                new Foo(nanos);
              }

              void boxed() {
                Long nanos = 0L;
                // BUG: Diagnostic contains: expected milliseconds but was nanoseconds
                long millis = nanos;
              }

              void optionalGet() {
                Optional<Long> maybeNanos = Optional.of(0L);
                // BUG: Diagnostic contains: expected milliseconds but was nanoseconds
                long millis = maybeNanos.get();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .addSourceLines(
            "TimeUnitMismatchNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.time.testdata;

            import static java.util.concurrent.TimeUnit.NANOSECONDS;

            import java.util.Optional;

            /**
             * @author cpovirk@google.com (Chris Povirk)
             */
            public class TimeUnitMismatchNegativeCases {
              static final int THE_MILLIS = 0;
              int startMillis;
              int stopMillis;

              void fields() {
                startMillis = THE_MILLIS;

                startMillis = stopMillis;
              }

              void memberSelect() {
                this.startMillis = this.stopMillis;
              }

              void locals() {
                int millis = 0;
                startMillis = millis;
              }

              long getMicros() {
                return 0;
              }

              void returns() {
                long fooUs = getMicros();
              }

              void doSomething(double startSec, double endSec) {}

              void args() {
                double seconds = 0;
                doSomething(seconds, seconds);
              }

              void timeUnit() {
                int nanos = 0;
                NANOSECONDS.toMillis(nanos);
              }

              class Foo {
                Foo(long seconds) {}
              }

              void constructor() {
                int seconds = 0;
                new Foo(seconds);
              }

              String milliseconds() {
                return "0";
              }

              void nonNumeric() {
                String seconds = milliseconds();
              }

              void boxed() {
                Long startNanos = 0L;
                long endNanos = startNanos;
              }

              void optionalGet() {
                Optional<Long> maybeNanos = Optional.of(0L);
                long nanos = maybeNanos.get();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void mismatchedTypeAfterManualConversion() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: MICROSECONDS.toMillis(getMicros())
              long fooMillis = getMicros() * 1000;
              // BUG: Diagnostic contains: MICROSECONDS.toMillis(getMicros())
              long barMillis = 1000 * getMicros();
              // BUG: Diagnostic contains:
              long fooNanos = getMicros() / 1000;
              // BUG: Diagnostic contains: SECONDS.toNanos(getSeconds())
              long barNanos = getSeconds() * 1000 * 1000;

              long getMicros() {
                return 1;
              }

              long getSeconds() {
                return 1;
              }

              void setMillis(long x) {}

              void test(int timeMicros) {
                // BUG: Diagnostic contains:
                setMillis(timeMicros * 1000);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void noopConversion_isRemoved() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              long fooMicros = getMicros() * 1000;

              long getMicros() {
                return 1;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              long fooMicros = getMicros();

              long getMicros() {
                return 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void zeroMultiplier_noComplaint() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              static int MILLIS_PER_MINUTE = 42;
              long fooMicros = 0 * MILLIS_PER_MINUTE;
            }
            """)
        .doTest();
  }

  @Test
  public void matchedTypeAfterManualConversion() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              long fooNanos = getMicros() * 1000;
              long fooMillis = getMicros() / 1000;

              long getMicros() {
                return 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void binaryTree() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract long getStartMillis();

              abstract long getEndMillis();

              abstract long getStartNanos();

              abstract long getEndNanos();

              void test() {
                var b1 = getStartMillis() < getEndMillis();
                var b2 = getStartNanos() + getEndNanos();
                // BUG: Diagnostic contains: MILLISECONDS.toNanos(getStartMillis()) < getEndNanos()
                var b3 = getStartMillis() < getEndNanos();
                // BUG: Diagnostic contains: MILLISECONDS.toNanos(getStartMillis()) + getEndNanos()
                var b4 = getStartMillis() + getEndNanos();
                // BUG: Diagnostic contains: MILLISECONDS.toNanos(getStartMillis()) + getEndNanos()
                var b5 = getStartMillis() * 1000 + getEndNanos();
                var b6 = getStartMillis() * 1_000_000 + getEndNanos();
              }
            }
            """)
        .doTest();
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
        "millisecs",
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
