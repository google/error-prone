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

import static org.junit.Assume.assumeFalse;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link HashCodeToString}Test */
@RunWith(JUnit4.class)
public class HashCodeToStringTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(HashCodeToString.class, getClass());

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "HashCodeOnly.java",
            "public class HashCodeOnly {",
            "  // BUG: Diagnostic contains: HashCodeToString",
            "  public int hashCode() {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_bothHashCodeAndToString() {
    compilationHelper
        .addSourceLines(
            "HashCodeAndToString.java",
            "public class HashCodeAndToString {",
            "  public int hashCode() {",
            "    return 0;",
            "  }",
            "  public String toString() {",
            "    return \"42\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_toStringOnly() {
    compilationHelper
        .addSourceLines(
            "ToStringOnly.java",
            "public class ToStringOnly {",
            "  public String toString() {",
            "    return \"42\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_neitherHashCodeAndToString() {
    compilationHelper
        .addSourceLines(
            "NeitherHashCodeAndToString.java", //
            "public class NeitherHashCodeAndToString {",
            "}")
        .doTest();
  }

  @Test
  public void superClassWithoutToString() {
    compilationHelper
        .addSourceLines("Super.java", "abstract class Super {}")
        .addSourceLines(
            "Test.java",
            "class Test extends Super {",
            "  // BUG: Diagnostic contains: HashCodeToString",
            "  public int hashCode() { return 0; }",
            "}")
        .doTest();
  }

  @Test
  public void inherited() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "class Super {",
            "  public String toString() {",
            "    return \"42\";",
            "  }",
            "}")
        .addSourceLines(
            "Test.java", //
            "class Test extends Super {",
            "  public int hashCode() { return 0; }",
            "}")
        .doTest();
  }

  @Test
  public void interfaceHashCode() {
    compilationHelper
        .addSourceLines(
            "I.java", //
            "interface I {",
            "  int hashCode();",
            "}")
        .doTest();
  }

  @Test
  public void abstractToString() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "abstract class Super {",
            "  public abstract int hashCode();",
            "  public abstract String toString();",
            "}")
        .doTest();
  }

  @Test
  public void abstractNoToString() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "abstract class Super {",
            "  // BUG: Diagnostic contains:",
            "  public abstract int hashCode();",
            "}")
        .doTest();
  }

  @Test
  public void suppressOnHashCode() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @SuppressWarnings(\"HashCodeToString\")",
            "  public int hashCode() { return 0; }",
            "}")
        .doTest();
  }

  @Test
  public void nopHashCode() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public int hashCode() {",
            "    return super.hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dontFlagJavaLangObject() {
    assumeFalse(RuntimeVersion.isAtLeast9());
    compilationHelper
        .addSourceLines(
            "Object.java",
            "package java.lang;",
            "public class Object {",
            "  public int hashCode() {",
            "    return 0;",
            "  }",
            "  public String toString() {",
            "    return \"42\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dontFlagAutoValue() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  @Override",
            "  public int hashCode() {",
            "    return 1;",
            "  }",
            "}")
        .doTest();
  }
}
