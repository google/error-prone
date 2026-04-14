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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link ExposedPrivateType} bug pattern. */
@RunWith(JUnit4.class)
public final class ExposedPrivateTypeTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ExposedPrivateType.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(ExposedPrivateType.class, getClass());

  @Test
  public void publicFieldExposingPrivateType_fails() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateInner {}

              // BUG: Diagnostic contains: non-private member 'field' should not reference private classes: PrivateInner
              public PrivateInner field;
            }
            """)
        .doTest();
  }

  @Test
  public void publicMethodReturningPrivateType_fails() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateInner {}

              // BUG: Diagnostic contains: PrivateInner
              public PrivateInner method() {
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void publicMethodAcceptingPrivateType_fails() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateInner {}

              // BUG: Diagnostic contains: PrivateInner
              public void method(PrivateInner p) {}
            }
            """)
        .doTest();
  }

  @Test
  public void publicClassExtendingPrivateType_fails() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateInner {}

              // BUG: Diagnostic contains: PrivateInner
              public static class PublicInner extends PrivateInner {}
            }
            """)
        .doTest();
  }

  @Test
  public void typeParameterBoundUsingPrivateType_fails() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateInner {}

              // BUG: Diagnostic contains: PrivateInner
              public <T extends PrivateInner> void method() {}
            }
            """)
        .doTest();
  }

  @Test
  public void privateFieldUsingPrivateType_passes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateInner {}

              private PrivateInner field;
            }
            """)
        .doTest();
  }

  @Test
  public void publicFieldUsingPublicNestedType_passes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              public static class PublicInner {}

              public PublicInner field;
            }
            """)
        .doTest();
  }

  @Test
  public void effectivelyPrivateClass_noWarning() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateOuter {
                private static class PrivateInner {}

                public PrivateInner field; // Fine, because PrivateOuter is private.
              }
            }
            """)
        .doTest();
  }

  @Test
  public void reduceVisibility() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateInner {}

              public PrivateInner field;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateInner {}

              private PrivateInner field;
            }
            """)
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void increaseVisibility() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateInner {}

              public PrivateInner field;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Outer {
              public static class PrivateInner {}

              public PrivateInner field;
            }
            """)
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void publicMethodUsingPrivateAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              private @interface PrivateAnnotation {}

              @PrivateAnnotation
              public void method() {}
            }
            """)
        .doTest();
  }

  @Test
  public void publicMethodUsingPrivateAnnotationAsClassLiteral() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            class Outer {
              private static class PrivateInner {}

              @Retention(RetentionPolicy.RUNTIME)
              public @interface PublicAnnotation {
                Class<?> value();
              }

              @PublicAnnotation(PrivateInner.class)
              public void method() {}
            }
            """)
        .doTest();
  }

  @Test
  public void publicMethodUsingPrivateAnnotationAsEnumConstant() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            class Outer {
              private enum PrivateEnum {
                A
              }

              @Retention(RetentionPolicy.RUNTIME)
              public @interface PublicAnnotation {
                // BUG: Diagnostic contains: PrivateEnum
                PrivateEnum value();
              }

              @PublicAnnotation(PrivateEnum.A)
              public void method() {}
            }
            """)
        .doTest();
  }

  @Test
  public void publicMethodUsingPrivateAnnotationAsNestedAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            class Outer {
              private @interface PrivateAnnotation {}

              @Retention(RetentionPolicy.RUNTIME)
              public @interface PublicAnnotation {
                // BUG: Diagnostic contains: PrivateAnnotation
                PrivateAnnotation value();
              }

              @PublicAnnotation(@PrivateAnnotation)
              public void method() {}
            }
            """)
        .doTest();
  }

  @Test
  public void dontReduceVisibilityOverride() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Function;

            class Outer implements Function<Object, Object> {
              private static class PrivateInner {}

              @Override
              public PrivateInner apply(Object o) {
                return null;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Function;

            class Outer implements Function<Object, Object> {
              public static class PrivateInner {}

              @Override
              public PrivateInner apply(Object o) {
                return null;
              }
            }
            """)
        .setFixChooser(FixChoosers.FIRST)
        .doTest();
  }

  @Test
  public void privateAnnotationOnParameter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              private @interface PrivateAnnotation {}

              interface I {
                void method(@PrivateAnnotation Object o);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void injectAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.inject.Inject;

            class Outer {
              private static class PrivateInner {}

              @Inject
              public Outer(PrivateInner inner) {}
            }
            """)
        .doTest();
  }

  @Test
  public void packageVisibility() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Outer {
              private static class PrivateInner {}

              // BUG: Diagnostic contains: PrivateInner
              void f(PrivateInner inner) {}
            }
            """)
        .doTest();
  }
}
