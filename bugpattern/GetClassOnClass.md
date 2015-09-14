---
title: GetClassOnClass
summary: Calling getClass() on an object of type Class returns the Class object for java.lang.Class; you probably meant to operate on the object directly
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
Calling `getClass()` on an object of type Class returns the Class object for java.lang.Class.  Usually this is a mistake, and people intend to operate on the object itself (for example, to print an error message).  If you really did intend to operate on the Class object for java.lang.Class, please use `Class.class` instead for clarity.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("GetClassOnClass")` annotation to the enclosing element.

----------

### Positive examples
__GetClassOnClassPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

/**
 * @author chy@google.com (Christine Yang)
 * @author kmuhlrad@google.com (Katy Muhlrad)
 */
public class GetClassOnClassPositiveCases {

  public void getClassOnClass(Class clazz) {
    // BUG: Diagnostic contains: clazz.getName()
    System.out.println(clazz.getClass().getName());
  }

  public void getClassOnClass2() {
    String s = "hi";
    // BUG: Diagnostic contains: s.getClass().getName()
    s.getClass().getClass().getName();
  }

  public void getClassOnClass3() {
    // BUG: Diagnostic contains: String.class.getName()
    System.out.println(String.class.getClass().getName());
  }
}
{% endhighlight %}

### Negative examples
__GetClassOnClassNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

/**
 * @author chy@google.com (Christine Yang)
 * @author kmuhlrad@google.com (Katy Muhlrad)
 */
public class GetClassOnClassNegativeCases {

  public void getClassOnClass(Object obj) {
    System.out.println(obj.getClass().getName());
  }

  public void getClassOnClass2() {
    String s = "hi";
    DummyObject.getClass(s);
  }

  public static class DummyObject {
    public static boolean getClass(Object a) {
      return true;
    }
  }
}
{% endhighlight %}

