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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ParameterName}Test */
@RunWith(JUnit4.class)
public class ParameterNameTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ParameterName.class, getClass());

  @Test
  public void positive() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new ParameterName(), getClass())
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(int foo, int bar) {}",
            "  {",
            "    f(/* bar= */ 1, /* foo= */ 2);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(int foo, int bar) {}",
            "  {",
            "    f(/* foo= */ 1, /* bar= */ 2);",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f(int foo, int bar) {}",
            "  {",
            "    f(/* foo= */ 1, 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void issue781() {
    testHelper
        .addSourceLines(
            "a/Baz.java",
            "package a.b;",
            "import a.AbstractFoo;",
            "class Baz extends AbstractFoo {",
            "  @Override",
            "  protected String getFoo() {",
            "    return \"foo\";",
            "  }",
            "}")
        .addSourceLines(
            "a/AbstractFoo.java",
            "package a;",
            "import java.util.function.Function;",
            "class Bar {",
            "  private final Function<String, String> args;",
            "  public Bar(Function<String, String> args) {",
            "    this.args = args;",
            "  }",
            "}",
            "public abstract class AbstractFoo {",
            "  protected abstract String getFoo();",
            "  private String getCommandArguments(String parameters) {",
            "    return null;",
            "  }",
            "  public AbstractFoo() {",
            "    new Bar(this::getCommandArguments);",
            "  }",
            "}")
        .doTest();
  }
}
