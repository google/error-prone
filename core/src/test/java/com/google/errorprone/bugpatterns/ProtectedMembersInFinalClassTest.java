/*
 * Copyright 2020 The Error Prone Authors.
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

/**
 * Unit tests for {@link ProtectedMembersInFinalClass} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class ProtectedMembersInFinalClassTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(ProtectedMembersInFinalClass.class, getClass());
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ProtectedMembersInFinalClass.class, getClass());

  @Test
  public void testPositiveCases() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "final class Test {",
            "  protected void methodOne() {}",
            "  protected void methodTwo() {}",
            "  protected String var1;",
            "  protected int var2;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "final class Test {",
            "  void methodOne() {}",
            "  void methodTwo() {}",
            "  String var1;",
            "  int var2;",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper
        .addSourceLines(
            "in/Base.java", //
            "class Base {",
            "  protected void protectedMethod() {}",
            "}")
        .addSourceLines(
            "in/Test.java",
            "final class Test extends Base {",
            "  public void publicMethod() {}",
            "  void packageMethod() {}",
            "  private void privateMethod() {}",
            "  @Override protected void protectedMethod() {}",
            "  public int publicField;",
            "  int packageField;",
            "  private int privateField;",
            "}")
        .doTest();
  }

  @Test
  public void testDiagnosticStringWithMultipleMemberMatches() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "final class Test {",
            "  // BUG: Diagnostic contains: Make members of final classes package-private:"
                + " methodOne, methodTwo, fieldOne, fieldTwo",
            "  protected void methodOne() {}",
            "  protected void methodTwo() {}",
            "  public void publicMethod() {}",
            "  private void privateMethod() {}",
            "  void packageMethod() {}",
            "  protected int fieldOne;",
            "  protected long fieldTwo;",
            "  public int publicField;",
            "  int packageField;",
            "  private int privateField;",
            "}")
        .doTest();
  }

  @Test
  public void methodSuppression() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "final class Test {",
            "  @SuppressWarnings(\"ProtectedMembersInFinalClass\")",
            "  protected void methodOne() {}",
            "}")
        .doTest();
  }

  @Test
  public void constructorFindingDescription() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "final class Test {",
            "  // BUG: Diagnostic contains: Test",
            "  protected Test() {}",
            "}")
        .doTest();
  }
}
