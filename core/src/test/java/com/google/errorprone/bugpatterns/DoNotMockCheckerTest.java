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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.annotations.DoNotMock;
import java.lang.annotation.Retention;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/**
 * Tests for {@link DoNotMockChecker}.
 *
 * @author amalloy@google.com (Alan Malloy)
 */
@RunWith(JUnit4.class)
public final class DoNotMockCheckerTest {

  private static final String DO_NOT_MOCK_REASON =
      "the reason why OtherDoNotMockObject is DoNotMock";

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(DoNotMockChecker.class, getClass())
          .addSourceLines(
              "lib/DoNotMockObjects.java",
              "package lib;",
              "import org.mockito.Mockito;",
              "import com.google.errorprone.annotations.DoNotMock;",
              "import java.lang.annotation.Inherited;",
              "import java.lang.annotation.Retention;",
              "import java.lang.annotation.RetentionPolicy;",
              "class DoNotMockObjects {",
              "",
              "  class MockableObject {}",
              "  @DoNotMock(\"" + DO_NOT_MOCK_REASON + "\") class DoNotMockObject {",
              "    DoNotMockObject(String s) {}",
              "    DoNotMockObject() {}",
              "  }",
              "  @DoNotMock(\"\") class OtherDoNotMockObject {}",
              "",
              "  @Inherited",
              "  @DoNotMock(\"" + DO_NOT_MOCK_REASON + "\")",
              "  @Retention(RetentionPolicy.RUNTIME)",
              "  @interface MetaDoNotMock {}",
              "  @MetaDoNotMock static class MetaDoNotMockObject {}",
              "",
              "  @MetaDoNotMock @Retention(RetentionPolicy.RUNTIME)",
              "  @interface DoubleMetaDoNotMock {}",
              "  @DoubleMetaDoNotMock static class DoubleMetaAnnotatedDoNotMock {}",
              "",
              "  class ExtendsDoNotMockObject extends DoNotMockObject {}",
              "  class ExtendsMetaDoNotMockObject extends MetaDoNotMockObject {}",
              "",
              "  @DoNotMock(\"" + DO_NOT_MOCK_REASON + "\") interface DoNotMockInterface {}",
              "  @MetaDoNotMock interface MetaDoNotMockInterface {}",
              "  class ImplementsDoNotMockInterfaceObject implements DoNotMockInterface {}",
              "  class ImplementsMetaDoNotMockInterfaceObject",
              "      implements MetaDoNotMockInterface {} ",
              "}");

