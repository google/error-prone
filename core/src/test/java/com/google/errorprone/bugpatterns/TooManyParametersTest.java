/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.bugpatterns.TooManyParameters.TOO_MANY_PARAMETERS_FLAG_NAME;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.ErrorProneFlags;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link TooManyParameters}. */
@RunWith(JUnit4.class)
public class TooManyParametersTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(TooManyParameters.class, getClass());

  @Test
  public void zeroLimit() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TooManyParameters(
                ErrorProneFlags.builder().putFlag(TOO_MANY_PARAMETERS_FLAG_NAME, "0").build()));
  }

  @Test
  public void negativeLimit() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TooManyParameters(
                ErrorProneFlags.builder().putFlag(TOO_MANY_PARAMETERS_FLAG_NAME, "-1").build()));
  }

  @Test
  public void constructor() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:" + TOO_MANY_PARAMETERS_FLAG_NAME + "=3"))
        .addSourceLines(
            "ConstructorTest.java",
            """
            public class ConstructorTest {
              public ConstructorTest() {}

              public ConstructorTest(int a) {}

              public ConstructorTest(int a, int b) {}

              public ConstructorTest(int a, int b, int c) {}

              // BUG: Diagnostic contains: 4 parameters
              public ConstructorTest(int a, int b, int c, int d) {}

              // BUG: Diagnostic contains: 5 parameters
              public ConstructorTest(int a, int b, int c, int d, int e) {}

              // BUG: Diagnostic contains: 6 parameters
              public ConstructorTest(int a, int b, int c, int d, int e, int f) {}

              private ConstructorTest(int a, int b, int c, int d, int e, int f, int g) {}
            }
            """)
        .doTest();
  }

  @Test
  public void recordConstructor() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:" + TOO_MANY_PARAMETERS_FLAG_NAME + "=3"))
        .addSourceLines(
            "RecordExample.java",
            """
            public record RecordExample(int p0, int p1, int p2, int p3, int p4, int p5) {
              public RecordExample {}
            }
            """)
        .doTest();
  }

  @Test
  public void constructor_withAtInject() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:" + TOO_MANY_PARAMETERS_FLAG_NAME + "=3"))
        .addSourceLines(
            "ConstructorTest.java",
            """
            import javax.inject.Inject;

            public class ConstructorTest {
              public ConstructorTest() {}

              public ConstructorTest(int a) {}

              public ConstructorTest(int a, int b) {}

              public ConstructorTest(int a, int b, int c) {}

              @Inject
              public ConstructorTest(int a, int b, int c, int d) {}

              // BUG: Diagnostic contains: 4 parameters
              public ConstructorTest(short a, short b, short c, short d) {}
            }
            """)
        .doTest();
  }

  @Test
  public void ignoresAutoFactoryOnClass() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:" + TOO_MANY_PARAMETERS_FLAG_NAME + "=3"))
        .addSourceLines(
            "AutoFactory.java",
            """
            package com.google.auto.factory;

            public @interface AutoFactory {}
            """)
        .addSourceLines(
            "Test.java",
            """
            @com.google.auto.factory.AutoFactory
            public class Test {
              public Test(int a, int b, int c, int d) {}
            }
            """)
        .addSourceLines(
            "TestWithoutAutoFactory.java",
            """
            public class TestWithoutAutoFactory {
              // BUG: Diagnostic contains: 4 parameters
              public TestWithoutAutoFactory(int a, int b, int c, int d) {}
            }
            """)
        .doTest();
  }

  @Test
  public void ignoresAutoFactoryOnConstructor() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:" + TOO_MANY_PARAMETERS_FLAG_NAME + "=3"))
        .addSourceLines(
            "AutoFactory.java",
            """
            package com.google.auto.factory;

            public @interface AutoFactory {}
            """)
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              @com.google.auto.factory.AutoFactory
              public Test(int a, int b, int c, int d) {}
            }
            """)
        .addSourceLines(
            "TestWithoutAutoFactory.java",
            """
            public class TestWithoutAutoFactory {
              // BUG: Diagnostic contains: 4 parameters
              public TestWithoutAutoFactory(int a, int b, int c, int d) {}
            }
            """)
        .doTest();
  }

  @Test
  public void method() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:" + TOO_MANY_PARAMETERS_FLAG_NAME + "=3"))
        .addSourceLines(
            "MethodTest.java",
            """
            public class MethodTest {
              public void foo() {}

              public void foo(int a) {}

              public void foo(int a, int b) {}

              public void foo(int a, int b, int c) {}

              // BUG: Diagnostic contains: 4 parameters
              public void foo(int a, int b, int c, int d) {}

              // BUG: Diagnostic contains: 5 parameters
              public void foo(int a, int b, int c, int d, int e) {}

              // BUG: Diagnostic contains: 6 parameters
              public void foo(int a, int b, int c, int d, int e, int f) {}

              private void foo(int a, int b, int c, int d, int e, int f, int g) {}
            }
            """)
        .doTest();
  }

  @Test
  public void testJUnitTestMethod() {
    compilationHelper
        .addSourceLines(
            "ExampleWithTestParametersTest.java",
"""
import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameters;
import com.google.testing.junit.testparameterinjector.TestParametersValuesProvider;
import org.junit.Test;

public class ExampleWithTestParametersTest {
  @Test
  @TestParameters(valuesProvider = TestArgs.class)
  public void myTest(
      String a,
      String b,
      String c,
      String d,
      String e,
      String f,
      String g,
      String h,
      String i,
      String j,
      String k,
      String l)
      throws Exception {}

  static class TestArgs extends TestParametersValuesProvider {
    @Override
    public ImmutableList<TestParameters.TestParametersValues> provideValues(Context context) {
      return ImmutableList.of();
    }
  }
}
""")
        .doTest();
  }

  @Test
  public void testHttpMethods() {
    compilationHelper
        .setArgs(ImmutableList.of("-XepOpt:" + TOO_MANY_PARAMETERS_FLAG_NAME + "=3"))
        .addSourceLines(
            "HttpMethod.java",
            """
            package com.google.apps.framework.request;

            public final class HttpMethod {
              public @interface All {}

              public @interface Head {}

              public @interface Get {}

              public @interface Post {}

              public @interface Put {}

              public @interface Patch {}

              public @interface Delete {}

              public @interface Options {}
            }
            """)
        .addSourceLines(
            "SomeAction.java",
            """
            public final class SomeAction {
              @com.google.apps.framework.request.HttpMethod.All
              public Object executeAll(int a, int b, int c, int d, int e) {
                return new Object();
              }

              @com.google.apps.framework.request.HttpMethod.Head
              public Object executeHead(int a, int b, int c, int d, int e) {
                return new Object();
              }

              @com.google.apps.framework.request.HttpMethod.Get
              public Object executeGet(int a, int b, int c, int d, int e) {
                return new Object();
              }

              @com.google.apps.framework.request.HttpMethod.Post
              public Object executePost(int a, int b, int c, int d, int e) {
                return new Object();
              }

              @com.google.apps.framework.request.HttpMethod.Put
              public Object executePut(int a, int b, int c, int d, int e) {
                return new Object();
              }

              @com.google.apps.framework.request.HttpMethod.Patch
              public Object executePatch(int a, int b, int c, int d, int e) {
                return new Object();
              }

              @com.google.apps.framework.request.HttpMethod.Delete
              public Object executeDelete(int a, int b, int c, int d, int e) {
                return new Object();
              }

              @com.google.apps.framework.request.HttpMethod.Options
              public Object executeOptions(int a, int b, int c, int d, int e) {
                return new Object();
              }
            }
            """)
        .doTest();
  }
}
