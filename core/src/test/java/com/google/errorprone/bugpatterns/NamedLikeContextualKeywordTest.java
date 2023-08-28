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

import static org.junit.Assume.assumeTrue;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link NamedLikeContextualKeywordTest}. */
@RunWith(JUnit4.class)
public final class NamedLikeContextualKeywordTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(NamedLikeContextualKeyword.class, getClass());

  @Test
  public void instanceMethodName_error() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static Throwable foo;",
            "  public Test() {",
            "  }",
            " ",
            "  // BUG: Diagnostic contains: [NamedLikeContextualKeyword]",
            "  public void yield() { ",
            "    foo = new NullPointerException(\"uh oh\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticMethodName_error() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static Throwable foo;",
            "  public Test() {",
            "  }",
            " ",
            "  // BUG: Diagnostic contains: [NamedLikeContextualKeyword]",
            "  public static void yield() { ",
            "    foo = new NullPointerException(\"uh oh\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void autoOneOfMethodName_noError() {
    helper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.processing.Generated;",
            "@Generated(\"com.google.auto.value.processor.AutoOneOfProcessor\")",
            "class Test  {",
            "  static Throwable foo;",
            "  public Test() {",
            "  }",
            " ",
            "  public static void yield() { ",
            "    foo = new NullPointerException(\"uh oh\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void autoValueMethodName_noError() {
    helper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.processing.Generated;",
            "@Generated(\"com.google.auto.value.processor.AutoValueProcessor\")",
            "class Test  {",
            "  static Throwable foo;",
            "  public Test() {",
            "  }",
            " ",
            "  public static void yield() { ",
            "    foo = new NullPointerException(\"uh oh\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void generatedButNotAuto_error() {
    helper
        .addSourceLines(
            "Test.java",
            "import javax.annotation.processing.Generated;",
            "@Generated(\"com.google.foo.Bar\")",
            "class Test  {",
            "  static Throwable foo;",
            "  public Test() {",
            "  }",
            " ",
            "  // BUG: Diagnostic contains: [NamedLikeContextualKeyword]",
            "  public static void yield() { ",
            "    foo = new NullPointerException(\"uh oh\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void className_error() {
    helper
        .addSourceLines(
            "module.java",
            "// BUG: Diagnostic contains: [NamedLikeContextualKeyword]",
            "class module {",
            "  public module() {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void yieldInSwitch_noError() {

    assumeTrue(RuntimeVersion.isAtLeast14());
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public Test(int foo) {",
            "    int x = switch(foo) {",
            "      case 17: ",
            "        yield 17;",
            "      default:",
            "        yield 0;",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void interfaceImplementation_noError() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test implements RegrettablyNamedInterface {",
            "  static Throwable foo;",
            "  public Test() {",
            "  }",
            " ",
            "  public void yield() { ",
            "    foo = new NullPointerException(\"uh oh\");",
            "  }",
            "}")
        .addSourceLines(
            "RegrettablyNamedInterface.java",
            "interface RegrettablyNamedInterface {",
            "  @SuppressWarnings(\"NamedLikeContextualKeyword\")",
            "  void yield();",
            "}")
        .doTest();
  }

  @Test
  public void nonAnnotatedOverride_noError() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test extends RegrettablyNamedClass {",
            "  static Throwable foo;",
            "  public Test() {",
            "  }",
            " ",
            "  public void yield() { ",
            "    foo = new NullPointerException(\"uh oh\");",
            "  }",
            "}")
        .addSourceLines(
            "RegrettablyNamedClass.java",
            "class RegrettablyNamedClass {",
            "  @SuppressWarnings(\"NamedLikeContextualKeyword\")",
            "  void yield() {}",
            "}")
        .doTest();
  }

  @Test
  public void annotatedOverride_noError() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test extends RegrettablyNamedClass {",
            "  static Throwable foo;",
            "  public Test() {",
            "  }",
            " ",
            "  @Override",
            "  public void yield() { ",
            "    foo = new NullPointerException(\"uh oh\");",
            "  }",
            "}")
        .addSourceLines(
            "RegrettablyNamedClass.java",
            "class RegrettablyNamedClass {",
            "  @SuppressWarnings(\"NamedLikeContextualKeyword\")",
            "  void yield() {}",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  // BUG: Diagnostic contains: [NamedLikeContextualKeyword]",
            "  void yield() {}",
            "  {",
            "    // BUG: Diagnostic contains: this.yield();",
            "    yield();",
            "  }",
            "}")
        .setArgs(ImmutableList.of("--release", "11"))
        .doTest();
  }

  @Test
  public void enclosing() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  class A {",
            "    // BUG: Diagnostic contains: [NamedLikeContextualKeyword]",
            "    void yield() {}",
            "    class B {",
            "      class C {",
            "        {",
            "          // BUG: Diagnostic contains: A.this.yield();",
            "          yield();",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .setArgs(ImmutableList.of("--release", "11"))
        .doTest();
  }

  @Test
  public void staticMethod() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: [NamedLikeContextualKeyword]",
            "  static void yield() {}",
            "  static class I {",
            "    {",
            "      // BUG: Diagnostic contains: Test.yield();",
            "      yield();",
            "    }",
            "  }",
            "}")
        .setArgs(ImmutableList.of("--release", "11"))
        .doTest();
  }
}
