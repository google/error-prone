/*
 * Copyright 2018 The Error Prone Authors.
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

/** Unit tests for {@link AutoValueImmutableFields}. */
@RunWith(JUnit4.class)
public class AutoValueImmutableFieldsTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AutoValueImmutableFields.class, getClass());

  @Test
  public void matchesNonPrimitiveArray() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableList",
            "  public abstract String[] countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesIntArray() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableList",
            "  public abstract int[] countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesCollection() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import java.util.Collection;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableCollection",
            "  public abstract Collection<String> countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesList() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import java.util.List;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableList",
            "  public abstract List<String> countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesMap() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import java.util.Map;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableMap",
            "  public abstract Map<String, String> countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesMultimap() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import com.google.common.collect.Multimap;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableMultimap",
            "  public abstract Multimap<String, String> countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesListMultimap() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import com.google.common.collect.ListMultimap;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableListMultimap",
            "  public abstract ListMultimap<String, String> countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesSetMultimap() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import com.google.common.collect.SetMultimap;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableSetMultimap",
            "  public abstract SetMultimap<String, String> countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesMultiset() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import com.google.common.collect.Multiset;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableMultiset",
            "  public abstract Multiset<String> countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesSet() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import java.util.Set;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableSet",
            "  public abstract Set<String> countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesTable() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import com.google.common.collect.Table;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableTable",
            "  public abstract Table<String, String, String> countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesTwoProperties() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import java.util.Map;",
            "import java.util.Set;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableSet",
            "  public abstract Set<String> countriesSet();",
            "  // BUG: Diagnostic contains: ImmutableMap",
            "  public abstract Map<String, String> countriesMap();",
            "}")
        .doTest();
  }

  @Test
  public void noMatches() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import com.google.common.collect.ImmutableCollection;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableMap;",
            "import com.google.common.collect.ImmutableMultimap;",
            "import com.google.common.collect.ImmutableListMultimap;",
            "import com.google.common.collect.ImmutableSetMultimap;",
            "import com.google.common.collect.ImmutableMultiset;",
            "import com.google.common.collect.ImmutableSet;",
            "import com.google.common.collect.ImmutableTable;",
            "@AutoValue",
            "abstract class Test {",
            "  public abstract ImmutableCollection<String> immutableCountriesCollection();",
            "  public abstract ImmutableList<String> immutableCountriesList();",
            "  public abstract ImmutableSet<String> immutableCountriesSet();",
            "  public abstract ImmutableMap<String, String> immutableCountriesMap();",
            "  public abstract ImmutableMultimap<String, String> immutableCountriesMultimap();",
            "  public abstract ImmutableListMultimap<String, String> countriesListMultimap();",
            "  public abstract ImmutableSetMultimap<String, String> countriesSetMultimap();",
            "  public abstract ImmutableMultiset<String> immutableCountriesMultiset();",
            "  public abstract ImmutableTable<String, String, String> immutableCountriesTable();",
            "}")
        .doTest();
  }

  @Test
  public void suppressionOnMethod() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import java.util.Collection;",
            "@AutoValue",
            "abstract class Test {",
            "  @SuppressWarnings(\"AutoValueImmutableFields\")",
            "  public abstract Collection<String> countries();",
            "}")
        .doTest();
  }

  @Test
  public void matchesNonPublic() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  // BUG: Diagnostic contains: ImmutableList",
            "  abstract String[] countries();",
            "}")
        .doTest();
  }
}
