/*
 * Copyright 2020 The Error Prone Authors.
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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link AutoValueSubclassLeaked}. */
@RunWith(JUnit4.class)
public final class AutoValueSubclassLeakedTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(AutoValueSubclassLeaked.class, getClass())
          .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()));

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            "package test;",
            "import com.google.auto.value.AutoValue;",
            "class Test {",
            "  @AutoValue",
            "  abstract static class Foo {",
            "    abstract int foo();",
            "  }",
            "}")
        .addSourceLines(
            "Bar.java",
            "package test;",
            "class Bar {",
            "  public static Test.Foo create(int i) {",
            "    // BUG: Diagnostic contains:",
            "    return new AutoValue_Test_Foo(i);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeBuilder() {
    helper
        .addSourceLines(
            "Test.java",
            "package test;",
            "import com.google.auto.value.AutoValue;",
            "class Test {",
            "  @AutoValue",
            "  abstract static class Foo {",
            "    abstract int foo();",
            "    @AutoValue.Builder",
            "    abstract static class Builder {",
            "      abstract Builder setFoo(int i);",
            "      abstract Foo build();",
            "    }",
            "    public static Builder builder() {",
            "      return new AutoValue_Test_Foo.Builder();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            "package test;",
            "import com.google.auto.value.AutoValue;",
            "class Test {",
            "  @AutoValue",
            "  abstract static class Foo {",
            "    abstract int foo();",
            "    public static Foo create(int i) {",
            "      return new AutoValue_Test_Foo(i);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void generatedCode() {
    helper
        .addSourceLines(
            "Test.java",
            "package test;",
            "import com.google.auto.value.AutoValue;",
            "class Test {",
            "  @AutoValue",
            "  abstract static class Foo {",
            "    abstract int foo();",
            "  }",
            "}")
        .addSourceLines(
            "Bar.java",
            "package test;",
            "import javax.annotation.processing.Generated;",
            "",
            "@Generated(\"generator\")",
            "class Bar {",
            "  public static Test.Foo create(int i) {",
            "    return new AutoValue_Test_Foo(i);",
            "  }",
            "}")
        .doTest();
  }
}
