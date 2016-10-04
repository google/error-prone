---
title: PreconditionsInvalidPlaceholder
summary: Preconditions only accepts the %s placeholder in error message strings
layout: bugpattern
category: GUAVA
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The Guava Preconditions checks take error message template strings that look similar to format strings but only accept %s as a placeholder. This check points out places where there is a non-%s placeholder in a Preconditions error message template string and the number of arguments does not match the number of %s placeholders.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("PreconditionsInvalidPlaceholder")` annotation to the enclosing element.

----------

### Positive examples
__PreconditionsInvalidPlaceholderPositiveCase1.java__

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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;

public class PreconditionsInvalidPlaceholderPositiveCase1 {
  int foo;
  
  public void checkPositive(int x) {
    // BUG: Diagnostic contains: %s > 0
    checkArgument(x > 0, "%d > 0", x);
  }
  
  public void checkFoo() {
    // BUG: Diagnostic contains: foo must be equal to 0 but was %s
    Preconditions.checkState(foo == 0, "foo must be equal to 0 but was {0}", foo);
  }
}
{% endhighlight %}

### Negative examples
__PreconditionsInvalidPlaceholderNegativeCase1.java__

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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;

public class PreconditionsInvalidPlaceholderNegativeCase1 {
  Integer foo;
  
  public void checkPositive(int x) {
    checkArgument(x > 0, "%s > 0", x);
  }

  public void checkTooFewArgs(int x) {
    checkArgument(x > 0, "%s %s", x);
  }
  
  public void checkFoo() {
    Preconditions.checkState(foo.intValue() == 0, "foo must be equal to 0 but was %s", foo);
  }
  
  public static void checkNotNull(Object foo, String bar, Object baz) {
    
  }
  
  public void checkSelf() {
    checkNotNull(foo, "Foo", this);
  }
}
{% endhighlight %}

