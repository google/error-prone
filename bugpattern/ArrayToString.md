---
title: ArrayToString
summary: "Calling toString on an array does not provide useful information"
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The toString method on an array will print its identity, such as [I@4488aabb. This is almost never needed. Use Arrays.toString to print a human-readable array summary.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ArrayToString")` annotation to the enclosing element.

----------

## Examples
__ArrayToStringNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import java.util.*;

/**
 * @author adgar@google.com (Mike Edgar)
 */
public class ArrayToStringNegativeCases {
  public void objectEquals() {
    Object a = new Object();

    if (a.toString().isEmpty()) {
      System.out.println("string is empty!");
    } else {
      System.out.println("string is not empty!");
    }
  }
}
{% endhighlight %}

__ArrayToStringPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import java.util.*;

/**
 * @author adgar@google.com (Mike Edgar)
 */
public class ArrayToStringPositiveCases {

  public void intArray() {
    int[] a = {1, 2, 3};

    // BUG: Diagnostic contains: Arrays.toString(a)
    if (a.toString().isEmpty()) {
      System.out.println("int array string is empty!");
    } else {
      System.out.println("int array string is nonempty!");
    }
  }

  public void objectArray() {
    Object[] a = new Object[3];

    // BUG: Diagnostic contains: Arrays.toString(a)
    if (a.toString().isEmpty()) {
      System.out.println("object array string is empty!");
    } else {
      System.out.println("object array string is nonempty!");
    }
  }

  public void firstMethodCall() {
    String s = "hello";

    // BUG: Diagnostic contains: Arrays.toString(s.toCharArray())
    if (s.toCharArray().toString().isEmpty()) {
      System.out.println("char array string is empty!");
    } else {
      System.out.println("char array string is nonempty!");
    }
  }

  public void secondMethodCall() {
    char[] a = new char[3];

    // BUG: Diagnostic contains: Arrays.toString(a)
    if (a.toString().isEmpty()) {
      System.out.println("array string is empty!");
    } else {
      System.out.println("array string is nonempty!");
    }
  }
  
  public void throwable() {
    Exception e = new RuntimeException();
    // BUG: Diagnostic contains: Throwables.getStackTraceAsString(e)
    System.out.println(e.getStackTrace().toString());
  }
}
{% endhighlight %}

