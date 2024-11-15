/*
 * Copyright 2017 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author anishvisaria98@gmail.com (Anish Visaria)
 */
@RunWith(JUnit4.class)
public class ModifyCollectionInEnhancedForLoopTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ModifyCollectionInEnhancedForLoop.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "ModifyCollectionInEnhancedForLoopPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns;

            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.LinkedList;
            import java.util.Map;

            /**
             * @author anishvisaria98@gmail.com (Anish Visaria)
             */
            public class ModifyCollectionInEnhancedForLoopPositiveCases {

              public static void testBasic(ArrayList<Integer> arr, HashSet<Integer> set) {
                for (Integer a : arr) {
                  // BUG: Diagnostic contains:
                  arr.add(new Integer("42"));
                  // BUG: Diagnostic contains:
                  arr.addAll(set);
                  // BUG: Diagnostic contains:
                  arr.clear();
                  // BUG: Diagnostic contains:
                  arr.remove(a);
                  // BUG: Diagnostic contains:
                  arr.removeAll(set);
                  // BUG: Diagnostic contains:
                  arr.retainAll(set);
                }
              }

              public static void testNested(ArrayList<Integer> arr, LinkedList<Integer> list) {
                for (Integer x : arr) {
                  for (Integer y : list) {
                    // BUG: Diagnostic contains:
                    arr.add(y);
                    // BUG: Diagnostic contains:
                    arr.addAll(list);
                    // BUG: Diagnostic contains:
                    arr.clear();
                    // BUG: Diagnostic contains:
                    arr.remove(x);
                    // BUG: Diagnostic contains:
                    arr.removeAll(list);
                    // BUG: Diagnostic contains:
                    arr.retainAll(list);
                    // BUG: Diagnostic contains:
                    list.add(x);
                    // BUG: Diagnostic contains:
                    list.addAll(arr);
                    // BUG: Diagnostic contains:
                    list.clear();
                    // BUG: Diagnostic contains:
                    list.remove(y);
                    // BUG: Diagnostic contains:
                    list.removeAll(arr);
                    // BUG: Diagnostic contains:
                    list.retainAll(arr);
                  }
                }
              }

              public static void testMapKeySet(HashMap<Integer, Integer> map) {
                for (Integer a : map.keySet()) {
                  // BUG: Diagnostic contains:
                  map.putIfAbsent(new Integer("42"), new Integer("43"));
                  // BUG: Diagnostic contains:
                  map.clear();
                  // BUG: Diagnostic contains:
                  map.remove(a);
                }
              }

              public static void testMapValues(HashMap<Integer, Integer> map) {
                for (Integer a : map.values()) {
                  // BUG: Diagnostic contains:
                  map.putIfAbsent(new Integer("42"), new Integer("43"));
                  // BUG: Diagnostic contains:
                  map.putIfAbsent(new Integer("42"), a);
                  // BUG: Diagnostic contains:
                  map.clear();
                }
              }

              public static void testMapEntrySet(HashMap<Integer, Integer> map) {
                for (Map.Entry<Integer, Integer> a : map.entrySet()) {
                  // BUG: Diagnostic contains:
                  map.putIfAbsent(new Integer("42"), new Integer("43"));
                  // BUG: Diagnostic contains:
                  map.clear();
                  // BUG: Diagnostic contains:
                  map.remove(a.getKey());
                }
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "ModifyCollectionInEnhancedForLoopNegativeCases.java",
            """
package com.google.errorprone.bugpatterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author anishvisaria98@gmail.com (Anish Visaria)
 */
public class ModifyCollectionInEnhancedForLoopNegativeCases {

  public static void testBasic(ArrayList<Integer> arr, HashSet<Integer> set) {
    for (Integer a : arr) {
      set.add(a);
      set.addAll(arr);
      set.clear();
      set.removeAll(arr);
      set.retainAll(arr);
    }

    for (Integer i : set) {
      arr.add(i);
      arr.addAll(set);
      arr.clear();
      arr.removeAll(set);
      arr.retainAll(set);
    }
  }

  public static void testNested(ArrayList<Integer> arr, LinkedList<Integer> list) {
    for (Integer x : arr) {
      for (Integer y : list) {}

      list.add(x);
      list.addAll(arr);
      list.clear();
      list.removeAll(arr);
      list.retainAll(arr);
    }
  }

  public static void testBreakOutOfLoop(ArrayList<Integer> xs) {
    for (Integer x : xs) {
      xs.remove(x);
      return;
    }
    for (Integer x : xs) {
      xs.remove(x);
      System.err.println();
      break;
    }
  }

  public static void testMapKeySet(HashMap<Integer, Integer> map1, HashMap<Integer, Integer> map2) {
    for (Integer a : map1.keySet()) {
      map2.putIfAbsent(Integer.parseInt("42"), Integer.parseInt("43"));
      map2.clear();
      map2.remove(a);
    }
  }

  public static void testMapValues(HashMap<Integer, Integer> map1, HashMap<Integer, Integer> map2) {
    for (Integer a : map1.values()) {
      map2.putIfAbsent(Integer.parseInt("42"), a);
      map2.clear();
      map2.remove(Integer.parseInt("42"));
    }
  }

  public static void testMapEntrySet(
      HashMap<Integer, Integer> map1, HashMap<Integer, Integer> map2) {
    for (Map.Entry<Integer, Integer> a : map1.entrySet()) {
      map2.putIfAbsent(Integer.parseInt("42"), Integer.parseInt("43"));
      map2.clear();
      map2.remove(a.getKey());
    }
  }

  private static void concurrent() {
    CopyOnWriteArrayList<Integer> cowal = new CopyOnWriteArrayList<>();
    for (int i : cowal) {
      cowal.remove(i);
    }
  }

  interface MyBlockingQueue<T> extends BlockingQueue<T> {}

  private static void customConcurrent(MyBlockingQueue<Integer> mbq) {
    for (Integer i : mbq) {
      mbq.add(i);
    }
  }
}\
""")
        .doTest();
  }

  @Test
  public void modifyCollectionInItself() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.AbstractCollection;
            import java.util.Collection;

            abstract class Test<E> extends AbstractCollection<E> {
              public boolean addAll(Collection<? extends E> c) {
                boolean modified = false;
                for (E e : c) if (add(e)) modified = true;
                return modified;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void concurrentMap() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Map;
            import java.util.concurrent.ConcurrentMap;

            class Test {
              void f(ConcurrentMap<String, Integer> map) {
                for (Map.Entry<String, Integer> e : map.entrySet()) {
                  map.remove(e.getKey());
                }
              }
            }
            """)
        .doTest();
  }
}
