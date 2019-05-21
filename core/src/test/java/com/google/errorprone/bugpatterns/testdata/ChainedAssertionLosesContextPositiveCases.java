/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.Truth.assert_;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

/** @author cpovirk@google.com (Chris Povirk) */
public class ChainedAssertionLosesContextPositiveCases {
  static final class FooSubject extends Subject<FooSubject, Foo> {
    private final Foo actual;

    static Factory<FooSubject, Foo> foos() {
      return FooSubject::new;
    }

    static FooSubject assertThat(Foo foo) {
      return assertAbout(foos()).that(foo);
    }

    private FooSubject(FailureMetadata metadata, Foo actual) {
      super(metadata, actual);
      this.actual = actual;
    }

    void hasString(String expected) {
      // BUG: Diagnostic contains: check("string()").that(actual.string()).isEqualTo(expected)
      Truth.assertThat(actual.string()).isEqualTo(expected);
    }

    void hasOtherFooInteger(int expected) {
      // BUG: Diagnostic contains:
      // check("otherFoo().integer()").that(actual.otherFoo().integer()).isEqualTo(expected)
      Truth.assertThat(actual.otherFoo().integer()).isEqualTo(expected);
    }

    FooSubject otherFooAbout() {
      // BUG: Diagnostic contains: check("otherFoo()").about(foos()).that(actual.otherFoo())
      return assertAbout(foos()).that(actual.otherFoo());
    }

    FooSubject otherFooThat() {
      // BUG: Diagnostic contains: check("otherFoo()").about(foos()).that(actual.otherFoo())
      return assertThat(actual.otherFoo());
    }

    void withMessage(String expected) {
      // BUG: Diagnostic contains:
      // check("string()").withMessage("blah").that(actual.string()).isEqualTo(expected)
      assertWithMessage("blah").that(actual.string()).isEqualTo(expected);
    }

    void withMessageWithArgs(String expected) {
      // BUG: Diagnostic contains:
      // check("string()").withMessage("%s", "blah").that(actual.string()).isEqualTo(expected)
      assertWithMessage("%s", "blah").that(actual.string()).isEqualTo(expected);
    }

    void plainAssert(String expected) {
      // BUG: Diagnostic contains:
      // check("string()").that(actual.string()).isEqualTo(expected)
      assert_().that(actual.string()).isEqualTo(expected);
    }
  }

  private static final class Foo {
    final String string;
    final int integer;

    Foo(String string, int integer) {
      this.string = string;
      this.integer = integer;
    }

    String string() {
      return string;
    }

    int integer() {
      return integer;
    }

    Foo otherFoo() {
      return this;
    }
  }
}
