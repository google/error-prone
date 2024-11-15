/*
 * Copyright 2012 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@RunWith(JUnit4.class)
public class CollectionIncompatibleTypeTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CollectionIncompatibleType.class, getClass());

  private final BugCheckerRefactoringTestHelper refactorTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(CollectionIncompatibleType.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "CollectionIncompatibleTypePositiveCases.java",
            """
package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

import com.google.common.collect.ClassToInstanceMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/** Positive test cases for {@link CollectionIncompatibleType}. */
public class CollectionIncompatibleTypePositiveCases {

  /* Tests for API coverage */

  public void collection(Collection<Integer> collection1, Collection<String> collection2) {
    // BUG: Diagnostic contains: Argument '"bad"' should not be passed to this method
    // its type String is not compatible with its collection's type argument Integer
    collection1.contains("bad");
    // BUG: Diagnostic contains:
    collection1.remove("bad");
    // BUG: Diagnostic contains: Argument 'collection2' should not be passed to this method
    // its type Collection<String> has a type argument String that is not compatible with its
    // collection's type argument Integer
    collection1.containsAll(collection2);
    // BUG: Diagnostic contains:
    collection1.removeAll(collection2);
    // BUG: Diagnostic contains:
    collection1.retainAll(collection2);
  }

  public void collectionSubtype(ArrayList<Integer> arrayList1, ArrayList<String> arrayList2) {
    // BUG: Diagnostic contains: Argument '"bad"' should not be passed to this method
    // its type String is not compatible with its collection's type argument Integer
    arrayList1.contains("bad");
    // BUG: Diagnostic contains:
    arrayList1.remove("bad");
    // BUG: Diagnostic contains: Argument 'arrayList2' should not be passed to this method
    // its type ArrayList<String> has a type argument String that is not compatible with its
    // collection's type argument Integer
    arrayList1.containsAll(arrayList2);
    // BUG: Diagnostic contains:
    arrayList1.removeAll(arrayList2);
    // BUG: Diagnostic contains:
    arrayList1.retainAll(arrayList2);
  }

  public boolean deque(Deque<Integer> deque) {
    // BUG: Diagnostic contains:
    boolean result = deque.removeFirstOccurrence("bad");
    // BUG: Diagnostic contains:
    return result && deque.removeLastOccurrence("bad");
  }

  public boolean dequeSubtype(LinkedList<Integer> linkedList) {
    // BUG: Diagnostic contains:
    boolean result = linkedList.removeFirstOccurrence("bad");
    // BUG: Diagnostic contains:
    return result && linkedList.removeLastOccurrence("bad");
  }

  public String dictionary(Dictionary<Integer, String> dictionary) {
    // BUG: Diagnostic contains:
    String result = dictionary.get("bad");
    // BUG: Diagnostic contains:
    return result + dictionary.remove("bad");
  }

  public String dictionarySubtype(Hashtable<Integer, String> hashtable) {
    // BUG: Diagnostic contains:
    String result = hashtable.get("bad");
    // BUG: Diagnostic contains:
    return result + hashtable.remove("bad");
  }

  public int list() {
    List<String> list = new ArrayList<String>();
    // BUG: Diagnostic contains:
    int result = list.indexOf(1);
    // BUG: Diagnostic contains:
    return result + list.lastIndexOf(1);
  }

  public void listSubtype() {
    ArrayList<String> arrayList = new ArrayList<>();
    // BUG: Diagnostic contains:
    int result = arrayList.indexOf(1);
    // BUG: Diagnostic contains:
    result = arrayList.lastIndexOf(1);
  }

  public boolean map() {
    Map<Integer, String> map = new HashMap<>();
    // BUG: Diagnostic contains:
    String result = map.get("bad");
    // BUG: Diagnostic contains:
    result = map.getOrDefault("bad", "soBad");
    // BUG: Diagnostic contains:
    boolean result2 = map.containsKey("bad");
    // BUG: Diagnostic contains:
    result2 = map.containsValue(1);
    // BUG: Diagnostic contains:
    result = map.remove("bad");
    return false;
  }

  public boolean mapSubtype() {
    ConcurrentNavigableMap<Integer, String> concurrentNavigableMap = new ConcurrentSkipListMap<>();
    // BUG: Diagnostic contains:
    String result = concurrentNavigableMap.get("bad");
    // BUG: Diagnostic contains:
    boolean result2 = concurrentNavigableMap.containsKey("bad");
    // BUG: Diagnostic contains:
    result2 = concurrentNavigableMap.containsValue(1);
    // BUG: Diagnostic contains:
    result = concurrentNavigableMap.remove("bad");
    return false;
  }

