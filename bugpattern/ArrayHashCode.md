---
title: ArrayHashCode
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

# Bug pattern: ArrayHashCode
__hashcode method on array does not hash array contents__

## The problem
Computing a hashcode for an array is tricky.  Typically you want a hashcode that depends on the value of each element in the array, but many of the common ways to do this actually return a hashcode based on the _identity_ of the array rather than its contents.

This check flags attempts to compute a hashcode from an array that do not take the contents of the array into account. There are several ways to mess this up:
  * Call the instance .hashCode() method on an array.
  * Call the JDK method java.util.Objects#hashCode() with an argument of array type.
  * Call either the JDK method java.util.Objects#hash() or the Guava method com.google.common.base.Objects#hashCode() with a single argument of _primitive_ array type. Because these are varags methods that take Object..., the primitive array is autoboxed into a single-element Object array, and these methods use the identity hashcode of the primitive array rather than examining its contents. Note that calling these methods on an argument of _Object_ array type actually does the right thing because no boxing is needed.

Please use java.util.Arrays#hashCode() instead to compute a hash value that depends on the contents of the array. If you really intended to compute the identity hash code, consider using java.lang.System#identityHashCode() instead for clarity.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ArrayHashCode")` annotation to the enclosing element.

----------

# Examples
__ArrayHashCodeNegativeCases.java__
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

import com.google.common.base.Objects;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ArrayHashCodeNegativeCases {
  
  private Object[] objArray = {1, 2, 3};
  private String[] stringArray = {"1", "2", "3"};
  private int[] intArray = {1, 2, 3};
  private byte[] byteArray = {1, 2, 3};
  private Object obj = new Object();
  private String str = "foo";
  
  public void objectHashCodeOnNonArrayType() {
    int hashCode;
    hashCode = obj.hashCode();
    hashCode = str.hashCode();
  }
    
  public void varargsHashCodeOnMoreThanOneArg() {
    int hashCode;
    hashCode = Objects.hashCode(objArray, intArray);
    hashCode = Objects.hashCode(stringArray, byteArray);
  }
  
  public void varagsHashCodeOnNonArrayType() {
    int hashCode;
    hashCode = Objects.hashCode(obj);    
    hashCode = Objects.hashCode(str);
  }
  
  public void varagsHashCodeOnObjectOrStringArray() {
    int hashCode;
    hashCode = Objects.hashCode(objArray);
    hashCode = Objects.hashCode((Object[]) stringArray);
  }
}

{% endhighlight %}
__ArrayHashCodeNegativeCases2.java__
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
 * Java 7 specific tests
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ArrayHashCodeNegativeCases2 {
  
  private Object[] objArray = {1, 2, 3};
  private String[] stringArray = {"1", "2", "3"};
  private int[] intArray = {1, 2, 3};
  private byte[] byteArray = {1, 2, 3};
  private Object obj = new Object();
  private String str = "foo";
    
  public void nonVaragsHashCodeOnNonArrayType() {
    int hashCode;
    hashCode = Objects.hashCode(obj);    
    hashCode = Objects.hashCode(str);
  }
  
  public void varargsHashCodeOnMoreThanOneArg() {
    int hashCode;
    hashCode = Objects.hash(objArray, intArray);
    hashCode = Objects.hash(stringArray, byteArray);
  }
  
  public void varagsHashCodeOnNonArrayType() {
    int hashCode;
    hashCode = Objects.hash(obj);
    hashCode = Objects.hash(str);
  }
  
  public void varagsHashCodeOnObjectOrStringArray() {
    int hashCode;
    hashCode = Objects.hash(objArray);  
    hashCode = Objects.hash((Object[]) stringArray);    
  }
}

{% endhighlight %}
__ArrayHashCodePositiveCases.java__
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

import com.google.common.base.Objects;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ArrayHashCodePositiveCases {
  private Object[] objArray = {1, 2, 3};
  private String[] stringArray = {"1", "2", "3"};
  private int[] intArray = {1, 2, 3};
  private byte[] byteArray = {1, 2, 3};
  
  public void objectHashCode() {
    int hashCode;
    // BUG: Diagnostic contains: Arrays.hashCode(objArray)
    hashCode = objArray.hashCode();
    // BUG: Diagnostic contains: Arrays.hashCode(stringArray)
    hashCode = stringArray.hashCode();
    // BUG: Diagnostic contains: Arrays.hashCode(intArray)
    hashCode = intArray.hashCode();
  }

  public void guavaObjectsHashCode() {
    int hashCode;
    // BUG: Diagnostic contains: Arrays.hashCode(intArray)
    hashCode = Objects.hashCode(intArray);
    // BUG: Diagnostic contains: Arrays.hashCode(byteArray)
    hashCode = Objects.hashCode(byteArray);
  }
}

{% endhighlight %}
__ArrayHashCodePositiveCases2.java__
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
 * Java 7 specific tests
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ArrayHashCodePositiveCases2 {
  private Object[] objArray = {1, 2, 3};
  private String[] stringArray = {"1", "2", "3"};
  private int[] intArray = {1, 2, 3};
  private byte[] byteArray = {1, 2, 3};
   
  public void javaUtilObjectsHashCode() {
    int hashCode;
    // BUG: Diagnostic contains: Arrays.hashCode(objArray)
    hashCode = Objects.hashCode(objArray);
    // BUG: Diagnostic contains: Arrays.hashCode(stringArray)
    hashCode = Objects.hashCode(stringArray);
    // BUG: Diagnostic contains: Arrays.hashCode(intArray)
    hashCode = Objects.hashCode(intArray);
  }
  
  public void javaUtilObjectsHash() {
    int hashCode;
    // BUG: Diagnostic contains: Arrays.hashCode(intArray)
    hashCode = Objects.hash(intArray);
    // BUG: Diagnostic contains: Arrays.hashCode(byteArray)
    hashCode = Objects.hash(byteArray);
  }
}

{% endhighlight %}
