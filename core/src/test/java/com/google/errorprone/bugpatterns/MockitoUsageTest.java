/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link MockitoUsage}Test */
@RunWith(JUnit4.class)
public class MockitoUsageTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(MockitoUsage.class, getClass());
  }

  private static final String[] FOO_SOURCE = {
    "class Foo {",
    "  Object get() { return null; }",
    "  Object execute () {",
    "    return null;",
    "  }",
    "}",
  };

  @Test
  public void negative_thenReturn() {
    compilationHelper
        .addSourceLines("Foo.java", FOO_SOURCE)
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "class Test {",
            "  void test() {",
            "    Foo mock = mock(Foo.class);",
            "    when(mock.get()).thenReturn(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_verify() {
    compilationHelper
        .addSourceLines("Foo.java", FOO_SOURCE)
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.verify;",
            "class Test {",
            "  void test() {",
            "    Foo mock = mock(Foo.class);",
            "    verify(mock).execute();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_verify_never_noMethod() {
    compilationHelper
        .addSourceLines("Foo.java", FOO_SOURCE)
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.verify;",
            "import static org.mockito.Mockito.never;",
            "class Test {",
            "  void test() {",
            "    Foo mock = mock(Foo.class);",
            "    // BUG: Diagnostic contains:",
            "    // Missing method call for verify(mock, never())",
            "    // verifyZeroInteractions(mock);",
            "    verify(mock, never());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_verify_never_method() {
    compilationHelper
        .addSourceLines("Foo.java", FOO_SOURCE)
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.verify;",
            "import static org.mockito.Mockito.never;",
            "class Test {",
            "  void test() {",
            "    Foo mock = mock(Foo.class);",
            "    // BUG: Diagnostic contains:",
            "    // Missing method call for verify(mock.execute(), never())",
            "    // verify(mock, never()).execute();",
            "    verify(mock.execute(), never());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_thenReturn() {
    compilationHelper
        .addSourceLines("Foo.java", FOO_SOURCE)
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.when;",
            "class Test {",
            "  void test() {",
            "    Foo mock = mock(Foo.class);",
            "    // BUG: Diagnostic contains:",
            "    // Missing method call for when(mock.get()) here",
            "    // remove this line",
            "    when(mock.get());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_verify_methodInside() {
    compilationHelper
        .addSourceLines("Foo.java", FOO_SOURCE)
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.verify;",
            "class Test {",
            "  void test() {",
            "    Foo mock = mock(Foo.class);",
            "    // BUG: Diagnostic contains:",
            "    // Missing method call for verify(mock.execute()) here",
            "    // verify(mock).execute();",
            "    verify(mock.execute());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_verify_noMethod() {
    compilationHelper
        .addSourceLines("Foo.java", FOO_SOURCE)
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.verify;",
            "class Test {",
            "  void test() {",
            "    Foo mock = mock(Foo.class);",
            "    // BUG: Diagnostic contains:",
            "    // Missing method call for verify(mock) here",
            "    // remove this line",
            "    verify(mock);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_verify_times() {
    compilationHelper
        .addSourceLines("Foo.java", FOO_SOURCE)
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.mock;",
            "import static org.mockito.Mockito.verify;",
            "import static org.mockito.Mockito.times;",
            "class Test {",
            "  void test() {",
            "    Foo mock = mock(Foo.class);",
            "    // BUG: Diagnostic contains:",
            "    // Missing method call for verify(mock.execute(), times(1))",
            "    // verify(mock, times(1)).execute();",
            "    verify(mock.execute(), times(1));",
            "  }",
            "}")
        .doTest();
  }
}
