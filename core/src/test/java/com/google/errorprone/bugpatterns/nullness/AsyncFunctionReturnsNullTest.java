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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link AsyncFunctionReturnsNull}. */
@RunWith(JUnit4.class)
public class AsyncFunctionReturnsNullTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AsyncFunctionReturnsNull.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "AsyncFunctionReturnsNullPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.util.concurrent.Futures.immediateFuture;

            import com.google.common.util.concurrent.AsyncFunction;
            import com.google.common.util.concurrent.ListenableFuture;

            /** Positive cases for {@link AsyncFunctionReturnsNull}. */
            public class AsyncFunctionReturnsNullPositiveCases {
              static void listenableFutures() {
                new AsyncFunction<String, Object>() {
                  @Override
                  public ListenableFuture<Object> apply(String input) throws Exception {
                    // BUG: Diagnostic contains: immediateFuture(null)
                    return null;
                  }
                };

                new AsyncFunction<Object, String>() {
                  @Override
                  public ListenableFuture<String> apply(Object o) {
                    if (o instanceof String) {
                      return immediateFuture((String) o);
                    }
                    // BUG: Diagnostic contains: immediateFuture(null)
                    return null;
                  }
                };
              }

              static class MyAsyncFunction implements AsyncFunction<Object, String> {
                @Override
                public ListenableFuture<String> apply(Object input) throws Exception {
                  return immediateFuture(input.toString());
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "AsyncFunctionReturnsNullNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.util.concurrent.Futures.immediateFuture;

            import com.google.common.base.Function;
            import com.google.common.util.concurrent.AsyncFunction;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.function.Supplier;
            import org.jspecify.annotations.Nullable;

            /** Negative cases for {@link AsyncFunctionReturnsNull}. */
            public class AsyncFunctionReturnsNullNegativeCases {
              static {
                new AsyncFunction<String, Object>() {
                  @Override
                  public ListenableFuture<Object> apply(String input) throws Exception {
                    return immediateFuture(null);
                  }
                };

                new Function<String, Object>() {
                  @Override
                  public Object apply(String input) {
                    return null;
                  }
                };

                new AsyncFunction<String, Object>() {
                  @Override
                  public ListenableFuture<Object> apply(String input) throws Exception {
                    return apply(input, input);
                  }

                  public ListenableFuture<Object> apply(String input1, String input2) {
                    return null;
                  }
                };

                new MyNonAsyncFunction<String, Object>() {
                  @Override
                  public ListenableFuture<Object> apply(String input) throws Exception {
                    return null;
                  }
                };

                new AsyncFunction<String, Object>() {
                  @Override
                  public ListenableFuture<Object> apply(String input) throws Exception {
                    Supplier<String> s =
                        () -> {
                          return null;
                        };
                    return immediateFuture(s.get());
                  }
                };
              }

              interface MyNonAsyncFunction<I, O> {
                ListenableFuture<O> apply(@Nullable I input) throws Exception;
              }
            }
            """)
        .doTest();
  }
}
