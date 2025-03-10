/*
 * Copyright 2024 The Error Prone Authors.
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

/** Unit tests for {@link FutureTransformAsync}. */
@RunWith(JUnit4.class)
public class FutureTransformAsyncTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FutureTransformAsync.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(FutureTransformAsync.class, getClass());

  @Test
  public void transformAsync_expressionLambda() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    Futures.transformAsync(
                        Futures.immediateFuture(5),
                        value -> Futures.immediateFuture("value: " + value),
                        executor);
                return future;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;

class Test {
  private Executor executor;

  ListenableFuture<String> test() {
    ListenableFuture<String> future =
        Futures.transform(Futures.immediateFuture(5), value -> "value: " + value, executor);
    return future;
  }
}
""")
        .doTest();
  }

  @Test
  public void transformAsync_statementLambda() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    Futures.transformAsync(
                        Futures.immediateFuture(5),
                        value -> {
                          if (value > 5) {
                            return Futures.immediateFuture("large");
                          } else if (value < 5) {
                            return Futures.immediateFuture("small");
                          }
                          return Futures.immediateFuture("value: " + value);
                        },
                        executor);
                return future;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    Futures.transform(
                        Futures.immediateFuture(5),
                        value -> {
                          if (value > 5) {
                            return "large";
                          } else if (value < 5) {
                            return "small";
                          }
                          return "value: " + value;
                        },
                        executor);
                return future;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void transformAsync_notAllImmediateFutures() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              ListenableFuture<String> foo(String s) {
                return Futures.immediateFuture(s);
              }

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    Futures.transformAsync(
                        Futures.immediateFuture(5),
                        value -> {
                          if (value > 0) {
                            return foo("large");
                          }
                          return Futures.immediateFuture("value: " + value);
                        },
                        executor);
                return future;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void transformAsync_statementLambda_throwsCheckedException() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.io.FileNotFoundException;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    Futures.transformAsync(
                        Futures.immediateFuture(5),
                        value -> {
                          if (value > 0) {
                            throw new FileNotFoundException("large");
                          }
                          return Futures.immediateFuture("value: " + value);
                        },
                        executor);
                return future;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void transformAsync_statementLambda_methodThrowsCheckedException() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.io.FileNotFoundException;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              private void throwIfLarge(int unused) throws FileNotFoundException {}

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    Futures.transformAsync(
                        Futures.immediateFuture(5),
                        value -> {
                          throwIfLarge(value);
                          return Futures.immediateFuture("value: " + value);
                        },
                        executor);
                return future;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void transformAsync_expressionLambda_methodThrowsCheckedException() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.io.FileNotFoundException;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              private String throwIfLarge(int value) throws FileNotFoundException {
                return "value: " + value;
              }

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    Futures.transformAsync(
                        Futures.immediateFuture(5),
                        value -> Futures.immediateFuture(throwIfLarge(value)),
                        executor);
                return future;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void transformAsync_uncheckedException() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    Futures.transformAsync(
                        Futures.immediateFuture(5),
                        value -> {
                          if (value > 0) {
                            throw new IllegalStateException("large");
                          }
                          return Futures.immediateFuture("value: " + value);
                        },
                        executor);
                return future;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    Futures.transform(
                        Futures.immediateFuture(5),
                        value -> {
                          if (value > 0) {
                            throw new IllegalStateException("large");
                          }
                          return "value: " + value;
                        },
                        executor);
                return future;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void transformAsync_returnTransformAsyncResult() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;

class Test {
  private Executor executor;

  ListenableFuture<String> test() {
    return Futures.transformAsync(
        Futures.immediateFuture(5), value -> Futures.immediateFuture("value: " + value), executor);
  }
}
""")
        .addOutputLines(
            "out/Test.java",
            """
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;

class Test {
  private Executor executor;

  ListenableFuture<String> test() {
    return Futures.transform(Futures.immediateFuture(5), value -> "value: " + value, executor);
  }
}
""")
        .doTest();
  }

  @Test
  public void transformAsync_staticImports() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            import static com.google.common.util.concurrent.Futures.immediateFuture;
            import static com.google.common.util.concurrent.Futures.transformAsync;
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              ListenableFuture<String> foo(String s) {
                return immediateFuture(s);
              }

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    transformAsync(foo("x"), value -> immediateFuture("value: " + value), executor);
                return future;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.transform;
