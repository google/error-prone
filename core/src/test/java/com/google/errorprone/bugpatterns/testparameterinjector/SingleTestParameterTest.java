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

import static com.google.common.truth.Truth.assertThat;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link SingleTestParameter}. */
@RunWith(TestParameterInjector.class)
public final class SingleTestParameterTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(SingleTestParameter.class, getClass());

  @Test
  public void singleParameterParameterisedOver_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters("{a: 1}")
              @TestParameters("{a: 2}")
              @TestParameters("{a: 3}")
              public void test(int a) {
                System.out.println(a);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {

              public void test(@TestParameter({"1", "2", "3"}) int a) {
                System.out.println(a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleParameter_singleValue_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters("{a: 1}")
              public void test(int a) {
                System.out.println(a);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {

              public void test(@TestParameter({"1"}) int a) {
                System.out.println(a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleParameter_repeatedAnnotationsWithRepeatedValues_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters("{a: 1}")
              @TestParameters({"{a: 2}", "{a: 3}"})
              public void test(int a) {
                System.out.println(a);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {

              public void test(@TestParameter({"1", "2", "3"}) int a) {
                System.out.println(a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleEnumParameterParameterisedOver_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              enum E {
                A,
                B,
                C
              }

              @TestParameters("{a: A}")
              @TestParameters("{a: B}")
              @TestParameters("{a: C}")
              public void test(E a) {
                System.out.println(a);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {
              enum E {
                A,
                B,
                C
              }

              public void test(@TestParameter E a) {
                System.out.println(a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleBooleanParameterExhaustive_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters({"{b: true}", "{b: false}"})
              public void test(boolean b) {
                System.out.println(b);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {

              public void test(@TestParameter boolean b) {
                System.out.println(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleBoxedBooleanParameterExhaustive_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters({"{b: true}", "{b: false}"})
              public void test(Boolean b) {
                System.out.println(b);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {

              public void test(@TestParameter Boolean b) {
                System.out.println(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleBooleanParameterExhaustive_outOfOrderAndCaseInsensitive_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters({"{b: FALSE}", "{b: TRUE}"})
              public void test(boolean b) {
                System.out.println(b);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {

              public void test(@TestParameter boolean b) {
                System.out.println(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleEnumParameterExhaustive_outOfOrder_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              enum E {
                A,
                B,
                C
              }

              @TestParameters({"{e: C}", "{e: A}", "{e: B}"})
              public void test(E e) {
                System.out.println(e);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {
              enum E {
                A,
                B,
                C
              }

              public void test(@TestParameter E e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleBooleanWithNull_keepsExplicitValues() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters({"{b: true}", "{b: false}", "{b: null}"})
              public void test(Boolean b) {
                System.out.println(b);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {

              public void test(@TestParameter({"true", "false", "null"}) Boolean b) {
                System.out.println(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleEnumWithNull_keepsExplicitValues() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              enum E {
                A,
                B
              }

              @TestParameters({"{e: A}", "{e: B}", "{e: null}"})
              public void test(E e) {
                System.out.println(e);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {
              enum E {
                A,
                B
              }

              public void test(@TestParameter({"A", "B", "null"}) E e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleParameterParameterisedOver_expressedInOneAnnotation_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters({"{a: 1}", "{a: 2}", "{a: 3}"})
              public void test(int a) {
                System.out.println(a);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {

              public void test(@TestParameter({"1", "2", "3"}) int a) {
                System.out.println(a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void strings() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters("{a: \\"foo bar\\"}")
              @TestParameters("{a: \\"baz\\"}")
              @TestParameters("{a: quux}")
              @TestParameters("{a: \\"qu\\\\\\"ux\\"}")
              public void test(String a) {
                System.out.println(a);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {

              public void test(@TestParameter({"foo bar", "baz", "quux", "qu\\"ux"}) String a) {
                System.out.println(a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleParameters_cannotBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters("{a: 1, b: 1}")
              @TestParameters("{a: 2, b: 2}")
              public void test(int a, int b) {
                System.out.println(a);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void customName_cannotBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters(value = "{a: 1}", customName = "foo")
              @TestParameters(value = "{a: 2}", customName = "bar")
              public void test(int a) {
                System.out.println(a);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void nullValues_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;

            class Test {
              @TestParameters("{a: 'foo'}")
              @TestParameters("{a: null}")
              public void test(String a) {
                System.out.println(a);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            class Test {

              public void test(@TestParameter({"foo", "null"}) String a) {
                System.out.println(a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void genericType_notRefactorable() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import java.util.List;

            class Test {
              @TestParameters("a: []")
              @TestParameters("a: ['foo']")
              public void test(List<String> a) {
                System.out.println(a);
              }

              @TestParameters("{a: null}")
              public void testNull(List<String> a) {
                System.out.println(a);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void enum_notExhaustive() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              enum MyEnum {
                FOO,
                BAR,
                BAZ
              }

              @Test
              @TestParameters({"{mode: FOO}", "{mode: BAR}"})
              public void myTest(MyEnum mode) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum MyEnum {
                FOO,
                BAR,
                BAZ
              }

              @Test
              public void myTest(@TestParameter({"FOO", "BAR"}) MyEnum mode) {}
            }
            """)
        .doTest();
  }

  @Test
  public void strings_withoutQuotes() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              @Test
              @TestParameters({"{name: kurt}", "{name: chris}", "{name: liam}"})
              public void myTest(String name) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(@TestParameter({"kurt", "chris", "liam"}) String name) {}
            }
            """)
        .doTest();
  }

  @Test
  public void strings_withQuotes() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              @Test
              @TestParameters({"{name: 'kurt'}", "{name: \\"chris\\"}", "{name: 'liam'}"})
              public void myTest(String name) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(@TestParameter({"kurt", "chris", "liam"}) String name) {}
            }
            """)
        .doTest();
  }

  @Test
  public void unbracedAndTrailingComma_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              @Test
              @TestParameters({"a: 1", "{a: 2,}", "{'a': 3}"})
              public void myTest(int a) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(@TestParameter({"1", "2", "3"}) int a) {}
            }
            """)
        .doTest();
  }

  @Test
  public void quotedStringEscapesAndEmptyString_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              @Test
              @TestParameters({"{a: 'it''s'}", "{a: \\"line1\\\\nline2\\"}", "{a: ''}", "{a: '  '}"})
              public void myTest(String a) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(@TestParameter({"it's", "line1\\nline2", "", "  "}) String a) {}
            }
            """)
        .doTest();
  }

  @Test
  public void durationWithQuotes_canBeSimplified() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import java.time.Duration;
            import org.junit.Test;

            public class TestType {
              @Test
              @TestParameters({"{d: '5s'}", "{d: 100ms}"})
              public void myTest(Duration d) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import java.time.Duration;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(@TestParameter({"5s", "100ms"}) Duration d) {}
            }
            """)
        .doTest();
  }

  @Test
  public void quotedNullString_notRefactorable() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              @Test
              @TestParameters({"{a: 'null'}", "{a: \\"null\\"}"})
              public void myTest(String a) {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void parameterAlreadyHasTestParameter_notRefactorable() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              @Test
              @TestParameters("{a: 1}")
              public void myTest(@TestParameter int a) {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void complexOrMalformedYaml_notRefactorable() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              record Pair(int x) {}

              @Test
              @TestParameters("{p: {x: 1}}")
              public void recordTest(Pair p) {}

              @Test
              @TestParameters("{wrongKey: 1}")
              public void mismatchedKey(int a) {}

              @Test
              @TestParameters("{a: 1, a: 2}")
              public void duplicateKey(int a) {}

              @Test
              @TestParameters("{a: [unclosed")
              public void malformedYaml(int a) {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void noTestParametersOrNonValueAttribute_notRefactorable() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import com.google.testing.junit.testparameterinjector.TestParametersValuesProvider;
            import org.junit.Test;

            public class TestType {
              public void helperMethod(int a) {}

              @Test
              @TestParameters({})
              public void emptyValues(int a) {}

              @Test
              @TestParameters
              public void defaultTestParameters(int a) {}

              @Test
              @TestParameters(valuesProvider = MyProvider.class)
              public void valuesProvider(int a) {}

              abstract static class MyProvider extends TestParametersValuesProvider {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void repeatedTestParameters_notRefactorable() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              @Test
              @TestParameters.RepeatedTestParameters({
                @TestParameters("{a: 1}"),
                @TestParameters("{a: 2}")
              })
              public void explicitRepeated(int a) {}

              @Test
              @TestParameters("{a: 1}")
              @TestParameters.RepeatedTestParameters({
                @TestParameters("{a: 2}"),
                @TestParameters("{a: 3}")
              })
              public void mixedRepeated(int a) {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  // The tests below just validate the behavior of TestParameterInjector when it extracts strings
  // from the annotations. The tl;dr is that the @TestParameters annotation allows quotes (single or
  // double) around values as part of its YAML-like syntax, whereas @TestParameter treats quotes as
  // literal parts of the string value.

  @SuppressWarnings("SingleValueTestParameter")
  @Test
  public void testParameter_withSingleQuotedValues(@TestParameter({"'kurt'"}) String name) {
    assertThat(name).isEqualTo("'kurt'");
  }

  @SuppressWarnings("SingleValueTestParameter")
  @Test
  public void testParameter_withDoubleQuotedValues(@TestParameter({"\"kurt\""}) String name) {
    assertThat(name).isEqualTo("\"kurt\"");
  }

  @SuppressWarnings("SingleValueTestParameter")
  @Test
  public void testParameter_withUnquotedValues(@TestParameter({"kurt"}) String name) {
    assertThat(name).isEqualTo("kurt");
  }
}
