---
title: CollectionIncompatibleType
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>EXPERIMENTAL</td></tr>
</table></div>

# Bug pattern: CollectionIncompatibleType
__Incompatible type as argument to non-generic Java collections method.__

## The problem
Java Collections API has non-generic methods such as Collection.contains(Object). If an argument is given which isn't of a type that may appear in the collection, these methods always return false. This commonly happens when the type of a collection is refactored and the developer relies on the Java compiler to detect callsites where the collection access needs to be updated.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("CollectionIncompatibleType")` annotation to the enclosing element.

----------

# Examples
__CollectionIncompatibleTypeNegativeCases.java__

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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CollectionIncompatibleTypeNegativeCases {

  public boolean ok1() {
    return new ArrayList<String>().contains("ok");
  }

  class B extends Date {}

  public boolean ok2() {
    return new ArrayList<Date>().contains(new B());
  }

  public boolean ok3() {
    return new OtherCollection<String>().contains(new B());
  }

  public void ok4() {
    Map<Integer, String> map = new HashMap<Integer, String>();
    Object notAString = null;
    System.out.println(map.containsValue(notAString));

    System.out.println(map.containsKey(new Object()));
  }
  
  private class OtherCollection<E> {
    public boolean contains(Object o) {
      return true;
    }
  }
}
{% endhighlight %}

__CollectionIncompatibleTypePositiveCases.java__

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
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CollectionIncompatibleTypePositiveCases {
  Collection<String> collection = new ArrayList<String>();

  public boolean bug() {
    // BUG: Diagnostic contains: return false
    return collection.contains(this);
  }

  public boolean bug2() {
    // BUG: Diagnostic contains: return false
    return new ArrayList<String>().remove(new Date());
  }

  public boolean bug3() {
    List<String> list = new ArrayList<String>(collection);
    // BUG: Diagnostic contains: false
    System.out.println(list.indexOf(new Integer(0)));
    // BUG: Diagnostic contains: false
    System.out.println(list.lastIndexOf(new Integer(0)));
    // BUG: Diagnostic contains: return false
    return list.contains(new Exception());
  }

  public String bug4() {
    Map<Integer, String> map = new HashMap<Integer, String>();
    // BUG: Diagnostic contains: false
    System.out.println(map.containsKey("not an integer"));

    Integer notAString = null;
    // BUG: Diagnostic contains: false
    System.out.println(map.containsValue(notAString));
    // BUG: Diagnostic contains: false
    System.out.println(map.remove("not an integer"));
    // BUG: Diagnostic contains: return false
    return map.get("not an integer");
  }
}
{% endhighlight %}

