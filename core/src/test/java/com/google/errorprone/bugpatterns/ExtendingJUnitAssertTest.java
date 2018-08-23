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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author kayco@google.com (Kayla Walker) */
@RunWith(JUnit4.class)
public class ExtendingJUnitAssertTest {

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ExtendingJUnitAssert(), getClass());

  @Test
  public void positive() {
    refactoringTestHelper
        .addInputLines(
            "in/Foo.java",
            "import org.junit.Assert;",
            "class Foo extends Assert {",
            "public void test() {",
            "assertEquals(5, 5);",
            "}",
            "}")
        .addOutputLines(
            "in/Foo.java",
            "import static org.junit.Assert.assertEquals;",
            "import org.junit.Assert;",
            "class Foo {",
            "public void test() {",
            "assertEquals(5, 5);",
            "}",
            "}")
        .doTest();
  }

  @Test
  public void positiveWithGenerics() {
    refactoringTestHelper
        .addInputLines(
            "in/Foo.java",
            "import org.junit.Assert;",
            "class Foo<T extends String> extends Assert {",
            "public void test() {",
            "assertEquals(5, 5);",
            "assertNull(2);",
            "assertNotNull(3);",
            "}",
            "}")
        .addOutputLines(
            "in/Foo.java",
            "import static org.junit.Assert.assertEquals;",
            "import static org.junit.Assert.assertNotNull;",
            "import static org.junit.Assert.assertNull;",
            "import org.junit.Assert;",
            "class Foo<T extends String> {",
            "public void test() {",
            "assertEquals(5, 5);",
            "assertNull(2);",
            "assertNotNull(3);",
            "}",
            "}")
        .doTest();
  }

  @Test
  public void positiveWithImplements() {
    refactoringTestHelper
        .addInputLines(
            "in/Foo.java",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import org.junit.Assert;",
            "abstract class Foo extends Assert implements ClassTreeMatcher {",
            "public void test() {",
            "assertEquals(5, 5);",
            "}",
            "}")
        .addOutputLines(
            "in/Foo.java",
            "import static org.junit.Assert.assertEquals;",
            "import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;",
            "import org.junit.Assert;",
            "abstract class Foo implements ClassTreeMatcher {",
            "public void test() {",
            "assertEquals(5, 5);",
            "}",
            "}")
        .doTest();
  }
}
