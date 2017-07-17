/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** @author jdesprez@google.com (Julien Desprez) */
@RunWith(JUnit4.class)
public class JUnit4ClassUsedInJUnit3Test {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnit4ClassUsedInJUnit3.class, getClass());

  @Test
  public void negative_regular_junit3() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import junit.framework.TestCase;",
            "public class Foo extends TestCase {",
            "  public void testName1() { System.out.println(\"here\");}",
            "  public void testName2() {}",
            "}")
        .doTest();
  }

  @Test
  public void negative_assume_in_junit4() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.junit.Assume;",
            "import org.junit.Test;",
            "@RunWith(JUnit4.class)",
            "public class Foo {",
            "  @Test",
            "  public void testOne() {",
            "    Assume.assumeTrue(true);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_annotation_in_junit4() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.junit.Rule;",
            "import org.junit.rules.TemporaryFolder;",
            "import org.junit.Test;",
            "import org.junit.Ignore;",
            "@RunWith(JUnit4.class)",
            "public class Foo {",
            "  @Rule public TemporaryFolder folder= new TemporaryFolder();",
            "  @Ignore",
            "  @Test",
            "  public void testOne() {}",
            "  @Test public void testTwo() {}",
            "}")
        .doTest();
  }

  @Test
  public void positive_assume_in_test() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "import org.junit.Assume;",
            "public class Foo extends TestCase {",
            "  public void testName1() { System.out.println(\"here\");}",
            "  public void testName2() {",
            "    // BUG: Diagnostic contains: Assume",
            "    Assume.assumeTrue(true);",
            "  }",
            "  public void testName3() {}",
            "}")
        .doTest();
  }

  @Test
  public void positive_assume_in_setUp() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "import org.junit.Assume;",
            "public class Foo extends TestCase {",
            "  @Override public void setUp() {",
            "    // BUG: Diagnostic contains: Assume",
            "    Assume.assumeTrue(true);",
            "  }",
            "  public void testName1() {}",
            "}")
        .doTest();
  }

  @Test
  public void positive_assume_in_tear_down() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "import org.junit.Assume;",
            "public class Foo extends TestCase {",
            "  @Override public void tearDown() {",
            "    // BUG: Diagnostic contains: Assume",
            "    Assume.assumeTrue(true);",
            "  }",
            "  public void testName1() {}",
            "}")
        .doTest();
  }

  @Test
  public void positive_static_assume_in_test() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import junit.framework.TestCase;",
            "import org.junit.Test;",
            "import static org.junit.Assume.assumeTrue;",
            "public class Foo extends TestCase {",
            "  public void testName1() { System.out.println(\"here\");}",
            "  public void testName2() {",
            "    // BUG: Diagnostic contains: Assume",
            "    assumeTrue(true);",
            "  }",
            "  public void testName3() {}",
            "}")
        .doTest();
  }

  @Test
  public void positive_ignore_on_test() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import junit.framework.TestCase;",
            "import org.junit.Ignore;",
            "public class Foo extends TestCase {",
            "  public void testName1() {}",
            "  // BUG: Diagnostic contains: @Ignore",
            "  @Ignore",
            "  public void testName2() {}",
            "}")
        .doTest();
  }

  @Test
  public void positive_ignore_on_class() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import junit.framework.TestCase;",
            "import org.junit.Ignore;",
            "// BUG: Diagnostic contains: @Ignore",
            "@Ignore",
            "public class Foo extends TestCase {",
            "  public void testName1() {}",
            "  public void testName2() {}",
            "}")
        .doTest();
  }

  @Test
  public void positive_rule_in_junit3() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import junit.framework.TestCase;",
            "import org.junit.Rule;",
            "import org.junit.rules.TemporaryFolder;",
            "public class Foo extends TestCase {",
            "  // BUG: Diagnostic contains: @Rule",
            "  @Rule public TemporaryFolder folder = new TemporaryFolder();",
            "  public void testName1() {}",
            "  public void testName2() {}",
            "}")
        .doTest();
  }
}
