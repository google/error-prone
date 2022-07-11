/*
 * Copyright 2022 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class MockNotUsedInProductionTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(MockNotUsedInProduction.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(MockNotUsedInProduction.class, getClass());

  @Test
  public void neverUsed() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "class Test {",
            "  public Object test() {",
            "    // BUG: Diagnostic contains:",
            "    Test test = mock(Test.class);",
            "    when(test.test()).thenCallRealMethod();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void spyNeverUsed() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.spy;",
            "import static org.mockito.Mockito.verify;",
            "class Test {",
            "  public Object test() {",
            "    // BUG: Diagnostic contains:",
            "    Test test = spy(new Test());",
            "    verify(test).test();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void passedToProduction() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "class Test {",
            "  public Object test() {",
            "    Test test = mock(Test.class);",
            "    when(test.test()).thenCallRealMethod();",
            "    return test.test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void possiblyBound() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Bind @Mock public Test test;",
            "  public Object test() {",
            "    when(test.test()).thenCallRealMethod();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void publicField() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock public Test test;",
            "  public Object test() {",
            "    when(test.test()).thenCallRealMethod();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void qualifiedWithThis_stillSeen() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock private Test test;",
            "  public Test test() {",
            "    return this.test;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void privateField() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import org.mockito.Mock;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  @Mock private Test test;",
            "  public Object test() {",
            "    when(test.test()).thenCallRealMethod();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void injectMocks_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import org.mockito.InjectMocks;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock private Test test;",
            "  @InjectMocks Test t;",
            "  public Object test() {",
            "    when(test.test()).thenCallRealMethod();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void suppressionWorks() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @SuppressWarnings(\"MockNotUsedInProduction\")",
            "  @Mock private Test test;",
            "  public Object test() {",
            "    when(test.test()).thenCallRealMethod();",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoring
        .addInputLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock private Test test;",
            "  public Object test() {",
            "    when(test.test()).thenCallRealMethod();",
            "    return null;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import org.mockito.Mock;",
            "class Test {",
            "  public Object test() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }
}
