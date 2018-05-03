/*
 * Copyright 2018 The Error Prone Authors.
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

/** Tests for {@link ClassNamedLikeTypeParameter} */
@RunWith(JUnit4.class)
public class ClassNamedLikeTypeParameterTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public final void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(ClassNamedLikeTypeParameter.class, getClass());
  }

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains: ",
            "  static class A {}",
            "  // BUG: Diagnostic contains: ",
            "  static class B2 {}",
            "  // BUG: Diagnostic contains: ",
            "  static class FooT {}",
            "  // BUG: Diagnostic contains: ",
            "  static class X {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  static class CAPITALT {}",
            "  static class MyClass {}",
            "  static class FooBar {}",
            "  static class HasGeneric<X> {",
            "    public <T> void genericMethod(X foo, T bar) {}",
            "  }",
            "}")
        .doTest();
  }
}
