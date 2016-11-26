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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link CompatibleWithMisuse} */
@RunWith(JUnit4.class)
public class CompatibleWithMisuseTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(CompatibleWithMisuse.class, getClass());
  }

  @Test
  public void testOK() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompatibleWith;",
            "class Test<X> {",
            "  static final String CONSTANT = \"X\";",
            "  void doSomething(@CompatibleWith(\"X\") Object ok) {}",
            "  void doSomethingWithConstant(@CompatibleWith(CONSTANT) Object ok) {}",
            "  <Y extends String> void doSomethingElse(@CompatibleWith(\"Y\") Object ok) {}",
            "  <X,Z> void doSomethingTwice(@CompatibleWith(\"X\") Object ok) {}",
            "}")
        .doTest();
  }

  @Test
  public void testBad() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompatibleWith;",
            "class Test<X> {",
            "  static final String CONSTANT = \"Y\";",
            "  // BUG: Diagnostic contains: Valid arguments are: X",
            "  void doSomething(@CompatibleWith(\"Y\") Object bad) {}",
            "  // BUG: Diagnostic contains: Valid arguments are: X",
            "  void doSomethingWithConstant(@CompatibleWith(CONSTANT) Object bad) {}",
            "  // BUG: Diagnostic contains: not be empty (valid arguments are X)",
            "  void doSomethingEmpty(@CompatibleWith(\"\") Object bad) {}",
            "  // BUG: Diagnostic contains: Valid arguments are: Z, X",
            "  <Z> void doSomethingElse(@CompatibleWith(\"Y\") Object ok) {}",
            "}")
        .doTest();
  }

  @Test
  public void overridesAlreadyAnnotated() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompatibleWith;",
            "class Test<X> {",
            "  void doSomething(@CompatibleWith(\"X\") Object bad) {}",
            "}",
            "class Foo<X> extends Test<X> {",
            "  // BUG: Diagnostic contains: in Test that already has @CompatibleWith",
            "  void doSomething(@CompatibleWith(\"X\") Object bad) {}",
            "}")
        .doTest();
  }

  @Test
  public void testNoTypeArgs() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompatibleWith;",
            "class Test {",
            "  // BUG: Diagnostic contains: There are no type arguments",
            "  void doSomething(@CompatibleWith(\"Y\") Object bad) {}",
            "}")
        .doTest();
  }

  @Test
  public void notAllowedOnVarArgs() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompatibleWith;",
            "class Test<Y> {",
            "  // BUG: Diagnostic contains: varargs",
            "  void doSomething(@CompatibleWith(\"Y\") Object... bad) {}",
            "}")
        .doTest();
  }

  @Test
  public void testInnerTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompatibleWith;",
            "class Test<X> {",
            "  class Test2<Y> {",
            "    class Test3<Z> {",
            "      // BUG: Diagnostic contains: Valid arguments are: Z, Y, X",
            "      void doSomething(@CompatibleWith(\"P\") Object bad) {}",
            "      // BUG: Diagnostic contains: Valid arguments are: Q, Z, Y, X",
            "      <Q> void doSomethingElse(@CompatibleWith(\"P\") Object bad) {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNestedTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompatibleWith;",
            "class Test<X> {",
            "  static class Test2<Y> {",
            "    class Test3<Z> {",
            "      // BUG: Diagnostic contains: Valid arguments are: Z, Y",
            "      void doSomething(@CompatibleWith(\"X\") Object bad) {}",
            "      // BUG: Diagnostic contains: Valid arguments are: Q, Z, Y",
            "      <Q> void doSomethingElse(@CompatibleWith(\"X\") Object bad) {}",
            "    }",
            "    // BUG: Diagnostic contains: Valid arguments are: Y",
            "    void doSomething(@CompatibleWith(\"X\") Object bad) {}",
            "  }",
            "}")
        .doTest();
  }
}
