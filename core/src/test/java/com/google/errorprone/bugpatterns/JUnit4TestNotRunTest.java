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

package com.google.errorprone.bugpatterns;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class JUnit4TestNotRunTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnit4TestNotRun.class, getClass());

  // TODO(b/34062183): Remove flag once cleanup complete.
  private final CompilationTestHelper expandedHeuristicCompilationHelper =
      CompilationTestHelper.newInstance(JUnit4TestNotRun.class, getClass())
          .setArgs(ImmutableList.of("-XDexpandedTestNotRunHeuristic=true"));

  @Test
  public void testPositiveCase1() throws Exception {
    compilationHelper.addSourceFile("JUnit4TestNotRunPositiveCase1.java").doTest();
  }

  @Test
  public void testPositiveCase2() throws Exception {
    compilationHelper.addSourceFile("JUnit4TestNotRunPositiveCase2.java").doTest();
  }

  @Test
  public void testContainsVerifyAsIdentifier() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.verify;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoSomething() {",
            "    verify(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testContainsQualifiedVerify() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.mockito.Mockito;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoSomething() {",
            "    Mockito.verify(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testContainsAssertAsIdentifier() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.util.Collections;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoSomething() {",
            "    assertThat(2).isEqualTo(2);",
            "  }",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoTwoThings() {",
            "    Collections.sort(Collections.<Integer>emptyList());",
            "    assertThat(3).isEqualTo(3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testContainsQualifiedAssert() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import com.google.common.truth.Truth;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoSomething() {",
            "    Truth.assertThat(1).isEqualTo(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testContainsCheckAsIdentifier() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkState;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoSomething() {",
            "    checkState(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testContainsQualifiedCheck() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoSomething() {",
            "    Preconditions.checkState(false);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testContainsFailAsIdentifier() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.fail;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoSomething() {",
            "    fail();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testContainsQualifiedFail() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.Assert;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoSomething() {",
            "    Assert.fail();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testContainsExpectAsIdentifier() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import static org.junit.Assert.expectThrows;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoSomething() {",
            "    expectThrows(null, null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testContainsQualifiedExpect() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.Assert;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  // BUG: Diagnostic contains: @Test",
            "  public void shouldDoSomething() {",
            "    Assert.expectThrows(null, null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noTestKeyword() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import java.util.Collections;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  public void shouldDoSomething() {",
            "    Collections.sort(Collections.<Integer>emptyList());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticMethodWithTestKeyword() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import java.util.Collections;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  private static void assertDoesSomething() {}",
            "  public static void shouldDoSomething() {",
            "    assertDoesSomething();",
            "  }",
            "}")
        .doTest();
  }


  @Test
  public void hasOtherAnnotation() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  @Deprecated",
            "  public void shouldDoSomething() {",
            "    verify();",
            "  }",
            "  private void verify() {}",
            "}")
        .doTest();
  }

  @Test
  public void hasOtherAnnotationAndNamedTest() {
    expandedHeuristicCompilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import java.util.Collections;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  @Deprecated",
            "  // BUG: Diagnostic contains: @Test",
            "  public void testShouldDoSomething() {",
            "    Collections.sort(Collections.<Integer>emptyList());",
            "  }",
            "  private void verify() {}",
            "}")
        .doTest();
  }

  // TODO(b/34062183): Remove this test once cleanup complete.
  @Test
  public void shouldNotUseExpandedHeuristicWithoutFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import com.google.common.truth.Truth;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  public void shouldDoSomething() {",
            "    Truth.assertThat(1).isEqualTo(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase1() throws Exception {
    compilationHelper.addSourceFile("JUnit4TestNotRunNegativeCase1.java").doTest();
  }

  @Test
  public void testNegativeCase2() throws Exception {
    compilationHelper.addSourceFile("JUnit4TestNotRunNegativeCase2.java").doTest();
  }

  @Test
  public void testNegativeCase3() throws Exception {
    compilationHelper.addSourceFile("JUnit4TestNotRunNegativeCase3.java").doTest();
  }

  @Test
  public void testNegativeCase4() throws Exception {
    compilationHelper.addSourceFile("JUnit4TestNotRunNegativeCase4.java").doTest();
  }

  @Test
  public void testNegativeCase5() throws Exception {
    compilationHelper
        .addSourceFile("JUnit4TestNotRunBaseClass.java")
        .addSourceFile("JUnit4TestNotRunNegativeCase5.java")
        .doTest();
  }

  @Test
  public void testSerialization() throws Exception {
    new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(new JUnit4TestNotRun());
  }
}
