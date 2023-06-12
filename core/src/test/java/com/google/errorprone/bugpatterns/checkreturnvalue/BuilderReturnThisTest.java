/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BuilderReturnThisTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(BuilderReturnThis.class, getClass());

  @Test
  public void negative() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  static class TestBuilder {",
            "    static TestBuilder builder() {",
            "      return new TestBuilder();",
            "    }",
            "    Test build() {",
            "      return new Test();",
            "    }",
            "    TestBuilder setFoo(String foo) {",
            "      return this;",
            "    }",
            "    TestBuilder setBar(String bar) {",
            "      return this;",
            "    }",
            "    TestBuilder setBaz(String baz) {",
            "      return setFoo(baz).setBar(baz);",
            "    }",
            "    TestBuilder setTernary(String baz) {",
            "      return true ? setFoo(baz) : this;",
            "    }",
            "    TestBuilder setCast(String baz) {",
            "      return (TestBuilder) this;",
            "    }",
            "    TestBuilder setParens(String bar) {",
            "      return (this);",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  static class TestBuilder {",
            "    TestBuilder setBar(String bar) {",
            "      return new TestBuilder();",
            "    }",
            "    TestBuilder setTernary(String baz) {",
            "      return true ? new TestBuilder() : this;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "class Test {",
            "  static class TestBuilder {",
            "    @CheckReturnValue",
            "    TestBuilder setBar(String bar) {",
            "      return new TestBuilder();",
            "    }",
            "    @CheckReturnValue",
            "    TestBuilder setTernary(String baz) {",
            "      return true ? new TestBuilder() : this;",
            "    }",
            "  }",
            "}")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }
}
