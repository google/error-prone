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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link VoidMissingNullable}Test */
@RunWith(JUnit4.class)
public class VoidMissingNullableTest {
  private final CompilationTestHelper conservativeCompilationHelper =
      CompilationTestHelper.newInstance(VoidMissingNullable.class, getClass());
  private final CompilationTestHelper aggressiveCompilationHelper =
      CompilationTestHelper.newInstance(VoidMissingNullable.class, getClass())
          .setArgs("-XepOpt:Nullness:Conservative=false");
  private final BugCheckerRefactoringTestHelper aggressiveRefactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(VoidMissingNullable.class, getClass())
          .setArgs("-XepOpt:Nullness:Conservative=false");

  @Test
  public void positive() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: @Nullable
              Void v;

              // BUG: Diagnostic contains: @Nullable
              Void f() {
                return v;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void declarationAnnotatedLocation() {
    aggressiveRefactoringHelper
        .addInputLines(
            "in/Foo.java",
            """
            import javax.annotation.Nullable;

            abstract class Foo {
              @Nullable Object existingDeclarationUsage;

              java.lang.Void v;

              final Void f() {
                return v;
              }
            }
            """)
        .addOutputLines(
            "out/Foo.java",
            """
            import javax.annotation.Nullable;

            abstract class Foo {
              @Nullable Object existingDeclarationUsage;

              @Nullable java.lang.Void v;

              @Nullable
              final Void f() {
                return v;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void typeAnnotatedLocation() {
    aggressiveRefactoringHelper
        .addInputLines(
            "in/Foo.java",
            """
            abstract class Foo {
              java.lang.Void v;

              final Void f() {
                return v;
              }
            }
            """)
        .addOutputLines(
            "out/Foo.java",
            """
            import org.jspecify.annotations.Nullable;

            abstract class Foo {
              java.lang.@Nullable Void v;

              final @Nullable Void f() {
                return v;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeAlreadyAnnotated() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.annotation.Nullable;

            class Test {
              @Nullable Void v;

              @Nullable
              Void f() {
                return v;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeNotVoid() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              String s;

              String f() {
                return s;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveTypeArgument() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              // BUG: Diagnostic contains: @Nullable
              List<Void> a;
              // BUG: Diagnostic contains: @Nullable
              List<? extends Void> b;
              // BUG: Diagnostic contains: @Nullable
              List<? super Void> c;
              List<?> d;
            }
            """)
        .doTest();
  }

  @Test
  public void positiveTypeArgumentOtherAnnotation() {
    aggressiveCompilationHelper
        .addSourceLines(
            "NonNull.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.TYPE_USE)
            public @interface NonNull {}
            """)
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              // BUG: Diagnostic contains: @Nullable
              List<@NonNull Void> a;
              // BUG: Diagnostic contains: @Nullable
              List<? extends @NonNull Void> b;
              // BUG: Diagnostic contains: @Nullable
              List<? super @NonNull Void> c;
              List<?> d;
            }
            """)
        .doTest();
  }

  @Test
  public void negativeTypeArgumentAlreadyAnnotated() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Nullable.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.TYPE_USE)
            public @interface Nullable {}
            """)
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              List<@Nullable Void> a;
              List<? extends @Nullable Void> b;
              List<? super @Nullable Void> c;
              List<?> d;
            }
            """)
        .doTest();
  }

  @Test
  public void negativeTypeArgumentAlreadyAnnotatedAnonymous() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Nullable.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.TYPE_USE)
            public @interface Nullable {}
            """)
        .addSourceLines("Bystander.java", "public interface Bystander<T> {}")
        .addSourceLines(
            "Test.java",
            """
            class Test {
              static {
                new Bystander<@Nullable Void>() {};
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeTypeArgumentNotVoid() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Nullable.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.TYPE_USE)
            public @interface Nullable {}
            """)
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              List<String> a;
              List<? extends String> b;
              List<? super String> c;
              List<?> d;
            }
            """)
        .doTest();
  }

  @Test
  public void negativeTypeArgumentClassAndTypeToken() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.reflect.TypeToken;
            import org.jspecify.annotations.Nullable;

            class Test {
              Class<Void> a;
              Class<? extends Void> b;
              Class<? super Void> c;
              TypeToken<Void> d = new TypeToken<Void>() {};
              TypeToken<? extends Void> e;
              TypeToken<? super Void> f;
            }
            """)
        .doTest();
  }

  @Test
  public void negativeTypeArgumentDeclarationNullable() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;
            import javax.annotation.Nullable;

            class Test {
              @Nullable Object existingDeclarationUsage;

              List<Void> a;
              List<? extends Void> b;
              List<? super Void> c;
              List<?> d;
            }
            """)
        .doTest();
  }

  @Test
  public void positiveLambdaParameter() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.annotation.Nullable;

            interface Test {
              void consume(@Nullable Void v);

              // BUG: Diagnostic contains: @Nullable
              Test TEST = (Void v) -> {};
            }
            """)
        .doTest();
  }

  @Test
  public void negativeLambdaParameterNoType() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.annotation.Nullable;

            interface Test {
              void consume(@Nullable Void v);

              Test TEST = v -> {};
            }
            """)
        .doTest();
  }

  @Test
  public void negativeGenericLambdaParameterNoType() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.jspecify.annotations.Nullable;

            interface Test {
              void consume(Iterable<@Nullable Void> it);

              Test TEST = it -> {};
            }
            """)
        .doTest();
  }

  // TODO(cpovirk): Test under Java 11+ with `(var x) -> {}` lambda syntax.

  @Test
  public void negativeVar() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.annotation.Nullable;

            class Test {
              @Nullable Void v;

              void f() {
                var v = this.v;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeOtherLocalVariable() {
    aggressiveCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.annotation.Nullable;

            class Test {
              @Nullable Void v;

              void f() {
                Void v = this.v;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveConservativeNullMarked() {
    conservativeCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.jspecify.annotations.NullMarked;

            @NullMarked
            class Test {
              // BUG: Diagnostic contains: @Nullable
              Void v;
            }
            """)
        .doTest();
  }

  @Test
  public void negativeConservativeNotNullMarked() {
    conservativeCompilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              Void v;
            }
            """)
        .doTest();
  }

  @Test
  public void likelyPublicApiOnly() {
    CompilationTestHelper.newInstance(VoidMissingNullable.class, getClass())
        .setArgs("-XepOpt:Nullness:Conservative=false", "-XepOpt:Nullness:LikelyPublicApiOnly=true")
        .addSourceLines(
            "PublicFoo.java",
            """
            import java.util.List;
            import java.util.function.Consumer;
            import java.util.function.Supplier;

            // BUG: Diagnostic contains: @Nullable
            public abstract class PublicFoo implements Iterable<Void> {
              // BUG: Diagnostic contains: @Nullable
              public Void publicField;
              Void packagePrivateField;

              // BUG: Diagnostic contains: @Nullable
              public Void publicMethod() {
                return null;
              }

              Void packagePrivateMethod() {
                return null;
              }

              // BUG: Diagnostic contains: @Nullable
              public void publicMethodWithVoidParam(Void v) {}

              void packagePrivateMethodWithVoidParam(Void v) {}

              // BUG: Diagnostic contains: @Nullable
              public List<Void> publicTypeArgField;
              List<Void> packagePrivateTypeArgField;

              // BUG: Diagnostic contains: @Nullable
              public List<Void> publicTypeArgMethod() {
                return null;
              }

              List<Void> packagePrivateTypeArgMethod() {
                return null;
              }

              public Object initializerTypeArg = (List<Void>) null;

              public void localAndLambda() {
                List<Void> local = null;
                local = (List<Void>) null;
                Consumer<Void> c = (Void v) -> {};
                Supplier<Object> s = () -> (List<Void>) null;
              }
            }
            """)
        .addSourceLines(
            "PackagePrivateFoo.java",
            """
            import java.util.List;

            abstract class PackagePrivateFoo implements Iterable<Void> {
              public Void publicField;
              public List<Void> publicTypeArgField;
            }
            """)
        .doTest();
  }
}
