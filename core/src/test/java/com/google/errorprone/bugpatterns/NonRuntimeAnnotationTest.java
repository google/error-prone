/*
 * Copyright 2012 The Error Prone Authors.
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
 * @author scottjohnson@google.com (Scott Johnson)
 */
@RunWith(JUnit4.class)
public class NonRuntimeAnnotationTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NonRuntimeAnnotation.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "NonRuntimeAnnotationPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author scottjohnson@google.com (Scott Johnsson)
 */
@NonRuntimeAnnotationPositiveCases.NotSpecified
@NonRuntimeAnnotationPositiveCases.NonRuntime
public class NonRuntimeAnnotationPositiveCases {

  public NonRuntime testAnnotation() {
    // BUG: Diagnostic contains: runtime; NonRuntime
    NonRuntimeAnnotationPositiveCases.class.getAnnotation(
        NonRuntimeAnnotationPositiveCases.NonRuntime.class);
    // BUG: Diagnostic contains:
    NonRuntimeAnnotationPositiveCases.class.getAnnotation(
        NonRuntimeAnnotationPositiveCases.NotSpecified.class);
    // BUG: Diagnostic contains:
    return this.getClass().getAnnotation(NonRuntimeAnnotationPositiveCases.NonRuntime.class);
  }

  /** Annotation that is explicitly NOT retained at runtime */
  @Retention(RetentionPolicy.SOURCE)
  public @interface NonRuntime {}

  /** Annotation that is implicitly NOT retained at runtime */
  public @interface NotSpecified {}
}
""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "NonRuntimeAnnotationNegativeCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author scottjohnson@google.com (Scott Johnsson)
 */
@NonRuntimeAnnotationNegativeCases.Runtime
public class NonRuntimeAnnotationNegativeCases {

  public Runtime testAnnotation() {
    return this.getClass().getAnnotation(NonRuntimeAnnotationNegativeCases.Runtime.class);
  }

