/*
 * Copyright 2023 The Error Prone Authors.
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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link SuperEqualsIsObjectEquals}Test */
@RunWith(JUnit4.class)
public class SuperEqualsIsObjectEqualsTest {
  @Test
  public void positive() {
    helper()
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  int i;",
            "  @Override",
            "  public boolean equals(Object obj) {",
            "    if (obj instanceof Foo) {",
            "      return i == ((Foo) obj).i;",
            "    }",
            "    // BUG: Diagnostic contains: ",
            "    return super.equals(obj);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveOtherSupertypeWithoutEquals() {
    helper()
        .addSourceLines(
            "Foo.java",
            "class Foo extends Exception {",
            "  int i;",
            "  @Override",
            "  public boolean equals(Object obj) {",
            "    if (obj instanceof Foo) {",
            "      return i == ((Foo) obj).i;",
            "    }",
            "    // BUG: Diagnostic contains: ",
            "    return super.equals(obj);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeOtherSupertypeWithEquals() {
    helper()
        .addSourceLines(
            "Foo.java",
            "import java.util.AbstractSet;",
            "abstract class Foo extends AbstractSet<String> {",
            "  int i;",
            "  @Override",
            "  public boolean equals(Object obj) {",
            "    if (obj instanceof Foo) {",
            "      return i == ((Foo) obj).i;",
            "    }",
            "    return super.equals(obj);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativePureSuperDelegation() {
    helper()
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  @Override",
            "  public boolean equals(Object obj) {",
            "    return super.equals(obj);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper()
        .addInputLines(
            "Foo.java",
            "class Foo {",
            "  int i;",
            "  @Override",
            "  public boolean equals(Object obj) {",
            "    if (obj instanceof Foo) {",
            "      return i == ((Foo) obj).i;",
            "    }",
            "    return super.equals(obj);",
            "  }",
            "}")
        .addOutputLines(
            "Foo.java",
            "class Foo {",
            "  int i;",
            "  @Override",
            "  public boolean equals(Object obj) {",
            "    if (obj instanceof Foo) {",
            "      return i == ((Foo) obj).i;",
            "    }",
            "    return this == obj;",
            "  }",
            "}")
        .doTest();
  }

  private CompilationTestHelper helper() {
    return CompilationTestHelper.newInstance(SuperEqualsIsObjectEquals.class, getClass());
  }

  private BugCheckerRefactoringTestHelper refactoringHelper() {
    return BugCheckerRefactoringTestHelper.newInstance(SuperEqualsIsObjectEquals.class, getClass());
  }
}
