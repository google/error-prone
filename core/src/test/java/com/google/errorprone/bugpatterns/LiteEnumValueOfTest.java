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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link LiteEnumValueOf}.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public final class LiteEnumValueOfTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(LiteEnumValueOf.class, getClass())
          .addSourceLines(
              "FakeLiteEnum.java",
              """
              package p;

              public enum FakeLiteEnum implements com.google.protobuf.Internal.EnumLite {
                FOO;

                @Override
                public int getNumber() {
                  return 0;
                }

                public static FakeLiteEnum forNumber(int number) {
                  return FOO;
                }
              }
              """);

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test() {
                // BUG: Diagnostic contains:
                p.FakeLiteEnum.valueOf("FOO");
                // BUG: Diagnostic contains:
                p.FakeLiteEnum.FOO.valueOf("FOO");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCaseStaticImport() {
    compilationHelper
        .addSourceLines(
            "TestStatic.java",
            """
            import static p.FakeLiteEnum.valueOf;

            class TestStatic {
              void test() {
                // BUG: Diagnostic contains:
                valueOf("FOO");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "Usage.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestEnum;

            class Usage {
              private TestEnum testMethod() {
                return TestEnum.valueOf("FOO");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCaseNumericLookup() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test() {
                p.FakeLiteEnum.forNumber(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCaseGeneratedByOther() {
    compilationHelper
        .addSourceLines(
            "FakeGenerated.java",
            """
            import javax.annotation.processing.Generated;

            @Generated("some.other.Generator")
            class FakeGenerated {
              void test() {
                // BUG: Diagnostic contains:
                p.FakeLiteEnum.valueOf("FOO");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeGeneratedCaller() {
    compilationHelper
        .addSourceLines(
            "AutoValue_TestData.java",
            """
            import javax.annotation.processing.Generated;

            @Generated("com.ryanharter.auto.value.parcel.AutoValueParcelExtension")
            class AutoValue_TestData {
              void test() {
                p.FakeLiteEnum.valueOf("FOO");
              }

              class Nested {
                void test() {
                  p.FakeLiteEnum.valueOf("FOO");
                }
              }
            }
            """)
        .doTest();
  }
}
