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

/** Tests for {@link DirectInvocationOnMock}. */
@RunWith(JUnit4.class)
public final class DirectInvocationOnMockTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(DirectInvocationOnMock.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(DirectInvocationOnMock.class, getClass());

  @Test
  public void directInvocationOnMock() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "class Test {",
            "  public void test() {",
            "    Test test = mock(Test.class);",
            "    // BUG: Diagnostic contains: test",
            "    test.test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void directInvocationOnMockAssignment() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "class Test {",
            "  public void test() {",
            "    Test test;",
            "    test = mock(Test.class);",
            "    // BUG: Diagnostic contains:",
            "    test.test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void directInvocationOnMock_suggestsVerify() {
    refactoring
        .addInputLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.verify;",
            "class Test {",
            "  public void test() {",
            "    Test test = mock(Test.class);",
            "    test.test();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.verify;",
            "class Test {",
            "  public void test() {",
            "    Test test = mock(Test.class);",
            "    verify(test).test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void directInvocationOnMock_mockHasExtraOptions_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import org.mockito.Answers;",
            "class Test {",
            "  public void test() {",
            "    Test test = mock(Test.class, Answers.RETURNS_DEEP_STUBS);",
            "    test.test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void directInvocationOnMockAnnotatedField() {
    helper
        .addSourceLines(
            "Test.java",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock public Test test;",
            "  public void test() {",
            "    // BUG: Diagnostic contains:",
            "    test.test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void directInvocationOnMockAnnotatedField_mockHasExtraOptions_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import org.mockito.Answers;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock(answer = Answers.RETURNS_DEEP_STUBS) public Test test;",
            "  public void test() {",
            "    test.test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void directInvocationOnMock_withinWhen_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "class Test {",
            "  public Object test() {",
            "    Test test = mock(Test.class);",
            "    when(test.test()).thenReturn(null);",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void directInvocationOnMock_setUpToCallRealMethod_noFinding() {
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
  public void directInvocationOnMock_setUpWithDoCallRealMethod_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.doCallRealMethod;",
            "class Test {",
            "  public Object test() {",
            "    Test test = mock(Test.class);",
            "    doCallRealMethod().when(test).test();",
            "    return test.test();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void directInvocationOnMock_withinCustomWhen_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import org.mockito.stubbing.OngoingStubbing;",
            "class Test {",
            "  public <T> OngoingStubbing<T> when(T t) {",
            "    return org.mockito.Mockito.when(t);",
            "  }",
            "  public Object test() {",
            "    Test test = mock(Test.class);",
            "    when(test.test()).thenReturn(null);",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void directInvocationOnMock_withinWhenWithCast_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "class Test {",
            "  public Object test() {",
            "    Test test = mock(Test.class);",
            "    when((Object) test.test()).thenReturn(null);",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalMethodInvoked_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "class Test {",
            "  public Object test() {",
            "    Test test = mock(Test.class);",
            "    return test.getClass();",
            "  }",
            "}")
        .doTest();
  }
}
