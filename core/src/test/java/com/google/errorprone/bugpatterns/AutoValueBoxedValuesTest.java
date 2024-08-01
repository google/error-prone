/*
 * Copyright 2024 The Error Prone Authors.
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

import static java.util.Arrays.stream;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link AutoValueBoxedValues}. */
@RunWith(TestParameterInjector.class)
public class AutoValueBoxedValuesTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AutoValueBoxedValues.class, getClass())
          .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()));
  ;
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(AutoValueBoxedValues.class, getClass())
          .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()));

  @TestParameter private boolean withBuilder;

  @Test
  public void unnecessaryBoxedTypes_refactoring() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            mergeLines(
                lines(
                    "import com.google.auto.value.AutoValue;",
                    "@AutoValue",
                    "abstract class Test {",
                    "  public abstract Long longId();",
                    "  public abstract Integer intId();",
                    "  public abstract Byte byteId();",
                    "  public abstract Short shortId();",
                    "  public abstract Float floatId();",
                    "  public abstract Double doubleId();",
                    "  public abstract Boolean booleanId();",
                    "  public abstract Character charId();"),
                linesWithoutBuilder(
                    "  static Test create(",
                    "      Long longId, Integer intId, Byte byteId, Short shortId,",
                    "      Float floatId, Double doubleId, Boolean booleanId, Character charId) {",
                    "    return new AutoValue_Test(longId, intId, byteId, shortId, floatId,",
                    "       doubleId, booleanId, charId);",
                    "  }"),
                linesWithBuilder(
                    "  @AutoValue.Builder",
                    "  abstract static class Builder {",
                    "    abstract Builder setLongId(Long value);",
                    "    abstract Builder setIntId(Integer value);",
                    "    abstract Builder setByteId(Byte value);",
                    "    abstract Builder setShortId(Short value);",
                    "    abstract Builder setFloatId(Float value);",
                    "    abstract Builder setDoubleId(Double value);",
                    "    abstract Builder setBooleanId(Boolean value);",
                    "    abstract Builder setCharId(Character value);",
                    "    abstract Test build();",
                    "  }"),
                lines("}")))
        .addOutputLines(
            "out/Test.java",
            mergeLines(
                lines(
                    "import com.google.auto.value.AutoValue;",
                    "@AutoValue",
                    "abstract class Test {",
                    "  public abstract long longId();",
                    "  public abstract int intId();",
                    "  public abstract byte byteId();",
                    "  public abstract short shortId();",
                    "  public abstract float floatId();",
                    "  public abstract double doubleId();",
                    "  public abstract boolean booleanId();",
                    "  public abstract char charId();"),
                linesWithoutBuilder(
                    "  static Test create(",
                    "      long longId, int intId, byte byteId, short shortId,",
                    "      float floatId, double doubleId, boolean booleanId, char charId) {",
                    "    return new AutoValue_Test(longId, intId, byteId, shortId, floatId,",
                    "       doubleId, booleanId, charId);",
                    "  }"),
                linesWithBuilder(
                    "  @AutoValue.Builder",
                    "  abstract static class Builder {",
                    "    abstract Builder setLongId(long value);",
                    "    abstract Builder setIntId(int value);",
                    "    abstract Builder setByteId(byte value);",
                    "    abstract Builder setShortId(short value);",
                    "    abstract Builder setFloatId(float value);",
                    "    abstract Builder setDoubleId(double value);",
                    "    abstract Builder setBooleanId(boolean value);",
                    "    abstract Builder setCharId(char value);",
                    "    abstract Test build();",
                    "  }"),
                lines("}")))
        .doTest();
  }

  @Test
  public void nullableBoxedTypes() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            mergeLines(
                lines(
                    "import com.google.auto.value.AutoValue;",
                    "import javax.annotation.Nullable;",
                    "@AutoValue",
                    "abstract class Test {",
                    "  public abstract @Nullable Long longId();",
                    "  public abstract @Nullable Integer intId();",
                    "  public abstract @Nullable Byte byteId();",
                    "  public abstract @Nullable Short shortId();",
                    "  public abstract @Nullable Float floatId();",
                    "  public abstract @Nullable Double doubleId();",
                    "  public abstract @Nullable Boolean booleanId();",
                    "  public abstract @Nullable Character charId();"),
                linesWithoutBuilder(
                    "  static Test create(",
                    "      @Nullable Long longId, @Nullable Integer intId, @Nullable Byte byteId,",
                    "      @Nullable Short shortId, @Nullable Float floatId,",
                    "      @Nullable Double doubleId, @Nullable Boolean booleanId,",
                    "      @Nullable Character charId) {",
                    "    return new AutoValue_Test(longId, intId, byteId, shortId, floatId,",
                    "       doubleId, booleanId, charId);",
                    "  }"),
                linesWithBuilder(
                    "  @AutoValue.Builder",
                    "  abstract static class Builder {",
                    "    abstract Builder setLongId(@Nullable Long value);",
                    "    abstract Builder setIntId(@Nullable Integer value);",
                    "    abstract Builder setByteId(@Nullable Byte value);",
                    "    abstract Builder setShortId(@Nullable Short value);",
                    "    abstract Builder setFloatId(@Nullable Float value);",
                    "    abstract Builder setDoubleId(@Nullable Double value);",
                    "    abstract Builder setBooleanId(@Nullable Boolean value);",
                    "    abstract Builder setCharId(@Nullable Character value);",
                    "    abstract Test build();",
                    "  }"),
                lines("}")))
        .doTest();
  }

  @Test
  public void genericNullableBoxedTypes() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            mergeLines(
                lines(
                    "import com.google.auto.value.AutoValue;",
                    "import org.checkerframework.checker.nullness.qual.Nullable;",
                    "@AutoValue",
                    "abstract class Test {",
                    "  public abstract @Nullable Long longId();",
                    "  public abstract @Nullable Integer intId();",
                    "  public abstract @Nullable Byte byteId();",
                    "  public abstract @Nullable Short shortId();",
                    "  public abstract @Nullable Float floatId();",
                    "  public abstract @Nullable Double doubleId();",
                    "  public abstract @Nullable Boolean booleanId();",
                    "  public abstract @Nullable Character charId();"),
                linesWithoutBuilder(
                    "  static Test create(",
                    "      @Nullable Long longId, @Nullable Integer intId, @Nullable Byte byteId,",
                    "      @Nullable Short shortId, @Nullable Float floatId,",
                    "      @Nullable Double doubleId, @Nullable Boolean booleanId,",
                    "      @Nullable Character charId) {",
                    "    return new AutoValue_Test(longId, intId, byteId, shortId, floatId,",
                    "       doubleId, booleanId, charId);",
                    "  }"),
                linesWithBuilder(
                    "  @AutoValue.Builder",
                    "  abstract static class Builder {",
                    "    abstract Builder setLongId(@Nullable Long value);",
                    "    abstract Builder setIntId(@Nullable Integer value);",
                    "    abstract Builder setByteId(@Nullable Byte value);",
                    "    abstract Builder setShortId(@Nullable Short value);",
                    "    abstract Builder setFloatId(@Nullable Float value);",
                    "    abstract Builder setDoubleId(@Nullable Double value);",
                    "    abstract Builder setBooleanId(@Nullable Boolean value);",
                    "    abstract Builder setCharId(@Nullable Character value);",
                    "    abstract Test build();",
                    "  }"),
                lines("}")))
        .doTest();
  }

  @Test
  public void primitiveTypes() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            mergeLines(
                lines(
                    "import com.google.auto.value.AutoValue;",
                    "@AutoValue",
                    "abstract class Test {",
                    "  public abstract long longId();",
                    "  public abstract int intId();",
                    "  public abstract byte byteId();",
                    "  public abstract short shortId();",
                    "  public abstract float floatId();",
                    "  public abstract double doubleId();",
                    "  public abstract boolean booleanId();",
                    "  public abstract char charId();"),
                linesWithoutBuilder(
                    "  static Test create(",
                    "      long longId, int intId, byte byteId, short shortId,",
                    "      float floatId, double doubleId, boolean booleanId, char charId) {",
                    "    return new AutoValue_Test(longId, intId, byteId, shortId, floatId,",
                    "       doubleId, booleanId, charId);",
                    "  }"),
                linesWithBuilder(
                    "  @AutoValue.Builder",
                    "  abstract static class Builder {",
                    "    abstract Builder setLongId(long value);",
                    "    abstract Builder setIntId(int value);",
                    "    abstract Builder setByteId(byte value);",
                    "    abstract Builder setShortId(short value);",
                    "    abstract Builder setFloatId(float value);",
                    "    abstract Builder setDoubleId(double value);",
                    "    abstract Builder setBooleanId(boolean value);",
                    "    abstract Builder setCharId(char value);",
                    "    abstract Test build();",
                    "  }"),
                lines("}")))
        .doTest();
  }

  @Test
  public void nonBoxableTypes() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            mergeLines(
                lines(
                    "import com.google.auto.value.AutoValue;",
                    "import javax.annotation.Nullable;",
                    "@AutoValue",
                    "abstract class Test {",
                    "  public abstract String stringId();",
                    "  public @Nullable abstract String nullableStringId();"),
                linesWithoutBuilder(
                    "  static Test create(String stringId, @Nullable String nullableStringId) {",
                    "    return new AutoValue_Test(stringId, nullableStringId);",
                    "  }"),
                linesWithBuilder(
                    "  @AutoValue.Builder",
                    "  abstract static class Builder {",
                    "    abstract Builder setStringId(String value);",
                    "    abstract Builder setNullableStringId(@Nullable String value);",
                    "    abstract Test build();",
                    "  }"),
                lines("}")))
        .doTest();
  }

  @Test
  public void mixedTypes_refactoring() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            mergeLines(
                lines(
                    "import com.google.auto.value.AutoValue;",
                    "import javax.annotation.Nullable;",
                    "@AutoValue",
                    "abstract class Test {",
                    "  public abstract @Nullable Long nullableId();",
                    "  public abstract Long unnecessaryBoxedId();",
                    "  public abstract long primitiveId();",
                    "  public abstract String nonBoxableId();"),
                linesWithoutBuilder(
                    "  static Test create(",
                    "      @Nullable Long nullableId, Long unnecessaryBoxedId,",
                    "      long primitiveId, String nonBoxableId) {",
                    "    return new AutoValue_Test(",
                    "        nullableId, unnecessaryBoxedId, primitiveId, nonBoxableId);",
                    "  }"),
                linesWithBuilder(
                    "  @AutoValue.Builder",
                    "  abstract static class Builder {",
                    "    abstract Builder setNullableId(@Nullable Long value);",
                    "    abstract Builder setUnnecessaryBoxedId(Long value);",
                    "    abstract Builder setPrimitiveId(long value);",
                    "    abstract Builder setNonBoxableId(String value);",
                    "    abstract Test build();",
                    "  }"),
                lines("}")))
        .addOutputLines(
            "out/Test.java",
            mergeLines(
                lines(
                    "import com.google.auto.value.AutoValue;",
                    "import javax.annotation.Nullable;",
                    "@AutoValue",
                    "abstract class Test {",
                    "  public abstract @Nullable Long nullableId();",
                    "  public abstract long unnecessaryBoxedId();",
                    "  public abstract long primitiveId();",
                    "  public abstract String nonBoxableId();"),
                linesWithoutBuilder(
                    "  static Test create(",
                    "      @Nullable Long nullableId, long unnecessaryBoxedId,",
                    "      long primitiveId, String nonBoxableId) {",
                    "    return new AutoValue_Test(",
                    "        nullableId, unnecessaryBoxedId, primitiveId, nonBoxableId);",
                    "  }"),
                linesWithBuilder(
                    "  @AutoValue.Builder",
                    "  abstract static class Builder {",
                    "    abstract Builder setNullableId(@Nullable Long value);",
                    "    abstract Builder setUnnecessaryBoxedId(long value);",
                    "    abstract Builder setPrimitiveId(long value);",
                    "    abstract Builder setNonBoxableId(String value);",
                    "    abstract Test build();",
                    "  }"),
                lines("}")))
        .doTest();
  }

  @Test
  public void unnecessaryBoxedTypes_suppressWarnings() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            mergeLines(
                lines(
                    "import com.google.auto.value.AutoValue;",
                    "@AutoValue",
                    "abstract class Test {",
                    "  public abstract Long longId();",
                    "  @SuppressWarnings(\"AutoValueBoxedValues\")",
                    "  public abstract Long longIdSuppressWarnings();"),
                linesWithoutBuilder(
                    "  static Test create(Long longId, Long longIdSuppressWarnings) {",
                    "    return new AutoValue_Test(longId, longIdSuppressWarnings);",
                    "  }"),
                linesWithBuilder(
                    "  @AutoValue.Builder",
                    "  abstract static class Builder {",
                    "    abstract Builder setLongId(Long value);",
                    "    @SuppressWarnings(\"AutoValueBoxedValues\")",
                    "    abstract Builder setLongIdSuppressWarnings(Long value);",
                    "    abstract Test build();",
                    "  }"),
                lines("}")))
        .addOutputLines(
            "out/Test.java",
            mergeLines(
                lines(
                    "import com.google.auto.value.AutoValue;",
                    "@AutoValue",
                    "abstract class Test {",
                    "  public abstract long longId();",
                    "  @SuppressWarnings(\"AutoValueBoxedValues\")",
                    "  public abstract Long longIdSuppressWarnings();"),
                linesWithoutBuilder(
                    "  static Test create(long longId, Long longIdSuppressWarnings) {",
                    "    return new AutoValue_Test(longId, longIdSuppressWarnings);",
                    "  }"),
                linesWithBuilder(
                    "  @AutoValue.Builder",
                    "  abstract static class Builder {",
                    "    abstract Builder setLongId(long value);",
                    "    @SuppressWarnings(\"AutoValueBoxedValues\")",
                    "    abstract Builder setLongIdSuppressWarnings(Long value);",
                    "    abstract Test build();",
                    "  }"),
                lines("}")))
        .doTest();
  }

  @Test
  public void nullableGettersWithNonNullableSetters_noChange() {
    if (!withBuilder) {
      return;
    }
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import javax.annotation.Nullable;",
            "@AutoValue",
            "abstract class Test {",
            "  public abstract @Nullable Long longId();",
            "  public abstract @Nullable Integer intId();",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder setLongId(Long value);",
            "    abstract Builder setIntId(Integer value);",
            "    abstract Test build();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonTrivialFactoryMethods_refectoring() {
    if (withBuilder) {
      return;
    }
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "package test;",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract int foo();",
            "  abstract Long bar();",
            "  static Test createTrivial(int foo, Long bar) {",
            "    return new AutoValue_Test(foo, bar);",
            "  }",
            "  static String notFactoryMethod(int foo, Long bar) {",
            "    return String.format(\"foo: %d, bar: %d\", foo, bar);",
            "  }",
            "  static Test createWrongOrder(Long bar, int foo) {",
            "    return new AutoValue_Test(foo, bar);",
            "  }",
            "  static Test createLessArguments(int foo) {",
            "    return new AutoValue_Test(foo, 0L);",
            "  }",
            "  static Test createMoreArguments(int foo, Long bar, Long baz) {",
            "    return new AutoValue_Test(foo, bar + baz);",
            "  }",
            "  static Test createWithValidation(int foo, Long bar) {",
            "    if (bar == null) { throw new AssertionError(); }",
            "    return new AutoValue_Test(foo, bar);",
            "  }",
            "  static Test createModifyArgs(int foo, Long bar) {",
            "    return new AutoValue_Test(foo + 1, bar);",
            "  }",
            "  static Test createModifyArgsIfNull(int foo, Long bar) {",
            "    return new AutoValue_Test(foo, bar == null ? 0L : bar);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "package test;",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract int foo();",
            "  abstract long bar();",
            "  static Test createTrivial(int foo, long bar) {",
            "    return new AutoValue_Test(foo, bar);",
            "  }",
            "  static String notFactoryMethod(int foo, Long bar) {",
            "    return String.format(\"foo: %d, bar: %d\", foo, bar);",
            "  }",
            "  static Test createWrongOrder(Long bar, int foo) {",
            "    return new AutoValue_Test(foo, bar);",
            "  }",
            "  static Test createLessArguments(int foo) {",
            "    return new AutoValue_Test(foo, 0L);",
            "  }",
            "  static Test createMoreArguments(int foo, Long bar, Long baz) {",
            "    return new AutoValue_Test(foo, bar + baz);",
            "  }",
            "  static Test createWithValidation(int foo, Long bar) {",
            "    if (bar == null) { throw new AssertionError(); }",
            "    return new AutoValue_Test(foo, bar);",
            "  }",
            "  static Test createModifyArgs(int foo, Long bar) {",
            "    return new AutoValue_Test(foo + 1, bar);",
            "  }",
            "  static Test createModifyArgsIfNull(int foo, Long bar) {",
            "    return new AutoValue_Test(foo, bar == null ? 0L : bar);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void settersWithoutSetPrefix() {
    if (!withBuilder) {
      return;
    }
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  public abstract Long longId();",
            "  public abstract Boolean booleanId();",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder longId(Long value);",
            "    abstract Builder booleanId(Boolean value);",
            "    abstract Test build();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  public abstract long longId();",
            "  public abstract boolean booleanId();",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder longId(long value);",
            "    abstract Builder booleanId(boolean value);",
            "    abstract Test build();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allGettersWithPrefix() {
    if (!withBuilder) {
      return;
    }
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  public abstract Long getLongId();",
            "  public abstract boolean isBooleanId();",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder setLongId(Long value);",
            "    abstract Builder setBooleanId(boolean value);",
            "    abstract Test build();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  public abstract long getLongId();",
            "  public abstract boolean isBooleanId();",
            "  @AutoValue.Builder",
            "  abstract static class Builder {",
            "    abstract Builder setLongId(long value);",
            "    abstract Builder setBooleanId(boolean value);",
            "    abstract Test build();",
            "  }",
            "}")
        .doTest();
  }

  private static List<String> lines(String... lines) {
    return Arrays.asList(lines);
  }

  private List<String> linesWithBuilder(String... lines) {
    return withBuilder ? Arrays.asList(lines) : new ArrayList<>();
  }

  private List<String> linesWithoutBuilder(String... lines) {
    return !withBuilder ? Arrays.asList(lines) : new ArrayList<>();
  }

  private static String[] mergeLines(List<String>... blocks) {
    return stream(blocks).flatMap(List::stream).toArray(String[]::new);
  }
}
