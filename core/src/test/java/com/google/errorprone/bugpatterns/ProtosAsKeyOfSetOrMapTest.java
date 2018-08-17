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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * {@link ProtosAsKeyOfSetOrMap}Test
 *
 * @author seibelsabrina@google.com (Sabrina Seibel)
 */
@RunWith(JUnit4.class)
public final class ProtosAsKeyOfSetOrMapTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ProtosAsKeyOfSetOrMap.class, getClass());

  @Ignore("b/74365407 test proto sources are broken")
  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "import java.util.Set;",
            "import java.util.Map;",
            "import java.util.LinkedHashMap;",
            "import java.util.HashMap;",
            "import java.util.HashSet;",
            "import java.util.Collection;",
            "import com.google.common.collect.Sets;",
            "import com.google.common.collect.Maps;",
            "import com.google.common.collect.HashMultiset;",
            "import com.google.common.collect.LinkedHashMultiset;",
            "import com.google.common.collect.HashBiMap;",
            "import com.google.common.collect.HashMultimap;",
            "import com.google.common.collect.LinkedHashMultimap;",
            "import com.google.common.collect.ArrayListMultimap;",
            "import com.google.common.collect.LinkedListMultimap;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "import com.google.protobuf.InvalidProtocolBufferException;",
            "import com.google.protobuf.ByteString;",
            "class Test {",
            "  void f(Collection<TestProtoMessage> x, TestProtoMessage m)"
                + " throws InvalidProtocolBufferException {",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    Map<TestProtoMessage, Integer> testNewMap = Maps.newHashMap();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    Set<TestProtoMessage> testNewSet = Sets.newHashSet();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    HashMap<TestProtoMessage, Integer> testNewHashMap = Maps.newHashMap();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    HashSet<TestProtoMessage> testNewHashSet = Sets.newHashSet();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    Map<TestProtoMessage, Integer> testMap = new HashMap<TestProtoMessage,"
                + "Integer>();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    Set<TestProtoMessage> testSet = new HashSet<TestProtoMessage>();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    HashMap<TestProtoMessage, Integer> testHashMap = new HashMap<TestProtoMessage,"
                + "Integer>();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    HashSet<TestProtoMessage> testHashSet = new HashSet<TestProtoMessage>();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    HashMultimap<TestProtoMessage, Integer> testHashMultimap ="
                + "HashMultimap.create();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    ArrayListMultimap<TestProtoMessage, Integer> testArrayListMultimap"
                + " = ArrayListMultimap.create();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    LinkedHashMultimap<TestProtoMessage, Integer> testLinkedHashMultimap"
                + "= LinkedHashMultimap.create();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    LinkedListMultimap<TestProtoMessage, Integer> testLinkedListMultimap"
                + "= LinkedListMultimap.create();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    HashBiMap<TestProtoMessage, Integer> testHashBiMap = HashBiMap.create();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    LinkedHashMap<TestProtoMessage, Integer> testLinkedHashMap"
                + "= new LinkedHashMap<TestProtoMessage, Integer>();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    LinkedHashMultiset<TestProtoMessage> testLinkedHashMultiSet"
                + "= LinkedHashMultiset.create();",
            "    // BUG: Diagnostic contains: ProtosAsKeyOfSetOrMap",
            "    HashMultiset<TestProtoMessage> testHashMultiSet = HashMultiset.create();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
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
