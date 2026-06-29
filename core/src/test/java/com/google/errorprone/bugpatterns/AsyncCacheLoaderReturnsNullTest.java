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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link AsyncCacheLoaderReturnsNull}. */
@RunWith(JUnit4.class)
public class AsyncCacheLoaderReturnsNullTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AsyncCacheLoaderReturnsNull.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "AsyncCacheLoaderReturnsNullPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.util.concurrent.Futures.immediateFuture;

            import com.google.common.cache.AsyncCacheLoader;
            import com.google.common.util.concurrent.ListenableFuture;

            /** Positive cases for {@link AsyncCacheLoaderReturnsNull}. */
            public class AsyncCacheLoaderReturnsNullPositiveCases {
              static void listenableFutures() {
                new AsyncCacheLoader<String, Object>() {
                  @Override
                  public ListenableFuture<Object> load(String key) {
                    // BUG: Diagnostic contains:
                    return null;
                  }
                };

                new AsyncCacheLoader<Object, String>() {
                  @Override
                  public ListenableFuture<String> load(Object o) {
                    if (o instanceof String) {
                      return immediateFuture((String) o);
                    }
                    // BUG: Diagnostic contains:
                    return null;
                  }
                };
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "AsyncCacheLoaderReturnsNullNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import static com.google.common.util.concurrent.Futures.immediateFuture;

            import com.google.common.base.Function;
            import com.google.common.cache.AsyncCacheLoader;
            import com.google.common.util.concurrent.ListenableFuture;
            import java.util.function.Supplier;
            import org.jspecify.annotations.Nullable;

            /** Negative cases for {@link AsyncCacheLoaderReturnsNull}. */
            public class AsyncCacheLoaderReturnsNullNegativeCases {
              static {
                new AsyncCacheLoader<String, Object>() {
                  @Override
                  public ListenableFuture<Object> load(String key) {
                    return immediateFuture(null);
                  }
                };

                new Function<String, Object>() {
                  @Override
                  public Object apply(String input) {
                    return null;
                  }
                };

                new AsyncCacheLoader<String, Object>() {
                  @Override
                  public ListenableFuture<Object> load(String key) {
                    return load(key, key);
                  }

                  public ListenableFuture<Object> load(String input1, String input2) {
                    return null;
                  }
                };

                new MyNonAsyncCacheLoader<String, Object>() {
                  @Override
                  public ListenableFuture<Object> load(String key) {
                    return null;
                  }
                };

                new AsyncCacheLoader<String, Object>() {
                  @Override
                  public ListenableFuture<Object> load(String key) {
                    Supplier<String> s =
                        () -> {
                          return null;
                        };
                    return immediateFuture(s.get());
                  }
                };
              }

              interface MyNonAsyncCacheLoader<K, V> {
                ListenableFuture<V> load(@Nullable K key);
              }
            }
            """)
        .doTest();
  }
}
