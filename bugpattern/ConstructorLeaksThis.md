---
title: ConstructorLeaksThis
summary: Constructors should not pass the 'this' reference out in method invocations, since the object may not be fully constructed.
layout: bugpattern
category: JDK
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
During the execution of a constructor, it's dangerous to make the new instance
accessible to other code. Fields of the instance, including `final` fields, may
not yet be initialized, and executing instance methods may yield unexpected
results.

This advice applies not only to constructors per se, but also to instance
variable initializers and instance initializer blocks.

The issue `ConstructorInvokesOverridable` is closely related.

## Avoiding the warning

One common reason for constructors to pass `this` to other code is to register
the new instance as a listener on some other object. (This pattern is especially
common in UI code.) This runs the risk that the listener method will be invoked
before construction is complete. Further, it means that the constructor has side
effects -- it modifies the object that's being listened to.

To avoid these risks, it's best to factor the listener registration out into
another method, for example a factory method taking the same parameters as the
constructor. The factory method can instantiate the new object and then perform
the registration safely.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ConstructorLeaksThis")` annotation to the enclosing element.

----------

### Positive examples
__ConstructorLeaksThisPositiveCases.java__

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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Positive test cases for {@link ConstructorLeaksThis}. */
@Immutable
public class ConstructorLeaksThisPositiveCases {
  // Named for com.google.testing.junit.junit4.rules.FixtureController,
  // which is generally initialized with a leaked 'this'.
  private static class FixtureController {
    FixtureController(@SuppressWarnings("unused") Object testObject) {}
  }

  // Method invocation in field initializer
  // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
  final int hash = Objects.hash(this);

  // New call in field initializer
  // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
  final FixtureController controller = new FixtureController(this);

  {
    // Method invocation in instance initializer
    // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
    System.out.println(this);

    // New call in instance initializer
    // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
    new FixtureController(this);
  }

  ConstructorLeaksThisPositiveCases() {
    // Method invocation in constructor
    // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
    System.out.println(this);

    // New call in constructor
    // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
    new AtomicReference<>(this);

    // Qualified reference inside a context where plain 'this' means something else
    new Thread() {
      @Override
      public void run() {
        // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
        System.out.println(ConstructorLeaksThisPositiveCases.this);
      }
    }.start();
    Runnable r =
        () ->
            System.out.println(
                // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
                com.google.errorprone.bugpatterns.testdata.ConstructorLeaksThisPositiveCases.this);
    // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
    r = () -> System.out.println(this);
  }

  public static class ThisInLambda {
    {
      // 'this' names not the lambda but the class under construction
      // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
      new Thread(() -> System.out.println(this)).start();
    }
  }

  /** Leak is signaled even if masked by cast expression */
  static class CastRunnable implements Runnable {
    CastRunnable() {
      // BUG: Diagnostic contains: Constructors should not pass the 'this' reference
      new Thread((Runnable) this).start();
    }

    @Override
    public void run() {}
  }
}
{% endhighlight %}

### Negative examples
__ConstructorLeaksThisNegativeCases.java__

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

import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.Immutable;
import java.util.Objects;

/** Negative test cases for the ConstructorLeaksThis checker. */
@Immutable
public class ConstructorLeaksThisNegativeCases {
  // Named for com.google.testing.junit.junit4.rules.FixtureController,
  // which is generally initialized with a leaked 'this'.
  private static class FixtureController {
    FixtureController(@SuppressWarnings("unused") Object testObject) {}
  }

  // Instance initializer containing some negative cases
  {
    // 'this' names the Runnable
    MoreExecutors.directExecutor()
        .execute(
            new Runnable() {
              @Override
              public void run() {
                new FixtureController(this);
              }
            });
  }

  public static class SafeReferences {
    static final FixtureController that = new FixtureController(FixtureController.class);

    final FixtureController controller = new FixtureController(that);
    final int hash = Objects.hash(that);
    final String str;

    // Passing out references other than 'this' from a constructor is safe
    public SafeReferences(String str) {
      new FixtureController(str);
      System.out.println(str);
      // 'this' on the LHS is not a leak
      this.str = "Hi";
      // Extracting a field from this, assuming it's initialized,
      // does not constitute a leak
      System.out.println(this.str);
      // This is silly but not unsafe
      System.out.println(SafeReferences.this.str);
    }

    // Exporting 'this' from a regular method is safe
    public void run() {
      new FixtureController(this);
      System.out.println(this);
    }
  }

  // Safe because local variable is not a field
  private void localVariable() {
    int i = java.util.Objects.hashCode(this);
  }

  public static class ThisIsAnonymous {
    ThisIsAnonymous() {
      // 'this' names not the object under construction, but the Thread
      new Thread() {
        @Override
        public void run() {
          System.out.println(this);
        }
      }.start();
    }
  }
}
{% endhighlight %}

