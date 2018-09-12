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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ParameterComment}Test */
@RunWith(JUnit4.class)
public class ParameterCommentTest {

  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(ParameterComment.class, getClass());

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ParameterComment(), getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(int x, int y) {}",
            "  {",
            "    f(0/*x*/, 1/*y=*/);",
            "    f(0/*x*/, 1); // y",
            "    f(/* x */ 0, /* y */ 1);",
            "    f(0 /* x */, /* y */ 1);",
            "    f(/* x */ 0, 1 /* y */);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(int x, int y) {}",
            "  {",
            "    f(/* x= */ 0, /* y= */ 1);",
            "    f(/* x= */ 0, /* y= */ 1);",
            "    f(/* x= */ 0, /* y= */ 1);",
            "    f(/* x= */ 0, /* y= */ 1);",
            "    f(/* x= */ 0, /* y= */ 1);",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void negative() {
    compilationTestHelper
        .addSourceLines(
            "in/Test.java",
            "class Test {",
            "  void f(int x, int y) {}",
            "  {",
            "    f(/* x= */0, /* y = */1);",
            "    f(0 /*y=*/, 1 /*x=*/); ",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void varargs() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(int y, int... xs) {}",
            "  {",
            "    f(0/*y*/);",
            "    f(0/*y*/, 1/*xs*/);",
            "    f(0, new int[]{0}/*xs*/);",
            "    f(0, 1, 2/*xs*/, 3/*xs*/);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(int y, int... xs) {}",
            "  {",
            "    f(/* y= */ 0);",
            "    f(/* y= */ 0, /* xs= */ 1);",
            "    f(0, /* xs= */ new int[]{0});",
            "    f(0, 1, /* xs= */ 2, /* xs= */ 3);",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void noParams() {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "class Test {",
            "  void f() {}",
            "  {",
            "    f();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positiveConstructor() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  Test (int x, int y) {}",
            "  {",
            "    new Test(0/*x*/, 1/*y=*/);",
            "    new Test(0/*x*/, 1); // y",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  Test (int x, int y) {}",
            "  {",
            "    new Test(/* x= */ 0, /* y= */ 1);",
            "    new Test(/* x= */ 0, /* y= */ 1); ",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void parameterComment_doesNotChange_whenNestedComment() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  abstract Object target2(Object second);",
            "  void test(Object first, Object second) {",
            "    target(first, target2(/* second= */ second));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  abstract Object target2(Object second);",
            "  void test(Object first, Object second) {",
            "    target(first, target2(/* second= */ second));",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void parameterComment_NestedComment() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  abstract Object target2(Object second);",
            "  void test(Object first, Object second) {",
            "    target(first, target2(second /* second */));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "abstract class Test {",
            "  abstract void target(Object first, Object second);",
            "  abstract Object target2(Object second);",
            "  void test(Object first, Object second) {",
            "    target(first, target2(/* second= */ second));",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
