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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;

/**
 * Tests for {@link ValueClassIdentity}.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class ValueClassIdentityTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ValueClassIdentity.class, getClass());

  @Test
  public void weakReferencePositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.ref.WeakReference;
            import java.util.Optional;

            class Test {
              void f(Integer i, Optional<String> opt) {
                // BUG: Diagnostic contains: ValueClassIdentity
                new WeakReference<Integer>(i);
                // BUG: Diagnostic contains: ValueClassIdentity
                new WeakReference<Optional<String>>(opt);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void softReferencePositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.ref.SoftReference;
            import java.util.Optional;

            class Test {
              void f(Integer i, Optional<String> opt) {
                // BUG: Diagnostic contains: ValueClassIdentity
                new SoftReference<Integer>(i);
                // BUG: Diagnostic contains: ValueClassIdentity
                new SoftReference<Optional<String>>(opt);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void weakReferenceNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.ref.WeakReference;

            class Test {
              void f(String s, Object obj) {
                new WeakReference<String>(s);
                new WeakReference<Object>(obj);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void weakReferenceSubclassPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.ref.WeakReference;
            import java.util.Optional;

            class Test {
              static class MyWeakRef<T> extends WeakReference<T> {
                MyWeakRef(T referent) {
                  super(referent);
                }
              }

              void f(Integer i, Optional<String> opt) {
                // BUG: Diagnostic contains: ValueClassIdentity
                new MyWeakRef<Integer>(i);
                // BUG: Diagnostic contains: ValueClassIdentity
                new MyWeakRef<Optional<String>>(opt);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void weakHashMapPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;
            import java.util.WeakHashMap;

            class Test {
              void f() {
                // BUG: Diagnostic contains: ValueClassIdentity
                new WeakHashMap<Integer, String>();
                // BUG: Diagnostic contains: ValueClassIdentity
                new WeakHashMap<Optional<String>, Boolean>();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void weakHashMapNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.WeakHashMap;

            class Test {
              void f() {
                new WeakHashMap<String, Integer>();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void weakHashMapSubclassPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;
            import java.util.WeakHashMap;

            class Test {
              static class MyWeakMap<K, V> extends WeakHashMap<K, V> {}

              void f() {
                // BUG: Diagnostic contains: ValueClassIdentity
                new MyWeakMap<Integer, String>();
                // BUG: Diagnostic contains: ValueClassIdentity
                new MyWeakMap<Optional<String>, Boolean>();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashMapPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.IdentityHashMap;
            import java.util.Optional;

            class Test {
              void f() {
                // BUG: Diagnostic contains: ValueClassIdentity
                new IdentityHashMap<Integer, String>();
                // BUG: Diagnostic contains: ValueClassIdentity
                new IdentityHashMap<String, Integer>();
                // BUG: Diagnostic contains: ValueClassIdentity
                new IdentityHashMap<Optional<String>, String>();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void chronoValueClassesPositiveWithOlderRelease() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.time.chrono.HijrahDate;
            import java.time.chrono.JapaneseDate;
            import java.time.chrono.MinguoDate;
            import java.time.chrono.ThaiBuddhistDate;
            import java.util.IdentityHashMap;

            class Test {
              void f() {
                // BUG: Diagnostic contains: ValueClassIdentity
                new IdentityHashMap<HijrahDate, String>();
                // BUG: Diagnostic contains: ValueClassIdentity
                new IdentityHashMap<JapaneseDate, String>();
                // BUG: Diagnostic contains: ValueClassIdentity
                new IdentityHashMap<MinguoDate, String>();
                // BUG: Diagnostic contains: ValueClassIdentity
                new IdentityHashMap<ThaiBuddhistDate, String>();
              }
            }
            """)
        .setArgs("--release", "15")
        .doTest();
  }

  @Test
  public void identityHashMapArgumentPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.time.LocalDate;
            import java.util.IdentityHashMap;

            class Test {
              void f(IdentityHashMap<Object, Object> map, LocalDate date) {
                // BUG: Diagnostic contains: ValueClassIdentity
                map.put(date, "x");
                // BUG: Diagnostic contains: ValueClassIdentity
                map.putIfAbsent(date, "x");
                // BUG: Diagnostic contains: ValueClassIdentity
                map.put("k", date);
                // BUG: Diagnostic contains: ValueClassIdentity
                map.get(date);
                // BUG: Diagnostic contains: ValueClassIdentity
                map.containsKey(date);
                // BUG: Diagnostic contains: ValueClassIdentity
                map.containsValue(date);
                // BUG: Diagnostic contains: ValueClassIdentity
                map.remove(date);
                // BUG: Diagnostic contains: ValueClassIdentity
                map.remove("k", date);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashMapArgumentBoxedPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.IdentityHashMap;

            class Test {
              void f(IdentityHashMap<Object, Object> map, Integer i) {
                // BUG: Diagnostic contains: ValueClassIdentity
                map.put(i, "x");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashMapArgumentNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.IdentityHashMap;

            class Test {
              void f(IdentityHashMap<Object, Object> map, String s) {
                map.put(s, s);
                map.get(s);
                map.containsValue(s);
                map.remove(s);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashMapArgumentNoVisibleConstructionPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.time.LocalDate;
            import java.util.IdentityHashMap;

            class Test {
              // The map is not created in this compilation unit, so the construction site
              // diagnostic never fires here.
              void fromParameter(IdentityHashMap<LocalDate, String> map, LocalDate date) {
                // BUG: Diagnostic contains: ValueClassIdentity
                map.put(date, "x");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashMapArgumentSubclassPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.time.LocalDate;
            import java.util.IdentityHashMap;

            class Test {
              static class MyMap<K, V> extends IdentityHashMap<K, V> {}

              void f(MyMap<Object, Object> map, LocalDate date) {
                // BUG: Diagnostic contains: ValueClassIdentity
                map.put(date, "x");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashMapArgumentUnrelatedOverloadNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.time.LocalDate;
            import java.util.IdentityHashMap;

            class Test {
              static class MyMap<K, V> extends IdentityHashMap<K, V> {
                void get(LocalDate date) {}
              }

              void f(MyMap<Object, Object> map) {
                map.get(LocalDate.now());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashMapArgumentRequiringDataflowNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.time.LocalDate;
            import java.util.IdentityHashMap;
            import java.util.Map;

            class Test {
              // Seeing through the Object local would require dataflow.
              void widenedLocal(IdentityHashMap<Object, Object> map) {
                Object key = LocalDate.now();
                map.put(key, "x");
              }

              // The static receiver type is Map, so IdentityHashMap methods do not match.
              void declaredAsMap(Map<Object, Object> map) {
                map.put(LocalDate.now(), "x");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashMapNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.IdentityHashMap;

            class Test {
              void f() {
                new IdentityHashMap<String, String>();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashMapSubclassPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.IdentityHashMap;
            import java.util.Optional;

            class Test {
              static class MyIdentityMap<K, V> extends IdentityHashMap<K, V> {}

              void f() {
                // BUG: Diagnostic contains: ValueClassIdentity
                new MyIdentityMap<Integer, String>();
                // BUG: Diagnostic contains: ValueClassIdentity
                new MyIdentityMap<String, Integer>();
                // BUG: Diagnostic contains: ValueClassIdentity
                new MyIdentityMap<Optional<String>, String>();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void synchronizedPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;

            class Test {
              void f(Integer i, Optional<String> opt) {
                // BUG: Diagnostic contains: ValueClassIdentity
                synchronized (i) {
                }
                // BUG: Diagnostic contains: ValueClassIdentity
                synchronized (opt) {
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void synchronizedNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f(String s, Object obj) {
                synchronized (s) {
                }
                synchronized (obj) {
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void waitNotifyPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;

            class Test {
              void f(Integer i, Optional<String> opt) throws Exception {
                // BUG: Diagnostic contains: ValueClassIdentity
                i.wait();
                // BUG: Diagnostic contains: ValueClassIdentity
                i.wait(1000);
                // BUG: Diagnostic contains: ValueClassIdentity
                opt.notify();
                // BUG: Diagnostic contains: ValueClassIdentity
                opt.notifyAll();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void waitNotifyNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f(String s, Object obj) throws Exception {
                s.wait();
                obj.notify();
                obj.notifyAll();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashCodePositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Optional;

            class Test {
              void f(Integer i, Optional<String> opt) {
                // BUG: Diagnostic contains: ValueClassIdentity
                System.identityHashCode(i);
                // BUG: Diagnostic contains: ValueClassIdentity
                System.identityHashCode(opt);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashCodeNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f(String s) {
                System.identityHashCode(s);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void guavaCachePositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.cache.Cache;
            import com.google.common.cache.CacheBuilder;
            import java.util.Optional;

            class Test {
              void f() {
                // BUG: Diagnostic contains: ValueClassIdentity
                Cache<Integer, String> c1 = CacheBuilder.newBuilder().weakKeys().build();
                Cache<String, Optional<String>> c2 =
                    // BUG: Diagnostic contains: ValueClassIdentity
                    CacheBuilder.newBuilder().weakValues().build();
                // BUG: Diagnostic contains: ValueClassIdentity
                Cache<String, Integer> c3 = CacheBuilder.newBuilder().softValues().build();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void guavaCacheNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.cache.Cache;
            import com.google.common.cache.CacheBuilder;

            class Test {
              void f() {
                Cache<String, String> c = CacheBuilder.newBuilder().weakKeys().weakValues().build();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void guavaMapMakerPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.MapMaker;
            import java.util.concurrent.ConcurrentMap;

            class Test {
              void f() {
                // BUG: Diagnostic contains: ValueClassIdentity
                ConcurrentMap<Integer, String> m1 = new MapMaker().weakKeys().makeMap();
                // BUG: Diagnostic contains: ValueClassIdentity
                ConcurrentMap<String, Integer> m2 = new MapMaker().weakValues().makeMap();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void guavaMapMakerNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.MapMaker;
            import java.util.concurrent.ConcurrentMap;

            class Test {
              void f() {
                ConcurrentMap<String, String> m = new MapMaker().weakKeys().weakValues().makeMap();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void caffeineCachePositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.github.benmanes.caffeine.cache.Cache;
            import com.github.benmanes.caffeine.cache.Caffeine;
            import java.util.Optional;

            class Test {
              void f() {
                // BUG: Diagnostic contains: ValueClassIdentity
                Cache<Integer, String> c1 = Caffeine.newBuilder().weakKeys().build();
                // BUG: Diagnostic contains: ValueClassIdentity
                Cache<String, Optional<String>> c2 = Caffeine.newBuilder().weakValues().build();
                // BUG: Diagnostic contains: ValueClassIdentity
                Cache<String, Integer> c3 = Caffeine.newBuilder().softValues().build();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void caffeineCacheNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.github.benmanes.caffeine.cache.Cache;
            import com.github.benmanes.caffeine.cache.Caffeine;

            class Test {
              void f() {
                Cache<String, String> c = Caffeine.newBuilder().weakKeys().weakValues().build();
              }
            }
            """)
        .doTest();
  }
}
