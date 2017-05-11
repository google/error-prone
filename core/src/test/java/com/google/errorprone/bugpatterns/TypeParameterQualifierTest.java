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

/** {@link TypeParameterQualifier}Test */
@RunWith(JUnit4.class)
public class TypeParameterQualifierTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(TypeParameterQualifier.class, getClass());
  }

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            // force a line break
            "class Foo {",
            "  static class Builder {}",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: populate(Foo.Builder builder)",
            "  static <T extends Foo> T populate(T.Builder builder) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static <T extends Enum<T>> T get(Class<T> clazz, String value) {",
            "    // BUG: Diagnostic contains: Enum.valueOf(clazz, value);",
            "    return T.valueOf(clazz, value);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            // force a line break
            "class Foo {",
            "  static class Builder {}",
            "}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static <T extends Foo> T populate(T builder) {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }
}
