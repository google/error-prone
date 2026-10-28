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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link SingleValueTestParameter}. */
@RunWith(JUnit4.class)
public final class SingleValueTestParameterTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(SingleValueTestParameter.class, getClass());

  @Test
  public void testParameter_strings() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void singleString(@TestParameter("hello") String s) {
                String upper = s.toUpperCase();
                System.out.println(upper);
              }

              @Test
              public void singleElementArray(@TestParameter({"world"}) String s) {
                System.out.println(s);
              }

              @Test
              public void namedValueAttribute(@TestParameter(value = "foo") String s) {
                System.out.println(s);
              }

              @Test
              public void namedValueArrayAttribute(@TestParameter(value = {"bar"}) String s) {
                System.out.println(s);
              }

              @Test
              public void emptyString(@TestParameter("") String s) {
                System.out.println(s);
              }

              @Test
              public void escapedCharacters(@TestParameter("a\\"b\\\\c\\nd") String s) {
                System.out.println(s);
              }

              @Test
              public void literalSingleQuotesPreserved(@TestParameter("'kurt'") String s) {
                System.out.println(s);
              }

              @Test
              public void literalQuotedNullPreserved(@TestParameter("'null'") String s) {
                System.out.println(s);
              }

              @Test
              public void parenthesizedString(@TestParameter({("parens")}) String s) {
                System.out.println(s);
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import org.junit.Test;

            public class TestType {
              @Test
              public void singleString() {
                String s = "hello";
                String upper = s.toUpperCase();
                System.out.println(upper);
              }

              @Test
              public void singleElementArray() {
                String s = "world";
                System.out.println(s);
              }

              @Test
              public void namedValueAttribute() {
                String s = "foo";
                System.out.println(s);
              }

              @Test
              public void namedValueArrayAttribute() {
                String s = "bar";
                System.out.println(s);
              }

              @Test
              public void emptyString() {
                String s = "";
                System.out.println(s);
              }

              @Test
              public void escapedCharacters() {
                String s = "a\\"b\\\\c\\nd";
                System.out.println(s);
              }

              @Test
              public void literalSingleQuotesPreserved() {
                String s = "'kurt'";
                System.out.println(s);
              }

              @Test
              public void literalQuotedNullPreserved() {
                String s = "'null'";
                System.out.println(s);
              }

              @Test
              public void parenthesizedString() {
                String s = "parens";
                System.out.println(s);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void testParameter_booleansAndNumbers() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void booleans(
                  @TestParameter("true") boolean a,
                  @TestParameter("True") boolean b,
                  @TestParameter("TRUE") Boolean c,
                  @TestParameter("false") boolean d,
                  @TestParameter("False") boolean e,
                  @TestParameter("FALSE") Boolean f) {
                System.out.println(a + " " + b + " " + c + " " + d + " " + e + " " + f);
              }

              @Test
              public void ints(
                  @TestParameter("0") int zero,
                  @TestParameter("42") int a,
                  @TestParameter("-7") Integer b,
                  @TestParameter("2147483647") int max,
                  @TestParameter("-2147483648") int min) {
                System.out.println(zero + a + b + max + min);
              }

              @Test
              public void longs(
                  @TestParameter("0") long zero,
                  @TestParameter("42") long a,
                  @TestParameter("-7") Long b,
                  @TestParameter("9223372036854775807") long max,
                  @TestParameter("-9223372036854775808") long min) {
                System.out.println(zero + a + b + max + min);
              }

              @Test
              public void floats(
                  @TestParameter("0") float zero,
                  @TestParameter("1.5") float a,
                  @TestParameter("-2.25") Float b,
                  @TestParameter("123") Float fromInt) {
                System.out.println(zero + a + b + fromInt);
              }

              @Test
              public void doubles(
                  @TestParameter("0") double zero,
                  @TestParameter("2.5") double a,
                  @TestParameter("-3.75") Double b,
                  @TestParameter("123") Double fromInt) {
                System.out.println(zero + a + b + fromInt);
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import org.junit.Test;

            public class TestType {
              @Test
              public void booleans() {
                boolean a = true;
                boolean b = true;
                Boolean c = true;
                boolean d = false;
                boolean e = false;
                Boolean f = false;
                System.out.println(a + " " + b + " " + c + " " + d + " " + e + " " + f);
              }

              @Test
              public void ints() {
                int zero = 0;
                int a = 42;
                Integer b = -7;
                int max = 2147483647;
                int min = -2147483648;
                System.out.println(zero + a + b + max + min);
              }

              @Test
              public void longs() {
                long zero = 0L;
                long a = 42L;
                Long b = -7L;
                long max = 9223372036854775807L;
                long min = -9223372036854775808L;
                System.out.println(zero + a + b + max + min);
              }

              @Test
              public void floats() {
                float zero = 0.0f;
                float a = 1.5f;
                Float b = -2.25f;
                Float fromInt = 123.0f;
                System.out.println(zero + a + b + fromInt);
              }

              @Test
              public void doubles() {
                double zero = 0.0;
                double a = 2.5;
                Double b = -3.75;
                Double fromInt = 123.0;
                System.out.println(zero + a + b + fromInt);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void testParameter_enums() {
    testHelper
        .addInputLines(
            "other/ImportedEnum.java",
            """
            package other;

            public enum ImportedEnum {
              ALPHA,
              BETA
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum Color {
                RED,
                GREEN,
                BLUE
              }

              static final class Outer {
                enum Shade {
                  LIGHT,
                  DARK
                }
              }

              @Test
              public void enumConstant(@TestParameter("GREEN") Color color) {
                System.out.println(color);
              }

              @Test
              public void qualifiedEnumConstant(@TestParameter("DARK") Outer.Shade shade) {
                System.out.println(shade);
              }

              @Test
              public void unimportedEnumConstant(@TestParameter("ALPHA") other.ImportedEnum value) {
                System.out.println(value);
              }

              public static class WithObscuringField {
                String Outer = "shadow";

                @Test
                public void fieldObscuringOuterClass(@TestParameter("DARK") Outer.Shade shade) {
                  System.out.println(Outer + shade);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import org.junit.Test;
            import other.ImportedEnum;

            public class TestType {
              enum Color {
                RED,
                GREEN,
                BLUE
              }

              static final class Outer {
                enum Shade {
                  LIGHT,
                  DARK
                }
              }

              @Test
              public void enumConstant() {
                Color color = Color.GREEN;
                System.out.println(color);
              }

              @Test
              public void qualifiedEnumConstant() {
                Outer.Shade shade = Outer.Shade.DARK;
                System.out.println(shade);
              }

              @Test
              public void unimportedEnumConstant() {
                other.ImportedEnum value = ImportedEnum.ALPHA;
                System.out.println(value);
              }

              public static class WithObscuringField {
                String Outer = "shadow";

                @Test
                public void fieldObscuringOuterClass() {
                  Outer.Shade shade = TestType.Outer.Shade.DARK;
                  System.out.println(Outer + shade);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void testParameter_subsetOfParametersInlined() {
    testHelper
        .addInputLines(
            "TestType.java",
"""
import com.google.testing.junit.testparameterinjector.TestParameter;
import org.junit.Test;

public class TestType {
  @Test
  public void leadingInlined(
      @TestParameter("1") int a, @TestParameter boolean b, @TestParameter({"x", "y"}) String c) {
    System.out.println(a + " " + b + " " + c);
  }

  @Test
  public void middleInlined(
      @TestParameter boolean a, @TestParameter("1") int b, @TestParameter({"x", "y"}) String c) {
    System.out.println(a + " " + b + " " + c);
  }

  @Test
  public void trailingInlined(
      @TestParameter boolean a, @TestParameter({"x", "y"}) String b, @TestParameter("1") int c) {
    System.out.println(a + " " + b + " " + c);
  }

  @Test
  public void multipleInlinedWithOneKept(
      @TestParameter("1") int a, @TestParameter boolean kept, @TestParameter("foo") String b) {
    System.out.println(a + " " + kept + " " + b);
  }

  @Test
  public void leadingInlinedBeforeVarargs(@TestParameter("foo") String first, String... rest) {
    System.out.println(first + rest.length);
  }
}
""")
        .addOutputLines(
            "TestType.java",
"""
import com.google.testing.junit.testparameterinjector.TestParameter;
import org.junit.Test;

public class TestType {
  @Test
  public void leadingInlined(@TestParameter boolean b, @TestParameter({"x", "y"}) String c) {
    int a = 1;
    System.out.println(a + " " + b + " " + c);
  }

  @Test
  public void middleInlined(@TestParameter boolean a, @TestParameter({"x", "y"}) String c) {
    int b = 1;
    System.out.println(a + " " + b + " " + c);
  }

  @Test
  public void trailingInlined(@TestParameter boolean a, @TestParameter({"x", "y"}) String b) {
    int c = 1;
    System.out.println(a + " " + b + " " + c);
  }

  @Test
  public void multipleInlinedWithOneKept(@TestParameter boolean kept) {
    int a = 1;
    String b = "foo";
    System.out.println(a + " " + kept + " " + b);
  }

  @Test
  public void leadingInlinedBeforeVarargs(String... rest) {
    String first = "foo";
    System.out.println(first + rest.length);
  }
}
""")
        .doTest();
  }

  @Test
  public void negativeCases_methodStructureAndMultiValue() {
    testHelper
        .addInputLines(
            "TestType.java",
"""
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import org.junit.Test;

public class TestType {
  enum Color {
    RED,
    BLUE
  }

  enum Singleton {
    INSTANCE
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface CustomAnnotation {}

  static final class MyProvider extends TestParameterValuesProvider {
    @Override
    protected List<?> provideValues(Context context) {
      return List.of("only");
    }
  }

  @TestParameter("fieldVal")
  String field;

  public TestType(@TestParameter("ctorVal") String ctorParam) {
    System.out.println(ctorParam);
  }

  public void nonTestHelperMethod(@TestParameter("foo") String s) {
    System.out.println(s);
  }

  @Test
  public void emptyBodySkipped(@TestParameter("foo") String s) {}

  @Test
  public void multipleValues(@TestParameter({"a", "b"}) String s) {
    System.out.println(s);
  }

  @Test
  public void duplicateValues(@TestParameter({"true", "true"}) boolean b) {
    System.out.println(b);
  }

  @Test
  public void implicitBoolean(@TestParameter boolean b) {
    System.out.println(b);
  }

  @Test
  public void implicitEnum(@TestParameter Color c) {
    System.out.println(c);
  }

  @Test
  public void implicitSingletonEnum(@TestParameter Singleton singleton) {
    System.out.println(singleton);
  }

  @Test
  public void withValuesProvider(@TestParameter(valuesProvider = MyProvider.class) String s) {
    System.out.println(s);
  }

  @Test
  public void varargsParameter(@TestParameter("foo") String... args) {
    System.out.println(args.length);
  }

  @Test
  public void parameterWithOtherAnnotation(@CustomAnnotation @TestParameter("foo") String s) {
    System.out.println(s);
  }
}
""")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testParameter_nullValues() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum Color {
                RED,
                BLUE
              }

              @Test
              public void nullReferenceTypes(
                  @TestParameter("null") String s,
                  @TestParameter("null") Integer i,
                  @TestParameter("null") Long l,
                  @TestParameter("null") Float f,
                  @TestParameter("null") Double d,
                  @TestParameter("null") Boolean b,
                  @TestParameter("null") Color c,
                  @TestParameter("null") Object o) {
                System.out.println("" + s + i + l + f + d + b + c + o);
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import org.junit.Test;

            public class TestType {
              enum Color {
                RED,
                BLUE
              }

              @Test
              public void nullReferenceTypes() {
                String s = null;
                Integer i = null;
                Long l = null;
                Float f = null;
                Double d = null;
                Boolean b = null;
                Color c = null;
                Object o = null;
                System.out.println("" + s + i + l + f + d + b + c + o);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCases_nullAndNullable() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.junit.Test;

            public class TestType {
              private static final String NULL_LITERAL = "null";

              @Target({ElementType.PARAMETER, ElementType.TYPE_USE})
              @Retention(RetentionPolicy.RUNTIME)
              @interface Nullable {}

              @Test
              public void nullPrimitiveInt(@TestParameter("null") int i) {
                System.out.println(i);
              }

              @Test
              public void nullPrimitiveBoolean(@TestParameter("null") boolean b) {
                System.out.println(b);
              }

              @Test
              public void nullStringConstant(@TestParameter(NULL_LITERAL) String constNull) {
                System.out.println(constNull);
              }

              @Test
              public void upperCaseNullInteger(@TestParameter("NULL") Integer i) {
                System.out.println(i);
              }

              @Test
              public void tildeNullInteger(@TestParameter("~") Integer i) {
                System.out.println(i);
              }

              @Test
              public void emptyInteger(@TestParameter("") Integer emptyInt) {
                System.out.println(emptyInt);
              }

              @Test
              public void nullableParameter(@TestParameter("foo") @Nullable String s) {
                System.out.println(s);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeCases_invalidValuesAndUnsupportedTypes() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum Color {
                RED,
                BLUE
              }

              @Test
              public void invalidEnum(@TestParameter("PURPLE") Color c) {
                System.out.println(c);
              }

              @Test
              public void yamlBooleanYes(@TestParameter("yes") boolean b) {
                System.out.println(b);
              }

              @Test
              public void nonNumericInt(@TestParameter("notAnInt") int x) {
                System.out.println(x);
              }

              @Test
              public void overflowInt(@TestParameter("2147483648") int x) {
                System.out.println(x);
              }

              @Test
              public void underflowInt(@TestParameter("-2147483649") int x) {
                System.out.println(x);
              }

              @Test
              public void nonNumericLong(@TestParameter("notALong") long x) {
                System.out.println(x);
              }

              @Test
              public void trailingLLong(@TestParameter("42L") long x) {
                System.out.println(x);
              }

              @Test
              public void overflowLong(@TestParameter("9223372036854775808") long x) {
                System.out.println(x);
              }

              @Test
              public void underflowLong(@TestParameter("-9223372036854775809") long x) {
                System.out.println(x);
              }

              @Test
              public void nonNumericFloat(@TestParameter("notAFloat") float f) {
                System.out.println(f);
              }

              @Test
              public void nanFloat(@TestParameter("NaN") float f) {
                System.out.println(f);
              }

              @Test
              public void nonNumericDouble(@TestParameter("notADouble") double d) {
                System.out.println(d);
              }

              @Test
              public void nanDouble(@TestParameter("NaN") double d) {
                System.out.println(d);
              }

              @Test
              public void infinityDouble(@TestParameter("Infinity") double d) {
                System.out.println(d);
              }

              @Test
              public void byteParam(@TestParameter("1") byte b) {
                System.out.println(b);
              }

              @Test
              public void boxedByteParam(@TestParameter("1") Byte b) {
                System.out.println(b);
              }

              @Test
              public void shortParam(@TestParameter("2") short s) {
                System.out.println(s);
              }

              @Test
              public void boxedShortParam(@TestParameter("2") Short s) {
                System.out.println(s);
              }

              @Test
              public void charParam(@TestParameter("a") char c) {
                System.out.println(c);
              }

              @Test
              public void boxedCharParam(@TestParameter("a") Character c) {
                System.out.println(c);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeCases_testParametersAnnotations() {
    testHelper
        .addInputLines(
            "TestType.java",
"""
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameters;
import org.junit.Test;

public class TestType {
  @Test
  @TestParameters("{x: 1}")
  public void singleTestParametersDeferred(int x) {
    System.out.println(x);
  }

  @Test
  @TestParameters("{x: 1}")
  public void mixedTestParametersAndTestParameter(@TestParameter("1") int x) {
    System.out.println(x);
  }

  @Test
  @TestParameters("{x: 1}")
  @TestParameters("{x: 2}")
  public void repeatedTestParameters(@TestParameter("1") int x) {
    System.out.println(x);
  }

  @Test
  @TestParameters.RepeatedTestParameters({@TestParameters("{x: 1}"), @TestParameters("{x: 2}")})
  public void explicitRepeatedContainer(@TestParameter("1") int x) {
    System.out.println(x);
  }
}
""")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeCases_overrides() {
    testHelper
        .addInputLines(
            "BaseTest.java",
            """
            import org.junit.Test;

            public class BaseTest {
              @Test
              public void overriddenWithAnnotation(String s) {}

              @Test
              public void overriddenWithoutAnnotation(String s) {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType extends BaseTest {
              @Test
              @Override
              public void overriddenWithAnnotation(@TestParameter("foo") String s) {
                System.out.println(s);
              }

              @Test
              public void overriddenWithoutAnnotation(@TestParameter("foo") String s) {
                System.out.println(s);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeCases_constantExpressions() {
    testHelper
        .addInputLines(
            "TestType.java",
"""
import com.google.testing.junit.testparameterinjector.TestParameter;
import org.junit.Test;

public class TestType {
  private static final int DES_V2 = 4;
  private static final String INT_STRING_CONSTANT = "4";
  private static final String DEFAULT_PATH = "/tmp/foo";
  private static final boolean DEFAULT_ENABLED = true;
  private static final String DEFAULT_COLOR = "RED";

  enum Color {
    RED,
    BLUE
  }

  @Test
  public void intConstantInArray(@TestParameter({"" + DES_V2}) int desLevel) {
    System.out.println(desLevel);
  }

  @Test
  public void intConstantConcatenation(@TestParameter("" + DES_V2) int desLevel) {
    System.out.println(desLevel);
  }

  @Test
  public void intFromStringConstant(@TestParameter(INT_STRING_CONSTANT) int desLevel) {
    System.out.println(desLevel);
  }

  @Test
  public void stringConstant(@TestParameter(DEFAULT_PATH) String path) {
    System.out.println(path);
  }

  @Test
  public void stringConstantInArray(@TestParameter({DEFAULT_PATH}) String path) {
    System.out.println(path);
  }

  @Test
  public void booleanConstantInArray(@TestParameter({"" + DEFAULT_ENABLED}) boolean enabled) {
    System.out.println(enabled);
  }

  @Test
  public void enumFromStringConstant(@TestParameter(DEFAULT_COLOR) Color color) {
    System.out.println(color);
  }
}
""")
        .expectUnchanged()
        .doTest();
  }
}
