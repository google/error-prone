/*
 * Copyright 2019 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link StronglyTypeTime}. */
@RunWith(JUnit4.class)
public final class StronglyTypeTimeTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(StronglyTypeTime.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(StronglyTypeTime.class, getClass());

  @Test
  public void findingLocatedOnField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  // BUG: Diagnostic contains: Duration instances",
            "  private static final long FOO_MILLIS = 100;",
            "  public Duration get() {",
            "    return Duration.ofMillis(FOO_MILLIS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void findingOnBoxedField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  private static final Long FOO_MILLIS = 100L;",
            "  public Duration get() {",
            "    return Duration.ofMillis(FOO_MILLIS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void jodaInstantConstructor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import org.joda.time.Instant;",
            "class Test {",
            "  private static final long FOO_MILLIS = 100L;",
            "  public Instant get() {",
            "    return new Instant(FOO_MILLIS);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import org.joda.time.Instant;",
            "class Test {",
            "  private static final Instant FOO = new Instant(100L);",
            "  public Instant get() {",
            "    return FOO;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void jodaInstantStaticFactory() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import org.joda.time.Instant;",
            "class Test {",
            "  private static final long FOO_MILLIS = 100L;",
            "  public Instant get() {",
            "    return Instant.ofEpochMilli(FOO_MILLIS);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import org.joda.time.Instant;",
            "class Test {",
            "  private static final Instant FOO = Instant.ofEpochMilli(100L);",
            "  public Instant get() {",
            "    return FOO;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void protoTimestamp() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.protobuf.Timestamp;",
            "import com.google.protobuf.util.Timestamps;",
            "class Test {",
            "  private static final long FOO_MILLIS = 100L;",
            "  public Timestamp get() {",
            "    return Timestamps.fromMillis(FOO_MILLIS);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.protobuf.Timestamp;",
            "import com.google.protobuf.util.Timestamps;",
            "class Test {",
            "  private static final Timestamp FOO = Timestamps.fromMillis(100L);",
            "  public Timestamp get() {",
            "    return FOO;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fieldQualifiedByThis() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  private final long fooMillis = 100;",
            "  public Duration get() {",
            "    return Duration.ofMillis(this.fooMillis);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void notInitializedInline_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private static final long FOO_MILLIS;",
            "  static {",
            "    FOO_MILLIS = 100;",
            "  }",
            "  public Duration get() {",
            "    return Duration.ofMillis(FOO_MILLIS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private final long FOO_MILLIS = 100;",
            "  public Duration get() {",
            "    return Duration.ofMillis(FOO_MILLIS);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private final Duration FOO = Duration.ofMillis(100);",
            "  public Duration get() {",
            "    return FOO;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fieldRenaming() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private final long FOO_MILLIS = 100;",
            "  private final long BAR_IN_MILLIS = 100;",
            "  private final long BAZ_MILLI = 100;",
            "  public Duration foo() {",
            "    return Duration.ofMillis(FOO_MILLIS);",
            "  }",
            "  public Duration bar() {",
            "    return Duration.ofMillis(BAR_IN_MILLIS);",
            "  }",
            "  public Duration baz() {",
            "    return Duration.ofMillis(BAZ_MILLI);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private final Duration FOO = Duration.ofMillis(100);",
            "  private final Duration BAR = Duration.ofMillis(100);",
            "  private final Duration BAZ = Duration.ofMillis(100);",
            "  public Duration foo() {",
            "    return FOO;",
            "  }",
            "  public Duration bar() {",
            "    return BAR;",
            "  }",
            "  public Duration baz() {",
            "    return BAZ;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variableUsedInOtherWays_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private final long FOO_MILLIS = 100;",
            "  public Duration get() {",
            "    return Duration.ofMillis(FOO_MILLIS);",
            "  }",
            "  public long frobnicate() {",
            "    return FOO_MILLIS + 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fieldNotPrivate_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  final long FOO_MILLIS = 100;",
            "  public Duration get() {",
            "    return Duration.ofMillis(FOO_MILLIS);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void unusedField_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.Serializable;",
            "class Test implements Serializable {",
            "  private static final long serialVersionUID = 1L;",
            "}")
        .expectNoDiagnostics()
        .doTest();
  }

  @Test
  public void whenJodaAndJavaInstantUsed_fullyQualifiesName() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.time.Instant;",
            "class Test {",
            "  private static final long FOO_MILLIS = 100L;",
            "  private static final Instant INSTANT = null;",
            "  public org.joda.time.Instant get() {",
            "    return new org.joda.time.Instant(FOO_MILLIS);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.Instant;",
            "class Test {",
            "  private static final org.joda.time.Instant FOO = new org.joda.time.Instant(100L);",
            "  private static final Instant INSTANT = null;",
            "  public org.joda.time.Instant get() {",
            "    return FOO;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void milliseconds() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private static final long F1_RETRY_MILLISECONDS = 5000;",
            "  public Duration frobber() {",
            "    return Duration.ofMillis(F1_RETRY_MILLISECONDS);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.time.Duration;",
            "class Test {",
            "  private static final Duration F1_RETRY = Duration.ofMillis(5000);",
            "  public Duration frobber() {",
            "    return F1_RETRY;",
            "  }",
            "}")
        .doTest();
  }
}
