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

package com.google.errorprone.suppress;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WarnOnUnneededSuppressionsTest {

  @BugPattern(
      name = "NoCallsToFoo",
      summary = "No calls to foo",
      severity = BugPattern.SeverityLevel.ERROR)
  static final class NoCallsToFoo extends BugChecker
      implements BugChecker.MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      String methodSelect = tree.getMethodSelect().toString();
      if (methodSelect.contains("foo")) {
        return buildDescription(tree).setMessage("No calls to foo").build();
      }
      return Description.NO_MATCH;
    }

    @Override
    public boolean supportsUnneededSuppressionWarnings() {
      return true;
    }
  }

  @BugPattern(
      name = "NoCallsToFooUnsupported",
      summary = "No calls to foo",
      severity = BugPattern.SeverityLevel.ERROR)
  static final class NoCallsToFooUnsupported extends BugChecker
      implements BugChecker.MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      String methodSelect = tree.getMethodSelect().toString();
      if (methodSelect.contains("foo")) {
        return buildDescription(tree).setMessage("No calls to foo").build();
      }
      return Description.NO_MATCH;
    }
  }

  @BugPattern(
      name = "SuppressibleTps",
      summary = "Uses SuppressibleTreePathScanner",
      severity = BugPattern.SeverityLevel.ERROR)
  static final class SuppressibleTreePathScannerChecker extends BugChecker
      implements BugChecker.CompilationUnitTreeMatcher {
    @Override
    public Description matchCompilationUnit(
        CompilationUnitTree tree, VisitorState stateForCompilationUnit) {
      new SuppressibleTreePathScanner<Void, Void>(stateForCompilationUnit) {
        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
          state().reportMatch(describeMatch(tree));
          return null;
        }

        private VisitorState state() {
          return stateForCompilationUnit.withPath(getCurrentPath());
        }
      }.scan(tree, null);
      return Description.NO_MATCH;
    }

    @Override
    public boolean supportsUnneededSuppressionWarnings() {
      return true;
    }
  }

  private final CompilationTestHelper testHelperSupported =
      CompilationTestHelper.newInstance(NoCallsToFoo.class, getClass())
          .setArgs("-XepWarnOnUnneededSuppressions")
          .matchAllDiagnostics();

  private final CompilationTestHelper testHelperUnsupported =
      CompilationTestHelper.newInstance(NoCallsToFooUnsupported.class, getClass())
          .setArgs("-XepWarnOnUnneededSuppressions")
          .matchAllDiagnostics();

  private final CompilationTestHelper testHelperSuppressibleScanner =
      CompilationTestHelper.newInstance(SuppressibleTreePathScannerChecker.class, getClass())
          .setArgs("-XepWarnOnUnneededSuppressions")
          .matchAllDiagnostics();

  @Test
  public void classLevelSuppression() {
    testHelperSupported
        .addSourceLines(
            "TestPositive.java",
            """
            @SuppressWarnings("NoCallsToFoo")
            // BUG: Diagnostic contains: Unnecessary
            class TestPositive {
              void test() {
                bar();
              }
              void bar() {}
            }
            """)
        .addSourceLines(
            "TestNegative.java",
            """
            @SuppressWarnings("NoCallsToFoo")
            class TestNegative {
              void test() {
                foo();
              }
              void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void suppressionUnsupportedButUsed() {
    testHelperUnsupported
        .addSourceLines(
            "TestNegative.java",
            """
            @SuppressWarnings("NoCallsToFooUnsupported")
            class TestNegative {
              void test() {
                foo();
              }
              void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void methodLevelSuppression() {
    testHelperSupported
        .addSourceLines(
            "TestPositive.java",
            """
            class TestPositive {
              @SuppressWarnings("NoCallsToFoo")
              // BUG: Diagnostic contains: Unnecessary
              void test() {
                bar();
              }
              void bar() {}
            }
            """)
        .addSourceLines(
            "TestNegative.java",
            """
            class TestNegative {
              @SuppressWarnings("NoCallsToFoo")
              void test() {
                foo();
              }
              void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void fieldLevelSuppression() {
    testHelperSupported
        .addSourceLines(
            "TestPositive.java",
            """
            class TestPositive {
              @SuppressWarnings("NoCallsToFoo")
              // BUG: Diagnostic contains: Unnecessary
              Object x = bar();
              Object bar() { return null; }
            }
            """)
        .addSourceLines(
            "TestNegative.java",
            """
            class TestNegative {
              @SuppressWarnings("NoCallsToFoo")
              Object x = foo();
              Object foo() { return null; }
            }
            """)
        .doTest();
  }

  @Test
  public void localVariableLevelSuppression() {
    testHelperSupported
        .addSourceLines(
            "TestPositive.java",
            """
            class TestPositive {
              void test() {
                @SuppressWarnings("NoCallsToFoo")
                // BUG: Diagnostic contains: Unnecessary
                Object x = bar();
              }
              Object bar() { return null; }
            }
            """)
        .addSourceLines(
            "TestNegative.java",
            """
            class TestNegative {
              void test() {
                @SuppressWarnings("NoCallsToFoo")
                Object x = foo();
              }
              Object foo() { return null; }
            }
            """)
        .doTest();
  }

  @Test
  public void suppressibleTreePathScannerSuppression() {
    testHelperSuppressibleScanner
        .addSourceLines(
            "TestNegative.java",
            """
            class TestNegative {
              @SuppressWarnings("SuppressibleTps")
              int value = 0;
            }
            """)
        .doTest();
  }
}
