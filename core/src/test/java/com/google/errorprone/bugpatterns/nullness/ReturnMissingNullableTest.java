/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author kmb@google.com (Kevin Bierhoff) */
@RunWith(JUnit4.class)
public class ReturnMissingNullableTest {

  @Test
  public void testLiteralNullReturn() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDefiniteNullReturn() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage(String message) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return message != null ? null : message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMaybeNullReturn() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return x >= 0 ? null : \"negative\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNullableMethodCall() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableMethodCallTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableMethodCallTest {",
            "  public String getMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return toSignString(x);",
            "  }",
            "",
            "  @Nullable",
            "  private String toSignString(int x) {",
            "    return x < 0 ? \"negative\" : \"positive\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNullableMethodCall_alternativeAnnotation() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/anno/my/Nullable.java",
            "package com.google.anno.my;",
            "public @interface Nullable {}")
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableMethodCallTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NullableMethodCallTest {",
            "  public String getMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return toSignString(x);",
            "  }",
            "",
            "  @com.google.anno.my.Nullable",
            "  private String toSignString(int x) {",
            "    return x < 0 ? \"negative\" : \"positive\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNullableField() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableFieldTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableFieldTest {",
            "  @Nullable private String message;",
            "  public String getMessage() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNullableParameter() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  public String apply(@Nullable String message) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    return message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyAnnotated() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class LiteralNullReturnTest {",
            "  @Nullable",
            "  public String getMessage() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullLiteral() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/LiteralNullReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class LiteralNullReturnTest {",
            "  public String getMessage() {",
            "    return \"hello\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullMethod() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullMethodTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullMethodTest {",
            "  public String getMessage(int x) {",
            "    return String.valueOf(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullField() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullFieldTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullFieldTest {",
            "  private String message;",
            "  public String getMessage() {",
            "    return message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullParameter() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullParameterTest {",
            "  public String apply(String message) {",
            "    return message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_this() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ThisTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class ThisTest {",
            "  private String message;",
            "  public ThisTest setMessage(String message) {",
            "    this.message = message;",
            "    return this;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_capturedLocal() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/CapturedLocalTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public abstract class CapturedLocalTest {",
            "  public abstract String getMessage();",
            "  public CapturedLocalTest withMessage(final String message) {",
            "    return new CapturedLocalTest() {",
            "      public String getMessage() {",
            "        return message;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  /**
   * Makes sure the check never flags methods returning a primitive. Returning null from them is a
   * bug, of course, but we're not trying to find those bugs in this check.
   */
  @Test
  public void testNegativeCases_primitiveReturnType() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/PrimitiveReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class PrimitiveReturnTest {",
            "  public int getCount() {",
            "    return (Integer) null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_voidMethod() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/VoidMethodTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class VoidMethodTest {",
            "  public void run(int iterations) {",
            "    if (iterations <= 0) { return; }",
            "    run(iterations - 1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_voidTypedMethod() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/VoidTypeTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class VoidTypeTest {",
            "  public Void run(int iterations) {",
            "    if (iterations <= 0) { return null; }",
            "    run(iterations - 1);",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nullableReturnInLambda() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/MissingNullableReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class MissingNullableReturnTest {",
            "  public static final java.util.function.Function<String, String> IDENTITY =",
            "      (s -> { return s != null ? s : null; });",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_returnLambda() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/MissingNullableReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class MissingNullableReturnTest {",
            "  public static java.util.function.Function<String, String> identity() {",
            "    return s -> s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_returnParenthesizedLambda() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/MissingNullableReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class MissingNullableReturnTest {",
            "  public static java.util.function.Function<String, String> identity() {",
            "    return (s -> s);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuggestNonJsr305Nullable() throws Exception {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "class T {",
            "  @Nullable private final Object obj1 = null;",
            "  private final Object method() { return null; }",
            "  @interface Nullable {}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class T {",
            "  @Nullable private final Object obj1 = null;",
            "  @Nullable private final Object method() { return null; }",
            "  @interface Nullable {}",
            "}")
        .doTest();
  }

  @Test
  public void testNonAnnotationNullable() throws Exception {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "class T {",
            "  private final Object method() { return null; }",
            "  class Nullable {}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class T {",
            "  @javax.annotation.Nullable private final Object method() { return null; }",
            "  class Nullable {}",
            "}")
        .doTest();
  }

  private CompilationTestHelper createCompilationTestHelper() {
    return CompilationTestHelper.newInstance(ReturnMissingNullable.class, getClass());
  }

  private BugCheckerRefactoringTestHelper createRefactoringTestHelper() {
    return BugCheckerRefactoringTestHelper.newInstance(new ReturnMissingNullable(), getClass());
  }
}
