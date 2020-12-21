/*
 * Copyright 2017 The Error Prone Authors.
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

/** {@link MutableMethodReturnType}Test */
@RunWith(JUnit4.class)
public class MutableMethodReturnTypeTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(MutableMethodReturnType.class, getClass());

  @Test
  public void constructor_doesNotSuggestFix() {
    testHelper.addSourceLines("Test.java", "class Test {", "  Test() { }", "}").doTest();
  }

  @Test
  public void returnTypeVoid_doesNotSuggestFix() {
    testHelper.addSourceLines("Test.java", "class Test {", "  void foo() { }", "}").doTest();
  }

  @Test
  public void nonFinalNonPrivateNonStaticMethodInNonFinalClass_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void providesAnnotatedMethod_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import dagger.Provides;",
            "import java.util.List;",
            "class Test {",
            "  @Provides",
            "  static List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void producesAnnotatedMethod_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import dagger.producers.Produces;",
            "import java.util.List;",
            "class Test {",
            "  @Produces",
            "  static List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonFinalNonPrivateNonStaticMethodInFinalClass_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "final class Test {",
            "  // BUG: Diagnostic contains: ImmutableList<String> foo()",
            "  List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalMethodInNonFinalClass_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: final ImmutableList<String> foo()",
            "  final List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void privateMethodInNonFinalClass_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: private ImmutableList<String> foo()",
            "  private List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void staticMethodInNonFinalClass_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: static ImmutableList<String> foo()",
            "  static List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeImmutableList_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  final ImmutableList<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeImmutableCollection_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableCollection;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  final ImmutableCollection<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_singleReturnStatementImmutableList_suggestsImmutableList() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: final ImmutableList<String> foo()",
            "  final List<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_singleReturnStatementArrayList_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  final List<String> foo() {",
            "    return new ArrayList<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_singleReturnStatementList_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  final List<String> foo() {",
            "    List<String> bar = ImmutableList.of();",
            "    return bar;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_multipleReturnStatementsImmutableList_suggestsImmutableList() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: ImmutableList<String> foo()",
            "  final List<String> foo() {",
            "    if (true) {",
            "      return ImmutableList.of();",
            "    } else {",
            "      return ImmutableList.of();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_multipleReturnStatementsArrayListImmutableList_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  final List<String> foo() {",
            "    if (true) {",
            "      return ImmutableList.of();",
            "    } else {",
            "      return new ArrayList<>();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void
      returnTypeList_multipleReturnStatementsImmutableSetImmutableList_suggestsImmutableCollection() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Collection;",
            "class Test {",
            "  // BUG: Diagnostic contains: ImmutableCollection<String> foo()",
            "  final Collection<String> foo() {",
            "    if (true) {",
            "      return ImmutableList.of();",
            "    } else {",
            "      return ImmutableSet.of();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_multipleReturnStatementsImmutableSetImmutableMap_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableMap;",
            "class Test {",
            "  final Object foo() {",
            "    if (true) {",
            "      return ImmutableList.of();",
            "    } else {",
            "      return ImmutableMap.of();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void
      returnTypeList_multipleReturnStatementsImmutableMapImmutableBiMap_suggestsImmutableMap() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableBiMap;",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Map;",
            "class Test {",
            "  // BUG: Diagnostic contains: final ImmutableMap<String, String> foo()",
            "  final Map<String, String> foo() {",
            "    if (true) {",
            "      return ImmutableBiMap.of();",
            "    } else {",
            "      return ImmutableMap.of();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_insideAnonymousNested_suggestsImmutableList() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.inject.Provider;",
            "import java.util.List;",
            "class Test {",
            "  private static final int getFooLength() {",
            "    final Provider<List<String>> fooProvider = ",
            "      new Provider<List<String>>() {",
            "        @Override",
            "        // BUG: Diagnostic contains: ImmutableList<String> get",
            "        public List<String> get() {",
            "          return ImmutableList.of(\"foo\", \"bar\");",
            "        }",
            "      };",
            "    List<String> foo = fooProvider.get();",
            "    return foo.size();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnTypeList_anonymousLambda_suggestsNothing() {
    testHelper
        .addSourceLines(
            "ApplyInterface.java",
            "import java.util.function.Function;",
            "import java.util.List;",
            "public interface ApplyInterface {",
            "  int applyAndGetSize(Function<String, List<String>> fun);",
            "}")
        .addSourceLines(
            "ApplyImpl.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.function.Function;",
            "import java.util.List;",
            "public class ApplyImpl implements ApplyInterface {",
            "  public int applyAndGetSize(Function<String, List<String>> fun) {",
            "    List<String> result = fun.apply(\"foo,bar\");",
            "    return result.size();",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.function.Function;",
            "import java.util.List;",
            "class Test {",
            "  private static final ApplyInterface APPLY = new ApplyImpl();",
            "  private int doApply() {",
            "    int result = APPLY.applyAndGetSize(str -> {",
            "        ImmutableList<String> res = ImmutableList.of(str, str);",
            "        return res;",
            "      });",
            "    return result;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void overridingMethod_specialNotice() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.inject.Provider;",
            "import java.util.List;",
            "class Test {",
            "  private static final int getFooLength() {",
            "    final Provider<List<String>> fooProvider = ",
            "      new Provider<List<String>>() {",
            "        @Override",
            "        // BUG: Diagnostic contains: narrow the return type",
            "        public List<String> get() {",
            "          return ImmutableList.of(\"foo\", \"bar\");",
            "        }",
            "      };",
            "    List<String> foo = fooProvider.get();",
            "    return foo.size();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_iterable() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  final Iterable<String> foo() {",
            "    return ImmutableList.of();",
            "  }",
            "}")
        .doTest();
  }
}
