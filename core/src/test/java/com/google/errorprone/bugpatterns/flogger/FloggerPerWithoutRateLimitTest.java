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
package com.google.errorprone.bugpatterns.flogger;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FloggerPerWithoutRateLimitTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FloggerPerWithoutRateLimit.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.flogger.FluentLogger;

            class Test {
              private static final FluentLogger logger = FluentLogger.forEnclosingClass();

              enum E {
                ONE,
                TWO;
              }

              public void test() {
                // BUG: Diagnostic contains: per() methods are no-ops
                logger.atInfo().per(E.ONE).log("foo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.flogger.FluentLogger;
            import java.util.concurrent.TimeUnit;

            class Test {
              private static final FluentLogger logger = FluentLogger.forEnclosingClass();

              enum E {
                ONE,
                TWO;
              }

              public void test() {
                logger.atInfo().log("foo");
                logger.atInfo().per(E.ONE).atMostEvery(1, TimeUnit.HOURS).log("foo");
                logger.atInfo().atMostEvery(1, TimeUnit.HOURS).per(E.ONE).log("foo");
                logger.atInfo().per(E.ONE).every(10).log("foo");
              }
            }
            """)
        .doTest();
  }
}
