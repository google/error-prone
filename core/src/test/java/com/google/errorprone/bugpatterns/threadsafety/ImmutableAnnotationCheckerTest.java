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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.errorprone.CompilationTestHelper;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ImmutableAnnotationChecker}Test */
@RunWith(JUnit4.class)
public class ImmutableAnnotationCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ImmutableAnnotationChecker.class, getClass());

  @Test
  public void nonFinalField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "class Test implements Deprecated {",
            "  public Class<? extends Annotation> annotationType() { return Deprecated.class; }",
            "  // BUG: Diagnostic contains: final int x;'",
            "  int x;",
            "  private Test(int x) {",
            "    this.x = x;",
            "  }",
            "  public boolean forRemoval() {",
            "    return false;",
            "  }",
            "  public String since() {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import com.google.common.collect.ImmutableSet;",
            "class Test implements Deprecated {",
            "  public Class<? extends Annotation> annotationType() { return Deprecated.class; }",
            "  final Annotation annotation;",
            "  private Test(Annotation annotation) {",
            "    this.annotation = annotation;",
            "  }",
            "  public boolean forRemoval() {",
            "    return false;",
            "  }",
            "  public String since() {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalMutableField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import java.util.Arrays;",
            "import java.util.HashSet;",
            "import java.util.Set;",
            "class Test implements Deprecated {",
            "  public Class<? extends Annotation> annotationType() { return Deprecated.class; }",
            "  // BUG: Diagnostic contains: annotations should be immutable: 'Test' has field 'xs'"
                + " of type 'java.util.Set<java.lang.Integer>', 'Set' is mutable",
            "  final Set<Integer> xs;",
            "  private Test(Integer... xs) {",
            "    this.xs = new HashSet<>(Arrays.asList(xs));",
            "  }",
            "  public boolean forRemoval() {",
            "    return false;",
            "  }",
            "  public String since() {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotated() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import com.google.errorprone.annotations.Immutable;",
            "// BUG: Diagnostic contains: annotations are immutable by default",
            "@Immutable",
            "class Test implements Deprecated {",
            "  public Class<? extends Annotation> annotationType() { return Deprecated.class; }",
            "  public boolean forRemoval() {",
            "    return false;",
            "  }",
            "  public String since() {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mutableFieldType() {
    compilationHelper
        .addSourceLines("Foo.java", "class Foo {", "}")
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import java.util.Arrays;",
            "import java.util.HashSet;",
            "import java.util.Set;",
            "class Test implements Deprecated {",
            "  public Class<? extends Annotation> annotationType() { return Deprecated.class; }",
            "  // BUG: Diagnostic contains:"
                + " the declaration of type 'Foo' is not annotated with"
                + " @com.google.errorprone.annotations.Immutable",
            "  final Foo f;",
            "  private Test(Foo f) {",
            "    this.f = f;",
            "  }",
            "  public boolean forRemoval() {",
            "    return false;",
            "  }",
            "  public String since() {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void anonymous() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {{",
            "  new Deprecated() {",
            "    public Class<? extends Annotation> annotationType() { return Deprecated.class; }",
            "    public boolean forRemoval() {",
            "      return false;",
            "    }",
            "    public String since() {",
            "      return \"\";",
            "    }",
            "  };",
            "}}")
        .doTest();
  }

  @Test
  public void anonymousReferencesEnclosing() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import java.util.Objects;",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {{",
            "  // BUG: Diagnostic contains: 'Deprecated' has mutable enclosing instance",
            "  new Deprecated() {",
            "    {",
            "      Objects.requireNonNull(Test.this);",
            "    }",
            "    public Class<? extends Annotation> annotationType() { return Deprecated.class; }",
            "    public boolean forRemoval() {",
            "      return false;",
            "    }",
            "    public String since() {",
            "      return \"\";",
            "    }",
            "  };",
            "}}")
        .doTest();
  }

  @Test
  public void anonymousReferencesEnum() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import java.util.Objects;",
            "import com.google.common.collect.ImmutableSet;",
            "enum Test {",
            "  ;",
            "  {",
            "    new Deprecated() {",
            "      {",
            "        Objects.requireNonNull(Test.this);",
            "      }",
            "      public Class<? extends Annotation> annotationType() {",
            "        return Deprecated.class;",
            "      }",
            "      public boolean forRemoval() {",
            "        return false;",
            "      }",
            "      public String since() {",
            "        return \"\";",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void anonymousCapturesLocal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import java.util.Objects;",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {",
            "  int x;", // Test is mutable
            "  void f(int y) {",
            "    new Deprecated() {",
            "      void g() {",
            "        System.err.println(y);", // capture a local (but not the enclosing instance)
            "      }",
            "      public Class<? extends Annotation> annotationType() {",
            "        return Deprecated.class;",
            "      }",
            "      public boolean forRemoval() {",
            "        return false;",
            "      }",
            "      public String since() {",
            "        return \"\";",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void otherAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import java.util.Objects;",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {",
            "  @SuppressWarnings(\"Immutable\")",
            "  class A implements Annotation {",
            "    private int i = 0;",
            "    public int count() {",
            "      return i++;",
            "    }",
            "    public Class<? extends Annotation> annotationType() {",
            "      return Deprecated.class;",
            "    }",
            "  }",
            "  class B implements Annotation {",
            "    final A a = null;",
            "    public Class<? extends Annotation> annotationType() {",
            "      return Deprecated.class;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotationSuper() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            "import java.util.Objects;",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {",
            "  class MyAnno implements Annotation {",
            "    public Class<? extends Annotation> annotationType() {",
            "      return Deprecated.class;",
            "    }",
            "  }",
            "  {",
            "    new MyAnno() {",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void jucImmutable() {
    compilationHelper
        .addSourceLines(
            "Lib.java", //
            "import javax.annotation.concurrent.Immutable;",
            "@Immutable",
            "class Lib {",
            "}")
        .addSourceLines(
            "Test.java", //
            "import java.lang.annotation.Annotation;",
            "class MyAnno implements Annotation {",
            "  // BUG: Diagnostic contains:"
                + " not annotated with @com.google.errorprone.annotations.Immutable",
            "  final Lib l = new Lib();",
            "  public Class<? extends Annotation> annotationType() {",
            "    return Deprecated.class;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void generated() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.annotation.Annotation;",
            (isJdk8OrEarlier()
                ? "import javax.annotation.Generated;"
                : "import javax.annotation.processing.Generated;"),
            "@Generated(\"com.google.auto.value.processor.AutoAnnotationProcessor\")",
            "class Test implements Deprecated {",
            "  public Class<? extends Annotation> annotationType() { return Deprecated.class; }",
            "  int x;",
            "  private Test(int x) {",
            "    this.x = x;",
            "  }",
            "  public boolean forRemoval() {",
            "    return false;",
            "  }",
            "  public String since() {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  static boolean isJdk8OrEarlier() {
    try {
      Method versionMethod = Runtime.class.getMethod("version");
      Object version = versionMethod.invoke(null);
      int majorVersion = (int) version.getClass().getMethod("major").invoke(version);
      return majorVersion <= 8;
    } catch (ReflectiveOperationException e) {
      return true;
    }
  }
}
