---
title: ReturnValueIgnored
summary: Return value of this method must be used
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: ResultOfMethodCallIgnored, CheckReturnValue_

## The problem
Certain library methods do nothing useful if their return value is ignored. For example, String.trim() has no side effects, and you must store the return value of String.intern() to access the interned string.  This check encodes a list of methods in the JDK whose return value must be used and issues an error if they are not.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ReturnValueIgnored")` annotation to the enclosing element.

----------

### Positive examples
__ReturnValueIgnoredPositiveCases.java__

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

package com.google.errorprone.bugpatterns.testdata;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/** @author alexeagle@google.com (Alex Eagle) */
public class ReturnValueIgnoredPositiveCases {
  String a = "thing";

  // BUG: Diagnostic contains: Return value of this method must be used
  private Runnable r = () -> String.valueOf("");

  { // String methods
    // BUG: Diagnostic contains: remove this line
    String.format("%d", 10);
    // BUG: Diagnostic contains: remove this line
    String.format("%d", 10).trim();
    // BUG: Diagnostic contains: remove this line
    java.lang.String.format("%d", 10).trim();
    // BUG: Diagnostic contains: a = a.intern()
    a.intern();
    // BUG: Diagnostic contains: a = a.trim()
    a.trim();
    // BUG: Diagnostic contains: a = a.trim().concat("b")
    a.trim().concat("b");
    // BUG: Diagnostic contains: a = a.concat("append this")
    a.concat("append this");
    // BUG: Diagnostic contains: a = a.replace('t', 'b')
    a.replace('t', 'b');
    // BUG: Diagnostic contains: a = a.replace("thi", "fli")
    a.replace("thi", "fli");
    // BUG: Diagnostic contains: a = a.replaceAll("i", "b")
    a.replaceAll("i", "b");
    // BUG: Diagnostic contains: a = a.replaceFirst("a", "b")
    a.replaceFirst("a", "b");
    // BUG: Diagnostic contains: a = a.toLowerCase()
    a.toLowerCase();
    // BUG: Diagnostic contains: a = a.toLowerCase(Locale.ENGLISH)
    a.toLowerCase(Locale.ENGLISH);
    // BUG: Diagnostic contains: a = a.toUpperCase()
    a.toUpperCase();
    // BUG: Diagnostic contains: a = a.toUpperCase(Locale.ENGLISH)
    a.toUpperCase(Locale.ENGLISH);
    // BUG: Diagnostic contains: a = a.substring(0)
    a.substring(0);
    // BUG: Diagnostic contains: a = a.substring(0, 1)
    a.substring(0, 1);
  }

  StringBuffer sb = new StringBuffer("hello");

  {
    // BUG: Diagnostic contains: remove this line
    sb.toString().trim();
  }

  BigInteger b = new BigInteger("123456789");

  { // BigInteger methods
    // BUG: Diagnostic contains: b = b.add(new BigInteger("3"))
    b.add(new BigInteger("3"));
    // BUG: Diagnostic contains: b = b.abs()
    b.abs();
    // BUG: Diagnostic contains: b = b.shiftLeft(3)
    b.shiftLeft(3);
    // BUG: Diagnostic contains: b = b.subtract(BigInteger.TEN)
    b.subtract(BigInteger.TEN);
  }

  BigDecimal c = new BigDecimal("1234.5678");

  { // BigDecimal methods
    // BUG: Diagnostic contains: c = c.add(new BigDecimal("1.3"))
    c.add(new BigDecimal("1.3"));
    // BUG: Diagnostic contains: c = c.abs()
    c.abs();
    // BUG: Diagnostic contains: c = c.divide(new BigDecimal("4.5"))
    c.divide(new BigDecimal("4.5"));
    // BUG: Diagnostic contains: remove this line
    new BigDecimal("10").add(c);
  }

  Path p = Paths.get("foo/bar/baz");

  { // Path methods
    // BUG: Diagnostic contains: p = p.getFileName();
    p.getFileName();
    // BUG: Diagnostic contains: p = p.getName(0);
    p.getName(0);
    // BUG: Diagnostic contains: p = p.getParent();
    p.getParent();
    // BUG: Diagnostic contains: p = p.getRoot();
    p.getRoot();
    // BUG: Diagnostic contains: p = p.normalize();
    p.normalize();
    // BUG: Diagnostic contains: p = p.relativize(p);
    p.relativize(p);
    // BUG: Diagnostic contains: p = p.resolve(p);
    p.resolve(p);
    // BUG: Diagnostic contains: p = p.resolve("string");
    p.resolve("string");
    // BUG: Diagnostic contains: p = p.resolveSibling(p);
    p.resolveSibling(p);
    // BUG: Diagnostic contains: p = p.resolveSibling("string");
    p.resolveSibling("string");
    // BUG: Diagnostic contains: p = p.subpath(0, 1);
    p.subpath(0, 1);
    // BUG: Diagnostic contains: p = p.toAbsolutePath();
    p.toAbsolutePath();
    try {
      // BUG: Diagnostic contains: p = p.toRealPath();
      p.toRealPath();
    } catch (IOException e) {
    }
  }
}
{% endhighlight %}

### Negative examples
__ReturnValueIgnoredNegativeCases.java__

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

package com.google.errorprone.bugpatterns.testdata;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/** @author alexeagle@google.com (Alex Eagle) */
public class ReturnValueIgnoredNegativeCases {

  private String a = "thing";

  {
    String b = a.trim();
    System.out.println(a.trim());
    new String(new BigInteger(new byte[] {0x01}).add(BigInteger.ONE).toString());
  }

  String run() {
    a.trim().hashCode();
    return a.trim();
  }

  public void methodDoesntMatch() {
    Map<String, Integer> map = new HashMap<String, Integer>();
    map.put("test", 1);
  }

  public void methodDoesntMatch2() {
    final String b = a.toString().trim();
  }
}
{% endhighlight %}

