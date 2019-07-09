---
title: UnnecessaryCheckNotNull
summary: This null check is unnecessary; the expression can never be null
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: PreconditionsCheckNotNull, PreconditionsCheckNotNullPrimitive_

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryCheckNotNull")` to the enclosing element.

----------

### Positive examples
__UnnecessaryCheckNotNullPositiveCase.java__

{% highlight java %}
/*
 * Copyright 2011 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness.testdata;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.Objects;

public class UnnecessaryCheckNotNullPositiveCase {
  public void error_checkNotNull() {
    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull("string literal");

    // BUG: Diagnostic contains: remove this line
    checkNotNull("string literal");

    String thing = null;
    // BUG: Diagnostic contains: (thing,
    checkNotNull("thing is null", thing);
    // BUG: Diagnostic contains:
    Preconditions.checkNotNull("a string literal " + "that's got two parts", thing);
  }

  public void error_verifyNotNull() {
    // BUG: Diagnostic contains: remove this line
    Verify.verifyNotNull("string literal");

    // BUG: Diagnostic contains: remove this line
    verifyNotNull("string literal");

    String thing = null;
    // BUG: Diagnostic contains: (thing,
    verifyNotNull("thing is null", thing);
    // BUG: Diagnostic contains:
    Verify.verifyNotNull("a string literal " + "that's got two parts", thing);
  }

  public void error_requireNonNull() {
    // BUG: Diagnostic contains: remove this line
    Objects.requireNonNull("string literal");

    // BUG: Diagnostic contains: remove this line
    requireNonNull("string literal");

    String thing = null;
    // BUG: Diagnostic contains: (thing,
    requireNonNull("thing is null", thing);
    // BUG: Diagnostic contains:
    Objects.requireNonNull("a string literal " + "that's got two parts", thing);
  }

  public void error_fully_qualified_import_checkNotNull() {
    // BUG: Diagnostic contains: remove this line
    com.google.common.base.Preconditions.checkNotNull("string literal");
  }

  public void error_fully_qualified_import_verifyNotNull() {
    // BUG: Diagnostic contains: remove this line
    com.google.common.base.Verify.verifyNotNull("string literal");
  }

  public void error_fully_qualified_import_requireNonNull() {
    // BUG: Diagnostic contains: remove this line
    java.util.Objects.requireNonNull("string literal");
  }
}
{% endhighlight %}

### Negative examples
__UnnecessaryCheckNotNullNegativeCase.java__

{% highlight java %}
/*
 * Copyright 2011 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness.testdata;

public class UnnecessaryCheckNotNullNegativeCase {
  public void go_checkNotNull() {
    Preconditions.checkNotNull("this is ok");
  }

  public void go_verifyNotNull() {
    Verify.verifyNotNull("this is ok");
  }

  public void go_requireNonNull() {
    Objects.requireNonNull("this is ok");
  }

  private static class Preconditions {
    static void checkNotNull(String string) {
      System.out.println(string);
    }
  }

  private static class Verify {
    static void verifyNotNull(String string) {
      System.out.println(string);
    }
  }

  private static class Objects {
    static void requireNonNull(String string) {
      System.out.println(string);
    }
  }

  public void go() {
    Object testObj = null;
    com.google.common.base.Preconditions.checkNotNull(testObj, "this is ok");
    com.google.common.base.Verify.verifyNotNull(testObj, "this is ok");
    java.util.Objects.requireNonNull(testObj, "this is ok");
  }
}
{% endhighlight %}

