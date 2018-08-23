/*
 * Copyright 2015 The Error Prone Authors.
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

/** {@link IsInstanceOfClass}Test */
@RunWith(JUnit4.class)
public class IsInstanceOfClassTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(IsInstanceOfClass.class, getClass());
  }

  @Test
  public void positive_clazz_enclosingClass() {
    compilationHelper
        .addSourceLines(
            "pkg/A.java",
            "class A {",
            "  boolean m(Class<?> clazz) {",
            "    // BUG: Diagnostic contains: clazz.isAssignableFrom(getClass())",
            "    return getClass().isInstance(clazz);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_enclosingClass_clazz() {
    compilationHelper
        .addSourceLines(
            "pkg/A.java",
            "class A {",
            "  boolean m(Class<?> clazz) {",
            "    // BUG: Diagnostic contains: getClass().isAssignableFrom(clazz)",
            "    return clazz.isInstance(getClass());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_getClass_getClass() {
    compilationHelper
        .addSourceLines(
            "pkg/A.java",
            "class A {",
            "  boolean m(Object a, Object b) {",
            "    // BUG: Diagnostic contains: b.getClass().isInstance(a)",
            "    return a.getClass().isInstance(b.getClass());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_getClass_literal() {
    compilationHelper
        .addSourceLines(
            "pkg/A.java",
            "class A {",
            "  boolean m(Object obj) {",
            "    // BUG: Diagnostic contains: obj instanceof String",
            "    return obj.getClass().isInstance(String.class);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_literal_getClass() {
    compilationHelper
        .addSourceLines(
            "pkg/A.java",
            "class A {",
            "  boolean m(Object obj) {",
            "    // BUG: Diagnostic contains: obj instanceof String",
            "    return String.class.isInstance(obj.getClass());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_literal_literal() {
    compilationHelper
        .addSourceLines(
            "pkg/A.java",
            "class A {",
            "  boolean m(Object obj) {",
            "    // BUG: Diagnostic contains: String.class == Class.class",
            "    return Number.class.isInstance(String.class);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_clazz_getClass() {
    compilationHelper
        .addSourceLines(
            "pkg/A.java",
            "class A {",
            "  boolean m(Object o, Class<?> clazz) {",
            "    // BUG: Diagnostic contains: clazz.isInstance(o)",
            "    return clazz.isInstance(o.getClass());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_getClass_clazz() {
    compilationHelper
        .addSourceLines(
            "pkg/A.java",
            "class A {",
            "  boolean m(Object o, Class<?> clazz) {",
            "    // BUG: Diagnostic contains: clazz.isInstance(o)",
            "    return o.getClass().isInstance(clazz);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_clazz_clazz() {
    compilationHelper
        .addSourceLines(
            "pkg/A.java",
            "class A {",
            "  boolean m(Class<?> a, Class<?> b) {",
            "    // BUG: Diagnostic contains: b.isAssignableFrom(a)",
            "    return a.isInstance(b);",
            "  }",
            "}")
        .doTest();
  }
}
