/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.CompilationTestHelper;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
@RunWith(JUnit4.class)
public class EqualsIncompatibleTypeTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(EqualsIncompatibleType.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "EqualsIncompatibleTypePositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
public class EqualsIncompatibleTypePositiveCases {
  class A {}

  class B {}

  void checkEqualsAB(A a, B b) {
    // BUG: Diagnostic contains: incompatible types
    a.equals(b);
    // BUG: Diagnostic contains: incompatible types
    b.equals(a);
  }

  class C {}

  abstract class C1 extends C {
    public abstract boolean equals(Object o);
  }

  abstract class C2 extends C1 {}

  abstract class C3 extends C {}

  void checkEqualsCC1C2C3(C c, C1 c1, C2 c2, C3 c3) {
    // BUG: Diagnostic contains: incompatible types
    c3.equals(c1);
    // BUG: Diagnostic contains: incompatible types
    c3.equals(c2);
    // BUG: Diagnostic contains: incompatible types
    c1.equals(c3);
    // BUG: Diagnostic contains: incompatible types
    c2.equals(c3);
  }

  void checkStaticEqualsCC1C2C3(C c, C1 c1, C2 c2, C3 c3) {
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c3, c1);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c3, c2);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c1, c3);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c2, c3);
  }

  void checkGuavaStaticEqualsCC1C2C3(C c, C1 c1, C2 c2, C3 c3) {
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c3, c1);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c3, c2);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c1, c3);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c2, c3);
  }

  void checkPrimitiveEquals(int a, long b) {
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(a, b);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(b, a);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(a, b);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(b, a);
  }

  interface I {
    boolean equals(Object o);
  }

  class D {}

  class D1 extends D {}

  class D2 extends D implements I {}

  void checkEqualsDD1D2(D d, D1 d1, D2 d2) {
    // BUG: Diagnostic contains: incompatible types
    d1.equals(d2);
    // BUG: Diagnostic contains: incompatible types
    d2.equals(d1);
  }

  enum MyEnum {}

  enum MyOtherEnum {}

  void enumEquals(MyEnum m, MyOtherEnum mm) {
    // BUG: Diagnostic contains: incompatible types
    m.equals(mm);
    // BUG: Diagnostic contains: incompatible types
    mm.equals(m);

    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(m, mm);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(mm, m);
  }

  void collectionsWithGenericMismatches(
      List<String> stringList,
      List<Integer> intList,
      Set<String> stringSet,
      Set<Integer> intSet,
      ImmutableList<String> stringImmutableList) {

    // BUG: Diagnostic contains: incompatible types
    stringList.equals(intList);

    // BUG: Diagnostic contains: incompatible types
    stringSet.equals(intSet);

    // BUG: Diagnostic contains: incompatible types
    stringList.equals(stringSet);

    // BUG: Diagnostic contains: incompatible types
    intList.equals(stringImmutableList);
  }

  void mapKeyChecking(
      Map<String, Integer> stringIntegerMap,
      Map<Integer, String> integerStringMap,
      Map<List<String>, Set<String>> stringListSetMap,
      Map<List<String>, Set<Integer>> intListSetMap) {
    // BUG: Diagnostic contains: incompatible types
    stringIntegerMap.equals(integerStringMap);

    // BUG: Diagnostic contains: incompatible types
    stringListSetMap.equals(intListSetMap);
  }

  void nestedColls(Set<List<String>> setListString, Set<List<Integer>> setListInteger) {
    // BUG: Diagnostic contains: String and Integer are incompatible
    boolean equals = setListString.equals(setListInteger);
  }

  class MyGenericClazz<T> {}

  <T extends I> void testSomeGenerics(
      MyGenericClazz<String> strClazz, MyGenericClazz<Integer> intClazz, MyGenericClazz<T> iClazz) {
    // BUG: Diagnostic contains: String and Integer are incompatible
    strClazz.equals(intClazz);

    // BUG: Diagnostic contains: T and String are incompatible
    iClazz.equals(strClazz);
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "EqualsIncompatibleTypeNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.common.collect.ImmutableList;
            import com.google.common.collect.ImmutableSet;
            import java.util.List;
            import java.util.Set;

            /**
             * @author avenet@google.com (Arnaud J. Venet)
             */
            public class EqualsIncompatibleTypeNegativeCases {
              class A {
                public boolean equals(Object o) {
                  if (o instanceof A) {
                    return true;
                  }
                  return false;
                }
              }

              class B1 extends A {}

              class B2 extends A {}

              class B3 extends B2 {}

              void checkEqualsAB1B2B3(A a, B1 b1, B2 b2, B3 b3) {
                a.equals(a);
                a.equals(b1);
                a.equals(b2);
                a.equals(b3);
                a.equals(null);

                b1.equals(a);
                b1.equals(b1);
                b1.equals(b2);
                b1.equals(b3);
                b1.equals(null);

                b2.equals(a);
                b2.equals(b1);
                b2.equals(b2);
                b2.equals(b3);
                b2.equals(null);

                b3.equals(a);
                b3.equals(b1);
                b3.equals(b2);
                b3.equals(b3);
                b3.equals(null);
              }

              void checks(Object o, boolean[] bools, boolean bool) {
                o.equals(bool);
                o.equals(bools[0]);
              }

              void checkJUnit(B1 b1, B2 b2) {
                org.junit.Assert.assertFalse(b1.equals(b2));
              }

              void checkStaticEquals(A a, B1 b1, B2 b2, B3 b3) {
                java.util.Objects.equals(a, a);
                java.util.Objects.equals(a, b1);
                java.util.Objects.equals(a, b2);
                java.util.Objects.equals(a, b3);
                java.util.Objects.equals(a, null);

                java.util.Objects.equals(b1, b3);
                java.util.Objects.equals(b2, b3);
                java.util.Objects.equals(b3, b3);
                java.util.Objects.equals(null, b3);
              }

              void checkGuavaStaticEquals(A a, B1 b1, B2 b2, B3 b3) {
                com.google.common.base.Objects.equal(a, a);
                com.google.common.base.Objects.equal(a, b1);
                com.google.common.base.Objects.equal(a, b2);
                com.google.common.base.Objects.equal(a, b3);
                com.google.common.base.Objects.equal(a, null);

                com.google.common.base.Objects.equal(b1, b3);
                com.google.common.base.Objects.equal(b2, b3);
                com.google.common.base.Objects.equal(b3, b3);
                com.google.common.base.Objects.equal(null, b3);
              }

              class C {}

              abstract class C1 extends C {
                public abstract boolean equals(Object o);
              }

              abstract class C2 extends C1 {}

              abstract class C3 extends C1 {}

              void checkEqualsC1C2C3(C1 c1, C2 c2, C3 c3) {
                c1.equals(c1);
                c1.equals(c2);
                c1.equals(c3);
                c1.equals(null);

                c2.equals(c1);
                c2.equals(c2);
                c2.equals(c3);
                c2.equals(null);

                c3.equals(c1);
                c3.equals(c2);
                c3.equals(c3);
                c3.equals(null);
              }

              interface I {
                boolean equals(Object o);
              }

              class E1 implements I {}

              class E2 implements I {}

              class E3 extends E2 {}

              void checkEqualsIE1E2E3(
                  I e, E1 e1, E2 e2, E3 e3, List<I> eList, List<E1> e1List, List<E2> e2List) {
                e.equals(e);
                e.equals(e1);
                e.equals(e2);
                e.equals(e3);
                e.equals(null);

                e1.equals(e);
                e1.equals(e1);
                e1.equals(e2);
                e1.equals(e3);
                e1.equals(null);

                e2.equals(e);
                e2.equals(e1);
                e2.equals(e2);
                e2.equals(e3);
                e2.equals(null);

                e3.equals(e);
                e3.equals(e1);
                e3.equals(e2);
                e3.equals(e3);
                e3.equals(null);

                eList.equals(e1List);
                eList.equals(e2List);
                eList.equals(null);

                e1List.equals(eList);
                e1List.equals(e2List);
                e1List.equals(null);

                e2List.equals(eList);
                e2List.equals(e1List);
                e2List.equals(null);
              }

              void collectionStuff(
                  List rawList,
                  List<String> stringList,
                  Set<String> stringSet,
                  ImmutableSet<String> immutableStringSet,
                  ImmutableList<String> immutableStringList) {

                // With raw types, we can't be sure. So... /shrug
                rawList.equals(stringList);

                stringSet.equals(immutableStringSet);
                stringList.equals(immutableStringList);
              }

              interface J {}

              class F1 implements J {}

              abstract class F2 {
                public abstract boolean equals(J o);
              }

              void checkOtherEquals(F1 f1, F2 f2) {
                f2.equals(f1);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase_recursive() {
    compilationHelper
        .addSourceLines(
            "EqualsIncompatibleTypeRecursiveTypes.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import java.time.DayOfWeek;
import java.time.Month;
import java.util.Objects;

/** Checks for objects with recursive type bounds. */
public class EqualsIncompatibleTypeRecursiveTypes {

  interface Bar<X, Y> {}

  final class ConcreteBar<X, Y> implements Bar<X, Y> {}

  static class Foo<T extends Enum<T>> implements Bar<String, T> {
    T field;

    void check(Foo<?> other) {
      // OK since Enum<?> and Enum<T> are not incompatible
      this.field.equals(other.field);
    }

    <X extends ConcreteBar<String, X>> void badCheck(Bar<String, X> other) {
      // BUG: Diagnostic contains: T and X are incompatible
      this.equals(other);
    }
  }

  interface RandomInterface {}

  interface Entity<
      E extends Entity<E, K, V, V2>,
      K extends EntityKey<K>,
      V extends Enum<V>,
      V2 extends Enum<V2>> {}

  interface EntityKey<K extends EntityKey<K>> extends Comparable<K> {}

  static final class EK1 implements EntityKey<EK1> {
    @Override
    public int compareTo(EK1 o) {
      return 0;
    }
  }

  static final class E1 implements Entity<E1, EK1, DayOfWeek, Month>, RandomInterface {}

  static final class E2 implements Entity<E2, EK1, Month, DayOfWeek>, RandomInterface {}

  void testMultilayer(Class<? extends Entity<?, ?, ?, ?>> eClazz, Class<? extends E2> e2Clazz) {
    if (Objects.equals(eClazz, E1.class)) {
      System.out.println("yay");
    }

    if (Objects.equals(eClazz, E2.class)) {
      System.out.println("yay");
    }

    if (Objects.equals(e2Clazz, E2.class)) {
      System.out.println("yay");
    }

    // BUG: Diagnostic contains: E2 and E1 are incompatible.
    if (Objects.equals(e2Clazz, E1.class)) {
      System.out.println("boo");
    }
  }

  interface First<A extends First<A>> {
    default A get() {
      return null;
    }
  }

  interface Second<B> extends First<Second<B>> {}

  interface Third extends Second<Third> {}

  interface Fourth extends Second<Fourth> {}

  void testing(Third third, Fourth fourth) {
    // BUG: Diagnostic contains: Third and Fourth
    boolean equals = third.equals(fourth);
  }

  interface RecOne extends Comparable<Comparable<RecOne>> {}

  interface RecTwo extends Comparable<Comparable<RecTwo>> {}

  void testMultiRecursion(RecOne a, RecTwo b) {
    // BUG: Diagnostic contains: RecOne and RecTwo
    boolean bad = a.equals(b);
  }

  interface Quux<A extends Quux<A>> {}

  interface Quuz<A extends Quux<A>> extends Quux<A> {}

  interface Quiz<A extends Quux<A>> extends Quux<A> {}

  interface Id1 extends Quuz<Id1> {}

  interface Id2 extends Quiz<Id2> {}

  abstract static class Id3 implements Quuz<Id3>, Quiz<Id3> {}

  void test(Id1 first, Id3 second) {
    // BUG: Diagnostic contains: Id1 and Id3
    boolean res = Objects.equals(first, second);
  }

  class I<T> {}

  class J<A extends I<B>, B extends I<C>, C extends I<A>> {}

  <
          A1 extends I<B1>,
          B1 extends I<C1>,
          C1 extends I<A1>,
          A2 extends I<B2>,
          B2 extends I<C2>,
          C2 extends I<A2>>
      void something(J<A1, B1, C1> j1, J<A2, B2, C2> j2) {
    // Technically this could work, since there's nothing stopping A1 == A2, etc.
    boolean equals = j1.equals(j2);
  }
}\
""")
        .doTest();
  }

  @Test
  public void primitiveBoxingIntoObject() {
    assume()
        .that(Runtime.version().feature())
        .isLessThan(12); // https://bugs.openjdk.java.net/browse/JDK-8028563
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void something(boolean b, Object o) {
                o.equals(b);
              }
            }
            """)
        .setArgs(Arrays.asList("-source", "1.6", "-target", "1.6"))
        .doTest();
  }

  @Test
  public void i547() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              interface B {}

              <T extends B> void t(T x) {
                // BUG: Diagnostic contains: T and String
                x.equals("foo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void prettyNameForConflicts() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              interface B {}

              interface String {}

              void t(String x) {
                // BUG: Diagnostic contains: types Test.String and java.lang.String
                x.equals("foo");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReference_incompatibleTypes_finding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.stream.Stream;

            class Test {
              boolean t(Stream<Integer> xs, String x) {
                // BUG: Diagnostic contains:
                return xs.anyMatch(x::equals);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReference_comparableTypes_noFinding() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.stream.Stream;

            class Test {
              boolean t(Stream<Integer> xs, Object x) {
                return xs.anyMatch(x::equals);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void wildcards_whenIncompatible() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              public void test(Class<? extends Integer> a, Class<? extends String> b) {
                // BUG: Diagnostic contains:
                a.equals(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void unconstrainedWildcard_compatibleWithAnything() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            public class Test {
              public void test(java.lang.reflect.Method m, Class<?> c) {
                TestProtoMessage.class.equals(m.getParameterTypes()[0]);
                TestProtoMessage.class.equals(c);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void enumsCanBeEqual() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              enum E {
                A,
                B
              }

              public void test() {
                E.A.equals(E.B);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void protoBuildersCannotBeEqual() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestOneOfMessage;

            public class Test {
              public void test() {
                // BUG: Diagnostic contains: . Though
                TestProtoMessage.newBuilder().equals(TestProtoMessage.newBuilder());
                // BUG: Diagnostic contains:
                TestProtoMessage.newBuilder().equals(TestOneOfMessage.newBuilder());
                // BUG: Diagnostic contains:
                TestProtoMessage.newBuilder().equals(TestOneOfMessage.getDefaultInstance());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void enumsNamedBuilderCanBeEqual() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            public class Test {
              enum FooBuilder {
                A
              }

              public boolean test(FooBuilder a, FooBuilder b) {
                return a.equals(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void flaggedOff_protoBuildersNotConsideredIncomparable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
"""
import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

public class Test {
  public void test() {
    TestProtoMessage.newBuilder().equals(TestProtoMessage.newBuilder());
    TestProtoMessage.getDefaultInstance().equals(TestProtoMessage.getDefaultInstance());
  }
}
""")
        .setArgs("-XepOpt:TypeCompatibility:TreatBuildersAsIncomparable=false")
        .doTest();
  }

  @Test
  public void protoBuilderComparedWithinAutoValue() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.auto.value.AutoValue;
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            @AutoValue
            abstract class Test {
              abstract TestProtoMessage.Builder b();
            }
            """)
        .addSourceLines(
            "AutoValue_Test.java",
            """
            import javax.annotation.processing.Generated;

            @Generated("com.google.auto.value.processor.AutoValueProcessor")
            abstract class AutoValue_Test extends Test {
              @Override
              public boolean equals(Object o) {
                return ((Test) o).b().equals(b());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void predicateIsEqual_incompatible() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static java.util.function.Predicate.isEqual;
            import java.util.stream.Stream;

            class Test {
              boolean test(Stream<Long> xs) {
                // BUG: Diagnostic contains:
                return xs.allMatch(isEqual(1));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void predicateIsEqual_compatible() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static java.util.function.Predicate.isEqual;
            import java.util.stream.Stream;

            class Test {
              boolean test(Stream<Long> xs) {
                return xs.allMatch(isEqual(1L));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void predicateIsEqual_methodRef() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.function.Function;
            import java.util.function.Predicate;
            import java.util.stream.Stream;

            class Test {
              boolean test(Function<Long, Predicate<Integer>> fn) {
                // BUG: Diagnostic contains:
                return test(Predicate::isEqual);
              }
            }
            """)
        .doTest();
  }
}
