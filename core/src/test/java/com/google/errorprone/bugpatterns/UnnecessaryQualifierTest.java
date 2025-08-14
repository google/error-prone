/*
 * Copyright 2025 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public final class UnnecessaryQualifierTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnnecessaryQualifier.class, getClass())
          .addSourceLines(
              "Qual.java",
              """
              import static java.lang.annotation.RetentionPolicy.RUNTIME;

              import java.lang.annotation.Retention;
              import javax.inject.Qualifier;

              @Qualifier
              @Retention(RUNTIME)
              public @interface Qual {}
              """)
          .addSourceLines(
              "ProvidesSomething.java",
              """
              import static java.lang.annotation.RetentionPolicy.RUNTIME;

              import java.lang.annotation.Retention;
              import javax.inject.Qualifier;

              @Qualifier
              @Retention(RUNTIME)
              public @interface ProvidesSomething {}
              """);

  @Test
  public void unannotatedField() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains:
              @Qual int x;
            }
            """)
        .doTest();
  }

  @Test
  public void unannotatedLocal() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void foo() {
                // BUG: Diagnostic contains:
                @Qual int x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void parameterOnNonInjectionPointMethod() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void foo(
                  // BUG: Diagnostic contains:
                  @Qual int x) {}
            }
            """)
        .doTest();
  }

  @Test
  public void methodReturnType_notProvider_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains:
              @Qual
              int foo() {
                return 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReturnType_provider_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.inject.Provides;

            class Test {
              @Provides
              @Qual
              int foo() {
                return 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void customProvidesMethod_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              @ProvidesSomething
              @Qual
              int foo() {
                return 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void unannotatedConstructor_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains:
              Test(@Qual int x) {}
            }
            """)
        .doTest();
  }

  @Test
  public void injectedField_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import javax.inject.Inject;

            class Test {
              @Inject @Qual int x;
            }
            """)
        .doTest();
  }

  @Test
  public void exemptedClassAnnotation_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import dagger.Component;

            @Component.Builder
            interface Builder {
              Builder setName(@Qual String name);

              String build();
            }
            """)
        .doTest();
  }

  @Test
  public void exemptedClassAnnotation_subcomponentFactory_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import dagger.BindsInstance;
            import dagger.Subcomponent;

            @Subcomponent.Factory
            interface Factory {
              Object create(@BindsInstance @Qual boolean isEnabled);
            }
            """)
        .doTest();
  }

  @Test
  public void lambdas_neverMeaningful() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.function.Function;

            interface Test {
              // BUG: Diagnostic contains:
              Function<Integer, Integer> F = (@Qual Integer a) -> a;
            }
            """)
        .doTest();
  }

  @Test
  public void interface_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            interface Test {
              @Qual
              Object frobnicator();
            }
            """)
        .doTest();
  }

  @Test
  public void recordWithExplicitlyAnnotatedConstructor_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import javax.inject.Inject;

            record Test(@Qual int x) {
              @Inject
              Test {}
            }
            """)
        .doTest();
  }

  @Test
  public void recordWithoutExplicitlyAnnotatedConstructor_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import javax.inject.Inject;

            // BUG: Diagnostic contains:
            record Test(@Qual int x) {
              Test {}
            }
            """)
        .doTest();
  }

  @Test
  public void jukitoTestRunner_noFinding() {
    helper
        .addSourceLines(
            "org/jukito/All.java",
            """
            package org.jukito;

            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import javax.inject.Qualifier;

            @Retention(RetentionPolicy.RUNTIME)
            @Qualifier
            public @interface All {}
            """)
        .addSourceLines(
            "org/jukito/JukitoRunner.java",
            """
            package org.jukito;

            import org.junit.runner.Runner;

            public abstract class JukitoRunner extends Runner {}
            """)
        .addSourceLines(
            "SubRunner.java",
            """
            import org.jukito.JukitoRunner;

            public abstract class SubRunner extends JukitoRunner {}
            """)
        .addSourceLines(
            "JukitoTest.java",
            """
            import org.jukito.All;
            import org.jukito.JukitoRunner;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(JukitoRunner.class)
            public class JukitoTest {
              @Test
              void test(@All int x) {}
            }
            """)
        .addSourceLines(
            "SubrunnerTest.java",
            """
            import org.jukito.All;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(SubRunner.class)
            public class SubrunnerTest {
              @Test
              void test(@All int x) {}
            }
            """)
        .doTest();
  }
}
