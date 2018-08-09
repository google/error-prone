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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link MissingTestCallTest} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class MissingTestCallTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(MissingTestCall.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Case.java",
            "import com.google.common.testing.EqualsTester;",
            "import com.google.errorprone.BugCheckerRefactoringTestHelper;",
            "import com.google.errorprone.CompilationTestHelper;",
            "import org.junit.Test;",
            "class Case {",
            "  @Test",
            "  // BUG: Diagnostic contains:",
            "  void test() {",
            "    new EqualsTester().addEqualityGroup(this, this);",
            "    hashCode();",
            "  }",
            "  @Test",
            "  // BUG: Diagnostic contains:",
            "  void test2(CompilationTestHelper helper) {",
            "    helper.addSourceFile(\"Foo.java\");",
            "  }",
            "  @Test",
            "  // BUG: Diagnostic contains:",
            "  void test3(BugCheckerRefactoringTestHelper helper) {",
            "    helper.addInput(\"Foo.java\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Case.java",
            "import com.google.common.testing.EqualsTester;",
            "import com.google.errorprone.CompilationTestHelper;",
            "import org.junit.Test;",
            "class Case {",
            "  CompilationTestHelper helper;",
            "  @Test",
            "  void test() {",
            "    new EqualsTester().addEqualityGroup(this, this).testEquals();",
            "  }",
            "  @Test",
            "  void doesNotMatchIfNotAtEnd() {",
            "    helper.addSourceFile(\"Foo.java\");",
            "    hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNotTest() {
    helper
        .addSourceLines(
            "Case.java",
            "import com.google.common.testing.EqualsTester;",
            "class Case {",
            "  private EqualsTester et;",
            "  void add() {",
            "    et.addEqualityGroup(this, this);",
            "  }",
            "}")
        .doTest();
  }
}
