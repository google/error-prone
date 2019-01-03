/*
 * Copyright 2019 The Error Prone Authors.
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

import static org.junit.Assume.assumeTrue;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link DuplicateMapKeys} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class DuplicateMapKeysTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(DuplicateMapKeys.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    assumeTrue(RuntimeVersion.isAtLeast9());
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "import static java.util.Map.entry;",
            "import java.util.Map;",
            "class A {",
            "  public static void test() {",
            "    // BUG: Diagnostic contains: Foo",
            "    Map<String, String> map = Map.ofEntries(",
            "    entry(\"Foo\", \"Bar\"),",
            "    entry(\"Ping\", \"Pong\"),",
            "    entry(\"Kit\", \"Kat\"),",
            "    entry(\"Foo\", \"Bar\"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    assumeTrue(RuntimeVersion.isAtLeast9());
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "import static java.util.Map.entry;",
            "import java.util.Map;",
            "class A {",
            "  public static void test() {",
            "    Map<String, String> map = Map.ofEntries(",
            "    entry(\"Foo\", \"Bar\"),",
            "    entry(\"Ping\", \"Pong\"),",
            "    entry(\"Kit\", \"Kat\"),",
            "    entry(\"Food\", \"Bar\"));",
            "  }",
            "}")
        .doTest();
  }
}
