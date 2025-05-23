/*
 * Copyright 2018 The Error Prone Authors.
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ProtoDurationGetSecondsGetNano}.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@RunWith(JUnit4.class)
@Ignore("b/130667208")
public class ProtoDurationGetSecondsGetNanoTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ProtoDurationGetSecondsGetNano.class, getClass());

  @Test
  public void getSecondsWithGetNanos() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              public static void foo(Duration duration) {
                long seconds = duration.getSeconds();
                int nanos = duration.getNanos();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getSecondsWithGetNanosInReturnType() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.common.collect.ImmutableMap;
            import com.google.protobuf.Duration;

            public class TestCase {
              public static int foo(Duration duration) {
                // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                return duration.getNanos();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getSecondsWithGetNanosInReturnType2() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
"""
package test;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Duration;

public class TestCase {
  public static ImmutableMap<String, Object> foo(Duration duration) {
    return ImmutableMap.of("seconds", duration.getSeconds(), "nanos", duration.getNanos());
  }
}
""")
        .doTest();
  }

  @Test
  public void getSecondsWithGetNanosDifferentScope() {
    // Ideally we would also catch cases like this, but it requires scanning "too much" of the class
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              public static void foo(Duration duration) {
                long seconds = duration.getSeconds();
                if (true) {
                  // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                  int nanos = duration.getNanos();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getSecondsWithGetNanosInDifferentMethods() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              public static void foo(Duration duration) {
                long seconds = duration.getSeconds();
              }

              public static void bar(Duration duration) {
                // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                int nanos = duration.getNanos();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getSecondsOnly() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              public static void foo(Duration duration) {
                long seconds = duration.getSeconds();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getNanoOnly() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              public static void foo(Duration duration) {
                // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                int nanos = duration.getNanos();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getNanoInMethodGetSecondsInClassVariable() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              private static final Duration DURATION = Duration.getDefaultInstance();
              private static final long seconds = DURATION.getSeconds();

              public static void foo() {
                // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                int nanos = DURATION.getNanos();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getSecondsOnlyInStaticBlock() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              static {
                long seconds = Duration.getDefaultInstance().getSeconds();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getNanoOnlyInStaticBlock() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              static {
                // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                int nanos = Duration.getDefaultInstance().getNanos();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getSecondsOnlyInClassBlock() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              private final Duration DURATION = Duration.getDefaultInstance();
              private final long seconds = DURATION.getSeconds();
              private final int nanos = DURATION.getNanos();
            }
            """)
        .doTest();
  }

  @Test
  public void getNanoOnlyInClassBlock() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
              private final int nanos = Duration.getDefaultInstance().getNanos();
            }
            """)
        .doTest();
  }

  @Test
  public void getNanoInInnerClassGetSecondsInMethod() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              private static final Duration DURATION = Duration.getDefaultInstance();

              public static void foo() {
                long seconds = DURATION.getSeconds();
                Object obj =
                    new Object() {
                      @Override
                      public String toString() {
                        // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                        return String.valueOf(DURATION.getNanos());
                      }
                    };
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getNanoInInnerClassGetSecondsInClassVariable() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              Duration DURATION = Duration.getDefaultInstance();
              long seconds = DURATION.getSeconds();
              Object obj =
                  new Object() {
                    // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                    long nanos = DURATION.getNanos();
                  };
            }
            """)
        .doTest();
  }

  @Test
  public void getNanoInMethodGetSecondsInLambda() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              private static final Duration DURATION = Duration.getDefaultInstance();

              public static void foo() {
                Runnable r = () -> DURATION.getSeconds();
                // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                int nanos = DURATION.getNanos();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getSecondsInLambda() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;
            import java.util.function.Supplier;

            public class TestCase {
              private static final Duration DURATION = Duration.getDefaultInstance();

              public void foo() {
                doSomething(() -> DURATION.getSeconds());
                // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                int nanos = DURATION.getNanos();
              }

              public void doSomething(Supplier<Long> supplier) {}
            }
            """)
        .doTest();
  }

  @Test
  public void getNanoInLambda() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.protobuf.Duration;

            public class TestCase {
              private static final Duration DURATION = Duration.getDefaultInstance();

              public static void foo() {
                // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                Runnable r = () -> DURATION.getNanos();
                long seconds = DURATION.getSeconds();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getMessageGetSecondsGetNanos() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.errorprone.bugpatterns.time.Test.DurationTimestamp;

            public class TestCase {
              public static void foo(DurationTimestamp durationTimestamp) {
                long seconds = durationTimestamp.getTestDuration().getSeconds();
                int nanos = durationTimestamp.getTestDuration().getNanos();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getNestedMessageGetSecondsGetNanos() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.errorprone.bugpatterns.time.Test.DurationTimestamp;

            public class TestCase {
              public static void foo(DurationTimestamp dt) {
                long seconds = dt.getNestedMessage().getNestedTestDuration().getSeconds();
                int nanos = dt.getNestedMessage().getNestedTestDuration().getNanos();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getNestedMessageGetSecondsGetNanos_onDifferentProtoInstances() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.errorprone.bugpatterns.time.Test.DurationTimestamp;

            public class TestCase {
              public static void foo(DurationTimestamp dt1, DurationTimestamp dt2) {
                long seconds = dt1.getNestedMessage().getNestedTestDuration().getSeconds();
                // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                int nanos = dt2.getNestedMessage().getNestedTestDuration().getNanos();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getMessageGetSecondsGetNanosDifferentSubMessage() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            """
            package test;

            import com.google.errorprone.bugpatterns.time.Test.DurationTimestamp;

            public class TestCase {
              public static void foo(DurationTimestamp durationTimestamp) {
                long seconds = durationTimestamp.getTestDuration().getSeconds();
                // BUG: Diagnostic contains: ProtoDurationGetSecondsGetNano
                int nanos = durationTimestamp.getAnotherTestDuration().getNanos();
              }
            }
            """)
        .doTest();
  }
}
