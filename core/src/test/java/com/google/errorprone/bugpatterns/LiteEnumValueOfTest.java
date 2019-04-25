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

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link LiteEnumValueOf}.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class LiteEnumValueOfTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(LiteEnumValueOf.class, getClass())
          .addSourceFile("android/testdata/stubs/android/os/Parcel.java")
          .addSourceFile("android/testdata/stubs/android/os/Parcelable.java");

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("LiteEnumValueOfPositiveCases.java").doTest();
  }

  @Test
  public void testPositiveCase2() {
    compilationHelper
        .addSourceLines("ProtoLiteEnum.java", PROTOLITE_ENUM)
        .addSourceLines(
            "Usage.java",
            "class Usage {",
            "  private ProtoLiteEnum testMethod() {",
            "    // BUG: Diagnostic contains: LiteEnumValueOf",
            "    return ProtoLiteEnum.valueOf(\"FOO\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("LiteEnumValueOfNegativeCases.java").doTest();
  }

  @Test
  public void testWrappedCaseInLite() {
    compilationHelper
        .addSourceLines("OuterClass.java", generateProtoLiteEnumInGeneratedMessageLite())
        .addSourceLines("Usage.java", generateUsageProtoLiteEnumValueOf(true))
        .doTest();
  }

  @Test
  public void testWrappedCaseInFullProto() {
    compilationHelper
        .addSourceLines("OuterClass.java", generateProtoLiteEnumInGeneratedMessage())
        .addSourceLines("Usage.java", generateUsageProtoLiteEnumValueOf(false))
        .doTest();
  }

  @Test
  public void testNegativeCaseJDK8OrEarlier() {
    assumeFalse(RuntimeVersion.isAtLeast9());
    testGeneratedAutoValueClass("javax.annotation.Generated");
  }

  @Test
  public void testNegativeCaseJDK9OrAbove() {
    assumeTrue(RuntimeVersion.isAtLeast9());
    testGeneratedAutoValueClass("javax.annotation.processing.Generated");
  }

  private static final String[] PROTOLITE_ENUM =
      new String[] {
        "enum ProtoLiteEnum implements com.google.protobuf.Internal.EnumLite {",
        "  FOO(1),",
        "  BAR(2);",
        "  private final int number;",
        "  private ProtoLiteEnum(int number) {",
        "    this.number = number;",
        "  }",
        "  @Override",
        "  public int getNumber() {",
        "    return number;",
        "  }",
        "}"
      };

  private static String[] generateUsageProtoLiteEnumValueOf(boolean addDiagnostic) {
    return new String[] {
      "class Usage {",
      "  private OuterClass.ProtoLiteEnum testMethod() {",
      addDiagnostic ? "    // BUG: Diagnostic contains: LiteEnumValueOf" : "",
      "    return OuterClass.ProtoLiteEnum.valueOf(\"FOO\");",
      "  }",
      "}"
    };
  }

  private void testGeneratedAutoValueClass(String importClass) {
    String importStatement = "import " + importClass + ";";
    compilationHelper
        .addSourceLines("ProtoLiteEnum.java", PROTOLITE_ENUM)
        .addSourceLines("TestData.java", "class TestData {}")
        .addSourceLines("$AutoValue_TestData.java", createDollarAutoValueClass(importStatement))
        .addSourceLines("AutoValue_TestData.java", createAutoValueClass(importStatement))
        .doTest();
  }

  private static String[] generateProtoLiteEnumInGeneratedMessageLite() {
    String[] start =
        new String[] {
          // abstract instead of implementing dynamicMethod(MethodToInvoke,Object,Object) in
          // GeneratedMessageLite
          "public abstract class OuterClass extends ",
          "    com.google.protobuf.GeneratedMessageLite<OuterClass, OuterClass.Builder> {"
        };
    String[] end =
        new String[] {
          "  public static final class Builder extends",
          "      com.google.protobuf.GeneratedMessageLite.Builder<OuterClass, Builder> {",
          "    private Builder() { super(null); }",
          "  }",
          "};"
        };
    return Stream.of(start, PROTOLITE_ENUM, end).flatMap(Stream::of).toArray(String[]::new);
  }

  private static String[] generateProtoLiteEnumInGeneratedMessage() {
    String[] start =
        new String[] {
          // abstract instead of implementing newBuilderForType(BuilderParent) in GeneratedMessage
          "public abstract class OuterClass extends com.google.protobuf.GeneratedMessage {"
        };
    String[] end = new String[] {"};"};
    return Stream.of(start, PROTOLITE_ENUM, end).flatMap(Stream::of).toArray(String[]::new);
  }

  private static String[] createDollarAutoValueClass(String importStatement) {
    return new String[] {
      importStatement,
      "@Generated(\"com.google.auto.value.processor.AutoValueProcessor\")",
      "class $AutoValue_TestData extends TestData {}"
    };
  }

  private static String[] createAutoValueClass(String importStatement) {
    return new String[] {
      "import android.os.Parcel;",
      "import android.os.Parcelable;",
      "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestEnum;",
      importStatement,
      "@Generated(\"com.ryanharter.auto.value.parcel.AutoValueParcelExtension\")",
      "class AutoValue_TestData extends $AutoValue_TestData {",
      "    AutoValue_TestData(ProtoLiteEnum protoLiteEnum) {}",
      "    public static final Parcelable.Creator<AutoValue_TestData> CREATOR =",
      "        new Parcelable.Creator<AutoValue_TestData>() {",
      "          @Override",
      "          public AutoValue_TestData createFromParcel(Parcel in) {",
      "            return new AutoValue_TestData(ProtoLiteEnum.valueOf(\"FOO\"));",
      "          }",
      "          @Override",
      "          public AutoValue_TestData[] newArray(int size) {",
      "            return null;",
      "          }",
      "        };",
      "}"
    };
  }
}
