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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link UnnecessaryAssignment}. */
@RunWith(JUnit4.class)
public final class UnnecessaryAssignmentTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(UnnecessaryAssignment.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessaryAssignment.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import org.mockito.Mock;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  @Mock Object mockObject = new Object();",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock Object mockObject;",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock Object mockObject = new Object();",
            "}")
        .addOutputLines(
            "Test.java", //
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock Object mockObject;",
            "}")
        .doTest();
  }

  @Test
  public void initializedViaInitMocks() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import org.mockito.Mock;",
            "import org.mockito.Mockito;",
            "import org.mockito.MockitoAnnotations;",
            "class Test {",
            "  @Mock Object mockObject = Mockito.mock(Object.class);",
            "  void before() {",
            "    MockitoAnnotations.initMocks(this);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import org.mockito.Mock;",
            "import org.mockito.Mockito;",
            "import org.mockito.MockitoAnnotations;",
            "class Test {",
            "  @Mock Object mockObject;",
            "  void before() {",
            "    MockitoAnnotations.initMocks(this);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noInitializerPresent_retainManualInitialization() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "import org.mockito.Mock;",
            "import org.mockito.Mockito;",
            "class Test {",
            "  @Mock Object mockObject = Mockito.mock(Object.class);",
            "}")
        .addOutputLines(
            "Test.java", //
            "import org.mockito.Mock;",
            "import org.mockito.Mockito;",
            "class Test {",
            "  Object mockObject = Mockito.mock(Object.class);",
            "}")
        .doTest();
  }

  @Test
  public void initializedViaRunner() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "import org.junit.runner.RunWith;",
            "import org.mockito.Mock;",
            "import org.mockito.Mockito;",
            "import org.mockito.junit.MockitoJUnitRunner;",
            "@RunWith(MockitoJUnitRunner.class)",
            "public class Test {",
            "  @Mock Object mockObject = Mockito.mock(Object.class);",
            "}")
        .addOutputLines(
            "Test.java", //
            "import org.junit.runner.RunWith;",
            "import org.mockito.Mock;",
            "import org.mockito.Mockito;",
            "import org.mockito.junit.MockitoJUnitRunner;",
            "@RunWith(MockitoJUnitRunner.class)",
            "public class Test {",
            "  @Mock Object mockObject;",
            "}")
        .doTest();
  }

  @Test
  public void positiveOnTestParameter() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import com.google.testing.junit.testparameterinjector.TestParameter;",
            "class Test {",
            "  // BUG: Diagnostic contains: @TestParameter",
            "  @TestParameter boolean myFoo = false;",
            "}")
        .doTest();
  }

  @Test
  public void optionalInject_notFlagged() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import com.google.inject.Inject;",
            "class Test {",
            "  @Inject(optional = true) boolean myFoo = false;",
            "}")
        .doTest();
  }

  @Test
  public void fixForTestParameter_deletesAssignment() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "import com.google.testing.junit.testparameterinjector.TestParameter;",
            "class Test {",
            "  @TestParameter boolean myFoo = false;",
            "}")
        .addOutputLines(
            "Test.java", //
            "import com.google.testing.junit.testparameterinjector.TestParameter;",
            "class Test {",
            "  @TestParameter boolean myFoo;",
            "}")
        .doTest();
  }

  @Test
  public void fixForTestParameter_ifFinal_deletesAnnotation() {
    refactoringHelper
        .addInputLines(
            "Test.java", //
            "import com.google.testing.junit.testparameterinjector.TestParameter;",
            "class Test {",
            "  @TestParameter final boolean myFoo = false;",
            "}")
        .addOutputLines(
            "Test.java", //
            "import com.google.testing.junit.testparameterinjector.TestParameter;",
            "class Test {",
            "  final boolean myFoo = false;",
            "}")
        .doTest();
  }
}
