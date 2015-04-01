---
title: ArrayToStringCompoundAssignment
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

# Bug pattern: ArrayToStringCompoundAssignment
__Implicit toString used on an array (String += Array)__

## The problem
When concatenating-and-assigning an array to a string, the implicit toString call on the array will yield its identity, such as [I@4488aabb. This is almost never needed. Use Arrays.toString to obtain a human-readable array summary.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ArrayToStringCompoundAssignment")` annotation to the enclosing element.

----------

# Examples
__ArrayToStringCompoundAssignmentNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

/**
 * @author adgar@google.com (Mike Edgar)
 */
public class ArrayToStringCompoundAssignmentNegativeCases {
  public void concatenateCompoundAssign_object() {
    Object a = new Object();
    String b = " a string";
    b += a;
  }

  public void concatenateCompoundAssign_int() {
    int a = 5;
    String b = " a string ";
    b += a;
  }
}
{% endhighlight %}

__ArrayToStringCompoundAssignmentPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
public class ArrayToStringCompoundAssignmentPositiveCases {

  private static final int[] a = {1, 2, 3};

  public void stringVariableAddsArrayAndAssigns() {
    String b = "a string";
    // BUG: Diagnostic contains: += Arrays.toString(a)
    b += a;
  }
}
{% endhighlight %}