  public int stack(Stack<Integer> stack) {
    // BUG: Diagnostic contains:
    return stack.search("bad");
  }

  private static class MyStack<E> extends Stack<E> {}

  public int stackSubtype(MyStack<Integer> myStack) {
    // BUG: Diagnostic contains:
    return myStack.search("bad");
  }

  public int vector(Vector<Integer> vector) {
    // BUG: Diagnostic contains:
    int result = vector.indexOf("bad", 0);
    // BUG: Diagnostic contains:
    return result + vector.lastIndexOf("bad", 0);
  }

  public int vectorSubtype(Stack<Integer> stack) {
    // BUG: Diagnostic contains:
    int result = stack.indexOf("bad", 0);
    // BUG: Diagnostic contains:
    return result + stack.lastIndexOf("bad", 0);
  }

  /* Tests for behavior */

  public boolean errorMessageUsesSimpleNames(Collection<Integer> collection) {
    // BUG: Diagnostic contains: Argument '"bad"' should not be passed to this method
    // its type String is not compatible with its collection's type argument Integer
    return collection.contains("bad");
  }

  private static class Date {}

  public boolean errorMessageUsesFullyQualifedNamesWhenSimpleNamesAreTheSame(
      Collection<java.util.Date> collection1, Collection<Date> collection2) {
    // BUG: Diagnostic contains: Argument 'new Date()' should not be passed to this method
    // its type
    // com.google.errorprone.bugpatterns.collectionincompatibletype.testdata.CollectionIncompatibleTypePositiveCases.Date is not compatible with its collection's type argument java.util.Date
    return collection1.contains(new Date());
  }

  public boolean boundedWildcard() {
    Collection<? extends Date> collection = new ArrayList<>();
    // BUG: Diagnostic contains:
    return collection.contains("bad");
  }

  private static class Pair<A, B> {
    public A first;
    public B second;
  }

  public boolean declaredTypeVsExpressionType(Pair<Integer, String> pair, List<Integer> list) {
    // BUG: Diagnostic contains:
    return list.contains(pair.second);
  }

  public String subclassHasDifferentTypeParameters(ClassToInstanceMap<String> map, String s) {
    // BUG: Diagnostic contains:
    return map.get(s);
  }

  private static class MyArrayList extends ArrayList<Integer> {}

  public void methodArgumentIsSubclassWithDifferentTypeParameters(
      Collection<String> collection, MyArrayList myArrayList) {
    // BUG: Diagnostic contains:
    collection.containsAll(myArrayList);
  }

  private static class IncompatibleBounds<K extends String, V extends Number> {
    private boolean function(Map<K, V> map, K key) {
      // BUG: Diagnostic contains:
      return map.containsValue(key);
    }
  }

  interface Interface {}

  private static final class FinalClass1 {}

  private static final class FinalClass2 {}

  private static class NonFinalClass1 {}

  private static class NonFinalClass2 {}

  public boolean oneInterfaceAndOneFinalClass(
      Collection<Interface> collection, FinalClass1 finalClass1) {
    // BUG: Diagnostic contains:
    return collection.contains(finalClass1);
  }

  public boolean oneFinalClassAndOneInterface(Collection<FinalClass1> collection, Interface iface) {
    // BUG: Diagnostic contains:
    return collection.contains(iface);
  }

  public boolean bothNonFinalClasses(
      Collection<NonFinalClass1> collection, NonFinalClass2 nonFinalClass2) {
    // BUG: Diagnostic contains:
    return collection.contains(nonFinalClass2);
  }

  public boolean bothFinalClasses(Collection<FinalClass1> collection, FinalClass2 finalClass2) {
    // BUG: Diagnostic contains:
    return collection.contains(finalClass2);
  }

  public boolean oneNonFinalClassAndOneFinalClass(
      Collection<NonFinalClass1> collection, FinalClass1 finalClass1) {
    // BUG: Diagnostic contains:
    return collection.contains(finalClass1);
  }

