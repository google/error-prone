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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ImmutableChecker}Test */
@RunWith(JUnit4.class)
public class ImmutableCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ImmutableChecker.class, getClass());

  @Test
  public void basicFields() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.common.collect.ImmutableList;",
            "@Immutable class Test {",
            "  final int a = 42;",
            "  final String b = null;",
            "  final java.lang.String c = null;",
            "  final com.google.common.collect.ImmutableList<String> d = null;",
            "  final ImmutableList<Integer> e = null;",
            "  final Deprecated dep = null;",
            "  final Class<?> clazz = Class.class;",
            "}")
        .doTest();
  }

  @Test
  public void interfacesMutableByDefault() {
    compilationHelper
        .addSourceLines("I.java", "interface I {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains:"
                + " 'I' is not annotated with @com.google.errorprone.annotations.Immutable",
            "  private final I i = new I() {};",
            "}")
        .doTest();
  }

  @Test
  public void annotationsAreImmutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable @interface Test {}")
        .doTest();
  }

  @Test
  public void customAnnotationsMightBeMutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable @interface Test {}")
        .addSourceLines(
            "MyTest.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.lang.annotation.Annotation;",
            "@Immutable final class MyTest implements Test {",
            "  // BUG: Diagnostic contains: non-final",
            "  public Object[] xs = {};",
            "  public Class<? extends Annotation> annotationType() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Ignore("b/25630189") // don't check annotations for immutability yet
  @Test
  public void customImplementionsOfImplicitlyImmutableAnnotationsMustBeImmutable() {
    compilationHelper
        .addSourceLines("Anno.java", "@interface Anno {}")
        .addSourceLines(
            "MyAnno.java",
            "import java.lang.annotation.Annotation;",
            "final class MyAnno implements Anno {",
            "  // BUG: Diagnostic contains:",
            "  public Object[] xs = {};",
            "  public Class<? extends Annotation> annotationType() {",
            "    return null;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  private final Anno anno = new MyAnno();",
            "}")
        .doTest();
  }

  @Test
  public void customAnnotationsSubtype() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable @interface Test {}")
        .addSourceLines(
            "MyTest.java",
            "import java.lang.annotation.Annotation;",
            "// BUG: Diagnostic contains:",
            "// extends @Immutable type Test, but is not annotated as immutable",
            "final class MyTest implements Test {",
            "  public Object[] xs = {};",
            "  public Class<? extends Annotation> annotationType() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotationsDefaultToImmutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.lang.model.element.ElementKind;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  private final Override override = null;",
            "}")
        .doTest();
  }

  @Test
  public void enumsDefaultToImmutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import javax.lang.model.element.ElementKind;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  private final ElementKind ek = null;",
            "}")
        .doTest();
  }

  @Test
  public void enumsMayBeImmutable() {
    compilationHelper
        .addSourceLines(
            "Kind.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable enum Kind { A, B, C; }")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  private final Kind k = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableArray() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains:",
            "  final int[] xs = {42};",
            "}")
        .doTest();
  }

  @Test
  public void annotatedImmutableInterfaces() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface Test {}")
        .doTest();
  }

  @Test
  public void immutableInterfaceField() {
    compilationHelper
        .addSourceLines(
            "MyInterface.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface MyInterface {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  final MyInterface i = null;",
            "}")
        .doTest();
  }

  @Test
  public void deeplyImmutableArguments() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.common.collect.ImmutableList;",
            "@Immutable class Test {",
            "  final ImmutableList<ImmutableList<ImmutableList<String>>> l = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableNonFinalField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains: non-final",
            "  int a = 42;",
            "}")
        .doTest();
  }

  @Test
  public void ignoreStaticFields() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  static int a = 42;",
            "}")
        .doTest();
  }

  @Test
  public void mutableField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.Map;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains:",
            "  final Map<String, String> a = null;",
            "}")
        .doTest();
  }

  @Test
  public void deeplyMutableTypeArguments() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.Map;",
            "import com.google.common.collect.ImmutableList;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains: instantiated with mutable type for 'E'",
            "  final ImmutableList<ImmutableList<ImmutableList<Map<String, String>>>> l = null;",
            "}")
        .doTest();
  }

  @Test
  public void rawImpliesImmutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.common.collect.ImmutableList;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains:",
            "  final ImmutableList l = null;",
            "}")
        .doTest();
  }

  @Test
  public void extendsImmutable() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable public class Super {",
            "  public final int x = 42;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test extends Super {",
            "}")
        .doTest();
  }

  @Test
  public void extendsMutable() {
    compilationHelper
        .addSourceLines("Super.java", "public class Super {", "  public int x = 42;", "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "// BUG: Diagnostic contains: 'Super' has non-final field 'x'",
            "@Immutable class Test extends Super {",
            "}")
        .doTest();
  }

  @Test
  public void extendsImmutableAnnotated_substBounds() {
    compilationHelper
        .addSourceLines(
            "SuperMost.java",
            "import com.google.errorprone.annotations.Immutable;",
            "public class SuperMost<B> {",
            "  public final B x = null;",
            "}")
        .addSourceLines(
            "Super.java",
            "import java.util.List;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf={\"A\"}) public class Super<A, B> extends SuperMost<A> {",
            "  public final int x = 42;",
            "}")
        .doTest();
  }

  @Test
  public void extendsImmutableAnnotated_mutableBounds() {
    compilationHelper
        .addSourceLines(
            "SuperMost.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf={\"A\"})",
            "public class SuperMost<A>  {",
            "  public final A x = null;",
            "}")
        .addSourceLines(
            "SubClass.java",
            "import java.util.List;",
            "import com.google.errorprone.annotations.Immutable;",
            "  // BUG: Diagnostic contains: instantiated with mutable type for 'A'",
            "@Immutable public class SubClass extends SuperMost<List<String>> {}")
        .doTest();
  }

  @Test
  public void typeParameterWithImmutableBound() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.common.collect.ImmutableList;",
            "@Immutable(containerOf=\"T\") class Test<T extends ImmutableList<String>> {",
            "  final T t = null;",
            "}")
        .doTest();
  }

  @Test
  public void immutableTypeArgumentInstantiation() {
    compilationHelper
        .addSourceLines(
            "Holder.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public class Holder<T> {",
            "  public final T t = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  final Holder<String> h = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableTypeArgumentInstantiation() {
    compilationHelper
        .addSourceLines(
            "Holder.java",
            "import com.google.errorprone.annotations.Immutable;",
            "public class Holder<T> {",
            "  public final T t = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains:",
            "  final Holder<Object> h = null;",
            "}")
        .doTest();
  }

  @Test
  public void instantiationWithMutableType() {
    compilationHelper
        .addSourceLines(
            "Holder.java",
            "import com.google.errorprone.annotations.Immutable;",
            "public class Holder<T> {",
            "  public final T t = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains: not annotated",
            "  final Holder<Object> h = null;",
            "}")
        .doTest();
  }

  @Test
  public void transitiveSuperSubstitutionImmutable() {
    compilationHelper
        .addSourceLines(
            "SuperMostType.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"N\") public class SuperMostType<N> {",
            "  public final N f = null;",
            "}")
        .addSourceLines(
            "MiddleClass.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"M\") public class MiddleClass<M> extends SuperMostType<M> {",
            "  // Empty",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test extends MiddleClass<String> {",
            "  final MiddleClass<String> f = null;",
            "}")
        .doTest();
  }

  @Ignore("http://b/72495910")
  @Test
  public void containerOf_extendsImmutable() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class X<V> {",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "  // BUG: Diagnostic contains: 'V' is a mutable type variable",
            "@Immutable(containerOf=\"V\") class Test<V> extends X<V> {",
            "  private final V t = null;",
            "}")
        .addSourceLines(
            "MutableLeak.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class MutableLeak {",
            "  private static class Mutable {",
            "    int mutableInt;",
            "  }",
            "  private final X<Mutable> bad = new Test<Mutable>();",
            "}")
        .doTest();
  }

  @Test
  public void containerOf_mutableInstantiation() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"V\") class X<V> {",
            "  private final V t = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test<T> {",
            "  // BUG: Diagnostic contains:",
            "  // 'X' was instantiated with mutable type for 'V'",
            "  // 'T' is a mutable type variable",
            "  private final X<T> t = null;",
            "}")
        .doTest();
  }

  @Test
  public void missingContainerOf() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.List;",
            "@Immutable class Test<T> {",
            "  // BUG: Diagnostic contains: 'T' is a mutable type variable",
            "  private final T t = null;",
            "}")
        .doTest();
  }

  @Test
  public void transitiveSuperSubstitutionMutable() {
    compilationHelper
        .addSourceLines(
            "SuperMostType.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"N\") public class SuperMostType<N> {",
            "  public final N f = null;",
            "}")
        .addSourceLines(
            "MiddleClass.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"M\") public class MiddleClass<M> extends SuperMostType<M> {",
            "  // Empty",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.List;",
            "// BUG: Diagnostic contains: instantiated with mutable type for 'M'",
            "@Immutable class Test extends MiddleClass<List> {",
            "}")
        .doTest();
  }

  @Test
  public void immutableInstantiation() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public class X<T> {",
            "  final ImmutableList<T> xs = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.List;",
            "@Immutable class Test {",
            "  final X<String> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableInstantiation() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.common.collect.ImmutableList;",
            "public class X<T> { final ImmutableList<T> xs = null; }")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "// BUG: Diagnostic contains:",
            "  final X<Object> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void immutableInstantiation_superBound() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.common.collect.ImmutableList;",
            "public class X<T> { final ImmutableList<? super T> xs = null; }")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.List;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains:",
            "  final X<String> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableInstantiation_superBound() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.common.collect.ImmutableList;",
            "public class X<T> { final ImmutableList<? super T> xs = null; }")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.List;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains: is not annotated",
            "  final X<String> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void immutableInstantiation_extendsBound() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public class X<T> {",
            "  final ImmutableList<? extends T> xs = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.List;",
            "@Immutable class Test {",
            "  final X<String> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableInstantiation_wildcard() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public class X<T> {",
            "  // BUG: Diagnostic contains: mutable type for 'E', 'Object' is mutable",
            "  final ImmutableList<?> xs = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.List;",
            "@Immutable class Test {",
            "  final X<String> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableInstantiation_extendsBound() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.common.collect.ImmutableList;",
            "@Immutable(containerOf=\"T\") public class X<T> {",
            "  final ImmutableList<? extends T> xs = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.List;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains: instantiated with mutable type",
            "  final X<Object> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void containerOf_noSuchType() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.errorprone.annotations.Immutable;",
            "// BUG: Diagnostic contains: could not find type(s) referenced by containerOf: Z",
            "@Immutable(containerOf=\"Z\") public class X<T> {",
            "  final int xs = 1;",
            "}")
        .doTest();
  }

  @Test
  public void immutableInstantiation_inferredImmutableType() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public class X<T> {",
            "  final T xs = null;",
            "}")
        .addSourceLines(
            "Y.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public class Y<T> {",
            "  final X<? extends T> xs = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  final Y<String> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableInstantiation_inferredImmutableType() {
    compilationHelper
        .addSourceLines("X.java", "public class X<T> {", "  final T xs = null;", "}")
        .addSourceLines("Y.java", "public class Y<T> {", "  final X<? extends T> xs = null;", "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains:",
            "  final Y<Object> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableWildInstantiation() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public class X<T> {",
            "  final ImmutableList<T> xs = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains: instantiated",
            "  final X<?> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableRawType() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public class X<T> {",
            "  final ImmutableList<T> xs = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains: raw",
            "  final X x = null;",
            "}")
        .doTest();
  }

  @Test
  public void testImmutableListImplementation() {
    compilationHelper
        .addSourceLines(
            "com/google/common/collect/ImmutableList.java",
            "package com.google.common.collect;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class ImmutableList<E> {",
            "  public Object[] veryMutable = null;",
            "}")
        .doTest();
  }

  @Test
  public void positiveAnonymous() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Super {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "class Test {{",
            "  new Super() {",
            "    // BUG: Diagnostic contains: non-final",
            "    int x = 0;",
            "    {",
            "      x++;",
            "    }",
            "  };",
            "}}")
        .doTest();
  }

  @Test
  public void positiveAnonymousInterface() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface Super {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "class Test {{",
            "  new Super() {",
            "    // BUG: Diagnostic contains: non-final",
            "    int x = 0;",
            "    {",
            "      x++;",
            "    }",
            "  };",
            "}}")
        .doTest();
  }

  @Test
  public void negativeParametricAnonymous() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") class Super<T> {",
            "  private final T t = null;",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "class Test {",
            "  static <T> Super<T> get() {",
            "    return new Super<T>() {};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void interface_containerOf_immutable() {
    compilationHelper
        .addSourceLines(
            "MyList.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public interface MyList<T> {",
            "  T get(int i);",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "public class Test {",
            "  private final MyList<Integer> l = null;",
            "}")
        .doTest();
  }

  @Test
  public void interface_containerOf_mutable() {
    compilationHelper
        .addSourceLines(
            "MyList.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public interface MyList<T> {",
            "  T get(int i);",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable public class Test<X> {",
            "  // BUG: Diagnostic contains: mutable type for 'T'",
            "  private final MyList<X> l = null;",
            "}")
        .doTest();
  }

  @Test
  public void implementsInterface_containerOf() {
    compilationHelper
        .addSourceLines(
            "MyList.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") public interface MyList<T> {",
            "  T get(int i);",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "// BUG: Diagnostic contains: 'X' is a mutable type",
            "@Immutable public class Test<X> implements MyList<X> {",
            "  public X get(int i) { return null; }",
            "}")
        .doTest();
  }

  // sub-type tests

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Super {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test extends Super {",
            "  // BUG: Diagnostic contains:"
                + " Class extends @Immutable type threadsafety.Super, but is not immutable: 'Test'"
                + " has non-final field 'x'",
            "  public int x = 0;",
            "}")
        .doTest();
  }

  @Test
  public void positiveContainerOf() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf={\"T\"}) class Super<T> {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test extends Super<Integer> {",
            "  // BUG: Diagnostic contains: non-final",
            "  public int x = 0;",
            "}")
        .doTest();
  }

  @Test
  public void positiveImplicitContainerOf() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf={\"T\"}) class Super<T> {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test<U> extends Super<U> {",
            "  // BUG: Diagnostic contains: mutable type for 'U'",
            "  public final Test<Object> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Super {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test extends Super {",
            "}")
        .doTest();
  }

  // Report errors in compilation order, and detect transitive errors even if immediate
  // supertype is unannotated.
  @Test
  public void transitive() {
    compilationHelper
        .addSourceLines(
            "threadsafety/I.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface I {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "// BUG: Diagnostic contains: extends @Immutable",
            "class Test implements J {",
            "  public int x = 0;",
            "}")
        .addSourceLines(
            "threadsafety/J.java", //
            "package threadsafety;",
            "// BUG: Diagnostic contains: extends @Immutable",
            "interface J extends I {",
            "}")
        .doTest();
  }

  // the type arguments are checked everywhere the super type is used
  @Test
  public void negativeAnonymousMutableBound() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") class Super<T> {",
            "  private final T t = null;",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "class Test {{",
            "  new Super<Object>() {};",
            "}}")
        .doTest();
  }

  @Test
  public void immutableAnonymousTypeScope() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"X\") class Super<X> {",
            "  private final X t = null;",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") class Test<T> {{",
            "  new Super<T>() {};",
            "}}")
        .doTest();
  }

  @Test
  public void immutableClassSuperTypeScope() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"Y\") class Super<Y> {",
            "  @Immutable(containerOf=\"X\") class Inner1<X> {",
            "    private final X x = null;",
            "    private final Y y = null;",
            "  }",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"U\") class Test<U> extends Super<U> {",
            "  @Immutable class Inner2 extends Inner1<U> {}",
            "}")
        .doTest();
  }

  @Test
  public void immutableClassTypeScope() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"X\") class Super<X> {",
            "  private final X t = null;",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") class Test<T> {",
            "  @Immutable class Inner extends Super<T> {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeAnonymousBound() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\") class Super<T> {",
            "  private final T t = null;",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "class Test {{",
            "  new Super<String>() {};",
            "}}")
        .doTest();
  }

  @Test
  public void negativeAnonymous() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Super {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "class Test {{",
            "  new Super() {};",
            "}}")
        .doTest();
  }

  @Test
  public void positiveEnumConstant() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface Super {",
            "  int f();",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable enum Test implements Super {",
            "  INSTANCE {",
            "    // BUG: Diagnostic contains: non-final",
            "    public int x = 0;",
            "    public int f() {",
            "      return x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeEnumConstant() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface Super {",
            "  void f();",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable enum Test implements Super {",
            "  INSTANCE {",
            "    public void f() {",
            "    }",
            "  }",
            "}")
        .doTest();
  }


  // any final null reference constant is immutable, but do we actually care?
  //
  // javac makes it annoying to figure this out - since null isn't a compile-time constant,
  // none of that machinery can be used. Instead, we need to look at the actual AST node
  // for the member declaration to see that it's initialized to null.
  @Ignore
  @Test
  public void immutableNull() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  final int[] xs = null;",
            "}")
        .doTest();
  }

  @Test
  public void suppressOnField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  @SuppressWarnings(\"Immutable\")",
            "  final int[] xs = {1};",
            "}")
        .doTest();
  }

  @Test
  public void suppressOnOneField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  @SuppressWarnings(\"Immutable\")",
            "  final int[] xs = {1};",
            "  // BUG: Diagnostic contains: arrays are mutable",
            "  final int[] ys = {1};",
            "}")
        .doTest();
  }

  @Test
  public void twoFieldsInSource() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains: arrays are mutable",
            "  final int[] xs = {1};",
            "  // BUG: Diagnostic contains: arrays are mutable",
            "  final int[] ys = {1};",
            "}")
        .doTest();
  }

  @Test
  public void protosNotOnClasspath() {
    compilationHelper
        .addSourceLines(
            "com/google/errorprone/annotations/Immutable.java",
            "package com.google.errorprone.annotations;",
            "import static java.lang.annotation.ElementType.TYPE;",
            "import static java.lang.annotation.RetentionPolicy.RUNTIME;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.Target;",
            "@Target(TYPE)",
            "@Retention(RUNTIME)",
            "public @interface Immutable {",
            "  String[] containerOf() default {};",
            "}")
        .addSourceLines("Foo.java", "class Foo {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains:"
                + " 'Foo' is not annotated with @com.google.errorprone.annotations.Immutable",
            "  final Foo f = null;",
            "}")
        .setArgs(Arrays.asList("-cp", "NOSUCH"))
        .doTest();
  }

  @Ignore("b/25630186") // don't check enums for immutability yet
  @Test
  public void mutableEnum() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "enum Test {",
            "  ;",
            "  // BUG: Diagnostic contains: @Immutable class has mutable field",
            "  private final Object o = null;",
            "}")
        .doTest();
  }

  @Ignore("b/25630186") // don't check enums for immutability yet
  @Test
  public void mutableEnumMember() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "enum Test {",
            "  ONE {",
            "    // BUG: Diagnostic contains: @Immutable class has mutable field",
            "    private final Object o = null;",
            "  }",
            "}")
        .doTest();
  }

  @Ignore("b/25630189") // don't check annotations for immutability yet
  @Test
  public void mutableExtendsAnnotation() {
    compilationHelper
        .addSourceLines(
            "Anno.java", //
            "@interface Anno {}")
        .addSourceLines(
            "Test.java",
            "abstract class Test implements Anno {",
            "  // BUG: Diagnostic contains: @Immutable class has mutable field",
            "  final Object o = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableEnclosing() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "public class Test {",
            "  int x = 0;",
            "  // BUG: Diagnostic contains: 'Inner' has mutable enclosing instance 'Test'",
            "  @Immutable public class Inner {",
            "    public int count() {",
            "      return x++;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  /** A sample superclass with a mutable field. */
  public static class SuperFieldSuppressionTest {
    @LazyInit public int x = 0;

    public int count() {
      return x++;
    }
  }

  @Test
  public void superFieldSuppression() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import " + SuperFieldSuppressionTest.class.getCanonicalName() + ";",
            "@Immutable public class Test extends SuperFieldSuppressionTest {}")
        .doTest();
  }

  @Test
  public void rawClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  final Class clazz = Test.class;",
            "}")
        .doTest();
  }

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  /** Test class annotated with @Immutable. */
  @Immutable(containerOf = {"T"})
  public static class ClassPathTest<T> {}

  static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
    String entryPath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
      jos.putNextEntry(new JarEntry(entryPath));
      ByteStreams.copy(is, jos);
    }
  }

  @Test
  public void incompleteClassPath() throws Exception {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      addClassToJar(jos, ImmutableCheckerTest.ClassPathTest.class);
      addClassToJar(jos, ImmutableCheckerTest.class);
    }
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import " + ClassPathTest.class.getCanonicalName() + ";",
            "class Test extends ClassPathTest<String> {",
            "  // BUG: Diagnostic contains: 'Test' has non-final field 'x'",
            "  int x;",
            "}")
        .setArgs(Arrays.asList("-cp", libJar.toString()))
        .doTest();
  }

  @Test
  public void knownImmutableFlag() {
    CompilationTestHelper.newInstance(ImmutableChecker.class, getClass())
        .setArgs(ImmutableList.of("-XepOpt:Immutable:KnownImmutable=threadsafety.SomeImmutable"))
        .addSourceLines(
            "threadsafety/SomeImmutable.java", "package threadsafety;", "class SomeImmutable {}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  public final SomeImmutable s = new SomeImmutable();",
            "}")
        .doTest();
  }

  @Test
  public void knownUnsafeFlag() {
    CompilationTestHelper.newInstance(ImmutableChecker.class, getClass())
        .setArgs(ImmutableList.of("-XepOpt:Immutable:KnownUnsafe=threadsafety.SomeUnsafe"))
        .addSourceLines(
            "threadsafety/SomeUnsafe.java", "package threadsafety;", "class SomeUnsafe {}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains: 'SomeUnsafe' is mutable",
            "  public final SomeUnsafe s = new SomeUnsafe();",
            "}")
        .doTest();
  }

  @Test
  public void lazyInit() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.errorprone.annotations.concurrent.LazyInit;",
            "@Immutable class Test {",
            "  @LazyInit int a = 42;",
            "}")
        .doTest();
  }

  @Test
  public void lazyInitMutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.errorprone.annotations.concurrent.LazyInit;",
            "import java.util.List;",
            "@Immutable class Test {",
            "  // BUG: Diagnostic contains: 'List' is mutable",
            "  @LazyInit List<Integer> a = null;",
            "}")
        .doTest();
  }

  @Test
  public void immutableTypeParameter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ImmutableTypeParameter;",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.common.collect.ImmutableList;",
            "@Immutable class Test<@ImmutableTypeParameter T> {",
            "  final T t = null;",
            "}")
        .doTest();
  }

  @Test
  public void immutableTypeParameterInstantiation() {
    compilationHelper
        .addSourceLines(
            "MyImmutableType.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class MyImmutableType {}")
        .addSourceLines(
            "MyMutableType.java",
            "import com.google.errorprone.annotations.Immutable;",
            "class MyMutableType {}")
        .addSourceLines(
            "A.java",
            "import com.google.errorprone.annotations.ImmutableTypeParameter;",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.common.collect.ImmutableList;",
            "@Immutable class A<@ImmutableTypeParameter T> {}")
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  A<String> f() {",
            "    return new A<>();",
            "  }",
            "  A<Object> g() {",
            "    // BUG: Diagnostic contains: instantiation of 'T' is mutable, 'Object' is mutable",
            "    return new A<>();",
            "  }",
            "  <T extends MyImmutableType> A<T> h() {",
            "    return new A<>();",
            "  }",
            "  <T extends MyMutableType> A<T> i() {",
            "    // BUG: Diagnostic contains: "
                + "instantiation of 'T' is mutable, 'T' is a mutable type variable",
            "    return new A<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableTypeParameterUsage() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ImmutableTypeParameter;",
            "class T {",
            "  static <@ImmutableTypeParameter T> void f() {}",
            "}")
        .doTest();
  }

  @Test
  public void immutableTypeParameterUsage_interface() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.errorprone.annotations.ImmutableTypeParameter;",
            "@Immutable interface T<@ImmutableTypeParameter T> {",
            "}")
        .doTest();
  }

  @Test
  public void immutableTypeParameterMutableClass() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "import com.google.errorprone.annotations.ImmutableTypeParameter;",
            " // BUG: Diagnostic contains: only supported on immutable classes",
            "class A<@ImmutableTypeParameter T> {}")
        .doTest();
  }

  @Test
  public void containerOf_extendsThreadSafe() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class X<V> {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "// BUG: Diagnostic contains: 'X' is not a container of 'V'",
            "@Immutable(containerOf = {\"Y\"}) class Test<Y> extends X<Y> {",
            "  private final Y t = null;",
            "}")
        .doTest();
  }

  @Test
  public void containerOf_extendsThreadSafeContainerOf() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf = {\"V\"}) class X<V> {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf = {\"Y\"}) class Test<Y> extends X<Y> {",
            "  private final Y t = null;",
            "}")
        .doTest();
  }

  @Test
  public void containerOf_extendsThreadSafe_nonContainer() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf = {\"V\"}) class X<U, V> {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf = {\"Y\"}) class Test<Y> extends X<Object, Y> {",
            "  private final Y t = null;",
            "}")
        .doTest();
  }

  @Test
  public void containerOf_extendsThreadSafe_interface() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface X<V> {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "// BUG: Diagnostic contains: 'X' is not a container of 'V'",
            "@Immutable(containerOf = {\"Y\"}) class Test<Y> implements X<Y> {",
            "  private final Y t = null;",
            "}")
        .doTest();
  }

  @Test
  public void containerOf_field() {
    compilationHelper
        .addSourceLines(
            "X.java", //
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface X<Y> {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"V\") class Test<V> {",
            "  private final X<V> t = null;",
            "}")
        .doTest();
  }

  @Test
  public void annotatedClassType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static java.lang.annotation.ElementType.TYPE_USE;",
            "import java.lang.annotation.Target;",
            "@Target(TYPE_USE) @interface A {}",
            "class Test {",
            "  Object o = new @A Object();",
            "}")
        .doTest();
  }

  @Ignore("b/77333859")
  @Test
  public void immutableInterfaceImplementationCapturesMutableState() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface I {",
            "  int f();",
            "}",
            "class Test {",
            "  int x;",
            "  I one = new I() {",
            "    public int f() {",
            "      return x++;",
            "    }",
            "  };",
            "  I two = () -> x++;",
            "}")
        .doTest();
  }

  @Test
  public void immutableUpperBound() {
    compilationHelper
        .addSourceLines(
            "MyImmutableType.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class MyImmutableType {}")
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test<T extends MyImmutableType, U extends T> {",
            "  final T t = null;",
            "  final U u = null;",
            "  final ImmutableList<? extends U> v = null;",
            "}")
        .doTest();
  }

  @Test
  public void immutableRecursiveUpperBound() {
    compilationHelper
        .addSourceLines(
            "Recursive.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable",
            "abstract class Recursive<T extends Recursive<T>> {",
            "  final T x = null;",
            "}")
        .doTest();
  }

  @Test
  public void immutableRecursiveUpperBound_notImmutable() {
    compilationHelper
        .addSourceLines(
            "Recursive.java",
            "import com.google.errorprone.annotations.Immutable;",
            "import java.util.List;",
            "@Immutable",
            "abstract class Recursive<T extends Recursive<T>> {",
            "  final T x = null;",
            "  // BUG: Diagnostic contains: 'Recursive' has field 'y' of type 'java.util.List<T>'",
            "  final List<T> y = null;",
            "}")
        .doTest();
  }

  @Test
  public void immutableUpperBoundAndContainerOfInconsistency() {
    compilationHelper
        .addSourceLines(
            "ImmutableInterface.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface ImmutableInterface {}")
        .addSourceLines(
            "MutableImpl.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@SuppressWarnings(\"Immutable\") class MutableImpl implements ImmutableInterface {",
            "  int mutableField;",
            "}")
        .addSourceLines(
            "WithContainerOf.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable(containerOf=\"T\")",
            "class WithContainerOf<T extends ImmutableInterface> { final T x = null; }")
        .addSourceLines(
            "WithoutContainerOf.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable",
            "class WithoutContainerOf<T extends ImmutableInterface> { final T x = null; }")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  final WithContainerOf<ImmutableInterface> a = null;",
            "  final WithoutContainerOf<ImmutableInterface> b = null;",
            "  // BUG: Diagnostic contains: field 'c' of type 'WithContainerOf<MutableImpl>'",
            "  final WithContainerOf<MutableImpl> c = null;",
            "  final WithoutContainerOf<MutableImpl> d = null;",
            "}")
        .doTest();
  }

  // regression test for b/77781008
  @Test
  public void immutableTypeParameter_twoInstantiations() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ImmutableTypeParameter;",
            "import com.google.errorprone.annotations.Immutable;",
            "import com.google.common.collect.ImmutableList;",
            "@Immutable class Test<@ImmutableTypeParameter T> {",
            "  <@ImmutableTypeParameter T> T f(T t) { return t; }",
            "  <@ImmutableTypeParameter T> void g(T a, T b) {}",
            "  @Immutable interface I {}",
            "  void test(I i) {",
            "    g(f(i), f(i));",
            "  }",
            "}")
        .doTest();
  }
}
