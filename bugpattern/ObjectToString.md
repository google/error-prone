---
title: ObjectToString
summary: Calling toString on Objects that don't override toString() doesn't provide useful information
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
Calling `toString` on objects that don't override `toString()` doesn't provide
useful information (just the class name and the `hashCode()`).

Consider overriding toString() function to return a meaningful String describing
the object.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ObjectToString")` to the enclosing element.

----------

### Positive examples
__ObjectToStringPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2018 The Error Prone Authors.
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

/** @author bhagwani@google.com (Sumit Bhagwani) */
public class ObjectToStringPositiveCases {

  public static final class FinalObjectClassWithoutToString {}

  void directToStringCalls() {
    FinalObjectClassWithoutToString finalObjectClassWithoutToString =
        new FinalObjectClassWithoutToString();
    // BUG: Diagnostic contains: ObjectToString
    System.out.println(finalObjectClassWithoutToString.toString());
  }
}
{% endhighlight %}

### Negative examples
__ObjectToStringNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2018 The Error Prone Authors.
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

import org.joda.time.Duration;

/** @author bhagwani@google.com (Sumit Bhagwani) */
public class ObjectToStringNegativeCases {

  public static final class FinalObjectClassWithoutToString {}

  public static class NonFinalObjectClassWithoutToString {}

  public static final class FinalObjectClassWithToString {

    @Override
    public String toString() {
      return "hakuna";
    }
  }

  public static class NonFinalObjectClassWithToString {

    @Override
    public String toString() {
      return "matata";
    }
  }

  public void log(Object o) {
    System.out.println(o.toString());
  }

  void directToStringCalls() {
    NonFinalObjectClassWithoutToString nonFinalObjectClassWithoutToString =
        new NonFinalObjectClassWithoutToString();
    System.out.println(nonFinalObjectClassWithoutToString.toString());

    FinalObjectClassWithToString finalObjectClassWithToString = new FinalObjectClassWithToString();
    System.out.println(finalObjectClassWithToString.toString());

    NonFinalObjectClassWithToString nonFinalObjectClassWithToString =
        new NonFinalObjectClassWithToString();
    System.out.println(nonFinalObjectClassWithToString.toString());
  }

  void callsTologMethod() {
    FinalObjectClassWithoutToString finalObjectClassWithoutToString =
        new FinalObjectClassWithoutToString();
    log(finalObjectClassWithoutToString);

    NonFinalObjectClassWithoutToString nonFinalObjectClassWithoutToString =
        new NonFinalObjectClassWithoutToString();
    log(nonFinalObjectClassWithoutToString);

    FinalObjectClassWithToString finalObjectClassWithToString = new FinalObjectClassWithToString();
    log(finalObjectClassWithToString);

    NonFinalObjectClassWithToString nonFinalObjectClassWithToString =
        new NonFinalObjectClassWithToString();
    log(nonFinalObjectClassWithToString);
  }

  public void overridePresentInAbstractClassInHierarchy(Duration durationArg) {
    String unusedString = Duration.standardSeconds(86400).toString();
    System.out.println("test joda string " + Duration.standardSeconds(86400));

    unusedString = durationArg.toString();
    System.out.println("test joda string " + durationArg);
  }
}
{% endhighlight %}

