---
title: QualifierWithTypeUse
summary: Injection frameworks currently don't understand Qualifiers in TYPE_PARAMETER or TYPE_USE contexts.
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Allowing a qualifier annotation in [`TYPE_PARAMETER`] or [`TYPE_USE`] contexts
allows end users to write code like:

```java
@Inject Foo(List<@MyAnnotation String> strings)
```

Guice, Dagger, and other dependency injection frameworks don't currently see
type annotations in this context, so the above code is equivalent to:

```java
@Inject Foo(List<String> strings)
```

[`TYPE_PARAMETER`]: https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/ElementType.html#TYPE_PARAMETER
[`TYPE_USE`]: https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/ElementType.html#TYPE_USE

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("QualifierWithTypeUse")` to the enclosing element.

----------

### Positive examples
__QualifierWithTypeUsePositiveCases.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject.testdata;

import com.google.inject.BindingAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/** Tests for {@code QualifierWithTypeUse} */
public class QualifierWithTypeUsePositiveCases {

  @Qualifier
  // BUG: Diagnostic contains: @Target({CONSTRUCTOR})
  @Target({ElementType.TYPE_USE, ElementType.CONSTRUCTOR})
  @interface Qualifier1 {}

  @Qualifier
  // BUG: Diagnostic contains: remove
  @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
  @interface Qualifier2 {}

  @BindingAnnotation
  // BUG: Diagnostic contains: @Target({FIELD})
  @Target({ElementType.FIELD, ElementType.TYPE_USE})
  @interface BindingAnnotation1 {}

  @BindingAnnotation
  // BUG: Diagnostic contains: remove
  @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
  @interface BindingAnnotation2 {}

  @BindingAnnotation
  // BUG: Diagnostic contains: remove
  @Target(ElementType.TYPE_USE)
  @interface BindingAnnotation3 {}
}
{% endhighlight %}

### Negative examples
__QualifierWithTypeUseNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject.testdata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/** Tests for {@code QualifierWithTypeUse} */
public class QualifierWithTypeUseNegativeCases {

  @Qualifier
  @Target({ElementType.CONSTRUCTOR})
  @interface Qualifier1 {}

  @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
  @interface NotAQualifier {}
}
{% endhighlight %}

