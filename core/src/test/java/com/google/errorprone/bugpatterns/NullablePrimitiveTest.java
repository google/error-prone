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

/** @author sebastian.h.monte@gmail.com (Sebastian Monte) */
@RunWith(JUnit4.class)
public class NullablePrimitiveTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(NullablePrimitive.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("NullablePrimitivePositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("NullablePrimitiveNegativeCases.java").doTest();
  }

  @Test
  public void negativeConstructor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  @Nullable public Test() {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeVoid() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.Nullable;",
            "class Test {",
            "  @Nullable void f() {}",
            "}")
        .doTest();
  }

  // regression test for #418
  @Test
  public void typeParameter() {
    compilationHelper
        .addSourceLines(
            "Nullable.java",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@Target(ElementType.TYPE_USE)",
            "public @interface Nullable {}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  @Nullable int x;",
            "  // BUG: Diagnostic contains:",
            "  @Nullable int f() {",
            "    return 42;",
            "  }",
            "  <@Nullable T> int g() {",
            "    return 42;",
            "  }",
            "  int @Nullable [] y;",
            "}")
        .doTest();
  }
}
