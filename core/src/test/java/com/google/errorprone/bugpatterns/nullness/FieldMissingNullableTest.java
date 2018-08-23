/*
 * Copyright 2016 The Error Prone Authors.
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
public class FieldMissingNullableTest {

  @Test
  public void testLiteralNullAssignment() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class FieldMissingNullTest {",
            "  private String message = \"hello\";",
            "  public void reset() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    message = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDefiniteNullAssignment() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class FieldMissingNullTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(String message) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    this.message = message != null ? null : message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMaybeNullAssignment() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class FieldMissingNullTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    message = x >= 0 ? null : \"negative\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAssignNullableMethodResult() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableMethodCallTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableMethodCallTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    message = toSignString(x);",
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
  public void testAssignNullableMethodCall_alternativeAnnotation() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/anno/my/Nullable.java",
            "package com.google.anno.my;",
            "public @interface Nullable {}")
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableMethodCallTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NullableMethodCallTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    message = toSignString(x);",
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
  public void testAssignNullableMethodCall_typeAnnotation() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/anno/my/Nullable.java",
            "package com.google.anno.my;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Target;",
            "@Target({ElementType.TYPE_USE})",
            "public @interface Nullable {}")
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableMethodCallTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NullableMethodCallTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    message = toSignString(x);",
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
  public void testAssignNullableField() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableFieldTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableFieldTest {",
            "  @Nullable private String message;",
            "  private String previous = \"\";",
            "  public void setMessage(String message) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    previous = this.message;",
            "    this.message = message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAssignNullableParameter() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  private String message = \"hello\";",
            "  public void apply(@Nullable String message) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    this.message = message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInitializeWithNullableParameter() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  private final String message;",
            "  public NullableParameterTest(@Nullable String message) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    this.message = message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNullInitializer() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  // BUG: Diagnostic contains: @Nullable",
            "  public static final String MESSAGE = null;",
            "}")
        .doTest();
  }

  @Test
  public void testMaybeNullAssignmentInLambda() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  private String message = \"hello\";",
            "  public void setMessageIfPresent(java.util.Optional<String> message) {",
            // Note this code is bogus: s is guaranteed non-null...
            "    // BUG: Diagnostic contains: @Nullable",
            "    message.ifPresent(s -> { this.message = s != null ? s : null; });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyAnnotated() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class FieldMissingNullTest {",
            "  @Nullable String message;",
            "  public void reset() {",
            "    this.message = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyTypeAnnotated() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/anno/my/Nullable.java",
            "package com.google.anno.my;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Target;",
            "@Target({ElementType.TYPE_USE})",
            "public @interface Nullable {}")
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import com.google.anno.my.Nullable;",
            "public class FieldMissingNullTest {",
            "  @Nullable String message;",
            "  public void reset() {",
            "    this.message = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_initializeWithNonNullLiteral() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class FieldMissingNullTest {",
            "  private final String message;",
            "  public FieldMissingNullTest() {",
            "    message = \"hello\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullInitializer() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class FieldMissingNullTest {",
            "  private String message = \"hello\";",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_lambdaInitializer() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import java.util.function.Predicate;",
            "import java.util.function.Function;",
            "public class FieldMissingNullTest {",
            "  private Runnable runnable = () -> {};",
            "  private Predicate<?> predicate1 = p -> true;",
            "  private Predicate<?> predicate2 = (p -> true);",
            "  private Predicate<?> predicate3 = (String p) -> { return false; };",
            "  private Function<?, ?> function1 = p -> null;",
            "  private Function<?, ?> function2 = (p -> null);",
            "  private Function<?, ?> function3 = (String p) -> { return null; };",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullMethod() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullMethodTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullMethodTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(int x) {",
            "    message = String.valueOf(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullField() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullFieldTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullFieldTest {",
            "  private String message = \"hello\";",
            "  private String previous = \"\";",
            "  public void save() {",
            "    previous = message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullParameter() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullParameterTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(String message) {",
            "    this.message = message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_this() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ThisTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class ThisTest {",
            "  private static ThisTest theInstance = new ThisTest();",
            "  public void makeDefault() {",
            "    this.theInstance = this;",
            "  }",
            "}")
        .doTest();
  }

  /**
   * Makes sure the check never flags methods returning a primitive. Returning null from them is a
   * bug, of course, but we're not trying to find those bugs in this check.
   */
  @Test
  public void testNegativeCases_primitiveFieldType() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/PrimitiveReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class PrimitiveReturnTest {",
            "  private int count = (Integer) null;",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_initializeWithLambda() {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  private String message = \"hello\";",
            "  public void setMessageIfPresent(java.util.Optional<String> message) {",
            "    message.ifPresent(s -> { this.message = s; });",
            "  }",
            "}")
        .doTest();
  }

  // regression test for https://github.com/google/error-prone/issues/708
  @Test
  public void i708() {
    createCompilationTestHelper()
        .addSourceLines(
            "Test.java",
            "import java.util.regex.Pattern;",
            "class T {",
            "  private static final Pattern FULLY_QUALIFIED_METHOD_NAME_PATTERN =",
            "      Pattern.compile(\"(.+)#([^()]+?)(\\\\((.*)\\\\))?\");",
            "}")
        .doTest();
  }

  @Test
  public void testSuggestNonJsr305Nullable() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "class T {",
            "  @Nullable private final Object obj1 = null;",
            "  private final Object obj2 = null;",
            "  @interface Nullable {}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class T {",
            "  @Nullable private final Object obj1 = null;",
            "  @Nullable private final Object obj2 = null;",
            "  @interface Nullable {}",
            "}")
        .doTest();
  }

  @Test
  public void testNonAnnotationNullable() {
    createRefactoringTestHelper()
        .addInputLines(
            "in/Test.java",
            "class T {",
            "  private final Object obj2 = null;",
            "  class Nullable {}",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class T {",
            "  @javax.annotation.Nullable private final Object obj2 = null;",
            "  class Nullable {}",
            "}")
        .doTest();
  }

  private CompilationTestHelper createCompilationTestHelper() {
    return CompilationTestHelper.newInstance(FieldMissingNullable.class, getClass());
  }

  private BugCheckerRefactoringTestHelper createRefactoringTestHelper() {
    return BugCheckerRefactoringTestHelper.newInstance(new FieldMissingNullable(), getClass());
  }
}
