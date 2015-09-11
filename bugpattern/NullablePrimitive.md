---
title: NullablePrimitive
summary: '@Nullable should not be used for primitive types.'
layout: bugpattern
category: JDK
severity: WARNING
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Primitives can never be null, annotating a primitive with @Nullable may be hinting at an intent that cannot be fulfilled.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("NullablePrimitive")` annotation to the enclosing element.

----------

## Examples
__NullablePrimitiveNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import javax.annotation.Nullable;

/**
 * @author sebastian.h.monte@gmail.com (Sebastian Monte)
 */
public class NullablePrimitiveNegativeCases {
  @Nullable
  Integer a;

  public void method(@Nullable Integer a) {}

  @Nullable
  public Integer method() {
    return new Integer(0);
  }
}
{% endhighlight %}

__NullablePrimitivePositiveCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import javax.annotation.Nullable;

/**
 * @author sebastian.h.monte@gmail.com (Sebastian Monte)
 */
public class NullablePrimitivePositiveCases {

  // BUG: Diagnostic contains: remove
  @Nullable
  int a;

  public void method(
      // BUG: Diagnostic contains: remove
      @Nullable
      int a) {
  }

  // BUG: Diagnostic contains: remove
  @Nullable
  public int method() {
    return 0;
  }
}
{% endhighlight %}

