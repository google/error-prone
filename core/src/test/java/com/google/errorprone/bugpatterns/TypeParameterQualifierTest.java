/*
 * Copyright 2015 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link TypeParameterQualifier}Test */
@RunWith(JUnit4.class)
public class TypeParameterQualifierTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(TypeParameterQualifier.class, getClass());

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "Foo.java",
            """
            class Foo {
              static class Builder {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              static <T extends Foo> T populate(T.Builder builder) {
                return null;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              static <T extends Foo> T populate(Foo.Builder builder) {
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveMethod() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              static <T extends Enum<T>> T get(Class<T> clazz, String value) {
                return T.valueOf(clazz, value);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              static <T extends Enum<T>> T get(Class<T> clazz, String value) {
                return Enum.valueOf(clazz, value);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void instanceMethodReference() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Function;

            class Test {
              static <T extends Enum<T>> void get() {
                Function<T, String> f = T::name;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Function;

            class Test {
              static <T extends Enum<T>> void get() {
                Function<T, String> f = Enum::name;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReference() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              static class Foo {
                static void bar() {}
              }

              static <T extends Foo> void get() {
                Runnable r = T::bar;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              static class Foo {
                static void bar() {}
              }

              static <T extends Foo> void get() {
                Runnable r = Foo::bar;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReference_flagDisabled() {
    refactoringHelper
        .setArgs("-XepOpt:TypeParameterQualifier:MatchMethodReferences=false")
        .addInputLines(
            "Test.java",
            """
            class Test {
              static class Foo {
                static void bar() {}
              }

              static <T extends Foo> void get() {
                Runnable r = T::bar;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void methodReference_inBeforeTemplate() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.refaster.annotation.BeforeTemplate;
            import java.util.function.Function;

            class Test {
              @BeforeTemplate
              <T extends Enum<T>> Function<T, String> rule() {
                return T::name;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void memberSelect_inBeforeTemplate() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.refaster.annotation.BeforeTemplate;

            class Test {
              @BeforeTemplate
              <T extends Enum<T>> T rule(Class<T> clazz, String value) {
                return T.valueOf(clazz, value);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.refaster.annotation.BeforeTemplate;

            class Test {
              @BeforeTemplate
              <T extends Enum<T>> T rule(Class<T> clazz, String value) {
                return Enum.valueOf(clazz, value);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    refactoringHelper
        .addInputLines(
            "Foo.java",
            """
            class Foo {
              static class Builder {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              static <T extends Foo> T populate(T builder) {
                return null;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
