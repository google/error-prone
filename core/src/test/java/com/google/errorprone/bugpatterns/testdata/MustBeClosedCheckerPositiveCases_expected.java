/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.testdata;

import com.google.errorprone.annotations.MustBeClosed;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
}
