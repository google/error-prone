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

public class DiscouragedHashKeyTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DiscouragedHashKey.class, getClass())
          .addSourceLines(
              "BadKey.java",
              """
              package com.example;

              public class BadKey {}
              """);

  @Test
  public void noTypesConfigured() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.example.BadKey;
            import java.util.HashSet;
            import java.util.Set;

            class Test {
              Set<BadKey> s = new HashSet<>();
            }
            """)
        .doTest();
  }

  @Test
  public void positive() {
    compilationHelper
        .setArgs("-XepOpt:DiscouragedHashKey:Types=com.example.BadKey")
        .addSourceLines(
            "Test.java",
            """
            import static java.util.stream.Collectors.toSet;

            import com.example.BadKey;
            import com.google.common.collect.HashMultimap;
            import com.google.common.collect.ImmutableMap;
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.Multimap;
            import java.util.HashMap;
            import java.util.Map;
            import java.util.Set;
            import java.util.stream.Stream;

            class Test {
              void f(BadKey k, Stream<BadKey> stream) {
                // BUG: Diagnostic contains: BadKey should not be used as an element of a hash-based
                Map<BadKey, String> m = new HashMap<>();
                // BUG: Diagnostic contains: BadKey should not be used
                ImmutableSet<BadKey> s = ImmutableSet.of(k);
                // BUG: Diagnostic contains: BadKey should not be used
                ImmutableMap<BadKey, String> i = ImmutableMap.of(k, "");
                // BUG: Diagnostic contains: BadKey should not be used
                Multimap<BadKey, String> mm = HashMultimap.create();
                // BUG: Diagnostic contains: BadKey should not be used
                Set<BadKey> c = stream.collect(toSet());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void subtype() {
    compilationHelper
        .setArgs("-XepOpt:DiscouragedHashKey:Types=com.example.BadKey")
        .addSourceLines(
            "Test.java",
            """
            import com.example.BadKey;
            import java.util.HashSet;
            import java.util.Set;

            class Test {
              static class SubKey extends BadKey {}

              // BUG: Diagnostic contains: SubKey should not be used
              Set<SubKey> s = new HashSet<>();
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .setArgs("-XepOpt:DiscouragedHashKey:Types=com.example.BadKey")
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            import com.example.BadKey;
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.IdentityHashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.Objects;
            import java.util.TreeMap;

            class Test {
              void f(BadKey a, BadKey b) {
                // Equality and containment are fine; only hashing is discouraged.
                boolean x = a.equals(b);
                boolean y = Objects.equals(a, b);
                assertThat(a).isEqualTo(b);
                List<BadKey> list = new ArrayList<>();
                boolean z = list.contains(a);
                Map<String, BadKey> values = new HashMap<>();
                Map<BadKey, String> identity = new IdentityHashMap<>();
                Map<BadKey, String> sorted = new TreeMap<>((l, r) -> 0);
              }
            }
            """)
        .doTest();
  }
}
