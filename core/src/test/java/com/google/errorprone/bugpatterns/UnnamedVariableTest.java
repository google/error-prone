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

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UnnamedVariableTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnnamedVariable.class, getClass())
          .setArgs("-XepOpt:UnnamedVariable:OnlyRenameVariablesNamedUnused=false");

  @Before
  public void setUp() {
    assume().that(Runtime.version().feature()).isAtLeast(22);
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;
            import java.util.function.Function;

            class Test {
              public static void main(String[] args) {
                {
                  int unused = 1;
                  int unused1 = 2;
                  String unused2 = "hello";
                }
                Object bar = new Object();
                bar = null;
                Function<String, Integer> f = unused -> 1;
                f = (String unused) -> 1;
                f = (unused) -> 1;
              }

              public void foo(List<String> list) {
                list.forEach(unused -> System.out.println());
                list.forEach((String unused1) -> System.out.println());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.List;
            import java.util.function.Function;

            class Test {
              public static void main(String[] args) {
                {
                  var _ = 1;
                  var _ = 2;
                  var _ = "hello";
                }
                Object bar = new Object();
                bar = null;
                Function<String, Integer> f = _ -> 1;
                f = (_) -> 1;
                f = (_) -> 1;
              }

              public void foo(List<String> list) {
                list.forEach(_ -> System.out.println());
                list.forEach((String _) -> System.out.println());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public static void main(String[] args) {
                var _ = 42;
                int x = 1;
                x++;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeAllNames() {
    CompilationTestHelper.newInstance(UnnamedVariable.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void main(String[] args) {
                int x = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void catchBlock() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.io.IOException;

            class Test {
              void f() {
                try {
                  throw new IOException();
                } catch (IOException ioe) {
                } catch (Exception e) {
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.io.IOException;

            class Test {
              void f() {
                try {
                  throw new IOException();
                } catch (IOException _) {
                } catch (Exception _) {
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void tryWithResources() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f1(AutoCloseable a) throws Exception {
                try (AutoCloseable c = a) {}
              }

              void f2(AutoCloseable a) throws Exception {
                try (var _ = a) {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f1(AutoCloseable a) throws Exception {
                try (var _ = a) {}
              }

              void f2(AutoCloseable a) throws Exception {
                try (var _ = a) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void tryWithResources_methodReference() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.io.Closeable;
            import java.io.IOException;

            class Test {
              void closeWriter() {}

              void f() throws IOException {
                try (Closeable unused = this::closeWriter) {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.io.Closeable;
            import java.io.IOException;

            class Test {
              void closeWriter() {}

              void f() throws IOException {
                try (Closeable _ = this::closeWriter) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void forLoop() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void f(List<Integer> xs) {
                for (Integer x : xs) {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void f(List<Integer> xs) {
                for (var _ : xs) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void patternVariable() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              boolean f(Object o) {
                return switch (o) {
                  case String unused -> true;
                  default -> false;
                };
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              boolean f(Object o) {
                return switch (o) {
                  case String _ -> true;
                  default -> false;
                };
              }
            }
            """)
        .doTest();
  }

  @Test
  public void declarationWithoutInitializer_noVar() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                String unused;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void localVariable_namedUnusedButActuallyUsed() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              public void foo() {
                String unused = "Hello world!";
                System.out.println(unused);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void traditionalForLoop_unused() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                for (int i = 0; ; ) {
                  break;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                for (var _ = 0; ; ) {
                  break;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void localVariable_initializedToNull() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                String unused = null;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                String _ = null;
              }
            }
            """)
        .doTest();
  }

  // TODO(b/585664542): The output doesn't compile, since javac can't infer the generic methods'
  // type variables without a target type. The explicit types should be kept.
  @Test
  public void localVariable_polyExpressionArgumentToGenericMethod() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;
            import java.util.Optional;

            class Test {
              void f() {
                List<Runnable> unused = List.of(() -> {});
                Optional<Runnable> unused2 = Optional.of(this::f);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.List;
            import java.util.Optional;

            class Test {
              void f() {
                var _ = List.of(() -> {});
                var _ = Optional.of(this::f);
              }
            }
            """)
        .allowBreakingChanges()
        .doTest();
  }

  @Test
  public void alreadyVar() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                var unused = 1;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                var _ = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleDeclarations() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                int unused = 1, unused2 = 2;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                int _ = 1, _ = 2;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void lambdaParameter_declaredType() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Consumer;

            class Test {
              void f() {
                Consumer<String> c = (String unused) -> {};
                c.accept("foo");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Consumer;

            class Test {
              void f() {
                Consumer<String> c = (_) -> {};
                c.accept("foo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void lambdaParameter_implicitType() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Consumer;

            class Test {
              void f() {
                Consumer<String> c = unused -> {};
                c.accept("foo");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Consumer;

            class Test {
              void f() {
                Consumer<String> c = _ -> {};
                c.accept("foo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void lambdaParameter_mixedDeclaration() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.BiConsumer;

            class Test {
              void f() {
                BiConsumer<String, String> c =
                    (String toPrint, String unused) -> {
                      System.out.println(toPrint);
                    };
                c.accept("foo", "bar");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.BiConsumer;

            class Test {
              void f() {
                BiConsumer<String, String> c =
                    (String toPrint, String _) -> {
                      System.out.println(toPrint);
                    };
                c.accept("foo", "bar");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void lambdaParameter_multipleUnused() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.BiConsumer;

            class Test {
              void f() {
                BiConsumer<String, String> c = (String unused1, String unused2) -> {};
                c.accept("foo", "bar");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.BiConsumer;

            class Test {
              void f() {
                BiConsumer<String, String> c = (_, _) -> {};
                c.accept("foo", "bar");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void lambdaParameter_annotated() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Consumer;
            import org.jspecify.annotations.Nullable;

            class Test {
              void f() {
                Consumer<String> c = (@Nullable String unused) -> {};
                c.accept("foo");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Consumer;
            import org.jspecify.annotations.Nullable;

            class Test {
              void f() {
                Consumer<String> c = (_) -> {};
                c.accept("foo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void localVariable_annotated() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.util.List;
            import org.jspecify.annotations.Nullable;

            class Test {
              @Target({ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
              @interface LocalAndTypeUse {}

              @Target({ElementType.FIELD, ElementType.TYPE_USE})
              @interface FieldAndTypeUse {}

              @interface NoTarget {}

              void f(List<String> list, AutoCloseable a) throws Exception {
                @Nullable String unused = "hello";
                final @Nullable String unused2 = "hello";
                @SuppressWarnings("foo") @Nullable String unused3 = "hello";
                @Nullable String unused4 = null;
                for (@Nullable String unused5 : list) {}
                try (@Nullable AutoCloseable unused6 = a) {}
                @Nullable String[] unused7 = new String[0];
                java.lang.@Nullable String unused8 = "hello";
                @LocalAndTypeUse String unused9 = "hello";
                @NoTarget String unused10 = "hello";
                @FieldAndTypeUse String unused11 = "hello";
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.util.List;
            import org.jspecify.annotations.Nullable;

            class Test {
              @Target({ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
              @interface LocalAndTypeUse {}

              @Target({ElementType.FIELD, ElementType.TYPE_USE})
              @interface FieldAndTypeUse {}

              @interface NoTarget {}

              void f(List<String> list, AutoCloseable a) throws Exception {
                var _ = "hello";
                final var _ = "hello";
                @SuppressWarnings("foo") var _ = "hello";
                @Nullable String _ = null;
                for (var _ : list) {}
                try (var _ = a) {}
                var _ = new String[0];
                var _ = "hello";
                @LocalAndTypeUse var _ = "hello";
                @NoTarget var _ = "hello";
                var _ = "hello";
              }
            }
            """)
        .doTest();
  }

  @Test
  public void localVariable_polyAndArrayInitializers() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Consumer;

            class Test {
              void bar() {}

              void f(boolean flag, Runnable fallback) {
                Runnable unused = this::bar;
                Runnable unused2 = (this::bar);
                Runnable unused3 = () -> {};
                Consumer<String> unused4 = x -> System.out.println(x);
                int[] unused5 = {1, 2};
                int[] unused6 = new int[] {1, 2};
                Runnable unused7 = flag ? this::bar : () -> {};
                Runnable unused8 = flag ? null : fallback;
                Object unused9 = flag ? null : null;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.function.Consumer;

            class Test {
              void bar() {}

              void f(boolean flag, Runnable fallback) {
                Runnable _ = this::bar;
                Runnable _ = (this::bar);
                Runnable _ = () -> {};
                Consumer<String> _ = x -> System.out.println(x);
                int[] _ = {1, 2};
                var _ = new int[] {1, 2};
                Runnable _ = flag ? this::bar : () -> {};
                var _ = flag ? null : fallback;
                Object _ = flag ? null : null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void localVariable_switchExpressionInitializers() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void bar() {}

              void f(int i) {
                String unused =
                    switch (i) {
                      case 0 -> "a";
                      case 1 -> {
                        yield "b";
                      }
                      default -> throw new IllegalStateException();
                    };
                Runnable unused2 =
                    switch (i) {
                      case 0 -> this::bar;
                      default -> () -> {};
                    };
                Runnable unused3 =
                    switch (i) {
                      case 0 -> {
                        yield this::bar;
                      }
                      default -> {
                        yield () -> {};
                      }
                    };
                Runnable unused4 =
                    switch (i) {
                      case 0:
                        yield this::bar;
                      default:
                        yield () -> {};
                    };
                String unused5 =
                    switch (i) {
                      case 0 -> {
                        int j =
                            switch (i) {
                              default -> {
                                yield 1;
                              }
                            };
                        Runnable r =
                            () -> {
                              String s =
                                  switch (j) {
                                    default -> {
                                      yield "s";
                                    }
                                  };
                            };
                        yield "c";
                      }
                      default -> "d";
                    };
                Object unused6 = (Runnable) () -> {};
                Thread unused7 = new Thread(this::bar);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void bar() {}

              void f(int i) {
                var _ =
                    switch (i) {
                      case 0 -> "a";
                      case 1 -> {
                        yield "b";
                      }
                      default -> throw new IllegalStateException();
                    };
                Runnable _ =
                    switch (i) {
                      case 0 -> this::bar;
                      default -> () -> {};
                    };
                Runnable _ =
                    switch (i) {
                      case 0 -> {
                        yield this::bar;
                      }
                      default -> {
                        yield () -> {};
                      }
                    };
                Runnable _ =
                    switch (i) {
                      case 0:
                        yield this::bar;
                      default:
                        yield () -> {};
                    };
                String _ =
                    switch (i) {
                      case 0 -> {
                        int j =
                            switch (i) {
                              default -> {
                                yield 1;
                              }
                            };
                        Runnable _ =
                            () -> {
                              var _ =
                                  switch (j) {
                                    default -> {
                                      yield "s";
                                    }
                                  };
                            };
                        yield "c";
                      }
                      default -> "d";
                    };
                var _ = (Runnable) () -> {};
                var _ = new Thread(this::bar);
              }
            }
            """)
        .doTest();
  }
}
