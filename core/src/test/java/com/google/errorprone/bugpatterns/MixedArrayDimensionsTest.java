/*
 * Copyright 2016 The Error Prone Authors.
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

/** {@link MixedArrayDimensions}Test */
@RunWith(JUnit4.class)
public class MixedArrayDimensionsTest {

  @Test
  public void positiveVariable() {
    BugCheckerRefactoringTestHelper.newInstance(new MixedArrayDimensions(), getClass())
        .addInputLines(
            "in/Test.java",
            "abstract class Test {",
            "  int a [] = null;",
            "  int [] b [][];",
            "  int [][] c [] = null;",
            "  int [][] d [][];",
            "}")
        .addOutputLines(
            "out/Test.java",
            "abstract class Test {",
            "  int[] a  = null;",
            "  int [][][] b ;",
            "  int [][][] c  = null;",
            "  int [][][][] d ;",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void positiveMethod() {
    BugCheckerRefactoringTestHelper.newInstance(new MixedArrayDimensions(), getClass())
        .addInputLines(
            "in/Test.java",
            "abstract class Test {",
            "  int f() [] { return null; }",
            "  abstract int[] g() [];",
            "  int[] h() [][] { return null; }",
            "  abstract int[][] i() [][];",
            "}")
        .addOutputLines(
            "out/Test.java",
            "abstract class Test {",
            "  int[] f()  { return null; }",
            "  abstract int[][] g() ;",
            "  int[][][] h()  { return null; }",
            "  abstract int[][][][] i() ;",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(MixedArrayDimensions.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  int[] f() { return null; }",
            "  abstract int[][] g();",
            "  int[][][] h() { return null; }",
            "  abstract int[][][][] i();",
            "  int  [] a  = null;",
            "  void f(boolean[]... xs) {}",
            "}")
        .doTest();
  }

  @Test
  public void comment() {
    CompilationTestHelper.newInstance(MixedArrayDimensions.class, getClass())
        .addSourceLines(
            "Test.java", //
            "abstract class Test {",
            "  int /*@Nullable*/ [] x;",
            "}")
        .doTest();
  }
}
