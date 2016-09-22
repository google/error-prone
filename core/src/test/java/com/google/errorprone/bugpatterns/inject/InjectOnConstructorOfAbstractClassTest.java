/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author glorioso@google.com (Nick Glorioso) */
@RunWith(JUnit4.class)
public class InjectOnConstructorOfAbstractClassTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(InjectOnConstructorOfAbstractClass.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Inject;",
            "abstract class Foo {",
            "  // BUG: Diagnostic contains: Foo() {}",
            "  @Inject Foo() {}",
            "}")
        .doTest();
  }

  @Test
  public void testGuiceConstructor() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import com.google.inject.Inject;",
            "abstract class Foo {",
            "  // BUG: Diagnostic contains: Foo() {}",
            "  @Inject Foo() {}",
            "}")
        .doTest();
  }

  @Test
  public void testAbstractClassInConcreteClass() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Inject;",
            "class Bar {",
            "  abstract static class Foo {",
            "     // BUG: Diagnostic contains: Foo() {}",
            "     @Inject Foo() {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .addSourceLines(
            "Foo.java", //
            "import javax.inject.Inject;",
            "class Foo {",
            "  @Inject Foo() {}",
            "}")
        .doTest();
  }

  @Test
  public void testConcreteClassInAbstractClass() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import javax.inject.Inject;",
            "abstract class Bar {",
            "  static class Foo {",
            "    @Inject Foo() {}",
            "  }",
            "}")
        .doTest();
  }
}
