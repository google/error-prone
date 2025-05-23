/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link IncompatibleArgumentType} */
@RunWith(JUnit4.class)
public class IncompatibleArgumentTypeTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(IncompatibleArgumentType.class, getClass());

  @Test
  public void genericMethod() {
    compilationHelper
        .addSourceLines(
            "IncompatibleArgumentTypeGenericMethod.java",
"""
package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

import com.google.errorprone.annotations.CompatibleWith;

/** Test IncompatibleArgumentType with a generic method */
public class IncompatibleArgumentTypeGenericMethod {
  class A<B> {
    <C> C remove(@CompatibleWith("B") Object b, @CompatibleWith("C") Object c) {
      return null;
    }

    <C> C varargs(@CompatibleWith("B") Object b, @CompatibleWith("C") Object... cs) {
      return (C) cs[0];
    }
  }

  class C extends A<String> {}

  void testfoo(C c, A<?> unbound, A<? extends Number> boundToNumber) {
    c.remove("a", null); // OK, match null to Double
    c.remove("a", 123.0); // OK, match Double to Double
    c.remove("a", 123); // OK, 2nd arg is unbound

    unbound.remove(null, 123); // OK, variables unbound

    // BUG: Diagnostic contains: String is not compatible with the required type: Number
    boundToNumber.remove("123", null);

    // BUG: Diagnostic contains: int is not compatible with the required type: Double
    Double d = c.remove("a", 123);
    // BUG: Diagnostic contains: int is not compatible with the required type: Double
    c.<Double>remove("a", 123);

    // BUG: Diagnostic contains: float is not compatible with the required type: Double
    c.<Double>remove(123, 123.0f);
  }

  void testVarargs(A<String> stringA) {
    // OK, all varargs elements compatible with Integer
    Integer first = stringA.varargs("hi", 2, 3, 4);

    // BUG: Diagnostic contains: long is not compatible with the required type: Integer
    first = stringA.varargs("foo", 2, 3L);

    // OK, everything compatible w/ Object
    Object o = stringA.varargs("foo", 2L, 1.0d, "a");
  }
}\
""")
        .doTest();
  }

  @Test
  public void owningTypes() {
    compilationHelper
        .addSourceLines(
            "IncompatibleArgumentTypeEnclosingTypes.java",
"""
package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

import com.google.errorprone.annotations.CompatibleWith;
import java.util.Set;

/** Test case for enclosing type */
public class IncompatibleArgumentTypeEnclosingTypes {
  static class Foo<Y> {
    class Bar {
      void doSomething(@CompatibleWith("Y") Object x) {}
    }

    class Sub<X> {
      class SubSub<X> {
        void doSomething(@CompatibleWith("X") Object nestedResolution) {}

        <X> X methodVarIsReturn(@CompatibleWith("X") Object nestedResolution) {
          return null;
        }

        <X> void methodVarIsFree(@CompatibleWith("X") Object nestedResolution) {}

        void compatibleWithBase(@CompatibleWith("Y") Object nestedResolution) {}
      }
    }

    static class Baz {
      // Shouldn't resolve to anything, would be a compile error due to CompatibleWithMisuse
      static void doSomething(@CompatibleWith("X") Object x) {}
    }
  }

  void testSubs() {
    new Foo<String>().new Bar().doSomething("a");
    // BUG: Diagnostic contains: int is not compatible with the required type: String
    new Foo<String>().new Bar().doSomething(123);
    new Foo<Integer>().new Bar().doSomething(123);

    Foo.Bar rawtype = new Foo<String>().new Bar();
    rawtype.doSomething(123); // Weakness, rawtype isn't specialized in Foo

    Foo.Baz.doSomething(123); // No resolution of X
  }

  void testMegasub() {
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().doSomething(true);
    // BUG: Diagnostic contains: int is not compatible with the required type: Boolean
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().doSomething(123);

    // X in method is unbound
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().methodVarIsReturn(123);

    // BUG: Diagnostic contains: int is not compatible with the required type: Set<?>
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().<Set<?>>methodVarIsReturn(123);

    // BUG: Diagnostic contains: int is not compatible with the required type: String
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().<Set<?>>compatibleWithBase(123);
  }

  void extraStuff() {
    // Javac throws away the type of <X> since it's not used in params/return type, so we can't
    // enforce it here.
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().<Set<?>>methodVarIsFree(123);
  }
}\
""")
        .doTest();
  }

  @Test
  public void multimapIntegration() {
    compilationHelper
        .addSourceLines(
            "IncompatibleArgumentTypeMultimapIntegration.java",
"""
package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

import com.google.errorprone.annotations.CompatibleWith;

/** Integration test testing a hypothetical multimap interface */
public class IncompatibleArgumentTypeMultimapIntegration {
  interface Multimap<K, V> {
    boolean containsKey(@CompatibleWith("K") Object key);

    boolean containsValue(@CompatibleWith("V") Object value);

    boolean containsEntry(@CompatibleWith("K") Object key, @CompatibleWith("V") Object value);

    boolean containsAllKeys(@CompatibleWith("K") Object key, Object... others);
  }

  class MyMultimap<K, V> implements Multimap<K, V> {
    @Override
    public boolean containsKey(Object key) {
      return false;
    }

    @Override
    public boolean containsValue(Object value) {
      return false;
    }

    @Override
    public boolean containsEntry(Object key, Object value) {
      return false;
    }

    @Override
    public boolean containsAllKeys(Object key, Object... keys) {
      return false;
    }
  }

  void testRegularValid(Multimap<Integer, String> intToString) {
    intToString.containsKey(123);
    intToString.containsEntry(123, "abc");
    intToString.containsValue("def");
    // 0-entry vararg doesn't crash
    intToString.containsAllKeys(123);
  }

  static <K extends Number, V extends String> void testIncompatibleWildcards(
      Multimap<? extends K, ? extends V> map, K key, V value) {
    map.containsKey(key);
    map.containsValue(value);
    map.containsEntry(key, value);

    // BUG: Diagnostic contains: V is not compatible with the required type: K
    map.containsEntry(value, key);
    // BUG: Diagnostic contains: K is not compatible with the required type: V
    map.containsValue(key);
    // BUG: Diagnostic contains: V is not compatible with the required type: K
    map.containsKey(value);
  }

  void testVarArgs(Multimap<Integer, String> intToString) {
    // Validates the first, not the varags params
    intToString.containsAllKeys(123, 123, 123);
    // TODO(glorioso): If we make it work with varargs, this should fail
    intToString.containsAllKeys(123, 123, "a");

    Integer[] keys = {123, 345};
    intToString.containsAllKeys(123, (Object[]) keys);
  }
}\
""")
        .doTest();
  }

  @Test
  public void intersectionTypes() {
    compilationHelper
        .addSourceLines(
            "IncompatibleArgumentTypeIntersectionTypes.java",
"""
package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

import com.google.errorprone.annotations.CompatibleWith;

/** Test intersection types. */
public class IncompatibleArgumentTypeIntersectionTypes {

  interface Nothing {}

  interface Something {}

  interface Everything extends Nothing, Something {}

  class Test<X extends Nothing & Something> {
    void doSomething(@CompatibleWith("X") Object whatever) {}
  }

  class ArrayTest<X> {
    void doSomething(@CompatibleWith("X") Object whatever) {}
  }

  void testStuff(Test<Everything> someTest, Everything[] everythings, Nothing nothing) {
    // Final classes (Integer) can't be cast to an interface they don't implement
    // BUG: Diagnostic contains: int is not compatible with the required type: Everything
    someTest.doSomething(123);

    // Non-final classes can.
    someTest.doSomething((Object) 123);

    // Arrays can't, since they can only be cast to Serializable
    // BUG: Diagnostic contains: Everything[] is not compatible with the required type: Everything
    someTest.doSomething(everythings);

    // BUG: Diagnostic contains: Everything[][] is not compatible with the required type: Everything
    someTest.doSomething(new Everything[][] {everythings});

    // OK (since some other implementer of Nothing could implement Everything)
    someTest.doSomething(nothing);
  }

  void testArraySpecialization(
      ArrayTest<Number[]> arrayTest, Integer[] ints, Object[] objz, String[] strings) {
    arrayTest.doSomething(ints);

    arrayTest.doSomething(objz);

    // BUG: Diagnostic contains: String[] is not compatible with the required type: Number[]
    arrayTest.doSomething(strings);
  }
}\
""")
        .doTest();
  }

  @Test
  public void typeWithinLambda() {
    assume().that(Runtime.version().feature()).isAtMost(21);

    compilationHelper
        .addSourceLines(
            "Test.java",
"""
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CompatibleWith;
import java.util.Map;
import java.util.Optional;

abstract class Test {
  abstract <K, V> Optional<V> getOrEmpty(Map<K, V> map, @CompatibleWith("K") Object key);

  void test(Map<Long, String> map, ImmutableList<Long> xs) {
    // BUG: Diagnostic contains:
    getOrEmpty(map, xs);
    Optional<String> x = Optional.empty().flatMap(k -> getOrEmpty(map, xs));
  }
}
""")
        .doTest();
  }

  @Test
  public void typeWithinLambda_jdkhead() {
    assume().that(Runtime.version().feature()).isAtLeast(25);

    compilationHelper
        .addSourceLines(
            "Test.java",
"""
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CompatibleWith;
import java.util.Map;
import java.util.Optional;

abstract class Test {
  abstract <K, V> Optional<V> getOrEmpty(Map<K, V> map, @CompatibleWith("K") Object key);

  void test(Map<Long, String> map, ImmutableList<Long> xs) {
    // BUG: Diagnostic contains:
    getOrEmpty(map, xs);
    // BUG: Diagnostic contains:
    Optional<String> x = Optional.empty().flatMap(k -> getOrEmpty(map, xs));
  }
}
""")
        .doTest();
  }
}
