---
title: BadShiftAmount
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: BadShiftAmount
__Shift by an amount that is out of range__

## The problem
For shift operations on int types, only the five lowest-order bits of the shift amount are used as the shift distance.  This means that shift amounts that are not in the range 0 to 31, inclusive, are silently mapped to values in that range. For example, a shift of an int by 32 is equivalent to shifting by 0, i.e., a no-op.

See JLS 15.19, "Shift Operators", for more details.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("BadShiftAmount")` annotation to the enclosing element.

----------

# Examples
__BadShiftAmountNegativeCases.java__

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
 * @author Bill Pugh (bill.pugh@gmail.com)
 */
public class BadShiftAmountNegativeCases {

  public void foo() {
    int x = 0;
    long result = 0;

    result += (long) x >> 3;
    result += x << 3;
    result += x >>> 3;
    result += (long) (x & 0xff) >> 40;
  }

}
{% endhighlight %}

__BadShiftAmountPositiveCases.java__

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
 * @author Bill Pugh (bill.pugh@gmail.com)
 */
public class BadShiftAmountPositiveCases {
  
  public void foo() {
    int x = 0;
    long result = 0;

    // BUG: Diagnostic contains: (long) x >> 32
    result += x >> 32;
    // BUG: Diagnostic contains: (long) x << 32
    result += x << 32;
    // BUG: Diagnostic contains: (long) x >>> 32
    result += x >>> 32;
    // BUG: Diagnostic contains: (long) x >> 40
    result += x >> 40;
    // BUG: Diagnostic contains: (long) (x & 255) >> 40
    result += (x & 255) >> 40;
    
    // BUG: Diagnostic contains: 1L << 48
    result += 1 << 48;
    
    // BUG: Diagnostic contains: x >> 4
    result += x >> 100;
    // BUG: Diagnostic contains: x >> 31
    result += x >> -1;
    
    byte b = 0;
    char c = 'a';
    
    // BUG: Diagnostic contains: (long) b >> 32
    result += b >> 32;
    // BUG: Diagnostic contains: (long) b << 32
    result += b << 32;
    // BUG: Diagnostic contains: (long) c >> 32
    result += c >> 32;
    // BUG: Diagnostic contains: (long) c >>> 32
    result += c >>> 32;
  }
  
}
{% endhighlight %}

