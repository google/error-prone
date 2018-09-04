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

package com.google.errorprone.bugpatterns.testdata;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.errorprone.annotations.MustBeClosed;

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
    // This is fine since the caller method is annotatGed.
    return new MustBeClosedAnnotatedConstructor();
  }

  @MustBeClosed
  Closeable positiveCase7() {
    // This is fine since the caller method is annotated.
    return new Foo().mustBeClosedAnnotatedMethod();
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

  abstract class ChildOfParentWithNoArgument extends ParentWithNoArgument {
    // Use a default constructor.
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
}