  /** Annotation that is retained at runtime */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Runtime {}
}
""")
        .doTest();
  }

  @Test
  public void additionalMethods() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            class Test {
              @Retention(RetentionPolicy.SOURCE)
              @interface SourceRetention {}

              @Retention(RetentionPolicy.CLASS)
              @interface ClassRetention {}

              @Retention(RetentionPolicy.RUNTIME)
              @interface RuntimeRetention {}

              void test(Class<?> clazz) {
                // BUG: Diagnostic contains: Calling getAnnotation on an annotation that is not retained at
                // runtime; SourceRetention has SOURCE retention
                clazz.getAnnotation(SourceRetention.class);
                // BUG: Diagnostic contains:
                clazz.isAnnotationPresent(ClassRetention.class);
                // BUG: Diagnostic contains:
                clazz.getAnnotationsByType(SourceRetention.class);
                // BUG: Diagnostic contains:
                clazz.getDeclaredAnnotation(ClassRetention.class);
                // BUG: Diagnostic contains:
                clazz.getDeclaredAnnotationsByType(SourceRetention.class);

                clazz.getAnnotation(RuntimeRetention.class);
                clazz.isAnnotationPresent(RuntimeRetention.class);
                clazz.getAnnotationsByType(RuntimeRetention.class);
                clazz.getDeclaredAnnotation(RuntimeRetention.class);
                clazz.getDeclaredAnnotationsByType(RuntimeRetention.class);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void additionalReceivers() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.reflect.Invokable;
            import java.lang.annotation.Annotation;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.reflect.AnnotatedElement;
            import java.lang.reflect.Constructor;
            import java.lang.reflect.Field;
            import java.lang.reflect.Method;
            import java.lang.reflect.Parameter;
            import java.lang.reflect.RecordComponent;
            import javax.lang.model.element.Element;

            class Test {
              @Retention(RetentionPolicy.CLASS)
              @interface NonRuntime {}

              @Retention(RetentionPolicy.RUNTIME)
              @interface Runtime {}

              void test(
                  AnnotatedElement element,
                  Method method,
                  Field field,
                  Constructor<?> constructor,
                  Parameter parameter,
                  RecordComponent recordComponent,
                  Package pkg,
                  Module module,
                  Invokable<?, ?> invokable,
                  com.google.common.reflect.Parameter guavaParameter,
                  Element javacElement,
                  Class<? extends Annotation> unknownAnnotation) {
                // BUG: Diagnostic contains:
                element.getAnnotation(NonRuntime.class);
                // BUG: Diagnostic contains:
                method.isAnnotationPresent(NonRuntime.class);
                // BUG: Diagnostic contains:
                field.getAnnotationsByType(NonRuntime.class);
                // BUG: Diagnostic contains:
                constructor.getDeclaredAnnotation(NonRuntime.class);
                // BUG: Diagnostic contains:
                parameter.getDeclaredAnnotationsByType(NonRuntime.class);
                // BUG: Diagnostic contains:
                recordComponent.getAnnotation(NonRuntime.class);
                // BUG: Diagnostic contains:
                pkg.isAnnotationPresent(NonRuntime.class);
                // BUG: Diagnostic contains:
                module.getAnnotation(NonRuntime.class);
                // BUG: Diagnostic contains:
                invokable.isAnnotationPresent(NonRuntime.class);
                // BUG: Diagnostic contains:
                guavaParameter.getAnnotation(NonRuntime.class);

                element.getAnnotation(Runtime.class);
                method.isAnnotationPresent(Runtime.class);
                invokable.isAnnotationPresent(Runtime.class);
                guavaParameter.getAnnotation(Runtime.class);
                javacElement.getAnnotation(NonRuntime.class);
                element.isAnnotationPresent(unknownAnnotation);
              }

              <A extends Annotation> void genericTest(
                  AnnotatedElement element, Class<A> clazz, Class<? extends A> wildcardClazz) {
                element.getAnnotation(clazz);
                element.isAnnotationPresent(wildcardClazz);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void typeAnnotations() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.reflect.Invokable;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import java.lang.reflect.AnnotatedParameterizedType;
            import java.lang.reflect.AnnotatedType;
            import java.lang.reflect.Field;
            import java.lang.reflect.Method;
            import java.lang.reflect.TypeVariable;

            class Test {
              @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
              @Retention(RetentionPolicy.CLASS)
              @interface NonRuntimeTypeUse {}

              @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
              @Retention(RetentionPolicy.RUNTIME)
              @interface RuntimeTypeUse {}

              void test(
                  AnnotatedType annotatedType,
                  AnnotatedParameterizedType parameterizedType,
                  TypeVariable<?> typeVariable,
                  Field field,
                  Method method,
                  Invokable<?, ?> invokable,
                  com.google.common.reflect.Parameter guavaParameter) {
                // BUG: Diagnostic contains:
                annotatedType.getAnnotation(NonRuntimeTypeUse.class);
                // BUG: Diagnostic contains:
                annotatedType.isAnnotationPresent(NonRuntimeTypeUse.class);
                // BUG: Diagnostic contains:
                parameterizedType.getAnnotatedActualTypeArguments()[0].getAnnotationsByType(
                    NonRuntimeTypeUse.class);
                // BUG: Diagnostic contains:
                typeVariable.getDeclaredAnnotation(NonRuntimeTypeUse.class);
                // BUG: Diagnostic contains:
                typeVariable.getAnnotatedBounds()[0].getDeclaredAnnotationsByType(NonRuntimeTypeUse.class);
                // BUG: Diagnostic contains:
                field.getAnnotatedType().getAnnotation(NonRuntimeTypeUse.class);
                // BUG: Diagnostic contains:
                method.getAnnotatedReturnType().isAnnotationPresent(NonRuntimeTypeUse.class);
                // BUG: Diagnostic contains:
                invokable.getAnnotatedReturnType().getAnnotation(NonRuntimeTypeUse.class);
                // BUG: Diagnostic contains:
                guavaParameter.getAnnotatedType().isAnnotationPresent(NonRuntimeTypeUse.class);

                annotatedType.getAnnotation(RuntimeTypeUse.class);
                typeVariable.isAnnotationPresent(RuntimeTypeUse.class);
                field.getAnnotatedType().getAnnotation(RuntimeTypeUse.class);
                method.getAnnotatedReturnType().isAnnotationPresent(RuntimeTypeUse.class);
                invokable.getAnnotatedReturnType().getAnnotation(RuntimeTypeUse.class);
                guavaParameter.getAnnotatedType().isAnnotationPresent(RuntimeTypeUse.class);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void additionalApisDisabled() {
    compilationHelper
        .setArgs("-XepOpt:NonRuntimeAnnotation:AdditionalApis=false")
        .addSourceLines(
            "Test.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import java.lang.reflect.AnnotatedType;
            import java.lang.reflect.Method;

            class Test {
              @Target({ElementType.TYPE, ElementType.METHOD, ElementType.TYPE_USE})
              @Retention(RetentionPolicy.CLASS)
              @interface NonRuntime {}

              void test(Class<?> clazz, Method method, AnnotatedType annotatedType) {
                // BUG: Diagnostic contains:
                clazz.getAnnotation(NonRuntime.class);

                clazz.isAnnotationPresent(NonRuntime.class);
                clazz.getAnnotationsByType(NonRuntime.class);
                clazz.getDeclaredAnnotation(NonRuntime.class);
                clazz.getDeclaredAnnotationsByType(NonRuntime.class);
                method.getAnnotation(NonRuntime.class);
                annotatedType.getAnnotation(NonRuntime.class);
              }
            }
            """)
        .doTest();
  }
}
