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
package com.google.errorprone.bugpatterns.time;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class TimeInStaticInitializerTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(TimeInStaticInitializer.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.time.Instant;

            class Test {
              // BUG: Diagnostic contains:
              private static final Instant NOW = Instant.now();
            }
            """)
        .doTest();
  }

  @Test
  public void negative_instanceField() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.time.Instant;

            class Test {
              private final Instant now = Instant.now();
            }
            """)
        .doTest();
  }

  @Test
  public void negative_inMethod() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.time.Instant;

            class Test {
              public static Instant now() {
                return Instant.now();
              }
            }
            """)
        .doTest();
  }
}
