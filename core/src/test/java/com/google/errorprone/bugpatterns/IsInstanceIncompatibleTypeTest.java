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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link IsInstanceIncompatibleType}Test */
@RunWith(JUnit4.class)
public class IsInstanceIncompatibleTypeTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(IsInstanceIncompatibleType.class, getClass());

  @Test
  public void positiveInstanceOf() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  Optional<String> f(Optional<String> s) {",
            "    // BUG: Diagnostic contains: String cannot be cast to Integer",
            "    return s.filter(Integer.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveInstanceOf_methodCall() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  Optional<String> f(Optional<String> s) {",
            "    // BUG: Diagnostic contains: String cannot be cast to Integer",
            "    return s.filter(x -> Integer.class.isInstance(x));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveInstanceOf2() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.util.HashMap;",
            "class Test {",
            "  Optional<HashMap<String,Integer>> f(Optional<HashMap<String,Integer>> m) {",
            "    // BUG: Diagnostic contains: HashMap cannot be cast to Integer",
            "    return m.filter(Integer.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveInstanceOfWithGenerics() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.lang.Number;",
            "class Test {",
            "  <T extends Number> Optional<T> f(Optional<T> t) {",
            "    // BUG: Diagnostic contains: Number cannot be cast to String",
            "    return t.filter(String.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInstanceOf() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.util.HashMap;",
            "import java.util.LinkedHashMap;",
            "class Test {",
            "  Optional<HashMap> f(Optional<HashMap> m) {",
            "    return m.filter(LinkedHashMap.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInstanceOf_methodCall() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.util.HashMap;",
            "import java.util.LinkedHashMap;",
            "class Test {",
            "  Optional<HashMap> f(Optional<HashMap> m) {",
            "    return m.filter(x -> LinkedHashMap.class.isInstance(x));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInstanceOf2() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "import java.util.HashMap;",
            "import java.util.LinkedHashMap;",
            "class Test {",
            "  Optional<HashMap<String, Integer>> f(Optional<HashMap<String,Integer>> m) {",
            "    return m.filter(LinkedHashMap.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeInstanceOfWithGenerics() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  <T> Optional<T> f(Optional<T> t) {",
            "    return t.filter(Object.class::isInstance);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rawTypes() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  boolean f(Object o, Class c) {",
            "    return c.isInstance(o);",
            "  }",
            "  <T> Optional<T> f(Optional<T> t, Class c) {",
            "    return t.filter(c::isInstance);",
            "  }",
            "}")
        .doTest();
  }
}