import static com.google.common.util.concurrent.Futures.transformAsync;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;

class Test {
  private Executor executor;

  ListenableFuture<String> foo(String s) {
    return immediateFuture(s);
  }

  ListenableFuture<String> test() {
    ListenableFuture<String> future = transform(foo("x"), value -> "value: " + value, executor);
    return future;
  }
}
""")
        .doTest();
  }

  @Test
  public void transformAsync_immediateVoidFuture() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;

class Test {
  private Executor executor;

  ListenableFuture<String> foo(String s) {
    return Futures.immediateFuture(s);
  }

  ListenableFuture<Void> test() {
    ListenableFuture<Void> future =
        Futures.transformAsync(foo("x"), value -> Futures.immediateVoidFuture(), executor);
    return future;
  }
}
""")
        .addOutputLines(
            "out/Test.java",
            """
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;

class Test {
  private Executor executor;

  ListenableFuture<String> foo(String s) {
    return Futures.immediateFuture(s);
  }

  ListenableFuture<Void> test() {
    ListenableFuture<Void> future = Futures.transform(foo("x"), value -> (Void) null, executor);
    return future;
  }
}
""")
        .doTest();
  }

  @Test
  public void transformAsync_withTypeArgument() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;

class Test {
  private Executor executor;

  ListenableFuture<Void> test() {
    ListenableFuture<Void> future =
        Futures.transformAsync(
            Futures.immediateFuture("x"), value -> Futures.<Void>immediateFuture(null), executor);
    return future;
  }
}
""")
        .addOutputLines(
            "out/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              ListenableFuture<Void> test() {
                ListenableFuture<Void> future =
                    Futures.transform(Futures.immediateFuture("x"), value -> (Void) null, executor);
                return future;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void transformAsync_nestedLambdas() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              interface TestInterface {
                ListenableFuture<Void> apply(String value);
              }

              void foo(TestInterface unused) {
                return;
              }

              ListenableFuture<Void> test() {
                ListenableFuture<Void> future =
                    Futures.transformAsync(
                        Futures.immediateFuture("x"),
                        unused -> {
                          foo(
                              x -> {
                                return Futures.immediateVoidFuture();
                              });
                          return Futures.immediateVoidFuture();
                        },
                        executor);
                return future;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              interface TestInterface {
                ListenableFuture<Void> apply(String value);
              }

              void foo(TestInterface unused) {
                return;
              }

              ListenableFuture<Void> test() {
                ListenableFuture<Void> future =
                    Futures.transform(
                        Futures.immediateFuture("x"),
                        unused -> {
                          foo(
                              x -> {
                                return Futures.immediateVoidFuture();
                              });
                          return (Void) null;
                        },
                        executor);
                return future;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void transformAsync_fluentFuture() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;

class Test {
  private Executor executor;

  ListenableFuture<String> test() {
    ListenableFuture<String> future =
        FluentFuture.from(Futures.immediateFuture(5))
            .transformAsync(value -> Futures.immediateFuture("value: " + value), executor);
    return future;
  }
}
""")
        .addOutputLines(
            "out/Test.java",
            """
            import com.google.common.util.concurrent.FluentFuture;
            import com.google.common.util.concurrent.Futures;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.concurrent.Executor;

            class Test {
              private Executor executor;

              ListenableFuture<String> test() {
                ListenableFuture<String> future =
                    FluentFuture.from(Futures.immediateFuture(5))
                        .transform(value -> "value: " + value, executor);
                return future;
              }
            }
            """)
        .doTest();
  }
}
