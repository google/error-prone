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

package com.google.errorprone.bugpatterns;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link AutoValueBuilderDefaultsInConstructor}. */
@RunWith(JUnit4.class)
public final class AutoValueBuilderDefaultsInConstructorTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(AutoValueBuilderDefaultsInConstructor.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(
              AutoValueBuilderDefaultsInConstructor.class, getClass())
          .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()));

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "import com.google.auto.value.AutoValue;",
            "class Test {",
            "  @AutoValue.Builder",
            "  abstract class Builder {",
            "    Builder() {}",
            "    abstract void setFoo(int foo);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "package test;",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract int foo();",
            "  Builder builder() {",
            "    return new AutoValue_Test.Builder();",
            "  }",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    Builder() {",
            "      this.setFoo(1);",
            "    }",
            "    abstract Builder setFoo(int foo);",
            "    abstract Test build();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "package test;",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract int foo();",
            "  Builder builder() {",
            "    return new AutoValue_Test.Builder().setFoo(1);",
            "  }",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder setFoo(int foo);",
            "    abstract Test build();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_nonAbstractMethodCalled() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "package test;",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract int foo();",
            "  Builder builder() {",
            "    return new AutoValue_Test.Builder();",
            "  }",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    Builder() {",
            "      doSomethingOdd();",
            "    }",
            "    void doSomethingOdd() {}",
            "    abstract Builder setFoo(int foo);",
            "    abstract Test build();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
