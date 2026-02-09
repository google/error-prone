/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@RunWith(JUnit4.class)
public class MissingRuntimeRetentionTest {

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(MissingRuntimeRetention.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MissingRuntimeRetention.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "MissingRuntimeRetentionPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import static java.lang.annotation.ElementType.METHOD;
            import static java.lang.annotation.ElementType.TYPE;
            import static java.lang.annotation.RetentionPolicy.SOURCE;

            import com.google.inject.BindingAnnotation;
            import com.google.inject.ScopeAnnotation;
            import java.lang.annotation.Retention;
            import java.lang.annotation.Target;
            import javax.inject.Qualifier;
            import javax.inject.Scope;

            /**
             * @author sgoldfeder@google.com (Steven Goldfeder)
             */
            public class MissingRuntimeRetentionPositiveCases {
              /** A scoping (@Scope) annotation with SOURCE retention */
              @Scope
              @Target({TYPE, METHOD})
              // BUG: Diagnostic contains: @Retention(RUNTIME)
              @Retention(SOURCE)
              public @interface TestAnnotation1 {}

              /** A scoping (@ScopingAnnotation) annotation with SOURCE retention. */
              @ScopeAnnotation
              @Target({TYPE, METHOD})
              // BUG: Diagnostic contains: @Retention(RUNTIME)
              @Retention(SOURCE)
              public @interface TestAnnotation2 {}

              /** A qualifier (@Qualifier) annotation with SOURCE retention. */
              @Qualifier
              @Target({TYPE, METHOD})
              // BUG: Diagnostic contains: @Retention(RUNTIME)
              @Retention(SOURCE)
              public @interface TestAnnotation3 {}

              /** A qualifier (@BindingAnnotation) annotation with SOURCE retention. */
              @BindingAnnotation
              @Target({TYPE, METHOD})
              // BUG: Diagnostic contains: @Retention(RUNTIME)
              @Retention(SOURCE)
              public @interface TestAnnotation4 {}

              /** A qualifier annotation with default retention. */
              @BindingAnnotation
              @Target({TYPE, METHOD})
              // BUG: Diagnostic contains: @Retention(RUNTIME)
              public @interface TestAnnotation5 {}

              /** A dagger map key annotation with default retention. */
              @dagger.MapKey
              @Target({TYPE, METHOD})
              // BUG: Diagnostic contains: @Retention(RUNTIME)
              public @interface TestAnnotation6 {}

              /** A Guice map key annotation with default retention. */
              @com.google.inject.multibindings.MapKey
              @Target({TYPE, METHOD})
              // BUG: Diagnostic contains: @Retention(RUNTIME)
              public @interface TestAnnotation7 {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "MissingRuntimeRetentionNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import static java.lang.annotation.ElementType.METHOD;
            import static java.lang.annotation.ElementType.TYPE;
            import static java.lang.annotation.RetentionPolicy.RUNTIME;
            import static java.lang.annotation.RetentionPolicy.SOURCE;

            import com.google.inject.BindingAnnotation;
            import com.google.inject.ScopeAnnotation;
            import java.lang.annotation.Retention;
            import java.lang.annotation.Target;
            import javax.inject.Qualifier;
            import javax.inject.Scope;

            /**
             * @author sgoldfeder@google.com (Steven Goldfeder)
             */
            public class MissingRuntimeRetentionNegativeCases {
              /** A scoping (@Scope) annotation with runtime retention */
              @Scope
              @Target({TYPE, METHOD})
              @Retention(RUNTIME)
              public @interface TestAnnotation1 {}

              /** A scoping (@ScopingAnnotation) annotation with runtime retention. */
              @ScopeAnnotation
              @Target({TYPE, METHOD})
              @Retention(RUNTIME)
              public @interface TestAnnotation2 {}

              /** A qualifier (@Qualifier) annotation with runtime retention. */
              @Qualifier
              @Target({TYPE, METHOD})
              @Retention(RUNTIME)
              public @interface TestAnnotation3 {}

              /** A qualifier (@BindingAnnotation) annotation with runtime retention. */
              @BindingAnnotation
              @Target({TYPE, METHOD})
              @Retention(RUNTIME)
              public @interface TestAnnotation4 {}

              /** A non-qualifier, non-scoping annotation without runtime retention. */
              @Retention(SOURCE)
              public @interface TestAnnotation5 {}

              /** A dagger map key annotation. */
              @dagger.MapKey
              @Retention(RUNTIME)
              public @interface TestAnnotation6 {}

              /** A Guice map key annotation. */
              @com.google.inject.multibindings.MapKey
              @Retention(RUNTIME)
              public @interface TestAnnotation7 {}
            }
            """)
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringTestHelper
        .addInputLines(
            "in/Anno.java",
            """
            import static java.lang.annotation.ElementType.METHOD;
            import static java.lang.annotation.ElementType.TYPE;

            import java.lang.annotation.Target;
            import javax.inject.Qualifier;

            @Qualifier
            @Target({TYPE, METHOD})
            public @interface Anno {}
            """)
        .addOutputLines(
            "out/Anno.java",
            """
            import static java.lang.annotation.ElementType.METHOD;
            import static java.lang.annotation.ElementType.TYPE;
            import static java.lang.annotation.RetentionPolicy.RUNTIME;

            import java.lang.annotation.Retention;
            import java.lang.annotation.Target;
            import javax.inject.Qualifier;

            @Qualifier
            @Target({TYPE, METHOD})
            @Retention(RUNTIME)
            public @interface Anno {}
            """)
        .doTest();
  }

  @Test
  public void nestedQualifierInDaggerModule() {
    compilationHelper
        .addSourceLines(
            "DaggerModule.java",
            """
            @dagger.Module
            class DaggerModule {
              @javax.inject.Scope
              public @interface TestAnnotation {}
            }
            """)
        .doTest();
  }

  @Test
  public void ignoredOnAndroid() {
    compilationHelper
        .setArgs(Collections.singletonList("-XDandroidCompatible=true"))
        .addSourceLines(
            "TestAnnotation.java",
            """
            @javax.inject.Scope
            public @interface TestAnnotation {}
            """)
        .doTest();
  }

  @Test
  public void sourceRetentionStillFiringOnAndroid() {
    compilationHelper
        .setArgs(Collections.singletonList("-XDandroidCompatible=true"))
        .addSourceLines(
            "TestAnnotation.java",
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @javax.inject.Scope
            // BUG: Diagnostic contains: @Retention(RUNTIME)
            @Retention(RetentionPolicy.SOURCE)
            public @interface TestAnnotation {}
            """)
        .doTest();
  }
}
