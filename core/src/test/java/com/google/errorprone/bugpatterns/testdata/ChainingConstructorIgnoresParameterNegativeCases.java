/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import java.io.File;

/** @author cpovirk@google.com (Chris Povirk) */
public class ChainingConstructorIgnoresParameterNegativeCases {
  static class ImplicitThisCall {
    ImplicitThisCall() {}

    ImplicitThisCall(String foo) {}
  }

  static class ExplicitNoArgThisCall {
    ExplicitNoArgThisCall() {}

    ExplicitNoArgThisCall(String foo) {
      this();
    }
  }

  static class ParameterNotAvailable {
    ParameterNotAvailable(String foo, boolean bar) {}

    ParameterNotAvailable(String foo) {
      this(foo, false);
    }
  }

  static class ParameterDifferentType {
    ParameterDifferentType(File foo) {}

    ParameterDifferentType(String foo) {
      this(new File("/tmp"));
    }
  }

  static class ParameterUsedInExpression {
    ParameterUsedInExpression(String foo, boolean bar) {}

    ParameterUsedInExpression(String foo) {
      this(foo.substring(0), false);
    }
  }

  /** Make sure that we don't confuse a nested class's constructor with the containing class's. */
  static class HasNestedClass {
    HasNestedClass(String foo) {
      this("somethingElse", false);
    }

    static class NestedClass {
      NestedClass(String foo, boolean bar) {}
    }

    HasNestedClass(String notFoo, boolean bar) {}
  }

  static class HasNestedClassesWithSameName {
    static class Outer1 {
      static class Inner {
        Inner(String foo, boolean bar) {}
      }
    }

    static class Outer2 {
      static class Inner {
        Inner(String foo) {
          this("somethingElse", false);
        }

        Inner(String notFoo, boolean bar) {}
      }
    }
  }

  class NonStaticClass {
    NonStaticClass(String foo, boolean bar) {}

    NonStaticClass(String foo) {
      this(foo, false);
    }
  }

  static class Varargs1 {
    Varargs1(String foo, boolean... bar) {}

    Varargs1() {
      this("something", false, false);
    }
  }

  static class Varargs2 {
    Varargs2(String foo, boolean... bar) {}

    Varargs2() {
      this("something");
    }
  }
}
