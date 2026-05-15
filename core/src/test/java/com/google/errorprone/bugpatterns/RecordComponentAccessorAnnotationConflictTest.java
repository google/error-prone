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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RecordComponentAccessorAnnotationConflictTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(
          RecordComponentAccessorAnnotationConflict.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          RecordComponentAccessorAnnotationConflict.class, getClass());

  @Test
  public void recordWithExplicitAccessorMissingAnnotation_warns() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target(ElementType.METHOD)
            @interface MyAnnotation {}

            record Foo(@MyAnnotation int x) {
              // BUG: Diagnostic contains: RecordComponentAccessorAnnotationConflict
              public int x() {
                return this.x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void recordWithExplicitAccessorMissingAnnotation_refactor() {
    refactoringHelper
        .addInputLines(
            "MyAnnotation.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target(ElementType.METHOD)
            @interface MyAnnotation {
              int value();
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Foo.java",
            """
            record Foo(@MyAnnotation(42) int x) {
              public int x() {
                return this.x;
              }
            }
            """)
        .addOutputLines(
            "Foo.java",
            """
            record Foo(@MyAnnotation(42) int x) {
              @MyAnnotation(42)
              public int x() {
                return this.x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void recordWithExplicitAccessorRepeatingAnnotation_noWarning() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target({ElementType.METHOD, ElementType.RECORD_COMPONENT})
            @interface MyAnnotation {}

            record Foo(@MyAnnotation int x) {
              @MyAnnotation
              public int x() {
                return this.x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void recordWithGeneratedAccessor_noWarning() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target({ElementType.METHOD, ElementType.RECORD_COMPONENT})
            @interface MyAnnotation {}

            record Foo(@MyAnnotation int x) {}
            """)
        .doTest();
  }

  @Test
  public void recordWithGeneratedAccessor_defaultTarget_noWarning() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import javax.annotation.Nullable;

            record Foo(@Nullable int x) {}
            """)
        .doTest();
  }

  @Test
  public void recordWithExplicitConstructor() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target(ElementType.PARAMETER)
            @interface MyAnnotation {}

            record Foo(@MyAnnotation int x) {
              // BUG: Diagnostic contains: RecordComponentAccessorAnnotationConflict
              Foo(int x) {
                this.x = x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void recordWithImplicitConstructor() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target(ElementType.PARAMETER)
            @interface MyAnnotation {}

            record Foo(@MyAnnotation int x) {}
            """)
        .doTest();
  }

  @Test
  public void recordWithCompactConstrucutor() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target(ElementType.PARAMETER)
            @interface MyAnnotation {}

            record Foo(@MyAnnotation int x) {
              Foo {
                x = x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void recordWithDualAnnotation_explicitAccessor() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target({ElementType.METHOD, ElementType.PARAMETER})
            @interface MyAnnotation {}

            record Foo(@MyAnnotation int x) {
              public int x() {
                return this.x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void recordWithDualAnnotation_explicitConstructor() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target({ElementType.METHOD, ElementType.PARAMETER})
            @interface MyAnnotation {}

            record Foo(@MyAnnotation int x) {
              Foo {
                x = x;
              }
            }
            """)
        .doTest();
  }
}
