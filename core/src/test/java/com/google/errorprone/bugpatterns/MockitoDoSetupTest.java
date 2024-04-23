/*
 * Copyright 2024 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class MockitoDoSetupTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(MockitoDoSetup.class, getClass());

  @Test
  public void happy() {
    helper
        .addInputLines(
            "Test.java",
            "import org.mockito.Mockito;",
            "public class Test {",
            "  public int test(Test test) {",
            "    Mockito.doReturn(1).when(test).test(null);",
            "    return 1;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import org.mockito.Mockito;",
            "public class Test {",
            "  public int test(Test test) {",
            "    when(test.test(null)).thenReturn(1);",
            "    return 1;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoresSpiesCreatedByAnnotation() {
    helper
        .addInputLines(
            "Test.java",
            "import org.mockito.Mockito;",
            "public class Test {",
            "  @org.mockito.Spy Test test;",
            "  public int test() {",
            "    Mockito.doReturn(1).when(test).test();",
            "    return 1;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void ignoresSpiesCreatedByStaticMethod() {
    helper
        .addInputLines(
            "Test.java",
            "import org.mockito.Mockito;",
            "public class Test {",
            "  Test test = Mockito.spy(Test.class);",
            "  public int test() {",
            "    Mockito.doReturn(1).when(test).test();",
            "    return 1;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void ignoresMocksConfiguredToThrow_viaThenThrow() {
    helper
        .addInputLines(
            "Test.java",
            "import org.mockito.Mockito;",
            "public class Test {",
            "  public int test(Test test) {",
            "    Mockito.doReturn(1).when(test).test(null);",
            "    Mockito.when(test.test(null)).thenThrow(new Exception());",
            "    return 1;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void ignoresMocksConfiguredToThrow_viaDoThrow() {
    helper
        .addInputLines(
            "Test.java",
            "import org.mockito.Mockito;",
            "public class Test {",
            "  public int test(Test test) {",
            "    Mockito.doReturn(1).when(test).test(null);",
            "    Mockito.doThrow(new Exception()).when(test).test(null);",
            "    return 1;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
