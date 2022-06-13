/*
 * Copyright 2019 The Error Prone Authors.
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

/** Tests for {@link ExtendsAutoValue}. */
@RunWith(JUnit4.class)
public class ExtendsAutoValueTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(ExtendsAutoValue.class, getClass())
          .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()));

  @Test
  public void extendsAutoValue_goodNoSuperclass() {
    helper
        .addSourceLines(
            "TestClass.java", //
            "public class TestClass {}")
        .doTest();
  }

  @Test
  public void extendsAutoValue_goodSuperclass() {
    helper
        .addSourceLines(
            "TestClass.java", //
            "class SuperClass {}",
            "public class TestClass extends SuperClass {}")
        .doTest();
  }

  @Test
  public void extendsAutoValue_goodAutoValueExtendsSuperclass() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.auto.value.AutoValue;",
            "public class TestClass {}",
            "@AutoValue class AutoClass extends TestClass {}")
        .doTest();
  }

  @Test
  public void extendsAutoValue_goodGeneratedIgnored() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.auto.value.AutoValue;",
            "import javax.annotation.processing.Generated;",
            "@AutoValue class AutoClass {}",
            "@Generated(value=\"hi\") public class TestClass extends AutoClass {}")
        .doTest();
  }

  @Test
  public void extendsAutoValue_bad() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue class AutoClass {}",
            "// BUG: Diagnostic contains: ExtendsAutoValue",
            "public class TestClass extends AutoClass {}")
        .doTest();
  }

  @Test
  public void extendsAutoOneOf_bad() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.auto.value.AutoOneOf;",
            "@AutoOneOf(AutoClass.Kind.class) class AutoClass {",
            "  enum Kind {}",
            "}",
            "// BUG: Diagnostic contains: ExtendsAutoValue",
            "public class TestClass extends AutoClass {",
            "}")
        .doTest();
  }

  @Test
  public void extendsAutoValue_badNoImport() {
    helper
        .addSourceLines(
            "TestClass.java",
            "@com.google.auto.value.AutoValue class AutoClass {}",
            "// BUG: Diagnostic contains: ExtendsAutoValue",
            "public class TestClass extends AutoClass {}")
        .doTest();
  }

  @Test
  public void extendsAutoValue_badInnerClass() {
    helper
        .addSourceLines(
            "OuterClass.java",
            "import com.google.auto.value.AutoValue;",
            "public class OuterClass { ",
            "  @AutoValue abstract static class AutoClass {}",
            "  // BUG: Diagnostic contains: ExtendsAutoValue",
            "  class TestClass extends AutoClass {}",
            "}")
        .doTest();
  }

  @Test
  public void extendsAutoValue_badInnerStaticClass() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.auto.value.AutoValue;",
            "class OuterClass { ",
            "  @AutoValue static class AutoClass {}",
            "}",
            "// BUG: Diagnostic contains: ExtendsAutoValue",
            "public class TestClass extends OuterClass.AutoClass {}")
        .doTest();
  }

  @Test
  public void extendsAutoValue_badButSuppressed() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue class AutoClass {}",
            "@SuppressWarnings(\"ExtendsAutoValue\")",
            "public class TestClass extends AutoClass {}")
        .doTest();
  }

  @Test
  public void extendsAutoValue_innerClassExtends() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue class AutoClass {}",
            "",
            "public class TestClass {",
            "  // BUG: Diagnostic contains: ExtendsAutoValue",
            "  public class Extends extends AutoClass {}",
            "}")
        .doTest();
  }

  @Test
  public void extendsAutoValue_innerClassExtends_generated() {
    helper
        .addSourceLines(
            "TestClass.java",
            "import com.google.auto.value.AutoValue;",
            "import javax.annotation.processing.Generated;",
            "@AutoValue class AutoClass {}",
            "",
            "@Generated(\"generator\")",
            "public class TestClass {",
            "  public class Extends extends AutoClass {}",
            "}")
        .doTest();
  }
}
