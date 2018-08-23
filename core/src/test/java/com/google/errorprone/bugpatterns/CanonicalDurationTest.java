/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link CanonicalDuration}Test */
@RunWith(JUnit4.class)
public class CanonicalDurationTest {

  @Test
  public void refactoringJavaTime() {
    BugCheckerRefactoringTestHelper.newInstance(new CanonicalDuration(), getClass())
        .addInputLines(
            "in/A.java", //
            "package a;",
            "import java.time.Duration;",
            "public class A {",
            "  static final int CONST = 86400;",
            "  {",
            "    Duration.ofSeconds(86400);",
            "    java.time.Duration.ofSeconds(86400);",
            "    Duration.ofSeconds(CONST);",
            "    Duration.ofMillis(0);",
            "    Duration.ofDays(1);",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java", //
            "package a;",
            "import java.time.Duration;",
            "public class A {",
            "  static final int CONST = 86400;",
            "  {",
            "    Duration.ofDays(1);",
            "    java.time.Duration.ofDays(1);",
            "    Duration.ofSeconds(CONST);",
            "    Duration.ofMillis(0);",
            "    Duration.ofDays(1);",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactoringJoda() {
    BugCheckerRefactoringTestHelper.newInstance(new CanonicalDuration(), getClass())
        .addInputLines(
            "in/A.java", //
            "package a;",
            "import org.joda.time.Duration;",
            "public class A {",
            "  static final int CONST = 86400;",
            "  {",
            "    Duration.standardSeconds(86400);",
            "    org.joda.time.Duration.standardSeconds(86400);",
            "    Duration.standardSeconds(CONST);",
            "    Duration zero = Duration.standardSeconds(0);",
            "    Duration.standardDays(1);",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java", //
            "package a;",
            "import org.joda.time.Duration;",
            "public class A {",
            "  static final int CONST = 86400;",
            "  {",
            "    Duration.standardDays(1);",
            "    org.joda.time.Duration.standardDays(1);",
            "    Duration.standardSeconds(CONST);",
            "    Duration zero = Duration.ZERO;",
            "    Duration.standardDays(1);",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactoringJavaTimeStaticImport() {
    BugCheckerRefactoringTestHelper.newInstance(new CanonicalDuration(), getClass())
        .addInputLines(
            "in/A.java", //
            "package a;",
            "import static java.time.Duration.ofSeconds;",
            "import java.time.Duration;",
            "public class A {",
            "  {",
            "    ofSeconds(86400);",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java", //
            "package a;",
            "",
            "import static java.time.Duration.ofDays;",
            "import static java.time.Duration.ofSeconds;",
            "",
            "import java.time.Duration;",
            "",
            "public class A {",
            "  {",
            "    ofDays(1);",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void refactoringJodaStaticImport() {
    BugCheckerRefactoringTestHelper.newInstance(new CanonicalDuration(), getClass())
        .addInputLines(
            "in/A.java", //
            "package a;",
            "import static org.joda.time.Duration.standardSeconds;",
            "public class A {",
            "  {",
            "    standardSeconds(86400);",
            "    standardSeconds(0).getStandardSeconds();",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java", //
            "package a;",
            "import static org.joda.time.Duration.standardDays;",
            "import static org.joda.time.Duration.standardSeconds;",
            "",
            "import org.joda.time.Duration;",
            "public class A {",
            "  {",
            "    standardDays(1);",
            "    Duration.ZERO.getStandardSeconds();",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void blacklist() {
    CompilationTestHelper.newInstance(CanonicalDuration.class, getClass())
        .addSourceLines(
            "A.java",
            "package a;",
            "import java.time.Duration;",
            "public class A {",
            "  static final int S = 60;",
            "  static final int M = 60;",
            "  static final int H = 24;",
            "  {",
            "    Duration.ofSeconds(S);",
            "    Duration.ofMinutes(H);",
            "    Duration.ofHours(24);",
            "  }",
            "}")
        .doTest();
  }
}