  @Test
  public void matchesMockitoDotMock_doNotMock() {
    testHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "import org.mockito.Mockito;",
            "import lib.DoNotMockObjects.*;",
            "class Lib {",
            "",
            "  public static void f() { ",
            "    Mockito.spy(MockableObject.class);",
            "// BUG: Diagnostic contains: "
                + String.format(
                    "Do not mock '%s'; %s is annotated as @DoNotMock: %s",
                    "lib.DoNotMockObjects.DoNotMockObject",
                    "lib.DoNotMockObjects.DoNotMockObject",
                    DO_NOT_MOCK_REASON),
            "    Mockito.mock(DoNotMockObject.class);",
            "// BUG: Diagnostic contains: "
                + String.format(
                    "Do not mock '%s'; %s is annotated as @DoNotMock: %s",
                    "lib.DoNotMockObjects.OtherDoNotMockObject",
                    "lib.DoNotMockObjects.OtherDoNotMockObject",
                    "It is annotated as DoNotMock."),
            "    Mockito.spy(OtherDoNotMockObject.class);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void matchesEasymockDotMock_doNotMock() {

    String expected =
        String.format(
            "Do not mock '%s'; %s is annotated as @DoNotMock: %s",
            "lib.DoNotMockObjects.DoNotMockObject",
            "lib.DoNotMockObjects.DoNotMockObject",
            DO_NOT_MOCK_REASON);

    testHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "import org.easymock.EasyMock;",
            "import org.easymock.ConstructorArgs;",
            "import lib.DoNotMockObjects.*;",
            "class Lib {",
            "",
            "  private static final ConstructorArgs CONSTRUCTOR_ARGS = new ConstructorArgs(",
            "      (java.lang.reflect.Constructor) null, \"foo\");",
            "  public static void f() { ",
            "    EasyMock.createMock(MockableObject.class);",
            "    // BUG: Diagnostic contains: " + expected,
            "    EasyMock.createMock(DoNotMockObject.class);",
            "    // BUG: Diagnostic contains: " + expected,
            "    EasyMock.createMock(\"my_name\", DoNotMockObject.class);",
            "    // BUG: Diagnostic contains: " + expected,
            "    EasyMock.createNiceMock(DoNotMockObject.class);",
            "    // BUG: Diagnostic contains: " + expected,
            "    EasyMock.createNiceMock(\"my_name\", DoNotMockObject.class);",
            "    // BUG: Diagnostic contains: " + expected,
            "    EasyMock.createStrictMock(DoNotMockObject.class);",
            "    // BUG: Diagnostic contains: " + expected,
            "    EasyMock.createStrictMock(\"my_name\", DoNotMockObject.class);",
            "    // BUG: Diagnostic contains: " + expected,
            "    EasyMock.createMockBuilder(DoNotMockObject.class);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void matchesMockitoMockAnnotation_doNotMock() {
    testHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "import org.mockito.Mock;",
            "import org.mockito.Spy;",
            "import lib.DoNotMockObjects.*;",
            "class Lib {",
            "",
            "  @Mock MockableObject mockableObject;",
            "  // BUG: Diagnostic contains: "
                + String.format(
                    "Do not mock '%s'; %s is annotated as @DoNotMock: %s",
                    "lib.DoNotMockObjects.DoNotMockObject",
                    "lib.DoNotMockObjects.DoNotMockObject",
                    DO_NOT_MOCK_REASON),
            "  @Mock DoNotMockObject unmockableObject;",
            "  // BUG: Diagnostic contains: "
                + String.format(
                    "Do not mock '%s'; %s is annotated as @DoNotMock: %s",
                    "lib.DoNotMockObjects.DoNotMockInterface",
                    "lib.DoNotMockObjects.DoNotMockInterface",
                    DO_NOT_MOCK_REASON),
            "  @Mock DoNotMockInterface doNotMockInterface;",
            "  @Spy MockableObject unspyableObject;",
            "  // BUG: Diagnostic contains:",
            "  @Spy DoNotMockObject spyableObject;",
            "}")
        .doTest();
  }

  @Test
  public void matchesMockAnnotation_doNotMock_extends() {
    testHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "import org.mockito.Mock;",
            "import org.mockito.Spy;",
            "import lib.DoNotMockObjects.*;",
            "class Lib {",
            "",
            "  // BUG: Diagnostic contains: "
                + String.format(
                    "Do not mock '%s' (which is-a '%s'); %s is annotated as @DoNotMock: %s.",
                    "lib.DoNotMockObjects.ExtendsDoNotMockObject",
                    "lib.DoNotMockObjects.DoNotMockObject",
                    "lib.DoNotMockObjects.DoNotMockObject",
                    DO_NOT_MOCK_REASON),
            "  @Mock ExtendsDoNotMockObject extendsDoNotMockObject;",
            "  // BUG: Diagnostic contains: "
                + String.format(
                    "Do not mock '%s' (which is-a '%s'); %s",
                    "lib.DoNotMockObjects.ExtendsMetaDoNotMockObject",
                    "lib.DoNotMockObjects.MetaDoNotMockObject",
                    String.format(
                        "%s is annotated as @%s (which is annotated as @DoNotMock): %s",
                        "lib.DoNotMockObjects.MetaDoNotMockObject",
                        "lib.DoNotMockObjects.MetaDoNotMock",
                        DO_NOT_MOCK_REASON)),
            "  @Mock ExtendsMetaDoNotMockObject extendsMetaDoNotMockObject;",
            "  @Mock MockableObject mockableObject;",
            "}")
        .doTest();
  }

  @Test
  public void matchesMockAnnotation_doNotMock_implements() {
    testHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "import org.mockito.Mock;",
            "import org.mockito.Spy;",
            "import lib.DoNotMockObjects.*;",
            "class Lib {",
            "",
            "  // BUG: Diagnostic contains: "
                + String.format(
                    "Do not mock '%s' (which is-a '%s'); %s is annotated as @DoNotMock: %s.",
                    "lib.DoNotMockObjects.ImplementsDoNotMockInterfaceObject",
                    "lib.DoNotMockObjects.DoNotMockInterface",
                    "lib.DoNotMockObjects.DoNotMockInterface",
                    DO_NOT_MOCK_REASON),
            "  @Mock ImplementsDoNotMockInterfaceObject implementsDoNotMockInterfaceObject;",
            "  // BUG: Diagnostic contains: "
                + String.format(
                    "Do not mock '%s' (which is-a '%s'); %s",
                    "lib.DoNotMockObjects.ImplementsMetaDoNotMockInterfaceObject",
                    "lib.DoNotMockObjects.MetaDoNotMockInterface",
                    String.format(
                        "%s is annotated as @%s (which is annotated as @DoNotMock): %s",
                        "lib.DoNotMockObjects.MetaDoNotMockInterface",
                        "lib.DoNotMockObjects.MetaDoNotMock",
                        DO_NOT_MOCK_REASON)),
            "  @Mock ImplementsMetaDoNotMockInterfaceObject"
                + " implementsMetaDoNotMockInterfaceObject;",
            "  @Mock MockableObject mockableObject;",
            "}")
        .doTest();
  }

  @Test
  public void matchesMockAnnotation_metaDoNotMock() {
    testHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "import org.mockito.Mock;",
            "import lib.DoNotMockObjects.*;",
            "class Lib {",
            "",
            "  // BUG: Diagnostic contains:",
            "  @Mock MetaDoNotMockObject metaAnnotatedDoNotMockObject;",
            "  // BUG: Diagnostic contains:",
            "  @Mock MetaDoNotMockInterface metaDoNotMockInterface;",
            "  @Mock MockableObject mockableObject;",
            "  @Mock DoubleMetaAnnotatedDoNotMock doubleMetaAnnotatedDoNotMock; // mockable",
            "}")
        .doTest();
  }

  @Test
  public void matchesMockitoDotMock_autoValue() {
    testHelper
        .addSourceLines(
            "lib/Lib.java",
            "package lib;",
            "import org.mockito.Mockito;",
            "import lib.AutoValueObjects.*;",
            "public class Lib {",
            "",
            "  class MockableObject {}",
            "",
            "  public static void f() { ",
            "    Mockito.mock(MockableObject.class);",
            "    // BUG: Diagnostic contains:",
            "    Mockito.mock(DoNotMockMyAutoValue.class);",
            "    Mockito.mock(MyAutoValue.class);",
            "    MyAutoValue myAutoValue = MyAutoValue.create(1);",
            "    DoNotMockMyAutoValue doNotMockMyAutoValue = DoNotMockMyAutoValue.create(1);",
            "  }",
            "}")
        .addSourceLines(
            "lib/MyAutoValue.java",
            "package lib;",
            "import com.google.auto.value.AutoValue;",
            "import com.google.errorprone.annotations.DoNotMock;",
            "class AutoValueObjects {",
            "  @DoNotMock(\"" + DO_NOT_MOCK_REASON + "\")",
            "  @AutoValue public abstract static class DoNotMockMyAutoValue {",
            "    public abstract int getFoo();",
            "    static DoNotMockMyAutoValue create(int foo) {",
            "      return new AutoValue_AutoValueObjects_DoNotMockMyAutoValue(foo);",
            "    }",
            "  }",
            "  @AutoValue public abstract static class MyAutoValue {",
            "    public abstract int getBar();",
            "    static MyAutoValue create(int bar) {",
            "      return new AutoValue_AutoValueObjects_MyAutoValue(bar);",
            "    }",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-processor", AutoValueProcessor.class.getName()))
        .doTest();
  }

  @Test
  public void typaram() {
    CompilationTestHelper.newInstance(DoNotMockChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "import org.mockito.Mockito;",
            "import org.mockito.Mock;",
            "class Test<E> {",
            "  @Mock E e;",
            "  <T> T f(T x) {",
            "    T m = Mockito.spy(x);",
            "    return m;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void rawClass() {
    CompilationTestHelper.newInstance(DoNotMockChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "import org.mockito.Mockito;",
            "class Test {",
            "  <T> T evil(Class<T> clazz) {",
            "    return (T) Mockito.mock((Class) clazz);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mockArray() {
    CompilationTestHelper.newInstance(DoNotMockChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock Annotation[] annotations;",
            "}")
        .doTest();
  }

  /** Example meta-annotation to put on the test's classpath. */
  @Retention(RUNTIME)
  @DoNotMock("unmockable")
  public @interface AnnotationWithDoNotMock {}

  /** An example usage of this meta-annotation. */
  @AnnotationWithDoNotMock
  public static class AnnotatedClass {}

  // test the check's behaviour if a mocked type is annotated, but the annotation's classfile
  // is missing from the compilation classpath
  @Test
  public void noMetaAnnotationIncompleteClasspath() {
    CompilationTestHelper.newInstance(DoNotMockChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "import " + AnnotatedClass.class.getCanonicalName() + ";",
            "import org.mockito.Mock;",
            "class Test {",
            "  @Mock AnnotatedClass x;",
            "}")
        .withClasspath(
            AnnotatedClass.class, DoNotMockCheckerTest.class, DoNotMock.class, Mock.class)
        .doTest();
  }
}
