/*
 * Copyright 2023 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ThreadSafeChecker}Test */
@RunWith(JUnit4.class)
public class ThreadSafeCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ThreadSafeChecker.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ThreadSafeChecker.class, getClass());

  @Test
  public void basicFields() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.util.concurrent.ConcurrentMap;",
            "import java.util.concurrent.atomic.AtomicLong;",
            "@ThreadSafe class Test {",
            "  final int a = 42;",
            "  final String b = null;",
            "  final java.lang.String c = null;",
            "  final com.google.common.collect.ImmutableList<String> d = null;",
            "  final ImmutableList<Integer> e = null;",
            "  final Deprecated dep = null;",
            "  final Class<?> clazz = Class.class;",
            "  final ConcurrentMap<Long, AtomicLong> concurrentMap = null;",
            "}")
        .doTest();
  }

  @Test
  public void interfacesMutableByDefault() {
    compilationHelper
        .addSourceLines("I.java", "interface I {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains: 'I' is not annotated with @"
                + ThreadSafe.class.getName(),
            "  private final I i = new I() {};",
            "}")
        .doTest();
  }

  @Test
  public void refactoringWithNameClash() {
    refactoringHelper
        .addInputLines(
            "I.java", //
            "@com.google.errorprone.annotations.ThreadSafe interface I {}")
        .expectUnchanged()
        .addInputLines(
            "ThreadSafe.java", //
            "class ThreadSafe implements I {",
            "}")
        .addOutputLines(
            "ThreadSafe.java",
            "@com.google.errorprone.annotations.ThreadSafe class ThreadSafe implements I {",
            "}")
        .doTest();
  }

  @Test
  public void annotationsCanBeAnnotatedWithThreadSafe() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe @interface Test {}")
        .doTest();
  }

  @Test
  public void customAnnotationsMightBeMutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe @interface Test {}")
        .addSourceLines(
            "MyTest.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.lang.annotation.Annotation;",
            "@ThreadSafe final class MyTest implements Test {",
            "  // BUG: Diagnostic contains: should be final or annotated",
            "  public Object[] xs = {};",
            "  public Class<? extends Annotation> annotationType() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void customAnnotationsSubtype() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe @interface Test {}")
        .addSourceLines(
            "MyTest.java",
            "import java.lang.annotation.Annotation;",
            "// BUG: Diagnostic contains:",
            "// extends @ThreadSafe type Test, but is not annotated as threadsafe",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import javax.lang.model.element.ElementKind;",
            "@ThreadSafe class Test {",
            "  private final Override override = null;",
            "}")
        .doTest();
  }

  @Test
  public void enumsDefaultToImmutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import javax.lang.model.element.ElementKind;",
            "@ThreadSafe class Test {",
            "  private final ElementKind ek = null;",
            "}")
        .doTest();
  }

  @Test
  public void enumsMayBeImmutable() {
    compilationHelper
        .addSourceLines(
            "Kind.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe enum Kind { A, B, C; }")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  private final Kind k = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableArray() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains:",
            "  final int[] xs = {42};",
            "}")
        .doTest();
  }

  @Test
  public void immutableAnnotatedNotTested() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable class Test {",
            "  final int[] xs = {42};",
            "}")
        .doTest();
  }

  @Test
  public void immutableAnnotatedNotTested_inheritance() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface Test {}")
        .addSourceLines(
            "MyTest.java",
            "final class MyTest implements Test {",
            "  public Object[] xs = {};",
            "}")
        .doTest();
  }

  @Test
  public void annotatedThreadSafeInterfaces() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe interface Test {}")
        .doTest();
  }

  @Test
  public void threadSafeInterfaceField() {
    compilationHelper
        .addSourceLines(
            "MyInterface.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe interface MyInterface {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  final MyInterface i = null;",
            "}")
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  final MyInterface i = null;",
            "}")
        .doTest();
  }

  @Test
  public void deeplyImmutableArguments() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  final ImmutableList<ImmutableList<ImmutableList<String>>> l = null;",
            "}")
        .doTest();
  }

  @Test
  public void deeplyThreadsafeArguments() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.util.concurrent.ConcurrentMap;",
            "import java.util.concurrent.atomic.AtomicInteger;",
            "@ThreadSafe class Test {",
            "  final ConcurrentMap<String, ConcurrentMap<Long,",
            "      ImmutableList<AtomicInteger>>> l = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableNonFinalField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains: should be final or annotated",
            "  int a = 42;",
            "}")
        .doTest();
  }

  @Test
  public void mutableStaticFields() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.util.List;",
            "import java.util.Map;",
            "@ThreadSafe class Test {",
            "  static int a = 42;",
            "  static final Map<Long, List<Long>> b = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableFieldGuardedByJsr305Annotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.util.List;",
            "import java.util.Map;",
            "import javax.annotation.concurrent.GuardedBy;",
            "@ThreadSafe class Test {",
            "  @GuardedBy(\"this\") int a = 42;",
            "  @GuardedBy(\"this\") final Map<Long, List<Long>> b = null;",
            "  @GuardedBy(\"this\") volatile int c = 42;",
            "}")
        .doTest();
  }

  @Test
  public void mutableFieldGuardedByErrorProneAnnotation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import com.google.errorprone.annotations.concurrent.GuardedBy;",
            "import java.util.List;",
            "import java.util.Map;",
            "@ThreadSafe class Test {",
            "  @GuardedBy(\"this\") int a = 42;",
            "  @GuardedBy(\"this\") final Map<Long, List<Long>> b = null;",
            "  @GuardedBy(\"this\") volatile int c = 42;",
            "}")
        .doTest();
  }

  @Test
  public void mutableFieldNotGuarded() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import javax.annotation.concurrent.GuardedBy;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains: @GuardedBy",
            "  volatile int a = 42;",
            "}")
        .doTest();
  }

  @Test
  public void mutableField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.util.Map;",
            "@ThreadSafe class Test {",
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
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.util.Map;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains: instantiated with non-thread-safe type for 'E'",
            "  final ImmutableList<ImmutableList<ImmutableList<Map<String, String>>>> l = null;",
            "}")
        .doTest();
  }

  @Test
  public void rawImpliesImmutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains: was raw",
            "  final ImmutableList l = null;",
            "}")
        .doTest();
  }

  @Test
  public void extendsThreadSafe() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe public class Super {",
            "  public final int x = 42;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test extends Super {",
            "}")
        .doTest();
  }

  @Test
  public void extendsThreadSafe_annotatedWithImmutable() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe public class Super {",
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
        .addSourceLines(
            "Super.java", //
            "public class Super {",
            "  public int x = 42;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "// BUG: Diagnostic contains: 'Super' has non-final field 'x'",
            "@ThreadSafe class Test extends Super {",
            "}")
        .doTest();
  }

  @Test
  public void mutableTypeArgumentInstantiation() {
    compilationHelper
        .addSourceLines(
            "Holder.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "public class Holder<T> {",
            "  public final T t = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "public class Holder<T> {",
            "  public final T t = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains: not annotated",
            "  final Holder<Object> h = null;",
            "}")
        .doTest();
  }

  @Test
  public void missingContainerOf() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.util.List;",
            "@ThreadSafe class Test<T> {",
            "  // BUG: Diagnostic contains: 'T' is a non-thread-safe type variable",
            "  private final T t = null;",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.util.List;",
            "@ThreadSafe class Test {",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.util.List;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains: is not annotated",
            "  final X<String> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void mutableInstantiation_inferredImmutableType() {
    compilationHelper
        .addSourceLines(
            "X.java", //
            "public class X<T> {",
            "  final T xs = null;",
            "}")
        .addSourceLines(
            "Y.java", //
            "public class Y<T> {",
            "  final X<? extends T> xs = null;",
            "}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains:",
            "  final Y<Object> x = null;",
            "}")
        .doTest();
  }

  @Test
  public void testImmutableListImplementation() {
    compilationHelper
        .addSourceLines(
            "com/google/common/collect/ImmutableList.java",
            "package com.google.common.collect;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class ImmutableList<E> {",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Super {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "class Test {{",
            "  new Super() {",
            "    // BUG: Diagnostic contains: should be final or annotated",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe interface Super {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "class Test {{",
            "  new Super() {",
            "    // BUG: Diagnostic contains: should be final or annotated",
            "    int x = 0;",
            "    {",
            "      x++;",
            "    }",
            "  };",
            "}}")
        .doTest();
  }

  // sub-type tests

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Super {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test extends Super {",
            "  // BUG: Diagnostic contains: should be final or annotated",
            "  public int x = 0;",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Super {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test extends Super {",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe interface I {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "// BUG: Diagnostic contains: extends @ThreadSafe",
            "class Test implements J {",
            "  public int x = 0;",
            "}")
        .addSourceLines(
            "threadsafety/J.java",
            "package threadsafety;",
            "// BUG: Diagnostic contains: extends @ThreadSafe",
            "interface J extends I {",
            "}")
        .doTest();
  }

  // the type arguments are checked everywhere the super type is used

  @Test
  public void negativeAnonymous() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Super.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Super {",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.ThreadSafe;",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe interface Super {",
            "  int f();",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe enum Test implements Super {",
            "  INSTANCE {",
            "    // BUG: Diagnostic contains: should be final or annotated",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe interface Super {",
            "  void f();",
            "}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe enum Test implements Super {",
            "  INSTANCE {",
            "    public void f() {",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  // TODO(cushon): we could probably run this externally, but we'd have to
  // build protos with maven.

  private String jarPath(Class<?> clazz) throws Exception {
    URI uri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
    return new File(uri).toString();
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  final int[] xs = null;",
            "}")
        .doTest();
  }

  @Test
  public void twoFieldsInSource() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains: arrays are not thread-safe",
            "  final int[] xs = {1};",
            "  // BUG: Diagnostic contains: arrays are not thread-safe",
            "  final int[] ys = {1};",
            "}")
        .doTest();
  }

  @Test
  public void protosNotOnClasspath() {
    compilationHelper
        .addSourceLines(
            "com/google/errorprone/annotations/ThreadSafe.java",
            "package com.google.errorprone.annotations;",
            "import static java.lang.annotation.ElementType.TYPE;",
            "import static java.lang.annotation.RetentionPolicy.RUNTIME;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.Target;",
            "@Target(TYPE)",
            "@Retention(RUNTIME)",
            "public @interface ThreadSafe {",
            "}")
        .addSourceLines("Foo.java", "class Foo {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains: 'Foo' is not annotated with @"
                + ThreadSafe.class.getName(),
            "  final Foo f = null;",
            "}")
        .setArgs(Arrays.asList("-cp", "NOSUCH"))
        .doTest();
  }

  @Test
  public void mutableEnclosing() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "public class Test {",
            "  int x = 0;",
            "  // BUG: Diagnostic contains: 'Inner' has non-thread-safe "
                + "enclosing instance 'Test'",
            "  @ThreadSafe public class Inner {",
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
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import " + SuperFieldSuppressionTest.class.getCanonicalName() + ";",
            "@ThreadSafe public class Test extends SuperFieldSuppressionTest {}")
        .doTest();
  }

  @Test
  public void lazyInit() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import com.google.errorprone.annotations.concurrent.LazyInit;",
            "@ThreadSafe class Test {",
            "  @LazyInit int a = 42;",
            "}")
        .doTest();
  }

  @Test
  public void lazyInitMutable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import com.google.errorprone.annotations.concurrent.LazyInit;",
            "import java.util.List;",
            "@ThreadSafe class Test {",
            "  // BUG: Diagnostic contains: 'List' is not thread-safe",
            "  @LazyInit List<Integer> a = null;",
            "}")
        .doTest();
  }

  @Test
  public void rawClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  final Class clazz = Test.class;",
            "}")
        .doTest();
  }

  @Ignore("b/26797524 - add tests for generic arguments")
  @Test
  public void mutableTypeParam() {
    compilationHelper
        .addSourceLines(
            "X.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "public class X {",
            "  final ImmutableList<@ThreadSafe ?> unknownSafeType;",
            "  X (ImmutableList<@ThreadSafe ?> unknownSafeType) {",
            "      this.unknownSafeType = unknownSafeType;",
            "  }",
            "}")
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "// BUG: Diagnostic contains:",
            "  final X badX = new X(ImmutableList.of(new ArrayList<String>()));",
            "}")
        .doTest();
  }

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
    String entryPath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
      jos.putNextEntry(new JarEntry(entryPath));
      ByteStreams.copy(is, jos);
    }
  }

  @Test
  public void knownThreadSafeFlag() {
    CompilationTestHelper.newInstance(ThreadSafeChecker.class, getClass())
        .setArgs(ImmutableList.of("-XepOpt:ThreadSafe:KnownThreadSafe=threadsafety.SomeImmutable"))
        .addSourceLines(
            "threadsafety/SomeImmutable.java", "package threadsafety;", "class SomeImmutable {}")
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test {",
            "  public final SomeImmutable s = new SomeImmutable();",
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

  // Regression test for b/117937500

  // javac does not instantiate type variables when they are not used for target typing, so we
  // cannot check whether their instantiations are thread-safe.

  @Test
  public void threadSafeUpperBound() {
    compilationHelper
        .addSourceLines(
            "MyThreadSafeType.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class MyThreadSafeType {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe class Test<T extends MyThreadSafeType> {",
            "  final T x = null;",
            "}")
        .doTest();
  }

  @Test
  public void threadSafeRecursiveUpperBound() {
    compilationHelper
        .addSourceLines(
            "Recursive.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "@ThreadSafe",
            "abstract class Recursive<T extends Recursive<T>> {",
            "  final T x = null;",
            "}")
        .doTest();
  }

  @Test
  public void threadSafeRecursiveUpperBound_notThreadsafe() {
    compilationHelper
        .addSourceLines(
            "Recursive.java",
            "import com.google.errorprone.annotations.ThreadSafe;",
            "import java.util.List;",
            "@ThreadSafe",
            "abstract class Recursive<T extends Recursive<T>> {",
            "  final T x = null;",
            "  // BUG: Diagnostic contains: @ThreadSafe class has "
                + "non-thread-safe field, 'List' is not thread-safe",
            "  final List<T> y = null;",
            "}")
        .doTest();
  }
}
