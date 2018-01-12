---
title: StringSplit
summary: String.split should never take only a single argument; it has surprising behavior
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
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

Prefer guava's
[`String.splitter`](http://google.github.io/guava/releases/23.0/api/docs/com/google/common/base/Splitter.html),
which has more predicitable behaviour and provides explicit control over the
handling of empty strings and the trimming of whitespace.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StringSplit")` to the enclosing element.

----------

### Positive examples
__StringSplitPositiveCases.java__

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
 * Positive test cases for StringSplit check.
 *
 * @author dturner@twosigma.com (David Turner)
 */
public class StringSplitPositiveCases {

  public void StringSplitOneArg() {
    String foo = "a:b";
    // BUG: Diagnostic contains: String.split
    foo.split(":");
  }
}
{% endhighlight %}

### Negative examples
__StringSplitNegativeCases.java__

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
 * Negative test cases for StringSplit check.
 *
 * @author dturner@twosigma.com (David Turner)
 */
public class StringSplitNegativeCases {
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

