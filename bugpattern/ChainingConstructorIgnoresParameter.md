---
title: ChainingConstructorIgnoresParameter
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: ChainingConstructorIgnoresParameter
__The called constructor accepts a parameter with the same name and type as one of its caller's parameters, but its caller doesn't pass that parameter to it.  It's likely that it was intended to.__

## The problem
A constructor parameter might not be being used as expected

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ChainingConstructorIgnoresParameter")` annotation to the enclosing element.

----------

# Examples
__ChainingConstructorIgnoresParameterNegativeCases.java__

{% highlight java %}
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

package com.google.errorprone.bugpatterns;

import java.io.File;

/**
 * @author cpovirk@google.com (Chris Povirk)
 */
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

{% endhighlight %}

__ChainingConstructorIgnoresParameterPositiveCases.java__

{% highlight java %}
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.bugpatterns.ChainingConstructorIgnoresParameterPositiveCases.Location.TEST_TARGET;

/**
 * @author cpovirk@google.com (Chris Povirk)
 */
public class ChainingConstructorIgnoresParameterPositiveCases {
  static class MissileLauncher {
    MissileLauncher(Location target, boolean askForConfirmation) {
    }

    MissileLauncher(Location target) {
      this(target, false);
    }

    MissileLauncher(boolean askForConfirmation) {
      // BUG: Diagnostic contains: this(TEST_TARGET, askForConfirmation)
      this(TEST_TARGET, false);
    }
  }

  static class ClassRatherThanPrimitive {
    ClassRatherThanPrimitive(String foo, boolean bar) {}

    ClassRatherThanPrimitive(String foo) {
      // BUG: Diagnostic contains: this(foo, false)
      this("default", false);
    }
  }

  static class CallerBeforeCallee {
    CallerBeforeCallee(String foo) {
      // BUG: Diagnostic contains: this(foo, false)
      this("default", false);
    }

    CallerBeforeCallee(String foo, boolean bar) {}
  }

  static class AssignableButNotEqual {
    AssignableButNotEqual(Object foo, boolean bar) {}

    AssignableButNotEqual(String foo) {
      // BUG: Diagnostic contains: this(foo, false)
      this("default", false);
    }
  }

  static class HasNestedClassCallerFirst {
    HasNestedClassCallerFirst(String foo) {
      // BUG: Diagnostic contains: this(foo, false)
      this("somethingElse", false);
    }

    static class NestedClass {}

    HasNestedClassCallerFirst(String foo, boolean bar) {}
  }

  static class HasNestedClassCalleeFirst {
    HasNestedClassCalleeFirst(String foo, boolean bar) {}

    static class NestedClass {}

    HasNestedClassCalleeFirst(String foo) {
      // BUG: Diagnostic contains: this(foo, false)
      this("somethingElse", false);
    }
  }

  static class MultipleQueuedErrors {
    MultipleQueuedErrors(Location target) {
      // BUG: Diagnostic contains: this(target, false)
      this(TEST_TARGET, false);
    }

    MultipleQueuedErrors(boolean askForConfirmation) {
      // BUG: Diagnostic contains: this(TEST_TARGET, askForConfirmation)
      this(TEST_TARGET, false);
    }

    MultipleQueuedErrors(Location target, boolean askForConfirmation) {
    }
  }

  enum Location { TEST_TARGET }
}

{% endhighlight %}

