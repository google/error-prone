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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * {@link ArrayAsKeyOfSetOrMap}Test
 *
 * @author siyuanl@google.com (Siyuan Liu)
 * @author eleanorh@google.com (Eleanor Harris) /
 *     <p>/** {@link ArrayAsKeyOfSetOrMap}Test
 */
@RunWith(JUnit4.class)
public final class ArrayAsKeyOfSetOrMapTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ArrayAsKeyOfSetOrMap.class, getClass());
  }

  @Test
  public void positive() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "import java.util.Set;",
            "import java.util.Map;",
            "import java.util.LinkedHashMap;",
            "import com.google.common.collect.Sets;",
            "import com.google.common.collect.Maps;",
            "import com.google.common.collect.HashMultiset;",
            "import com.google.common.collect.LinkedHashMultiset;",
            "import com.google.common.collect.HashBiMap;",
            "import com.google.common.collect.HashMultimap;",
            "import com.google.common.collect.LinkedHashMultimap;",
            "import com.google.common.collect.ArrayListMultimap;",
            "import com.google.common.collect.LinkedListMultimap;",
            "import java.util.HashMap;",
            "import java.util.HashSet;",
            "class Test{",
            "  public static void main(String[] args) {",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    Map<String[], Integer> testNewMap = Maps.newHashMap();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    Set<String[]> testNewSet = Sets.newHashSet();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    HashMap<String[], Integer> testNewHashMap = Maps.newHashMap();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    HashSet<String[]> testNewHashSet = Sets.newHashSet();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    Map<String[], Integer> testMap = new HashMap<String[], Integer>();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    Set<String[]> testSet = new HashSet<String[]>();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    HashMap<String[], Integer> testHashMap = new HashMap<String[], Integer>();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    HashSet<String[]> testHashSet = new HashSet<String[]>();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    HashMultimap<String[], Integer> testHashMultimap = HashMultimap.create();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    ArrayListMultimap<String[], Integer> testArrayListMultimap"
                + " = ArrayListMultimap.create();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    LinkedHashMultimap<String[], Integer> testLinkedHashMultimap"
                + "= LinkedHashMultimap.create();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    LinkedListMultimap<String[], Integer> testLinkedListMultimap"
                + "= LinkedListMultimap.create();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    HashBiMap<String[], Integer> testHashBiMap = HashBiMap.create();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    LinkedHashMap<String[], Integer> testLinkedHashMap"
                + "= new LinkedHashMap<String[], Integer>();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    HashMultiset<String[]> testHashMultiSet = HashMultiset.create();",
            "    // BUG: Diagnostic contains: ArrayAsKeyOfSetOrMap",
            "    LinkedHashMultiset<String[]> testLinkedHashMultiSet"
                + "= LinkedHashMultiset.create();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "import java.util.Set;",
            "import java.util.Map;",
            "import java.util.LinkedHashMap;",
            "import com.google.common.collect.Sets;",
            "import com.google.common.collect.Maps;",
            "import java.util.HashMap;",
            "import java.util.HashSet;",
            "import java.util.TreeSet;",
            "import com.google.common.collect.HashMultiset;",
            "import com.google.common.collect.LinkedHashMultiset;",
            "import com.google.common.collect.HashBiMap;",
            "import com.google.common.collect.HashMultimap;",
            "import com.google.common.collect.LinkedHashMultimap;",
            "import com.google.common.collect.ArrayListMultimap;",
            "import com.google.common.collect.LinkedListMultimap;",
            "import com.google.common.collect.Ordering;",
            "class Test {",
            "  public static void main(String[] args) {",
            "    Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();",
            "    Set<String> testSet = new HashSet<String>();",
            "    HashMap<Integer, Integer> testHashMap = new HashMap<Integer, Integer>();",
            "    HashSet<String> testHashSet = new HashSet<String>();",
            "    Set testSet2 = new HashSet();",
            "    Map testMap2 = new HashMap();",
            "    Map<Integer, Integer> mapFromMethod = Maps.newHashMap();",
            "    Set<String> setFromMethod = Sets.newHashSet();",
            "    Set<String[]> thisShouldWork = new TreeSet<String[]>"
                + "(Ordering.natural().lexicographical().onResultOf(Arrays::asList));",
            "    HashMultimap<String, Integer> testHashMultimap = HashMultimap.create();",
            "    ArrayListMultimap<String, Integer> testArrayListMultimap"
                + " = ArrayListMultimap.create();",
            "    LinkedHashMultimap<String, Integer> testLinkedHashMultimap"
                + "= LinkedHashMultimap.create();",
            "    LinkedListMultimap<String, Integer> testLinkedListMultimap"
                + "= LinkedListMultimap.create();",
            "    HashBiMap<String, Integer> testHashBiMap = HashBiMap.create();",
            "    LinkedHashMap<String, Integer> testLinkedHashMap"
                + "= new LinkedHashMap<String, Integer>();",
            "    HashMultiset<String> testHashMultiSet = HashMultiset.create();",
            "    LinkedHashMultiset<String> testLinkedHashMultiSet"
                + "= LinkedHashMultiset.create();",
            "  }",
            "}")
        .doTest();
  }
}
