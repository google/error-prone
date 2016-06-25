---
title: ComparisonOutOfRange
summary: Comparison to value that is out of range for the compared type
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
This checker looks for equality comparisons to values that are out of range for the compared type.  For example, bytes may have a value in the range -128 to 127. Comparing a byte for equality with a value outside that range will always evaluate to false and usually indicates an error in the code.

This checker currently supports checking for bad byte and character comparisons.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ComparisonOutOfRange")` annotation to the enclosing element.

----------

### Positive examples
__ComparisonOutOfRangePositiveCases.java__

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

package com.google.errorprone.bugpatterns.testdata;

import java.io.IOException;
import java.io.Reader;

/**
 * @author Bill Pugh (bill.pugh@gmail.com)
 */
public class ComparisonOutOfRangePositiveCases {

  public void testByteEquality() {
    boolean result;
    byte b = 0;
    byte[] barr = {1, 2, 3};

    // BUG: Diagnostic contains: b == -1
    result = b == 255;
    // BUG: Diagnostic contains: b == 1
    result = b == -255;
    // BUG: Diagnostic contains: b == -128
    result = b == 128;
    // BUG: Diagnostic contains: b != -1
    result = b != 255;
    // BUG: Diagnostic contains: b == 1
    result = b == - 255;

    // BUG: Diagnostic contains: barr[0] == -1
    result = barr[0] == 255;
    // BUG: Diagnostic contains: barr[0] == -128
    result = barr[0] == 128;
    // BUG: Diagnostic contains: barr[0] == 1
    result = barr[0] == -255;
  }
  
  public void testCharEquality() throws IOException {
    boolean result;
    char c = 'A';
    Reader reader = null;

    // BUG: Diagnostic contains: false
    result = c == -1;
    // BUG: Diagnostic contains: true
    result = c != -1;

    char d;
    // BUG: Diagnostic contains: false
    result = (d = (char) reader.read()) == -1;
  }
}
{% endhighlight %}

### Negative examples
__ComparisonOutOfRangeNegativeCases.java__

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

package com.google.errorprone.bugpatterns.testdata;

import java.io.IOException;
import java.io.Reader;

/**
 * @author Bill Pugh (bill.pugh@gmail.com)
 */
public class ComparisonOutOfRangeNegativeCases {

  public void testByteEquality() {
    boolean result;
    byte b = 0;
    byte[] barr = {1, 2, 3};
    
    result = b == 1;
    result = b == -2;
    result = b == 127;
    result = b != 1;
    
    result = b == (byte) 255;
    
    result = b == 'a';    // char
    result = b == 1L;     // long
    result = b == 1.123f; // float
    result = b == 1.123;  // double
    
    result = barr[0] == 1;
    result = barr[0] == -2;
    result = barr[0] == -128;
  }

  public void testCharEquality() throws IOException {
    boolean result;
    char c = 'A';
    Reader reader = null;

    result = c == 0;
    result = c == 0xffff;
    
    result = c == 1L;     // long
    result = c == 1.123f; // float
    result = c == 1.123;  // double
    
    int d;
    result = (d = reader.read()) == -1; 
  }

}
{% endhighlight %}

