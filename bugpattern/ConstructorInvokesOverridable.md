---
title: ConstructorInvokesOverridable
summary: Constructors should not invoke overridable methods.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
As Effective Java §17 warns: "Constructors must not invoke overridable methods".
The risk is that overrides of these methods in subclasses will observe the new
instance in an incompletely-constructed state. (Subclass state will certainly be
uninitialized, and base class state may be incomplete as well.)

This advice applies not only to constructors per se, but also to instance
variable initializers and instance initializer blocks.

The issue `ConstructorLeaksThis` is closely related.

## Avoiding the warning

If your constructor invokes a class method, and you don't intend it to be
overridden, mark the method private or final. (Its implementation will still
observe the instance in an incomplete state, so take care that all fields are
initialized first.)

If you need to invoke subclass logic as part of initialization, either put it in
the subclass constructor, or invoke it outside the constructor altogether. For
example, wrap the `new` call in a factory method and invoke the overridable
method afterward.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ConstructorInvokesOverridable")` annotation to the enclosing element.

----------

### Positive examples
__ConstructorInvokesOverridablePositiveCases.java__

{% highlight java %}
/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.errorprone.annotations.Immutable;

/** Positive test cases for {@link ConstructorInvokesOverridable}. */
@Immutable
public class ConstructorInvokesOverridablePositiveCases {

  // BUG: Diagnostic contains: Constructors should not invoke overridable
  final int i = unsafe();

  {
    // BUG: Diagnostic contains: Constructors should not invoke overridable
    unsafe();
  }

  ConstructorInvokesOverridablePositiveCases() {
    // BUG: Diagnostic contains: Constructors should not invoke overridable
    unsafe();

    new Thread() {
      @Override
      public void run() {
        // BUG: Diagnostic contains: Constructors should not invoke overridable
        unsafe();
      }
    }.start();

    // BUG: Diagnostic contains: Constructors should not invoke overridable
    new Thread(() -> unsafe()).start();
  }

  protected int unsafe() {
    return 0;
  }

  void localInitializer() {
    class Local extends java.util.HashMap<String, String> {
      {
        // BUG: Diagnostic contains: Constructors should not invoke overridable
        put("Hi", "Mom");
      }
    }
  }

  // Lookup is handled correctly for inner classes as well
  class Inner {
    // BUG: Diagnostic contains: Constructors should not invoke overridable
    final int unsafeValue = innerUnsafe();

    protected int innerUnsafe() {
      return 7;
    }
  }
}
{% endhighlight %}

### Negative examples
__ConstructorInvokesOverridableNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.errorprone.annotations.Immutable;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Negative test cases for {@link ConstructorInvokesOverridable}. */
@Immutable
public class ConstructorInvokesOverridableNegativeCases {

  final int i = safeFinal();
  final int j = safeStatic();
  final int k = safePrivate();

  {
    safeFinal();
    safeStatic();
    safePrivate();
  }

  public ConstructorInvokesOverridableNegativeCases() {
    safeFinal();
    safeStatic();
    safePrivate();

    new Thread() {
      @Override
      public void run() {
        safeFinal();
        safeStatic();
        safePrivate();
      }
    }.start();

    new Thread(() -> safeFinal()).start();
    new Thread(() -> safeStatic()).start();
    new Thread(() -> safePrivate()).start();
  }

  public void localVariable() {
    // Safe because this variable is not a field
    int i = unsafe();
  }

  @RunWith(JUnit4.class)
  public static class JUnitTest {
    // Safe because we skip the check in unit tests.
    final int i = unsafe();

    protected int unsafe() {
      return 3;
    }
  }

  /** Not overridable because final */
  protected final int safeFinal() {
    return 0;
  }

  /** Not overridable because static */
  protected static int safeStatic() {
    return 1;
  }

  /** Not overridable because private */
  private int safePrivate() {
    return 2;
  }

  protected int unsafe() {
    return 3;
  }

  void localInitializer() {
    class Local {
      {
        // safe because unsafe is not a member of this class
        unsafe();
      }
    }
  }

  // Lookup is handled correctly for inner classes as well
  class Inner {
    // OK to call an overridable method of the containing class
    final int safeValue = unsafe();
  }
}
{% endhighlight %}

