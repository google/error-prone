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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author bennostein@google.com (Benno Stein) */
@RunWith(JUnit4.class)
public class NullableDereferenceTest {

  @Test
  public void testAnnotatedFormal() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/AnnotatedFormalTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class AnnotatedFormalTest {",
            "  public void testPos(@Nullable Object nullable) {",
            "    // BUG: Diagnostic contains: possibly null",
            "    nullable.toString();",
            "  }",
            "  public void testNeg(Object unannotated, @NonNull Object nonnull) {",
            "    unannotated.toString();",
            "    nonnull.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDataflow() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/DataflowTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class DataflowTest {",
            "  public void test(@Nullable Object o) {",
            "    // BUG: Diagnostic contains: possibly null",
            "    o.toString();",
            // no error here: successful deref on previous line implies `o` is not null
            "    o.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInference() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/InferenceTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "public class InferenceTest {",
            "  public void test(@Nullable Object nullable, @NonNull Object nonnull) {",
            "    // BUG: Diagnostic contains: possibly null",
            "    nullable.toString();",
            "    // BUG: Diagnostic contains: possibly null",
            "    id(nullable).toString();",
            "    nonnull.toString();",
            "    id(nonnull).toString();",
            "  }",
            "  static <T> T id(T t) { return t; }",
            "}")
        .doTest();
  }

  @Test
  public void testNullLiteral() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullLiteralTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NullLiteralTest {",
            "  public void test() {",
            "    // BUG: Diagnostic contains: definitely null",
            "    ((Object) null).toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testStatics() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/StaticsTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class StaticsTest {",
            "  static Object staticField;",
            "  Object instanceField;",
            "  public void test() {",
            "    System.out.println();",
            "    System.out.println(StaticsTest.staticField);",
            "    System.out.println(staticField);",
            "    System.out.println(this.instanceField);",
            "    System.out.println(instanceField);",
            "    // BUG: Diagnostic contains: definitely null",
            "    System.out.println(((StaticsTest) null).instanceField);",
            "  }",
            "}")
        .doTest();
  }

  private CompilationTestHelper createCompilationTestHelper() {
    return CompilationTestHelper.newInstance(NullableDereference.class, getClass());
  }
}
