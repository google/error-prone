/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.suppress;

import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Suppressibility;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.ReturnTree;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class CustomSuppressionTest {

  /**
   * Custom suppression annotation for both checkers in this test.
   */
  public @interface SuppressBothCheckers {}

  @BugPattern(
    name = "MyChecker",
    summary = "Test checker that uses a custom suppression annotation",
    explanation = "Test checker that uses a custom suppression annotation",
    suppressibility = Suppressibility.CUSTOM_ANNOTATION,
    customSuppressionAnnotations = SuppressBothCheckers.class,
    category = ONE_OFF,
    severity = ERROR,
    maturity = MATURE
  )
  public static class MyChecker extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  /**
   * Custom suppression annotation for the second checker in this test.
   */
  public @interface SuppressMyChecker2{}

  @BugPattern(
    name = "MyChecker2",
    summary = "Test checker that accepts both custom suppression annotations",
    explanation = "Test checker that accepts both custom suppression annotations",
    suppressibility = Suppressibility.CUSTOM_ANNOTATION,
    customSuppressionAnnotations = {SuppressBothCheckers.class, SuppressMyChecker2.class},
    category = ONE_OFF,
    severity = ERROR,
    maturity = MATURE
  )
  public static class MyChecker2 extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree);
    }
  }

  @Test
  public void myCheckerIsNotSuppressedWithSuppressWarnings() {
    CompilationTestHelper.newInstance(MyChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"MyChecker\")",
            "  int identity(int value) {",
            "    // BUG: Diagnostic contains:",
            "    return value;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void myCheckerIsSuppressedWithCustomAnnotation() {
    CompilationTestHelper.newInstance(MyChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.suppress.CustomSuppressionTest.SuppressBothCheckers;",
            "class Test {",
            "  @SuppressBothCheckers",
            "  int identity(int value) {",
            "    return value;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void myCheckerIsSuppressedWithCustomAnnotationAtLocalVariableScope() {
    CompilationTestHelper.newInstance(MyChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.suppress.CustomSuppressionTest.SuppressBothCheckers;",
            "class Test {",
            "  @SuppressBothCheckers",
            "  Comparable<Integer> myComparable = new Comparable<Integer>() {",
            "    @Override public int compareTo(Integer other) {",
            "      return -1;",
            "    }",
            "  };",
            "}")
        .doTest();
  }

  @Test
  public void myCheckerIsNotSuppressedWithWrongCustomAnnotation() {
    CompilationTestHelper.newInstance(MyChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.suppress.CustomSuppressionTest.SuppressMyChecker2;",
            "class Test {",
            "  @SuppressMyChecker2",
            "  int identity(int value) {",
            "    // BUG: Diagnostic contains:",
            "    return value;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void myChecker2IsSuppressedWithEitherCustomAnnotation() {
    CompilationTestHelper.newInstance(MyChecker2.class, getClass())
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.suppress.CustomSuppressionTest.SuppressBothCheckers;",
            "import com.google.errorprone.suppress.CustomSuppressionTest.SuppressMyChecker2;",
            "class Test {",
            "  @SuppressBothCheckers",
            "  int identity(int value) {",
            "    return value;",
            "  }",
            "  @SuppressMyChecker2",
            "  int square(int value) {",
            "    return value * value;",
            "  }",
            "}")
        .doTest();
  }
}
