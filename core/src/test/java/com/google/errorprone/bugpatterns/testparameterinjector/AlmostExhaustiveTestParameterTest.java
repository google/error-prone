/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.testparameterinjector;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link AlmostExhaustiveTestParameter}. */
@RunWith(JUnit4.class)
public final class AlmostExhaustiveTestParameterTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(AlmostExhaustiveTestParameter.class, getClass());
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AlmostExhaustiveTestParameter.class, getClass());

  @Test
  public void excludesSingleEnumValue_replacesWithAssumeIsNotEqualTo() {
    testHelper
        .addInputLines(
            "TestType.java",
"""
import com.google.testing.junit.testparameterinjector.TestParameter;
import org.junit.Test;

public class TestType {
  enum ImageFormat {
    UNKNOWN,
    PNG,
    JPEG,
    GIF,
    WEBP
  }

  @Test
  public void inOrder(@TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat format) {
    System.out.println(format);
  }

  @Test
  public void outOfOrderAndNamedValue(
      @TestParameter(value = {"WEBP", "PNG", "UNKNOWN", "GIF"}) ImageFormat format) {
    System.out.println(format);
  }

  @Test
  public void qualifiedAnnotation(
      @com.google.testing.junit.testparameterinjector.TestParameter({
            "UNKNOWN", "PNG", "JPEG", "GIF"
          })
          ImageFormat format) {
    System.out.println(format);
  }
}
""")
        .addOutputLines(
            "TestType.java",
"""
import static com.google.common.truth.TruthJUnit.assume;

import com.google.testing.junit.testparameterinjector.TestParameter;
import org.junit.Test;

public class TestType {
  enum ImageFormat {
    UNKNOWN,
    PNG,
    JPEG,
    GIF,
    WEBP
  }

  @Test
  public void inOrder(@TestParameter ImageFormat format) {
    assume().that(format).isNotEqualTo(ImageFormat.UNKNOWN);
    System.out.println(format);
  }

  @Test
  public void outOfOrderAndNamedValue(@TestParameter ImageFormat format) {
    assume().that(format).isNotEqualTo(ImageFormat.JPEG);
    System.out.println(format);
  }

  @Test
  public void qualifiedAnnotation(
      @com.google.testing.junit.testparameterinjector.TestParameter ImageFormat format) {
    assume().that(format).isNotEqualTo(ImageFormat.WEBP);
    System.out.println(format);
  }
}
""")
        .doTest();
  }

  @Test
  public void emptyMethodBodyAndMultipleParameters_insertsAssumptionsInParameterOrder() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum ImageFormat {
                UNKNOWN,
                PNG,
                JPEG,
                GIF,
                WEBP
              }

              @Test
              public void emptyBodySingleParam(
                  @TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat format) {}

              @Test
              public void emptyBody(
                  @TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat first,
                  @TestParameter({"UNKNOWN", "PNG", "JPEG", "GIF"}) ImageFormat second) {}

              @Test
              public void nonEmptyBody(
                  @TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat first,
                  @TestParameter({"UNKNOWN", "PNG", "JPEG", "GIF"}) ImageFormat second) {
                // Keep comment attached to the statement below.
                System.out.println(first + " " + second);
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
"""
import static com.google.common.truth.TruthJUnit.assume;

import com.google.testing.junit.testparameterinjector.TestParameter;
import org.junit.Test;

public class TestType {
  enum ImageFormat {
    UNKNOWN,
    PNG,
    JPEG,
    GIF,
    WEBP
  }

  @Test
  public void emptyBodySingleParam(@TestParameter ImageFormat format) {
    assume().that(format).isNotEqualTo(ImageFormat.UNKNOWN);
  }

  @Test
  public void emptyBody(@TestParameter ImageFormat first, @TestParameter ImageFormat second) {
    assume().that(first).isNotEqualTo(ImageFormat.UNKNOWN);
    assume().that(second).isNotEqualTo(ImageFormat.WEBP);
  }

  @Test
  public void nonEmptyBody(@TestParameter ImageFormat first, @TestParameter ImageFormat second) {
    assume().that(first).isNotEqualTo(ImageFormat.UNKNOWN);
    assume().that(second).isNotEqualTo(ImageFormat.WEBP);
    // Keep comment attached to the statement below.
    System.out.println(first + " " + second);
  }
}
""")
        .doTest();
  }

  @Test
  public void enumWithUnrecognized_excludesUnspecifiedAndUnrecognized() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum ProtoImageFormat {
                IMAGE_FORMAT_UNSPECIFIED,
                PNG,
                JPEG,
                GIF,
                WEBP,
                UNRECOGNIZED
              }

              enum SmallerProtoImageFormat {
                IMAGE_FORMAT_UNSPECIFIED,
                PNG,
                JPEG,
                GIF,
                UNRECOGNIZED
              }

              @Test
              public void excludesUnspecifiedAndUnrecognized(
                  @TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ProtoImageFormat format) {
                System.out.println(format);
              }

              @Test
              public void excludesOnlyUnrecognized(
                  @TestParameter({"IMAGE_FORMAT_UNSPECIFIED", "PNG", "JPEG", "GIF"})
                      SmallerProtoImageFormat format) {
                System.out.println(format);
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
"""
import static com.google.common.truth.TruthJUnit.assume;

import com.google.testing.junit.testparameterinjector.TestParameter;
import org.junit.Test;

public class TestType {
  enum ProtoImageFormat {
    IMAGE_FORMAT_UNSPECIFIED,
    PNG,
    JPEG,
    GIF,
    WEBP,
    UNRECOGNIZED
  }

  enum SmallerProtoImageFormat {
    IMAGE_FORMAT_UNSPECIFIED,
    PNG,
    JPEG,
    GIF,
    UNRECOGNIZED
  }

  @Test
  public void excludesUnspecifiedAndUnrecognized(@TestParameter ProtoImageFormat format) {
    assume()
        .that(format)
        .isNoneOf(ProtoImageFormat.IMAGE_FORMAT_UNSPECIFIED, ProtoImageFormat.UNRECOGNIZED);
    System.out.println(format);
  }

  @Test
  public void excludesOnlyUnrecognized(@TestParameter SmallerProtoImageFormat format) {
    assume().that(format).isNotEqualTo(SmallerProtoImageFormat.UNRECOGNIZED);
    System.out.println(format);
  }
}
""")
        .doTest();
  }

  @Test
  public void existingAssumeMethodInClass_qualifiesTruthJUnit() {
    testHelper
        .addInputLines(
            "TestType.java",
"""
import com.google.testing.junit.testparameterinjector.TestParameter;
import org.junit.Test;

public class TestType {
  enum ImageFormat {
    UNKNOWN,
    PNG,
    JPEG,
    GIF,
    WEBP
  }

  private static void assume() {}

  @Test
  public void myTest(@TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat format) {
    assume();
  }
}
""")
        .addOutputLines(
            "TestType.java",
            """
            import com.google.common.truth.TruthJUnit;
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum ImageFormat {
                UNKNOWN,
                PNG,
                JPEG,
                GIF,
                WEBP
              }

              private static void assume() {}

              @Test
              public void myTest(@TestParameter ImageFormat format) {
                TruthJUnit.assume().that(format).isNotEqualTo(ImageFormat.UNKNOWN);
                assume();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void truthNotOnClasspath_usesJUnitAssumeFalse() {
    testHelper
        .addInputLines(
            "com/google/testing/junit/testparameterinjector/TestParameter.java",
            """
            package com.google.testing.junit.testparameterinjector;

            public @interface TestParameter {
              String[] value() default {};
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "org/junit/Test.java",
            """
            package org.junit;

            public @interface Test {}
            """)
        .expectUnchanged()
        .addInputLines(
            "org/junit/Assume.java",
            """
            package org.junit;

            public class Assume {
              public static void assumeFalse(boolean b) {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "TestType.java",
"""
import com.google.testing.junit.testparameterinjector.TestParameter;
import org.junit.Test;

public class TestType {
  enum ImageFormat {
    UNKNOWN,
    PNG,
    JPEG,
    GIF,
    WEBP
  }

  enum ProtoImageFormat {
    IMAGE_FORMAT_UNSPECIFIED,
    PNG,
    JPEG,
    GIF,
    WEBP,
    UNRECOGNIZED
  }

  @Test
  public void singleExcluded(@TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat format) {
    System.out.println(format);
  }

  @Test
  public void protoExcluded(
      @TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ProtoImageFormat format) {
    System.out.println(format);
  }
}
""")
        .addOutputLines(
            "TestType.java",
            """
            import static org.junit.Assume.assumeFalse;

            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum ImageFormat {
                UNKNOWN,
                PNG,
                JPEG,
                GIF,
                WEBP
              }

              enum ProtoImageFormat {
                IMAGE_FORMAT_UNSPECIFIED,
                PNG,
                JPEG,
                GIF,
                WEBP,
                UNRECOGNIZED
              }

              @Test
              public void singleExcluded(@TestParameter ImageFormat format) {
                assumeFalse(format == ImageFormat.UNKNOWN);
                System.out.println(format);
              }

              @Test
              public void protoExcluded(@TestParameter ProtoImageFormat format) {
                assumeFalse(
                    format == ProtoImageFormat.IMAGE_FORMAT_UNSPECIFIED
                        || format == ProtoImageFormat.UNRECOGNIZED);
                System.out.println(format);
              }
            }
            """)
        .setArgs("-cp", "NOSUCH")
        .doTest();
  }

  @Test
  public void reportsDiagnosticOnFirstMatchedAnnotation() {
    compilationHelper
        .addSourceLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum ImageFormat {
                UNKNOWN,
                PNG,
                JPEG,
                GIF,
                WEBP
              }

              @Test
              public void twoParams(
                  String prefix,
                  // BUG: Diagnostic contains: Prefer an exhaustive @TestParameter
                  @TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat first,
                  @TestParameter({"UNKNOWN", "PNG", "JPEG", "GIF"}) ImageFormat second) {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "TestType.java",
"""
import com.google.testing.junit.testparameterinjector.TestParameter;
import java.util.function.Consumer;
import org.junit.Test;

public abstract class TestType {
  enum SmallImageFormat {
    UNKNOWN,
    PNG,
    JPEG,
    GIF
  }

  enum ImageFormat {
    UNKNOWN,
    PNG,
    JPEG,
    GIF,
    WEBP
  }

  enum ProtoImageFormat {
    IMAGE_FORMAT_UNSPECIFIED,
    PNG,
    JPEG,
    GIF,
    WEBP,
    UNRECOGNIZED
  }

  enum SmallerProtoImageFormat {
    IMAGE_FORMAT_UNSPECIFIED,
    PNG,
    JPEG,
    GIF,
    UNRECOGNIZED
  }

  @TestParameter({"PNG", "JPEG", "GIF", "WEBP"})
  ImageFormat field;

  public TestType(@TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat ctorParam) {}

  public void helperMethodWithoutTestAnnotation(
      @TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat format) {}

  @Test
  public abstract void abstractMethod(
      @TestParameter({"PNG", "JPEG", "GIF", "WEBP"}) ImageFormat format);

  @Test
  public void localClassField() {
    class Local {
      @TestParameter({"PNG", "JPEG", "GIF", "WEBP"})
      ImageFormat localField;
    }
  }

  @Test
  public void lambdaParameter() {
    Consumer<ImageFormat> consumer =
        (@TestParameter({"PNG", "JPEG", "GIF", "WEBP"})
            ImageFormat f) -> {};
  }

  @Test
  public void suppressedParameter(
      @SuppressWarnings("AlmostExhaustiveTestParameter")
          @TestParameter({"PNG", "JPEG", "GIF", "WEBP"})
          ImageFormat format) {}

  @Test
  public void fourValueEnum(@TestParameter({"PNG", "JPEG", "GIF"}) SmallImageFormat format) {}

  @Test
  public void fiveValueEnumWithUnrecognizedExcludingTwo(
      @TestParameter({"PNG", "JPEG", "GIF"}) SmallerProtoImageFormat format) {}

  @Test
  public void enumWithUnrecognizedExcludingTwoNonUnrecognized(
      @TestParameter({"IMAGE_FORMAT_UNSPECIFIED", "PNG", "JPEG", "UNRECOGNIZED"})
          ProtoImageFormat format) {}

  @Test
  public void enumWithUnrecognizedExcludingThree(
      @TestParameter({"JPEG", "GIF", "WEBP"}) ProtoImageFormat format) {}

  @Test
  public void allValuesSpecified(
      @TestParameter({"UNKNOWN", "PNG", "JPEG", "GIF", "WEBP"}) ImageFormat format) {}

  @Test
  public void excludesTwoRegularEnumValues(
      @TestParameter({"JPEG", "GIF", "WEBP"}) ImageFormat format) {}

  @Test
  public void duplicateValues(
      @TestParameter({"PNG", "PNG", "JPEG", "GIF", "WEBP"}) ImageFormat format) {}

  @Test
  public void includesNull(
      @TestParameter({"PNG", "JPEG", "GIF", "WEBP", "null"}) ImageFormat format) {}

  @Test
  public void implicitTestParameter(@TestParameter ImageFormat format) {}

  @Test
  public void stringParameter(@TestParameter({"a", "b", "c", "d"}) String s) {}
}
""")
        .doTest();
  }
}
