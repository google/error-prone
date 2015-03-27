---
title: EmptyIf
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>EXPERIMENTAL</td></tr>
</table></div>

# Bug pattern: EmptyIf
__Empty statement after if__

_Alternate names: empty_

## The problem
An if statement contains an empty statement as the then clause. A semicolon may have been inserted by accident.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("EmptyIf")` annotation to the enclosing element.

----------

# Examples
__EmptyIfStatementNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class EmptyIfStatementNegativeCases {

  // just a normal use of if
  public static void negativeCase1() {
    int i = 10;
    if (i == 10) {
      System.out.println("foo");
    }
    i++;
  }

  // empty then part but nonempty else
  public static void negativeCase2() {
    int i = 0;
    if (i == 10)
      ;
    else
      System.out.println("not 10");
  }

  // multipart if with non-empty else
  public static void negativeCase3() {
    int i = 0;
    if (i == 10)
      ;
    else if (i == 11)
      ;
    else if (i == 12)
      ;
    else
      System.out.println("not 10, 11, or 12");
  }


}

{% endhighlight %}

__EmptyIfStatementPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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
 * Positive test cases for the empty if statement check.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class EmptyIfStatementPositiveCases {

  public static void positiveCase1() {
    int i = 10;
    // BUG: Diagnostic contains: if (i == 10) {
    if (i == 10); {
      i++;
    }
  }

  public static void positiveCase2() {
    int i = 10;
    // BUG: Diagnostic contains: if (i == 10)
    if (i == 10);
    i++;
    System.out.println("foo");
  }

  public static void positiveCase3() {
    int i = 10;
    if (i == 10)
      // BUG: Diagnostic contains: remove this line
      ;
    i++;
    System.out.println("foo");
  }

  public static void positiveCase4() {
    int i = 10;
    // BUG: Diagnostic contains: remove this line
    if (i == 10)            ;
  }
  
  public static void positiveCase5() {
    int i = 10;
    if (i == 10)
      // BUG: Diagnostic contains: remove this line
      ;
    {
      System.out.println("foo");
    }
  }
}

{% endhighlight %}

