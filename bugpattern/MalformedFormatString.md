---
title: MalformedFormatString
summary: "Printf-like format string does not match its arguments"
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
Format strings for printf family of functions contain format specifiers (placeholders) which must match amount and type of arguments that follow them. If there are more arguments then specifiers, redundant ones are silently ignored. If there are less, or their types don't match, runtime exception is thrown.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MalformedFormatString")` annotation to the enclosing element.

----------

## Examples
__MalformedFormatStringNegativeCases.java__

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

import java.io.Serializable;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * @author rburny@google.com (Radoslaw Burny)
 */
public class MalformedFormatStringNegativeCases {
  private static final Formatter formatter = new Formatter();
  private static final Locale locale = Locale.US;
  private static final String FORMAT_STRING = "%s %d";

  public void literalArguments() {
    System.out.printf("nothing here");
    System.out.printf("%d", 42);
    formatter.format("%f %b %s", 3.14, true, "foo");
    String.format("%c %x %s", '$', 7, new Object());
    String.format("%h %o", new Object(), 17);

    System.out.printf(locale, "%d", 42);
    formatter.format(locale, "%d", 42);
    String.format(locale, "%d", 42);
  }

  public void variableArguments() {
    String s = "bar";
    Integer i = 7;
    int j = 8;
    float f = 2.71f;
    Object o = new Object();

    System.out.printf("%d %s %d %f", i, s, j, f);
    formatter.format("%d %e", i, f);
    String.format("%d %h", j, o);
  }

  public void specialCases() {
    System.out.printf("%b", new Object());
    System.err.printf("%s %d %f", null, null, null);
    System.err.printf("%n %%");
    System.err.printf("%d", new Object[]{17});
    String.format("%s %s", (Object[]) new String[]{"foo", "bar"});
  }

  public void finalVariableFormat() {
    final String formatVar = "%s";
    String.format("%d" + "%f" + "%s", 1, 3.14, "foo");
    String.format(formatVar, true);
    String.format(FORMAT_STRING, "foo", 1);
    String.format(MalformedFormatStringNegativeCases.FORMAT_STRING, "foo", 1);
  }

  public void dynamicallyConstructedFormat() {
    // Using dynamically built format might be a bad practice, but it should still compile.
    Scanner scanner = new Scanner(System.in);
    String.format(scanner.nextLine(), 1);
    String variable = scanner.nextLine();
    String.format(variable, 1);
  }

  public <T> void typeVariables(T t, List<?> list) {
    // These calls are only safe when type variable is only ever substituted with Number. However,
    // we do not want to throw errors on them, as cleanup could be problematic.
    String.format("%d", t);
    String.format("%s", list);
    String.format("%d", list.get(0));
  }

  public <T> void integerSuperclasses() {
    Serializable s = 0;
    Comparable<Integer> c = 1;
    Number n = 2;
    String.format("%d %d %d", s, c, n);
  }
  // TODO(rburny): maybe test format flags, or more advanced usage?
}
{% endhighlight %}

__MalformedFormatStringPositiveCases.java__

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

import java.util.Formatter;
import java.util.Locale;
/**
 * @author rburny@google.com (Radoslaw Burny)
 */
public class MalformedFormatStringPositiveCases {
  private static final Formatter formatter = new Formatter();
  private static final Locale locale = Locale.US;
  private static final String FORMAT = "%s";

  public void extraArgs() throws Exception {
    // BUG: Diagnostic contains: System.out.printf("foo");
    System.out.printf("foo", "bar");
    // BUG: Diagnostic contains: formatter.format("%d", 42);
    formatter.format("%d", 42, 17);
    // BUG: Diagnostic contains: String.format(locale, "%n %%");
    String.format(locale, "%n %%", 1);
    // BUG: Diagnostic contains: expected 0, got 1
    System.out.printf("foo", "bar");

    // format call inside other statement
    // BUG: Diagnostic contains: throw new Exception(String.format(""));
    throw new Exception(String.format("", 42));
  }

  public void nonliteralFormats() {
    final String formatVar = "%s";
    // BUG: Diagnostic contains: String.format(formatVar, true);
    String.format(formatVar, true, false);
    // BUG: Diagnostic contains: String.format(FORMAT, true);
    String.format(FORMAT, true, false);
  }
}
{% endhighlight %}

