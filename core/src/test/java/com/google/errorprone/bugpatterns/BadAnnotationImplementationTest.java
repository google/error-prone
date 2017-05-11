/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link BadAnnotationImplementation}. */
@RunWith(JUnit4.class)
public class BadAnnotationImplementationTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(BadAnnotationImplementation.class, getClass());
  }

  @Test
  public void declaredClassImplementsAnnotation() {
    compilationHelper
        .addSourceLines(
            "TestAnnotation.java",
            "import java.lang.annotation.Annotation;",
            "// BUG: Diagnostic contains:",
            "public class TestAnnotation implements Annotation {",
            "  @Override public Class<? extends Annotation> annotationType() {",
            "    return TestAnnotation.class;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void anonymousClassImplementsAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "public class Test {",
            "  public Annotation getAnnotation() {",
            "    // BUG: Diagnostic contains:",
            "    return new Annotation() {",
            "      @Override public Class<? extends Annotation> annotationType() {",
            "        return Annotation.class;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void anonymousClassImplementsUserDefinedAnnotation() {
    compilationHelper
        .addSourceLines("MyAnnotation.java", "public @interface MyAnnotation {}")
        .addSourceLines(
            "AnonymousClass.java",
            "import java.lang.annotation.Annotation;",
            "public class AnonymousClass {",
            "  public Annotation getAnnotation() {",
            "    // BUG: Diagnostic contains:",
            "    return new MyAnnotation() {",
            "      @Override public Class<? extends Annotation> annotationType() {",
            "        return Annotation.class;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void overridesEqualsButNotHashCode() {
    compilationHelper
        .addSourceLines(
            "TestAnnotation.java",
            "import java.lang.annotation.Annotation;",
            "// BUG: Diagnostic contains:",
            "public class TestAnnotation implements Annotation {",
            "  @Override public Class<? extends Annotation> annotationType() {",
            "    return TestAnnotation.class;",
            "  }",
            "  @Override public boolean equals(Object other) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void overridesHashCodeButNotEquals() {
    compilationHelper
        .addSourceLines(
            "TestAnnotation.java",
            "import java.lang.annotation.Annotation;",
            "// BUG: Diagnostic contains:",
            "public class TestAnnotation implements Annotation {",
            "  @Override public Class<? extends Annotation> annotationType() {",
            "    return TestAnnotation.class;",
            "  }",
            "  @Override public int hashCode() {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void wrongEquals() {
    compilationHelper
        .addSourceLines(
            "TestAnnotation.java",
            "import java.lang.annotation.Annotation;",
            "// BUG: Diagnostic contains:",
            "public class TestAnnotation implements Annotation {",
            "  @Override public Class<? extends Annotation> annotationType() {",
            "    return TestAnnotation.class;",
            "  }",
            "  public boolean equals(TestAnnotation other) {",
            "    return false;",
            "  }",
            "  @Override public int hashCode() {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void wrongHashCode() {
    compilationHelper
        .addSourceLines(
            "TestAnnotation.java",
            "import java.lang.annotation.Annotation;",
            "// BUG: Diagnostic contains:",
            "public class TestAnnotation implements Annotation {",
            "  @Override public Class<? extends Annotation> annotationType() {",
            "    return TestAnnotation.class;",
            "  }",
            "  @Override public boolean equals(Object other) {",
            "    return false;",
            "  }",
            "  public int hashCode(Object obj) {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void overridesEqualsAndHashCode() {
    compilationHelper
        .addSourceLines(
            "TestAnnotation.java",
            "import java.lang.annotation.Annotation;",
            "public class TestAnnotation implements Annotation {",
            "  @Override public Class<? extends Annotation> annotationType() {",
            "    return TestAnnotation.class;",
            "  }",
            "  @Override public boolean equals(Object other) {",
            "    return false;",
            "  }",
            "  @Override public int hashCode() {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void declareInterfaceThatExtendsAnnotation() {
    compilationHelper
        .addSourceLines(
            "TestAnnotation.java",
            "import java.lang.annotation.Annotation;",
            "public interface TestAnnotation extends Annotation {}")
        .doTest();
  }

  @Test
  public void declareEnumThatImplementsAnnotation() {
    compilationHelper
        .addSourceLines(
            "TestEnum.java",
            "import java.lang.annotation.Annotation;",
            "// BUG: Diagnostic contains: Enums cannot correctly implement Annotation",
            "public enum TestEnum implements Annotation {",
            "  VALUE_1,",
            "  VALUE_2;",
            "  @Override public Class<? extends Annotation> annotationType() {",
            "    return TestEnum.class;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void extendsClassThatImplementsEqualsAndHashCode() {
    compilationHelper
        .addSourceLines(
            "BaseAnnotation.java",
            "import java.lang.annotation.Annotation;",
            "public class BaseAnnotation implements Annotation {",
            "  @Override public Class<? extends Annotation> annotationType() {",
            "    return Annotation.class;",
            "  }",
            "  @Override public boolean equals(Object other) {",
            "    return false;",
            "  }",
            "  @Override public int hashCode() {",
            "    return 0;",
            "  }",
            "}")
        .addSourceLines(
            "MyAnnotation.java",
            "import java.lang.annotation.Annotation;",
            "public class MyAnnotation extends BaseAnnotation {}")
        .doTest();
  }
}
