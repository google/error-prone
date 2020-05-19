---
title: StringSplitter
summary: String.split(String) has surprising behavior
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`String.split(String)` has surprising behaviour. For example, consider the
following puzzler from
http://konigsberg.blogspot.com/2009/11/final-thoughts-java-puzzler-splitting.html:

```java
String[] nothing = "".split(":");
String[] bunchOfNothing = ":".split(":");
```

The result is `[""]` and `[]`!

More examples:

input    | `input.split(":")`  | `Splitter.on(':').split(input)`
-------- | ------------------- | -------------------------------
`""`     | `[""]`              | `[""]`
`":"`    | `[]`                | `["", ""]`
`":::"`  | `[]`                | `["", "", "", ""]`
`"a:::"` | `["a"]`             | `["a", "", "", ""]`
`":::b"` | `["", "", "", "b"]` | `["", "", "", "b"]`

Prefer either:

*   Guava's
    [`Splitter`](http://google.github.io/guava/releases/23.0/api/docs/com/google/common/base/Splitter.html),
    which has less surprising behaviour and provides explicit control over the
    handling of empty strings and the trimming of whitespace with `trimResults`
    and `omitEmptyStrings`.

*   [`String.split(String, int)`](https://docs.oracle.com/javase/9/docs/api/java/lang/String.html#split-java.lang.String-int-)
    and setting an explicit 'limit' to `-1` to match the behaviour of
    `Splitter`.

TIP: if you use `Splitter`, consider extracting the instance to a `static`
`final` field.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StringSplitter")` to the enclosing element.


----------

### Positive examples
__StringSplitterPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2017 The Error Prone Authors.
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

/**
 * Positive test cases for StringSplitter check.
 *
 * @author dturner@twosigma.com (David Turner)
 */
public class StringSplitterPositiveCases {

  public void StringSplitOneArg() {
    String foo = "a:b";
    // BUG: Diagnostic contains:
    String[] xs = foo.split(":");
  }
}
{% endhighlight %}

### Negative examples
__StringSplitterNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2017 The Error Prone Authors.
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

/**
 * Negative test cases for StringSplitter check.
 *
 * @author dturner@twosigma.com (David Turner)
 */
public class StringSplitterNegativeCases {
  public void StringSplitTwoArgs() {
    String foo = "a:b";
    foo.split(":", 1);
  }

  public void StringSplitTwoArgsOneNegative() {
    String foo = "a:b";
    foo.split(":", -1);
  }
}
{% endhighlight %}

