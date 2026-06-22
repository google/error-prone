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

package com.google.errorprone.bugpatterns;

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ReferenceEquality}Test */
@RunWith(JUnit4.class)
public class ReferenceEqualityTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ReferenceEquality.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(ReferenceEquality.class, getClass());

  @Test
  public void protoGetter_nonnull() {
    compilationHelper
        .addSourceLines(
            "in/Foo.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            class Foo {
              void something(TestProtoMessage f1, TestProtoMessage f2) {
                // BUG: Diagnostic contains: boolean b = Objects.equals(f1, f2);
                boolean b = f1 == f2;
                // BUG: Diagnostic contains: b = f1.getMessage().equals(f2.getMessage())
                b = f1.getMessage() == f2.getMessage();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void extends_equalsObject() {
    compilationHelper
        .addSourceLines(
            "Sup.java",
            """
            class Sup {
              public boolean equals(Object o) {
                return false;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test extends Sup {
              boolean f(Object a, Test b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_extendsAbstract_equals() {
    compilationHelper
        .addSourceLines(
            "Sup.java",
            """
            abstract class Sup {
              public abstract boolean equals(Object o);
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            abstract class Test extends Sup {
              boolean f(Test a, Test b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void implementsInterface_equals() {
    compilationHelper
        .addSourceLines(
            "Sup.java",
            """
            interface Sup {
              public boolean equals(Object o);
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test implements Sup {
              boolean f(Test a, Test b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_noEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(Test a, Test b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_equal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Optional;

            class Test {
              boolean f(Optional<Integer> a, Optional<Integer> b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_equalWithOr() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            """
            import com.google.common.base.Optional;

            class Test {
              boolean f(Optional<Integer> a, Optional<Integer> b) {
                return a == b || (a.equals(b));
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import com.google.common.base.Optional;

            class Test {
              boolean f(Optional<Integer> a, Optional<Integer> b) {
                return a.equals(b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_equalWithOr_objectsEquals() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            """
            import com.google.common.base.Objects;
            import com.google.common.base.Optional;

            class Test {
              boolean f(Optional<Integer> a, Optional<Integer> b) {
                boolean eq = a == b || Objects.equal(a, b);
                return a == b || (java.util.Objects.equals(a, b));
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import com.google.common.base.Objects;
            import com.google.common.base.Optional;

            class Test {
              boolean f(Optional<Integer> a, Optional<Integer> b) {
                boolean eq = Objects.equal(a, b);
                return java.util.Objects.equals(a, b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_notEqual() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Optional;

            class Test {
              boolean f(Optional<Integer> a, Optional<Integer> b) {
                // BUG: Diagnostic contains: !a.equals(b)
                return a != b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_impl() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public boolean equals(Object o) {
                return this == o;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_enum() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.lang.model.element.ElementKind;

            class Test {
              boolean f(ElementKind a, ElementKind b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void customEnum() {
    compilationHelper
        .addSourceLines(
            "Kind.java",
            """
            enum Kind {
              FOO(42);
              private final int x;

              Kind(int x) {
                this.x = x;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(Kind a, Kind b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_null() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Optional;

            class Test {
              boolean f(Optional<Integer> b) {
                return b == null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_abstractEq() {
    compilationHelper
        .addSourceLines(
            "Sup.java",
            """
            interface Sup {
              public abstract boolean equals(Object o);
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Optional;

            class Test implements Sup {
              boolean f(Object a, Test b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase_class() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(String s) {
                return s.getClass() == String.class;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void transitiveEquals() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            """
            public class Super {
              public boolean equals(Object o) {
                return false;
              }
            }
            """)
        .addSourceLines(
            "Mid.java",
            """
            public class Mid extends Super {}
            """)
        .addSourceLines(
            "Sub.java",
            """
            public class Sub extends Mid {}
            """)
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              boolean f(Sub a, Sub b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  public static class Missing {}

  public static class MayImplementEquals {

    public void f(Missing m) {}

    public void g(Missing m) {}
  }

  @Test
  public void erroneous() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import %s;
            abstract class Test {
              abstract MayImplementEquals getter();
              boolean f(MayImplementEquals b) {
                // BUG: Diagnostic contains: getter().equals(b)
                return getter() == b;
              }
            }
            """
                .formatted(MayImplementEquals.class.getCanonicalName()))
        .withClasspath(MayImplementEquals.class, ReferenceEqualityTest.class)
        .doTest();
  }

  // regression test for #423
  @Test
  public void typaram() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test<T extends String, X> {
              boolean f(T t) {
                return t == null;
              }

              boolean g(T t1, T t2) {
                // BUG: Diagnostic contains:
                return t1 == t2;
              }

              boolean g(X x1, X x2) {
                // BUG: Diagnostic contains:
                return x1 == x2;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_compareTo() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test implements Comparable<Test> {
              public int compareTo(Test o) {
                return this == o ? 0 : -1;
              }

              public boolean equals(Object obj) {
                return obj instanceof Test;
              }

              public int hashCode() {
                return 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void likeCompareToButDifferentName() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test implements Comparable<Test> {
              public int compareTo(Test o) {
                return this == o ? 0 : -1;
              }

              public int notCompareTo(Test o) {
                // BUG: Diagnostic contains:
                return this == o ? 0 : -1;
              }

              public boolean equals(Object obj) {
                return obj instanceof Test;
              }

              public int hashCode() {
                return 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_compareTo() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test implements Comparable<String> {
              String f;

              public int compareTo(String o) {
                // BUG: Diagnostic contains:
                return f == o ? 0 : -1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_implementsComparator() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              static class Comparator1 implements Comparator<String> {
                @Override
                public int compare(String o1, String o2) {
                  if (o1 == o2) {
                    return 0;
                  } else if (o1 == null) {
                    return -1;
                  } else if (o2 == null) {
                    return 1;
                  } else {
                    return -1;
                  }
                }
              }

              static class Comparator2 implements Comparator<String> {
                @Override
                public int compare(String o1, String o2) {
                  return o1 == o2 ? 0 : -1;
                }
              }
            }
            """)
        .expectNoDiagnostics()
        .doTest();
  }

  @Test
  public void negative_lambdaComparator() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              private static final Comparator<String> comparator1 =
                  (o1, o2) -> {
                    if (o1 == o2) {
                      return 0;
                    } else if (o1 == null) {
                      return -1;
                    } else if (o2 == null) {
                      return 1;
                    } else {
                      return -1;
                    }
                  };
              private static final Comparator<String> comparator2 =
                  (o1, o2) -> {
                    return o1 == o2 ? 0 : -1;
                  };
              private static final Comparator<String> comparator3 = (o1, o2) -> o1 == o2 ? 0 : -1;
            }
            """)
        .expectNoDiagnostics()
        .doTest();
  }

  @Test
  public void arrayComparison() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(int[] a, int[] b) {
                // BUG: Diagnostic contains: ReferenceEquality
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void finalClassWithoutEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            final class Test {
              boolean f(Test a, Test b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void finalClassWithEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            final class Test {
              public boolean equals(Object o) {
                return true;
              }

              boolean f(Test a, Test b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sealedClassWithoutEquals() {
    compilationHelper
        .addSourceLines(
            "Sealed.java",
            """
            sealed interface Sealed permits Final1, Final2 {}
            """)
        .addSourceLines(
            "Final1.java",
            """
            final class Final1 implements Sealed {}
            """)
        .addSourceLines(
            "Final2.java",
            """
            final class Final2 implements Sealed {}
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(Sealed a, Sealed b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sealedClassWithEquals() {
    compilationHelper
        .addSourceLines(
            "Sealed.java",
            """
            sealed interface Sealed permits Final1, Final2 {}
            """)
        .addSourceLines(
            "Final1.java",
            """
            final class Final1 implements Sealed {
              public boolean equals(Object o) {
                return true;
              }
            }
            """)
        .addSourceLines(
            "Final2.java",
            """
            final class Final2 implements Sealed {}
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(Sealed a, Sealed b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sealedClassWithNonSealedSubclass() {
    compilationHelper
        .addSourceLines(
            "Sealed.java",
            """
            sealed interface Sealed permits NonSealedSub {}
            """)
        .addSourceLines(
            "NonSealedSub.java",
            """
            non-sealed class NonSealedSub implements Sealed {}
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(Sealed a, Sealed b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sealedClassWithEnumSubclass() {
    compilationHelper
        .addSourceLines(
            "Sealed.java",
            """
            sealed interface Sealed permits MyEnum {}
            """)
        .addSourceLines(
            "MyEnum.java",
            """
            enum MyEnum implements Sealed {
              INSTANCE;
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(Sealed a, Sealed b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void typeVariableBoundedByClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test<T extends Class<?>> {
              boolean f(T a, T b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void typeVariableBoundedByFinalClassWithoutEquals() {
    compilationHelper
        .addSourceLines(
            "Final.java",
            """
            final class Final {}
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test<T extends Final> {
              boolean f(T a, T b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void typeVariableBoundedByFinalClassWithEquals() {
    compilationHelper
        .addSourceLines(
            "Final.java",
            """
            final class Final {
              public boolean equals(Object o) {
                return true;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test<T extends Final> {
              boolean f(T a, T b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void typeVariableBoundedByNonFinalClass() {
    compilationHelper
        .addSourceLines(
            "NonFinal.java",
            """
            class NonFinal {}
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test<T extends NonFinal> {
              boolean f(T a, T b) {
                // BUG: Diagnostic contains: a.equals(b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void typeVariableWithTransitiveBound() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test<T extends Class<?>, U extends T> {
              boolean f(U a, U b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void memorySegment() {
    assume().that(Runtime.version().feature()).isAtLeast(22);
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.lang.foreign.MemorySegment;

            class Test {
              boolean f(MemorySegment a) {
                // BUG: Diagnostic contains: a.equals(MemorySegment.NULL)
                return a == MemorySegment.NULL;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void ambiguousImport() {
    compilationHelper
        .addSourceLines(
            "Objects.java",
            """
            package test;

            public class Objects {}
            """)
        .addSourceLines(
            "Test.java",
            """
            import java.util.*;
            import test.*;

            class Test {
              boolean f(String a, String b) {
                // BUG: Diagnostic contains: java.util.Objects.equals(a, b)
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateConstructor_noSubclasses() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private Test() {}

              boolean f(Test a, Test b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateConstructor_withSubclasses_allRefEq() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private Test() {}

              static final class Sub1 extends Test {}

              static final class Sub2 extends Test {
                static final Test INSTANCE = new Test() {};
              }

              boolean f(Test a, Test b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateConstructor_withSubclasses_oneOverridesEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private Test() {}

              static final class Sub1 extends Test {}

              static final class Sub2 extends Test {
                @Override
                public boolean equals(Object o) {
                  return true;
                }
              }

              boolean f(Test a, Test b) {
                // BUG: Diagnostic contains:
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void packagePrivateConstructor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              Test() {}

              boolean f(Test a, Test b) {
                // BUG: Diagnostic contains:
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void implicitDefaultConstructor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(Test a, Test b) {
                // BUG: Diagnostic contains:
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void anonymousClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(Test t) {
                return t == new Test() {};
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateConstructor_differentCompilationUnit() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            public class Foo {
              private Foo() {}
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(Foo a, Foo b) {
                // BUG: Diagnostic contains:
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateClass_nonPrivateConstructor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static class PrivateClass {
                public PrivateClass() {}
              }

              static final class Sub extends PrivateClass {}

              boolean f(PrivateClass a, PrivateClass b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateClass_subclassOverridesEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static class PrivateClass {
                public PrivateClass() {}
              }

              static class Sub extends PrivateClass {
                @Override
                public boolean equals(Object o) {
                  return true;
                }
              }

              boolean f(PrivateClass a, PrivateClass b) {
                // BUG: Diagnostic contains:
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void unionType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              boolean f(Throwable expected) {
                try {
                  doSomething();
                } catch (RuntimeException | Error e) {
                  // BUG: Diagnostic contains:
                  return e == expected;
                }
                return false;
              }

              void doSomething() {}
            }
            """)
        .doTest();
  }

  @Test
  public void intersectionType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              <T extends Foo & Bar> boolean f(T a, T b) {
                // BUG: Diagnostic contains:
                return a == b;
              }

              interface Foo {}

              interface Bar {}
            }
            """)
        .doTest();
  }

  @Test
  public void privateConstructor_subclassInSuppressedClassOverridesEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private Test() {}

              @SuppressWarnings("ReferenceEquality")
              static class Suppressed {
                static class Sub extends Test {
                  @Override
                  public boolean equals(Object o) {
                    return true;
                  }
                }
              }

              boolean f(Test a, Test b) {
                // BUG: Diagnostic contains:
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonPrivateInterface() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              interface NonPrivateInterface {}

              boolean f(NonPrivateInterface a, NonPrivateInterface b) {
                // BUG: Diagnostic contains:
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateInterface_implOverridesEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private interface PrivateInterface {}

              static class Impl implements PrivateInterface {
                @Override
                public boolean equals(Object o) {
                  return true;
                }
              }

              boolean f(PrivateInterface a, PrivateInterface b) {
                // BUG: Diagnostic contains:
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateInterface_implDoesNotOverrideEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private interface PrivateInterface {}

              static final class Impl implements PrivateInterface {}

              boolean f(PrivateInterface a, PrivateInterface b) {
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateClass_anonymousSubclassOverridesEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static class PrivateClass {}

              PrivateClass instance =
                  new PrivateClass() {
                    @Override
                    public boolean equals(Object o) {
                      return true;
                    }
                  };

              boolean f(PrivateClass a, PrivateClass b) {
                // BUG: Diagnostic contains:
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void privateInterface_anonymousSubclassOverridesEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private interface PrivateInterface {}

              PrivateInterface instance =
                  new PrivateInterface() {
                    @Override
                    public boolean equals(Object o) {
                      return true;
                    }
                  };

              boolean f(PrivateInterface a, PrivateInterface b) {
                // BUG: Diagnostic contains:
                return a == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sentinel_object_newClassNoEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static final Object SENTINEL = new Object();

              boolean f(Object o) {
                return o == SENTINEL;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sentinel_object_newClassWithEquals() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            class Foo {
              @Override
              public boolean equals(Object o) {
                return true;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static final Object SENTINEL = new Foo();

              boolean f(Object o) {
                // BUG: Diagnostic contains:
                return o == SENTINEL;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sentinel_enumConstant() {
    compilationHelper
        .addSourceLines(
            "MyEnum.java",
            """
            enum MyEnum {
              VAL;
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static final MyEnum SENTINEL = MyEnum.VAL;

              boolean f(Object o) {
                return o == SENTINEL;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void sentinel_string() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static final String SENTINEL = "hello";

              boolean f(String o) {
                // BUG: Diagnostic contains:
                return o == SENTINEL;
              }
            }
            """)
        .doTest();
  }
}
