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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link DoubleBraceInitialization}Test */
@RunWith(JUnit4.class)
public class DoubleBraceInitializationTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new DoubleBraceInitialization(), getClass());

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(DoubleBraceInitialization.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  static final List<Integer> a = new ArrayList<Integer>();",
            "  static final Object o = new Object() {{",
            "    System.err.println(hashCode());",
            "  }};",
            "  static final List<Integer> b = new ArrayList<Integer>() {",
            "    {",
            "      add(1);",
            "    }",
            "    @Override public boolean add(Integer i) {",
            "      return true;",
            "    }",
            "  };",
            "  static final List<Integer> c = new ArrayList<Integer>() {",
            "    @Override public boolean add(Integer i) {",
            "      return true;",
            "    }",
            "  };",
            "}")
        .doTest();
  }

  @Test
  public void positiveNoFix() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "// BUG: Diagnostic contains:",
            "class Test {",
            "  static final List<Integer> b = new ArrayList<Integer>() {{",
            "    addAll(this);",
            "  }};",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void list() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  static final List<Integer> a = new ArrayList<Integer>() {{ add(1); add(2); }};",
            "  static final List<Integer> b = Collections.unmodifiableList(",
            "      new ArrayList<Integer>() {{ add(1); add(2); }});",
            "  List<Integer> c = new ArrayList<Integer>() {{ add(1); add(2); }};",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  static final ImmutableList<Integer> a = ImmutableList.of(1, 2);",
            "  static final ImmutableList<Integer> b = ImmutableList.of(1, 2);",
            "  List<Integer> c = new ArrayList<Integer>(ImmutableList.of(1, 2));",
            "}")
        .doTest();
  }

  @Test
  public void set() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.Collections;",
            "import java.util.HashSet;",
            "import java.util.Set;",
            "class Test {",
            "  static final Set<Integer> a = new HashSet<Integer>() {{ add(1); add(2); }};",
            "  static final Set<Integer> b = Collections.unmodifiableSet(",
            "      new HashSet<Integer>() {{ add(1); add(2); }});",
            "  Set<Integer> c = new HashSet<Integer>() {{ add(1); add(2); }};",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Collections;",
            "import java.util.HashSet;",
            "import java.util.Set;",
            "class Test {",
            "  static final ImmutableSet<Integer> a = ImmutableSet.of(1, 2);",
            "  static final ImmutableSet<Integer> b = ImmutableSet.of(1, 2);",
            "  Set<Integer> c = new HashSet<Integer>(ImmutableSet.of(1, 2));",
            "}")
        .doTest();
  }

  @Test
  public void collection() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.ArrayDeque;",
            "import java.util.Collection;",
            "import java.util.Collections;",
            "import java.util.Deque;",
            "class Test {",
            "  static final Collection<Integer> a =",
            "      new ArrayDeque<Integer>() {{ add(1); add(2); }};",
            "  static final Collection<Integer> b = Collections.unmodifiableCollection(",
            "      new ArrayDeque<Integer>() {{ add(1); add(2); }});",
            "  Deque<Integer> c = new ArrayDeque<Integer>() {{ add(1); add(2); }};",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.collect.ImmutableCollection;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayDeque;",
            "import java.util.Collection;",
            "import java.util.Collections;",
            "import java.util.Deque;",
            "class Test {",
            "  static final ImmutableCollection<Integer> a = ImmutableList.of(1, 2);",
            "  static final ImmutableCollection<Integer> b = ImmutableList.of(1, 2);",
            "  Deque<Integer> c = new ArrayDeque<Integer>(ImmutableList.of(1, 2));",
            "}")
        .doTest();
  }

  @Test
  public void map() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.Collections;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "class Test {",
            "  static final Map<Integer, String> a = new HashMap<Integer, String>() {{",
            "    put(1, \"a\"); put(2, \"b\"); ",
            "  }};",
            "  static final Map<Integer, String> b =",
            "      Collections.unmodifiableMap(new HashMap<Integer, String>() {{",
            "    put(1, \"a\"); put(2, \"b\"); ",
            "  }});",
            "  Map<Integer, String> c = new HashMap<Integer, String>() {{",
            "    put(1, \"a\"); put(2, \"b\"); ",
            "  }};",
            "  static final Map<Integer, String> d = new HashMap<Integer, String>() {{",
            "    put(1, \"a\");",
            "    put(2, \"b\"); ",
            "    put(3, \"c\"); ",
            "    put(4, \"d\"); ",
            "    put(5, \"e\"); ",
            "    put(6, \"f\"); ",
            "  }};",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Collections;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "class Test {",
            "  static final ImmutableMap<Integer, String> a =",
            "    ImmutableMap.of(1, \"a\", 2, \"b\");",
            "  static final ImmutableMap<Integer, String> b =",
            "    ImmutableMap.of(1, \"a\", 2, \"b\");",
            "  Map<Integer, String> c =",
            "    new HashMap<Integer, String>(ImmutableMap.of(1, \"a\", 2, \"b\"));",
            "  static final ImmutableMap<Integer, String> d =",
            "      ImmutableMap.<Integer, String>builder()",
            "          .put(1, \"a\")",
            "          .put(2, \"b\")",
            "          .put(3, \"c\")",
            "          .put(4, \"d\")",
            "          .put(5, \"e\")",
            "          .put(6, \"f\")",
            "          .build();",
            "}")
        .doTest();
  }

  @Test
  public void nulls() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.*;",
            "// BUG: Diagnostic contains:",
            "class Test {",
            "  static final List<Integer> a = new ArrayList<Integer>() {{ add(null); }};",
            "  static final Set<Integer> b = new HashSet<Integer>() {{ add(null); }};",
            "  static final Map<String, Integer> c =",
            "      new HashMap<String, Integer>() {{ put(null, null); }};",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void returned() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "class Test {",
            "  private Map<String, Object> test() {",
            "    return Collections.unmodifiableMap(new HashMap<String, Object>() {",
            "      {}",
            "    });",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Collections;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "class Test {",
            "  private ImmutableMap<String, Object> test() {",
            "    return ImmutableMap.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lambda() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "import java.util.function.Supplier;",
            "class Test {",
            "  private Supplier<Map<String, Object>> test() {",
            "    return () -> Collections.unmodifiableMap(new HashMap<String, Object>() {",
            "      {}",
            "    });",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Collections;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "import java.util.function.Supplier;",
            "class Test {",
            "  private Supplier<Map<String, Object>> test() {",
            "    return () -> ImmutableMap.of();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void statement() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "class Test {",
            "  private void test() {",
            "    Collections.unmodifiableMap(new HashMap<String, Object>() {",
            "      {}",
            "    });",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Collections;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "class Test {",
            "  private void test() {",
            "    ImmutableMap.of();",
            "  }",
            "}")
        .doTest();
  }
}
