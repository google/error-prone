---
title: ArgumentParameterSwap
summary: An argument is more similar to a different parameter; the arguments may have been swapped.
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding an `@SuppressWarnings("ArgumentParameterSwap")` annotation to the enclosing element.

----------

### Positive examples
__ArgumentParameterSwapPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/** @author yulissa@google.com (Yulissa Arroyo-Paredes) */
public class ArgumentParameterSwapPositiveCases {
  // names are identical but they are swapped
  public String doSomething(String[] pizzazz, String[] jukebox, String quivery, String junkies) {
    return "wrong";
  }

  public void doSomethingElse() {
    int i = 0;
    String[] pizzazz = {};
    String[] jukebox = {};
    String quivery = "";
    String junkies = "";
    // BUG: Diagnostic contains: 'doSomething(pizzazz, jukebox, quivery, junkies);'
    doSomething(jukebox, pizzazz, junkies, quivery);
  }

  // three parameters, all in the wrong place
  public String doNothing(String[] pizzazz, String[] jukebox, String[] quivery) {
    return "nothing";
  }

  public void doNothingElse() {
    String[] pizzazz = {};
    String[] jukebox = {};
    String[] quivery = {};
    // BUG: Diagnostic contains: 'doNothing(pizzazz, jukebox, quivery);'
    doNothing(quivery, pizzazz, jukebox);
  }

  // tests for when creating new objects
  class TestObject {
    String pizzazz;
    String jukebox;

    TestObject(String pizzazz, String jukebox) {}

    public TestObject resolveString() {
      // BUG: Diagnostic contains: 'return new TestObject(pizzazz, jukebox);'
      return new TestObject(jukebox, pizzazz);
    }
  }

  // test for calling super constructor
  class DerivedObject extends TestObject {
    DerivedObject(String pizzazz, String jukebox, String quivery) {
      // BUG: Diagnostic contains: 'super(pizzazz, jukebox);'
      super(jukebox, pizzazz);
    }
  }

  // tests this(...) constructor
  static class ThisCall {
    ThisCall(String pizzazz, String jukebox) {}

    ThisCall(String pizzazz, String jukebox, String quivery) {
      // BUG: Diagnostic contains: 'this(pizzazz, jukebox);'
      this(jukebox, pizzazz);
    }
  }
}
{% endhighlight %}

### Negative examples
__ArgumentParameterSwapNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/** @author yulissa@google.com (Yulissa Arroyo-Paredes) */
public class ArgumentParameterSwapNegativeCases {
  public String doSomething(String[] keepPath, String[] dropPath) {
    return "wrong";
  }

  //names are identical
  public void doSomethingElse() {
    String[] keepPath = {"one", "two", "three"};
    String[] dropPath = {"one"};
    doSomething(keepPath, dropPath);
  }

  public void doNothing() {
    doSomething(null, new String[0]);
  }
}
{% endhighlight %}

