---
title: ArrayEquals
summary: Reference equality used to compare arrays
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
Generally when comparing arrays for equality, the programmer intends to check that the the contents of the arrays are equal rather than that they are actually the same object.  But many commonly used equals methods compare arrays for reference equality rather than content equality. These include the instance .equals() method, Guava's com.google.common.base.Objects#equal(), and the JDK's java.util.Objects#equals().

If reference equality is needed, == should be used instead for clarity. Otherwise, use java.util.Arrays#equals() to compare the contents of the arrays.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ArrayEquals")` annotation to the enclosing element.

----------

## Examples
__ArrayEqualsNegativeCases.java__

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

import com.google.common.base.Objects;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ArrayEqualsNegativeCases {
  public void neitherArray() {
    Object a = new Object();
    Object b = new Object();
    
    if (a.equals(b)) {
      System.out.println("Objects are equal!");
    } else {
      System.out.println("Objects are not equal!");
    }
    
    if (Objects.equal(a, b)) {
      System.out.println("Objects are equal!");
    } else {
      System.out.println("Objects are not equal!");
    }
  }
  
  public void firstArray() {
    Object[] a = new Object[3];
    Object b = new Object();
    
    if (a.equals(b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
    
    if (Objects.equal(a, b)) {
      System.out.println("Objects are equal!");
    } else {
      System.out.println("Objects are not equal!");
    }
  }
  
  public void secondArray() {
    Object a = new Object();
    Object[] b = new Object[3];
    
    if (a.equals(b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
    
    if (Objects.equal(a, b)) {
      System.out.println("Objects are equal!");
    } else {
      System.out.println("Objects are not equal!");
    }
  }
  
  
}
{% endhighlight %}

__ArrayEqualsNegativeCases2.java__

{% highlight java %}
/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import java.util.Objects;

/**
 * Tests that only run with Java 7 and above.
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ArrayEqualsNegativeCases2 {
  public void neitherArray() {
    Object a = new Object();
    Object b = new Object();
    
    if (Objects.equals(a, b)) {
      System.out.println("Objects are equal!");
    } else {
      System.out.println("Objects are not equal!");
    }
  }
  
  public void firstArray() {
    Object[] a = new Object[3];
    Object b = new Object();
    
    if (Objects.equals(a, b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
  
  public void secondArray() {
    Object a = new Object();
    Object[] b = new Object[3];
    
    if (Objects.equals(a, b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
}
{% endhighlight %}

__ArrayEqualsPositiveCases.java__

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

import com.google.common.base.Objects;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ArrayEqualsPositiveCases {
    
  public void intArray() {
    int[] a = {1, 2, 3};
    int[] b = {1, 2, 3};
    
    // BUG: Diagnostic contains: Arrays.equals(a, b)
    if (a.equals(b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
    
    // BUG: Diagnostic contains: Arrays.equals(a, b)
    if (Objects.equal(a, b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
  
  public void objectArray() {
    Object[] a = new Object[3];
    Object[] b = new Object[3];
    
    // BUG: Diagnostic contains: Arrays.equals(a, b)
    if (a.equals(b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
    
    // BUG: Diagnostic contains: Arrays.equals(a, b)
    if (Objects.equal(a, b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
  
  public void firstMethodCall() {
    String s = "hello";
    char[] b = new char[3];
    
    // BUG: Diagnostic contains: Arrays.equals(s.toCharArray(), b)
    if (s.toCharArray().equals(b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
  
  public void secondMethodCall() {
    char[] a = new char[3];
    String s = "hello";
    
    // BUG: Diagnostic contains: Arrays.equals(a, s.toCharArray())
    if (a.equals(s.toCharArray())) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
  
  public void bothMethodCalls() {
    String s1 = "hello";
    String s2 = "world";
    
    // BUG: Diagnostic contains: Arrays.equals(s1.toCharArray(), s2.toCharArray())
    if (s1.toCharArray().equals(s2.toCharArray())) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
}
{% endhighlight %}

__ArrayEqualsPositiveCases2.java__

{% highlight java %}
/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import java.util.Objects;

/**
 * Tests that only run with Java 7 and above.
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ArrayEqualsPositiveCases2 {
    
  public void intArray() {
    int[] a = {1, 2, 3};
    int[] b = {1, 2, 3};
    
    // BUG: Diagnostic contains: Arrays.equals(a, b)
    if (Objects.equals(a, b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
  
  public void objectArray() {
    Object[] a = new Object[3];
    Object[] b = new Object[3];
    
    // BUG: Diagnostic contains: Arrays.equals(a, b)
    if (Objects.equals(a, b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }
  }
}
{% endhighlight %}

