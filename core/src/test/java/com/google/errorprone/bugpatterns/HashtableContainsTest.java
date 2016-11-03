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

/** {@link HashtableContains}Test */
@RunWith(JUnit4.class)
public class HashtableContainsTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(HashtableContains.class, getClass());
  }

  @Test
  public void positive_CHM() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.concurrent.ConcurrentHashMap;",
            "class Test {",
            "  void f(ConcurrentHashMap<String, Integer> m, Integer v) {",
            "    // BUG: Diagnostic contains: containsValue(v)",
            "    m.contains(v);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_Hashtable() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.Hashtable;",
            "class Test {",
            "  void f(Hashtable<String, Integer> m, Integer v) {",
            "    // BUG: Diagnostic contains: containsValue(v)",
            "    m.contains(v);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_wildcardUpperBound() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.Hashtable;",
            "class Test {",
            "  void f(Hashtable<String, ? extends Number> m, Integer v) {",
            "    // BUG: Diagnostic contains: containsValue(v)",
            "    m.contains(v);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_wildcardLowerBound() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.Hashtable;",
            "class Test {",
            "  void f(Hashtable<String, ? super Integer> m, Integer v) {",
            "    // BUG: Diagnostic contains: containsValue(v)",
            "    m.contains(v);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_wildcard() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.Hashtable;",
            "class Test {",
            "  void f(Hashtable<String, ?> m, String k) {",
            "    // BUG: Diagnostic contains: 'java.lang.String' could be a key or a value",
            "    // Did you mean 'm.containsValue(k);' or 'm.containsKey(k);'?",
            "    m.contains(k);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_containsKey() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.Hashtable;",
            "class Test {",
            "  void f(Hashtable<String, Integer> m, String k) {",
            "    // BUG: Diagnostic contains:",
            "    // argument type 'java.lang.String' looks like a key",
            "    // Did you mean 'm.containsKey(k);'",
            "    m.contains(k);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_extendsHashtable() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.Hashtable;",
            "class MyHashTable<K, V> extends Hashtable<K, V> {",
            "  @Override public boolean contains(Object v) {",
            "    // BUG: Diagnostic contains:",
            "    // Did you mean 'return containsValue(v);' or 'return containsKey(v);'?",
            "    return contains(v);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_containsAmbiguous() {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "import java.util.Hashtable;",
            "class Test {",
            "  void f(Hashtable<Number, Integer> m, Integer v) {",
            "    // BUG: Diagnostic contains: 'java.lang.Number' could be a key or a value",
            "    // Did you mean 'm.containsValue(v);' or 'm.containsKey(v);'?",
            "    m.contains(v);",
            "  }",
            "}")
        .doTest();
  }
}
