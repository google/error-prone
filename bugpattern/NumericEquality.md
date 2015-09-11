---
title: NumericEquality
summary: Numeric comparison using reference equality instead of value equality
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Numbers are compared for reference equality/inequality using == or != instead of for value equality using .equals()

## Suppression
Suppress false positives by adding an `@SuppressWarnings("NumericEquality")` annotation to the enclosing element.

----------

## Examples
__NumericEqualityNegativeCases.java__

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

/**
 * @author scottjohnson@google.com (Scott Johnsson)
 */
public class NumericEqualityNegativeCases {

  public static final Integer NULLINT = null;

  public boolean testEquality(Integer x, Integer y) {
    boolean retVal;

    retVal = x.equals(y);
    retVal = (x == null);
    retVal = (x != null);
    retVal = (null == x);
    retVal = (null != x);
    retVal = x == 1000;
    retVal = x + y == y + x;
    retVal = x == NULLINT;
    retVal = NULLINT == x;

    return retVal;
  }

  @SuppressWarnings("NumericEquality")
  public boolean testSuppressWarnings(Integer x, Integer y) {
    boolean retVal;

    retVal = (x != y);
    retVal = (x == y);

    return retVal;
  }

}
{% endhighlight %}

__NumericEqualityPositiveCases.java__

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

/**
 * @author scottjohnson@google.com (Scott Johnson)
 */
public class NumericEqualityPositiveCases {

  public boolean testEquality(Integer x, Integer y) {
    boolean retVal;

    // BUG: Diagnostic contains: Objects.equal(x, y)
    retVal = (x == y);
    
    // BUG: Diagnostic contains: !Objects.equal(x, y)
    retVal = (x != y);
    final Integer constValue = new Integer(1000);
    
    // BUG: Diagnostic contains: Objects.equal(x, constValue)
    retVal = (x == constValue);
    
    // BUG: Diagnostic contains: !Objects.equal(x, constValue)
    retVal = (x != constValue);

    return retVal;
  }

}
{% endhighlight %}