  public boolean oneFinalClassAndOneNonFinalClass(
      Collection<FinalClass1> collection, NonFinalClass1 nonFinalClass1) {
    // BUG: Diagnostic contains:
    return collection.contains(nonFinalClass1);
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "CollectionIncompatibleTypeNegativeCases.java",
            """
package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

import com.google.common.base.Optional;
import com.google.common.collect.ClassToInstanceMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/** Negative test cases for {@link CollectionIncompatibleType}. */
public class CollectionIncompatibleTypeNegativeCases {

  /* Tests for API coverage */

  public boolean collection(Collection<String> collection1, Collection<String> collection2) {
    boolean result = collection1.contains("ok");
    result &= collection1.contains(null);
    result &= collection1.remove("ok");
    result &= collection1.remove(null);
    result &= collection1.containsAll(collection2);
    result &= collection1.containsAll(null);
    result &= collection1.removeAll(collection2);
    result &= collection1.removeAll(null);
    result &= collection1.retainAll(collection2);
    return result && collection1.retainAll(null);
  }

  public boolean collectionSubtype(ArrayList<String> arrayList1, ArrayList<String> arrayList2) {
    boolean result = arrayList1.contains("ok");
    result &= arrayList1.contains(null);
    result &= arrayList1.remove("ok");
    result &= arrayList1.remove(null);
    result &= arrayList1.containsAll(arrayList2);
    result &= arrayList1.containsAll(null);
    result &= arrayList1.removeAll(arrayList2);
    result &= arrayList1.removeAll(null);
    result &= arrayList1.retainAll(arrayList2);
    return result && arrayList1.retainAll(null);
  }

  public boolean deque(Deque<String> deque) {
    boolean result = deque.removeFirstOccurrence("ok");
    result &= deque.removeFirstOccurrence(null);
    result &= deque.removeLastOccurrence("ok");
    return result && deque.removeLastOccurrence(null);
  }

  public boolean dequeSubtype(LinkedList<String> linkedList) {
    boolean result = linkedList.removeFirstOccurrence("ok");
    result &= linkedList.removeFirstOccurrence(null);
    result &= linkedList.removeLastOccurrence("ok");
    return result && linkedList.removeLastOccurrence(null);
  }

  public int dictionary(Dictionary<String, Integer> dictionary) {
    int result = dictionary.get("ok");
    result += dictionary.get(null);
    result += dictionary.remove("ok");
    return result + dictionary.remove(null);
  }

  public int dictionarySubtype(Hashtable<String, Integer> hashtable) {
    int result = hashtable.get("ok");
    result += hashtable.get(null);
    result += hashtable.remove("ok");
    return result + hashtable.remove(null);
  }

  public int list() {
    List<String> list = new ArrayList<String>();
    int result = list.indexOf("ok");
    result += list.indexOf(null);
    result += list.lastIndexOf("ok");
    return result + list.lastIndexOf(null);
  }

  public int listSubtype() {
    ArrayList<String> arrayList = new ArrayList<>();
    int result = arrayList.indexOf("ok");
    result += arrayList.indexOf(null);
    result += arrayList.lastIndexOf("ok");
    return result + arrayList.lastIndexOf(null);
  }

  public boolean map() {
    Map<Integer, String> map = new HashMap<>();
    String result = map.get(1);
    result = map.getOrDefault(1, "hello");
    boolean result2 = map.containsKey(1);
    result2 = map.containsValue("ok");
    result2 &= map.containsValue(null);
    result = map.remove(1);
    return result2;
  }

  public boolean mapSubtype() {
    ConcurrentNavigableMap<Integer, String> concurrentNavigableMap = new ConcurrentSkipListMap<>();
    String result = concurrentNavigableMap.get(1);
    boolean result2 = concurrentNavigableMap.containsKey(1);
    result2 &= concurrentNavigableMap.containsValue("ok");
    result2 &= concurrentNavigableMap.containsValue(null);
    result = concurrentNavigableMap.remove(1);
    return result2;
  }

  public int stack(Stack<String> stack) {
    int result = stack.search("ok");
    return result + stack.search(null);
  }

  private static class MyStack<E> extends Stack<E> {}

  public int stackSubtype(MyStack<String> myStack) {
    int result = myStack.search("ok");
    return result + myStack.search(null);
  }

  public int vector(Vector<String> vector) {
    int result = vector.indexOf("ok", 0);
    result += vector.indexOf(null, 0);
    result += vector.lastIndexOf("ok", 0);
    return result + vector.lastIndexOf(null, 0);
  }

  public int vectorSubtype(Stack<String> stack) {
    int result = stack.indexOf("ok", 0);
    result += stack.indexOf(null, 0);
    result += stack.lastIndexOf("ok", 0);
    return result + stack.lastIndexOf(null, 0);
  }

  /* Tests for behavior */

  private class B extends Date {}

  public boolean argTypeExtendsContainedType() {
    Collection<Date> collection = new ArrayList<>();
    return collection.contains(new B());
  }

  public boolean containedTypeExtendsArgType() {
    Collection<String> collection = new ArrayList<>();
    Object actuallyAString = "ok";
    return collection.contains(actuallyAString);
  }

  public boolean boundedWildcard() {
    Collection<? extends Date> collection = new ArrayList<>();
    return collection.contains(new Date()) || collection.contains(new B());
  }

  public boolean unboundedWildcard() {
    Collection<?> collection = new ArrayList<>();
    return collection.contains("ok") || collection.contains(new Object());
  }

  public boolean rawType() {
    Collection collection = new ArrayList();
    return collection.contains("ok");
  }

  private class DoesntExtendCollection<E> {
    public boolean contains(Object o) {
      return true;
    }
  }

  public boolean doesntExtendCollection() {
    DoesntExtendCollection<String> collection = new DoesntExtendCollection<>();
    return collection.contains(new Date());
  }

  private static class Pair<A, B> {
    public A first;
    public B second;
  }

  public boolean declaredTypeVsExpressionType(Pair<Integer, String> pair, List<Integer> list) {
    return list.contains(pair.first);
  }

  public boolean containsParameterizedType(
      Collection<Class<? extends String>> collection, Class<?> clazz) {
    return collection.contains(clazz);
  }

  public boolean containsWildcard(Collection<String> collection, Optional<?> optional) {
    return collection.contains(optional.get());
  }

  public <T extends String> T subclassHasDifferentTypeParameters(
      ClassToInstanceMap<String> map, Class<T> klass) {
    return klass.cast(map.get(klass));
  }

  // Ensure we don't match Hashtable.contains and ConcurrentHashtable.contains because there is a
  // separate check, HashtableContains, specifically for them.
  public boolean hashtableContains() {
    Hashtable<Integer, String> hashtable = new Hashtable<>();
    ConcurrentHashMap<Integer, String> concurrentHashMap = new ConcurrentHashMap<>();
    return hashtable.contains(1) || concurrentHashMap.contains(1);
  }

  private static class MyHashMap<K extends Integer, V extends String> extends HashMap<K, V> {}

  public boolean boundedTypeParameters(MyHashMap<?, ?> myHashMap) {
    return myHashMap.containsKey(1);
  }

  interface Interface1 {}

  interface Interface2 {}

  private static class NonFinalClass {}

  public boolean bothInterfaces(Collection<Interface1> collection, Interface2 iface2) {
    return collection.contains(iface2);
  }

  public boolean oneInterfaceAndOneNonFinalClass(
      Collection<Interface1> collection, NonFinalClass nonFinalClass) {
    return collection.contains(nonFinalClass);
  }

  public boolean oneNonFinalClassAndOneInterface(
      Collection<NonFinalClass> collection, Interface1 iface) {
    return collection.contains(iface);
  }

  public void methodArgHasSubtypeTypeArgument(
      Collection<Number> collection1, Collection<Integer> collection2) {
    collection1.containsAll(collection2);
  }

  public void methodArgHasSuperTypeArgument(
      Collection<Integer> collection1, Collection<Number> collection2) {
    collection1.containsAll(collection2);
  }

  public void methodArgHasWildcardTypeArgument(
      Collection<? extends Number> collection1, Collection<? extends Integer> collection2) {
    collection1.containsAll(collection2);
  }

  public void methodArgCastToCollectionWildcard(
      Collection<Integer> collection1, Collection<String> collection2) {
    collection1.containsAll((Collection<?>) collection2);
  }

  public void classToken(
      Set<Class<? extends Iterable<?>>> iterables, Class<ArrayList> arrayListClass) {
    iterables.contains(arrayListClass);
  }
}\
""")
        .doTest();
  }

  @Test
  public void outOfBounds() {
    compilationHelper
        .addSourceLines(
            "CollectionIncompatibleTypeOutOfBounds.java",
            """
            package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

            import java.util.Properties;

            /** This is a regression test for Issue 222. */
            public class CollectionIncompatibleTypeOutOfBounds {
              public void test() {
                Properties properties = new Properties();
                properties.get("");
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void classCast() {
    compilationHelper
        .addSourceLines(
            "CollectionIncompatibleTypeClassCast.java",
            """
            package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

            import java.util.HashMap;

            /** This is a regression test for Issue 222. */
            public class CollectionIncompatibleTypeClassCast<K, V> extends HashMap<K, V> {
              public void test(K k) {
                get(k);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void castFixes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Collection;

            public class Test {
              public void doIt(Collection<String> c1, Collection<Integer> c2) {
                // BUG: Diagnostic contains: c1.contains((Object) 1);
                c1.contains(1);
                // BUG: Diagnostic contains: c1.containsAll((Collection<?>) c2);
                c1.containsAll(c2);
              }
            }
            """)
        .setArgs(ImmutableList.of("-XepOpt:CollectionIncompatibleType:FixType=CAST"))
        .doTest();
  }

  @Test
  public void suppressWarningsFix() {
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            """
            import java.util.Collection;

            public class Test {
              public void doIt(Collection<String> c1, Collection<Integer> c2) {
                c1.contains(1);
                c1.containsAll(c2);
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            "import java.util.Collection;",
            "public class Test {",
            "  @SuppressWarnings(\"CollectionIncompatibleType\")",
            // In this test environment, the fix doesn't include formatting
            "public void doIt(Collection<String> c1, Collection<Integer> c2) {",
            "    c1.contains(/* expected: String, actual: int */ 1);",
            "    c1.containsAll(/* expected: String, actual: Integer */ c2);",
            "  }",
            "}")
        .setArgs("-XepOpt:CollectionIncompatibleType:FixType=SUPPRESS_WARNINGS")
        .doTest(TestMode.TEXT_MATCH);
  }

  // This test is disabled because calling Types#asSuper in the check removes the upper bound on K.
  @Test
  @Ignore
  public void boundedTypeParameters() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
import java.util.HashMap;

public class Test {
  private static class MyHashMap<K extends Integer, V extends String> extends HashMap<K, V> {}

  public boolean boundedTypeParameters(MyHashMap<?, ?> myHashMap) {
    // BUG: Diagnostic contains:
    return myHashMap.containsKey("bad");
  }
}
""")
        .doTest();
  }

  @Test
  public void disjoint() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Collections;
            import java.util.List;

            public class Test {
              void f(List<String> a, List<String> b) {
                Collections.disjoint(a, b);
              }

              void g(List<String> a, List<Integer> b) {
                // BUG: Diagnostic contains: not compatible
                Collections.disjoint(a, b);
              }

              void h(List<?> a, List<Integer> b) {
                Collections.disjoint(a, b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void difference() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Set;
            import com.google.common.collect.Sets;

            public class Test {
              void f(Set<String> a, Set<String> b) {
                Sets.difference(a, b);
              }

              void g(Set<String> a, Set<Integer> b) {
                // BUG: Diagnostic contains: not compatible
                Sets.difference(a, b);
              }

              void h(Set<?> a, Set<Integer> b) {
                Sets.difference(a, b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReference() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            public class Test {
              java.util.stream.Stream filter(List<Integer> xs, List<String> ss) {
                // BUG: Diagnostic contains:
                return xs.stream().filter(ss::contains);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReferenceBinOp() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            public class Test {
              void removeAll(List<List<Integer>> xs, List<String> ss) {
                // BUG: Diagnostic contains:
                xs.forEach(ss::removeAll);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReference_compatibleType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            public class Test {
              java.util.stream.Stream filter(List<Integer> xs, List<Object> ss) {
                return xs.stream().filter(ss::contains);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void memberReferenceWithBoundedGenerics() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.Sets;
            import java.util.function.BiFunction;
            import java.util.Set;

            public class Test {
              <T extends String, M extends Integer> void a(BiFunction<Set<T>, Set<M>, Set<T>> b) {}

              void b() {
                // BUG: Diagnostic contains:
                a(Sets::difference);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void memberReferenceWithBoundedGenericsDependentOnEachOther() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.Sets;
            import java.util.function.BiFunction;
            import java.util.Set;

            public class Test {
              <T extends String, M extends T> void a(BiFunction<Set<T>, Set<M>, Set<T>> b) {}

              void b() {
                a(Sets::difference);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void memberReferenceWithConcreteIncompatibleTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.Sets;
            import java.util.function.BiFunction;
            import java.util.Set;

            public class Test {
              void a(BiFunction<Set<Integer>, Set<String>, Set<Integer>> b) {}

              void b() {
                // BUG: Diagnostic contains:
                a(Sets::difference);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void memberReferenceWithConcreteCompatibleTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.Sets;
            import java.util.function.BiFunction;
            import java.util.Set;

            public class Test {
              void a(BiFunction<Set<Integer>, Set<Number>, Set<Integer>> b) {}

              void b() {
                a(Sets::difference);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void memberReferenceWithCustomFunctionalInterface() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.Sets;
            import java.util.function.BiFunction;
            import java.util.Set;

            public interface Test {
              Set<Integer> test(Set<Integer> a, Set<String> b);

              static void a(Test b) {}

              static void b() {
                // BUG: Diagnostic contains: Integer is not compatible with String
                a(Sets::difference);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void wildcardBoundedCollectionTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;
            import java.util.Set;

            public interface Test {
              static void test(Set<? extends List<Integer>> xs, Set<? extends Set<Integer>> ys) {
                // BUG: Diagnostic contains:
                xs.containsAll(ys);
              }
            }
            """)
        .doTest();
  }
}
