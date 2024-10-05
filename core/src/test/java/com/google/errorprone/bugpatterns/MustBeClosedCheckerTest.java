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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link MustBeClosedChecker}. */
@RunWith(JUnit4.class)
public class MustBeClosedCheckerTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(MustBeClosedChecker.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MustBeClosedChecker.class, getClass());

  private static final String POSITIVE_CASES =
      """
package com.google.errorprone.bugpatterns.testdata;

import static java.io.OutputStream.nullOutputStream;

import com.google.errorprone.annotations.MustBeClosed;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings({"UnusedNestedClass", "UnusedVariable"})
class MustBeClosedCheckerPositiveCases {

  class DoesNotImplementAutoCloseable {
    @MustBeClosed
    // BUG: Diagnostic contains: MustBeClosed should only annotate constructors of AutoCloseables.
    DoesNotImplementAutoCloseable() {}

    @MustBeClosed
    // BUG: Diagnostic contains: MustBeClosed should only annotate methods that return an
    // AutoCloseable.
    void doesNotReturnAutoCloseable() {}
  }

  class Closeable implements AutoCloseable {

    @Override
    public void close() {}

    public int method() {
      return 1;
    }
  }

  class Foo {

    @MustBeClosed
    Closeable mustBeClosedAnnotatedMethod() {
      return new Closeable();
    }

    void sameClass() {
      // BUG: Diagnostic contains:
      mustBeClosedAnnotatedMethod();
    }
  }

  class MustBeClosedAnnotatedConstructor extends Closeable {

    @MustBeClosed
    MustBeClosedAnnotatedConstructor() {}

    void sameClass() {
      // BUG: Diagnostic contains:
      new MustBeClosedAnnotatedConstructor();
    }
  }

  void positiveCase1() {
    // BUG: Diagnostic contains:
    new Foo().mustBeClosedAnnotatedMethod();
  }

  void positiveCase2() {
    // BUG: Diagnostic contains:
    Closeable closeable = new Foo().mustBeClosedAnnotatedMethod();
  }

  void positiveCase3() {
    try {
      // BUG: Diagnostic contains:
      new Foo().mustBeClosedAnnotatedMethod();
    } finally {
    }
  }

  void positiveCase4() {
    try (Closeable c = new Foo().mustBeClosedAnnotatedMethod()) {
      // BUG: Diagnostic contains:
      new Foo().mustBeClosedAnnotatedMethod();
    }
  }

  void positiveCase5() {
    // BUG: Diagnostic contains:
    new MustBeClosedAnnotatedConstructor();
  }

  Closeable positiveCase6() {
    // BUG: Diagnostic contains:
    return new MustBeClosedAnnotatedConstructor();
  }

  Closeable positiveCase7() {
    // BUG: Diagnostic contains:
    return new Foo().mustBeClosedAnnotatedMethod();
  }

  int existingDeclarationUsesVar() {
    // BUG: Diagnostic contains:
    var result = new Foo().mustBeClosedAnnotatedMethod();
    return 0;
  }

  boolean twoCloseablesInOneExpression() {
    // BUG: Diagnostic contains:
    return new Foo().mustBeClosedAnnotatedMethod() == new Foo().mustBeClosedAnnotatedMethod();
  }

  void voidLambda() {
    // Lambda has a fixless finding because no reasonable fix can be suggested.
    // BUG: Diagnostic contains:
    Runnable runnable = () -> new Foo().mustBeClosedAnnotatedMethod();
  }

  void expressionLambda() {
    Supplier<Closeable> supplier =
        () ->
            // BUG: Diagnostic contains:
            new Foo().mustBeClosedAnnotatedMethod();
  }

  void statementLambda() {
    Supplier<Closeable> supplier =
        () -> {
          // BUG: Diagnostic contains:
          return new Foo().mustBeClosedAnnotatedMethod();
        };
  }

  void methodReference() {
    Supplier<Closeable> supplier =
        // TODO(b/218377318): BUG: Diagnostic contains:
        new Foo()::mustBeClosedAnnotatedMethod;
  }

  void anonymousClass() {
    new Foo() {
      @Override
      public Closeable mustBeClosedAnnotatedMethod() {
        // BUG: Diagnostic contains:
        return new MustBeClosedAnnotatedConstructor();
      }
    };
  }

  void subexpression() {
    // BUG: Diagnostic contains:
    new Foo().mustBeClosedAnnotatedMethod().method();
  }

  void ternary(boolean condition) {
    // BUG: Diagnostic contains:
    int result = condition ? new Foo().mustBeClosedAnnotatedMethod().method() : 0;
  }

  int variableDeclaration() {
    // BUG: Diagnostic contains:
    int result = new Foo().mustBeClosedAnnotatedMethod().method();
    return result;
  }

  void tryWithResources_nonFinal() {
    Foo foo = new Foo();
    // BUG: Diagnostic contains:
    Closeable closeable = foo.mustBeClosedAnnotatedMethod();
    try {
      closeable = null;
    } finally {
      closeable.close();
    }
  }

  void tryWithResources_noClose() {
    Foo foo = new Foo();
    // BUG: Diagnostic contains:
    Closeable closeable = foo.mustBeClosedAnnotatedMethod();
    try {
    } finally {
    }
  }

  class CloseableFoo implements AutoCloseable {

    @MustBeClosed
    CloseableFoo() {}

    // Doesn't autoclose Foo on Stream close.
    Stream<String> stream() {
      return null;
    }

    @Override
    public void close() {}
  }

  void twrStream() {
    // BUG: Diagnostic contains:
    try (Stream<String> stream = new CloseableFoo().stream()) {}
  }

  void constructorsTransitivelyRequiredAnnotation() {
    abstract class Parent implements AutoCloseable {
      @MustBeClosed
      Parent() {}

      // BUG: Diagnostic contains: Invoked constructor is marked @MustBeClosed
      Parent(int i) {
        this();
      }
    }

    // BUG: Diagnostic contains: Implicitly invoked constructor is marked @MustBeClosed
    abstract class ChildDefaultConstructor extends Parent {}

    abstract class ChildExplicitConstructor extends Parent {
      // BUG: Diagnostic contains: Invoked constructor is marked @MustBeClosed
      ChildExplicitConstructor() {}

      // BUG: Diagnostic contains: Invoked constructor is marked @MustBeClosed
      ChildExplicitConstructor(int a) {
        super();
      }
    }
  }

  @MustBeClosed
  OutputStream mustBeClosedOutputStream() {
    return nullOutputStream();
  }

  void decoratorConstructorThrows() throws IOException {
    // BUG: Diagnostic contains:
    try (var s = new GZIPOutputStream(mustBeClosedOutputStream())) {}
  }

  void notClosedByDecorator() throws IOException {
    class NotFilterOutputStream extends ByteArrayOutputStream {
      NotFilterOutputStream(OutputStream out) {}
    }
    // BUG: Diagnostic contains:
    try (var s = new NotFilterOutputStream(mustBeClosedOutputStream())) {}
  }

  OutputStream decoratorMustBeClosed() {
    class MustBeClosedFilter extends FilterOutputStream {
      @MustBeClosed
      MustBeClosedFilter(OutputStream out) {
        super(out);
      }
    }
    // BUG: Diagnostic contains:
    return new MustBeClosedFilter(
        // handled above
        mustBeClosedOutputStream());
  }
}
""";

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines("MustBeClosedCheckerPositiveCases.java", POSITIVE_CASES)
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "MustBeClosedCheckerNegativeCases.java",
            """
package com.google.errorprone.bugpatterns.testdata;

import static java.io.InputStream.nullInputStream;
import static java.io.OutputStream.nullOutputStream;
import static java.io.Reader.nullReader;
import static java.io.Writer.nullWriter;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.MustBeClosed;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

@SuppressWarnings({"UnnecessaryCast", "LambdaToMemberReference"})
public class MustBeClosedCheckerNegativeCases {

  class Closeable implements AutoCloseable {

    @Override
    public void close() {}
  }

  class Foo {

    void bar() {}

    @MustBeClosed
    Closeable mustBeClosedAnnotatedMethod() {
      return new Closeable();
    }
  }

  class MustBeClosedAnnotatedConstructor extends Closeable {

    @MustBeClosed
    MustBeClosedAnnotatedConstructor() {}
  }

  @SuppressWarnings("MustBeClosedChecker")
  void respectsSuppressWarnings_onMethod() {
    new Foo().mustBeClosedAnnotatedMethod();
  }

  void respectsSuppressWarnings_onLocal() {
    @SuppressWarnings("MustBeClosedChecker")
    var unused = new Foo().mustBeClosedAnnotatedMethod();
  }

  void negativeCase3() {
    try (Closeable closeable = new Foo().mustBeClosedAnnotatedMethod()) {}
  }

  void negativeCase4() {
    Foo foo = new Foo();
    try (Closeable closeable = foo.mustBeClosedAnnotatedMethod()) {}
  }

  void negativeCase5() {
    new Foo().bar();
  }

  void negativeCase6() {
    try (MustBeClosedAnnotatedConstructor foo = new MustBeClosedAnnotatedConstructor()) {}
  }

  void negativeCase7() {
    try (MustBeClosedAnnotatedConstructor foo = new MustBeClosedAnnotatedConstructor();
        Closeable closeable = new Foo().mustBeClosedAnnotatedMethod()) {}
  }

  @MustBeClosed
  Closeable positiveCase8() {
    // This is fine since the caller method is annotated.
    return new MustBeClosedAnnotatedConstructor();
  }

  @MustBeClosed
  Closeable positiveCase7() {
    // This is fine since the caller method is annotated.
    return new Foo().mustBeClosedAnnotatedMethod();
  }

  @MustBeClosed
  Closeable ternary(boolean condition) {
    return condition ? new Foo().mustBeClosedAnnotatedMethod() : null;
  }

  @MustBeClosed
  Closeable cast() {
    // TODO(b/241012760): remove the following line after the bug is fixed.
    // BUG: Diagnostic contains:
    return (Closeable) new Foo().mustBeClosedAnnotatedMethod();
  }

  void tryWithResources() {
    Foo foo = new Foo();
    Closeable closeable = foo.mustBeClosedAnnotatedMethod();
    try {
    } finally {
      closeable.close();
    }
  }

  void mockitoWhen(Foo mockFoo) {
    when(mockFoo.mustBeClosedAnnotatedMethod()).thenReturn(null);
    doReturn(null).when(mockFoo).mustBeClosedAnnotatedMethod();
  }

  void testException() {
    try {
      ((Foo) null).mustBeClosedAnnotatedMethod();
      fail();
    } catch (NullPointerException e) {
    }
  }

  abstract class ParentWithNoArgument implements AutoCloseable {
    @MustBeClosed
    ParentWithNoArgument() {}
  }

  abstract class ParentWithArgument implements AutoCloseable {
    @MustBeClosed
    ParentWithArgument(int i) {}
  }

  abstract class ChildOfParentWithArgument extends ParentWithArgument {
    @MustBeClosed
    ChildOfParentWithArgument() {
      super(0);
    }
  }

  interface ResourceFactory {
    @MustBeClosed
    MustBeClosedAnnotatedConstructor getResource();
  }

  void consumeCloseable(ResourceFactory factory) {
    try (Closeable c = factory.getResource()) {}
  }

  void expressionLambdaReturningCloseable() {
    consumeCloseable(() -> new MustBeClosedAnnotatedConstructor());
  }

  void statementLambdaReturningCloseable() {
    consumeCloseable(
        () -> {
          return new MustBeClosedAnnotatedConstructor();
        });
  }

  void methodReferenceReturningCloseable() {
    consumeCloseable(MustBeClosedAnnotatedConstructor::new);
  }

  void ternaryFunctionalExpressionReturningCloseable(boolean condition) {
    consumeCloseable(
        condition
            ? () -> new MustBeClosedAnnotatedConstructor()
            : MustBeClosedAnnotatedConstructor::new);
  }

  void inferredFunctionalExpressionReturningCloseable(ResourceFactory factory) {
    ImmutableList.of(
            factory,
            () -> new MustBeClosedAnnotatedConstructor(),
            MustBeClosedAnnotatedConstructor::new)
        .forEach(this::consumeCloseable);
  }

  @MustBeClosed
  <C extends AutoCloseable> C mustBeClosed(C c) {
    return c;
  }

  void closedByDecorator() throws IOException {
    try (var in = new BufferedInputStream(mustBeClosed(nullInputStream()))) {}
    try (var out = new BufferedOutputStream(mustBeClosed(nullOutputStream()))) {}

    try (var in = new BufferedInputStream(mustBeClosed(nullInputStream()), 1024)) {}
    try (var out = new BufferedOutputStream(mustBeClosed(nullOutputStream()), 1024)) {}

    try (var r = new InputStreamReader(mustBeClosed(nullInputStream()))) {}
    try (var w = new OutputStreamWriter(mustBeClosed(nullOutputStream()))) {}

    try (var r = new BufferedReader(mustBeClosed(nullReader()))) {}
    try (var w = new BufferedWriter(mustBeClosed(nullWriter()))) {}
  }
}
""")
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines("MustBeClosedCheckerPositiveCases.java", POSITIVE_CASES)
        .addOutputLines(
            "MustBeClosedCheckerPositiveCases_expected.java",
            """
package com.google.errorprone.bugpatterns.testdata;

import static java.io.OutputStream.nullOutputStream;

import com.google.errorprone.annotations.MustBeClosed;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings({"UnusedNestedClass", "UnusedVariable"})
class MustBeClosedCheckerPositiveCases {

  class DoesNotImplementAutoCloseable {
    @MustBeClosed
    // BUG: Diagnostic contains: MustBeClosed should only annotate constructors of AutoCloseables.
    DoesNotImplementAutoCloseable() {}

    @MustBeClosed
    // BUG: Diagnostic contains: MustBeClosed should only annotate methods that return an
    // AutoCloseable.
    void doesNotReturnAutoCloseable() {}
  }

  class Closeable implements AutoCloseable {

    @Override
    public void close() {}

    public int method() {
      return 1;
    }
  }

  class Foo {

    @MustBeClosed
    Closeable mustBeClosedAnnotatedMethod() {
      return new Closeable();
    }

    void sameClass() {
      // BUG: Diagnostic contains:
      try (var closeable = mustBeClosedAnnotatedMethod()) {}
    }
  }

  class MustBeClosedAnnotatedConstructor extends Closeable {

    @MustBeClosed
    MustBeClosedAnnotatedConstructor() {}

    void sameClass() {
      // BUG: Diagnostic contains:
      try (var mustBeClosedAnnotatedConstructor = new MustBeClosedAnnotatedConstructor()) {}
    }
  }

  void positiveCase1() {
    // BUG: Diagnostic contains:
    try (var closeable = new Foo().mustBeClosedAnnotatedMethod()) {}
  }

  void positiveCase2() {
    // BUG: Diagnostic contains:
    try (Closeable closeable = new Foo().mustBeClosedAnnotatedMethod()) {}
  }

  void positiveCase3() {
    try {
      // BUG: Diagnostic contains:
      try (var closeable = new Foo().mustBeClosedAnnotatedMethod()) {}
    } finally {
    }
  }

  void positiveCase4() {
    try (Closeable c = new Foo().mustBeClosedAnnotatedMethod()) {
      // BUG: Diagnostic contains:
      try (var closeable = new Foo().mustBeClosedAnnotatedMethod()) {}
    }
  }

  void positiveCase5() {
    // BUG: Diagnostic contains:
    try (var mustBeClosedAnnotatedConstructor = new MustBeClosedAnnotatedConstructor()) {}
  }

  @MustBeClosed
  Closeable positiveCase6() {
    // BUG: Diagnostic contains:
    return new MustBeClosedAnnotatedConstructor();
  }

  @MustBeClosed
  Closeable positiveCase7() {
    // BUG: Diagnostic contains:
    return new Foo().mustBeClosedAnnotatedMethod();
  }

  int existingDeclarationUsesVar() {
    // Bug: Diagnostic contains:
    try (var result = new Foo().mustBeClosedAnnotatedMethod()) {
      return 0;
    }
  }

  boolean twoCloseablesInOneExpression() {
    // BUG: Diagnostic contains:
    try (var closeable = new Foo().mustBeClosedAnnotatedMethod()) {
      try (var closeable2 = new Foo().mustBeClosedAnnotatedMethod()) {
        return closeable == closeable2;
      }
    }
  }

  void voidLambda() {
    // Lambda has a fixless finding because no reasonable fix can be suggested.
    // BUG: Diagnostic contains:
    Runnable runnable = () -> new Foo().mustBeClosedAnnotatedMethod();
  }

  void expressionLambda() {
    Supplier<Closeable> supplier =
        () ->
            // BUG: Diagnostic contains:
            new Foo().mustBeClosedAnnotatedMethod();
  }

  void statementLambda() {
    Supplier<Closeable> supplier =
        () -> {
          // BUG: Diagnostic contains:
          return new Foo().mustBeClosedAnnotatedMethod();
        };
  }

  void methodReference() {
    Supplier<Closeable> supplier =
        // TODO(b/218377318): BUG: Diagnostic contains:
        new Foo()::mustBeClosedAnnotatedMethod;
  }

  void anonymousClass() {
    new Foo() {
      @MustBeClosed
      @Override
      public Closeable mustBeClosedAnnotatedMethod() {
        // BUG: Diagnostic contains:
        return new MustBeClosedAnnotatedConstructor();
      }
    };
  }

  void subexpression() {
    // BUG: Diagnostic contains:
    try (var closeable = new Foo().mustBeClosedAnnotatedMethod()) {
      closeable.method();
    }
  }

  void ternary(boolean condition) {
    // BUG: Diagnostic contains:
    int result;
    try (var closeable = new Foo().mustBeClosedAnnotatedMethod()) {
      result = condition ? closeable.method() : 0;
    }
  }

  int variableDeclaration() {
    // BUG: Diagnostic contains:
    int result;
    try (var closeable = new Foo().mustBeClosedAnnotatedMethod()) {
      result = closeable.method();
    }
    return result;
  }

  void tryWithResources_nonFinal() {
    Foo foo = new Foo();
    // BUG: Diagnostic contains:
    try (Closeable closeable = foo.mustBeClosedAnnotatedMethod()) {
      try {
        closeable = null;
      } finally {
        closeable.close();
      }
    }
  }

  void tryWithResources_noClose() {
    Foo foo = new Foo();
    // BUG: Diagnostic contains:
    try (Closeable closeable = foo.mustBeClosedAnnotatedMethod()) {
      try {
      } finally {
      }
    }
  }

  class CloseableFoo implements AutoCloseable {

    @MustBeClosed
    CloseableFoo() {}

    // Doesn't autoclose Foo on Stream close.
    Stream<String> stream() {
      return null;
    }

    @Override
    public void close() {}
  }

  void twrStream() {
    // BUG: Diagnostic contains:
    try (CloseableFoo closeableFoo = new CloseableFoo();
        Stream<String> stream = closeableFoo.stream()) {}
  }

  void constructorsTransitivelyRequiredAnnotation() {
    abstract class Parent implements AutoCloseable {
      @MustBeClosed
      Parent() {}

      // BUG: Diagnostic contains: Invoked constructor is marked @MustBeClosed
      @MustBeClosed
      Parent(int i) {
        this();
      }
    }

    // BUG: Diagnostic contains: Implicitly invoked constructor is marked @MustBeClosed
    abstract class ChildDefaultConstructor extends Parent {}

    abstract class ChildExplicitConstructor extends Parent {
      // BUG: Diagnostic contains: Invoked constructor is marked @MustBeClosed
      @MustBeClosed
      ChildExplicitConstructor() {}

      // BUG: Diagnostic contains: Invoked constructor is marked @MustBeClosed
      @MustBeClosed
      ChildExplicitConstructor(int a) {
        super();
      }
    }
  }

  @MustBeClosed
  OutputStream mustBeClosedOutputStream() {
    return nullOutputStream();
  }

  void decoratorConstructorThrows() throws IOException {
    // BUG: Diagnostic contains:
    try (OutputStream outputStream = mustBeClosedOutputStream();
        var s = new GZIPOutputStream(outputStream)) {}
  }

  void notClosedByDecorator() throws IOException {
    class NotFilterOutputStream extends ByteArrayOutputStream {
      NotFilterOutputStream(OutputStream out) {}
    }
    // BUG: Diagnostic contains:
    try (OutputStream outputStream = mustBeClosedOutputStream();
        var s = new NotFilterOutputStream(outputStream)) {}
  }

  @MustBeClosed
  OutputStream decoratorMustBeClosed() {
    class MustBeClosedFilter extends FilterOutputStream {
      @MustBeClosed
      MustBeClosedFilter(OutputStream out) {
        super(out);
      }
    }
    // BUG: Diagnostic contains:
    return new MustBeClosedFilter(
        // handled above
        mustBeClosedOutputStream());
  }
}
""")
        .allowBreakingChanges() // The fix is best-effort, and some variable names may clash
        .doTest();
  }

  @Test
  public void enumInitializer() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.MustBeClosed;
            import java.io.Closeable;

            enum Test {
              A;

              interface Foo extends Closeable {}

              @MustBeClosed
              static Foo createResource() {
                return null;
              }

              private final Foo resource;
              private final Foo resource2 = createResource();

              Test() {
                this.resource = createResource();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void forLoop() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.MustBeClosed;

            class Test {
              class Closeable implements AutoCloseable {
                @Override
                public void close() {}

                public int method() {
                  return 1;
                }
              }

              class Foo {
                @MustBeClosed
                Closeable mustBeClosedMethod() {
                  return null;
                }
              }

              void forLoopCondition() {
                for (int i = 0; i < new Foo().mustBeClosedMethod().method(); ++i) {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.MustBeClosed;

            class Test {
              class Closeable implements AutoCloseable {
                @Override
                public void close() {}

                public int method() {
                  return 1;
                }
              }

              class Foo {
                @MustBeClosed
                Closeable mustBeClosedMethod() {
                  return null;
                }
              }

              void forLoopCondition() {
                try (var closeable = new Foo().mustBeClosedMethod()) {
                  for (int i = 0; i < closeable.method(); ++i) {}
                }
              }
            }
            """)
        .doTest();
  }

  @Ignore("b/236715080")
  @Test
  public void forLoopUnfixable() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.MustBeClosed;

            class Test {
              class Closeable implements AutoCloseable {
                @Override
                public void close() {}

                public int method() {
                  return 1;
                }
              }

              class Foo {
                @MustBeClosed
                Closeable mustBeClosedMethod() {
                  return null;
                }
              }

              void forLoopInitialization() {
                for (int i = new Foo().mustBeClosedMethod().method(); i > 0; --i) {}
              }

              void forLoopUpdate() {
                for (int i = 0; i < 100; i += new Foo().mustBeClosedMethod().method()) {}
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void localVariableTypeInference() {
    refactoringHelper
        .addInputLines(
            "Closeable.java",
            """
            class Closeable implements AutoCloseable {
              @Override
              public void close() {}

              public int method() {
                return 1;
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Foo.java",
            """
            import com.google.errorprone.annotations.MustBeClosed;

            class Foo {
              @MustBeClosed
              Closeable mustBeClosedMethod() {
                return null;
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Foo foo) {
                var bar = foo.mustBeClosedMethod().method();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void test(Foo foo) {
                int bar;
                try (var closeable = foo.mustBeClosedMethod()) {
                  bar = closeable.method();
                }
              }
            }
            """)
        .doTest();
  }
}
