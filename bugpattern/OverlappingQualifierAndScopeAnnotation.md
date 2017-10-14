---
title: OverlappingQualifierAndScopeAnnotation
summary: 'Annotations cannot be both Scope annotations and Qualifier annotations:
  this causes confusion when trying to use them.'
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Qualifiers and Scoping annotations have different semantic meanings and a single
annotation should not be both a qualifier and a scoping annotation.

If an annotation is both a scoping annotation and a qualifier, unless great care
is taken with its application and usage, the semantics of objects annotated with
the annotation are unclear.

Take a look at this example:

```java
@Retention(RetentionPolicy.RUNTIME)
@Scope
@Qualifier
@interface DayScoped {}

static class Allowance {}
static class DailyAllowance extends Allowance {}
static class Spender {
  @Inject
  Spender(Allowance allowance) {}
}

static class BindingModule extends AbstractModule {
  ...
  @Provides
  @DayScoped
  Allowance providesAllowance() {
    return new DailyAllowance();
  }
}
```

Here, the `Allowance` instance used by Spender isn't actually scoped to a single
day, as the `@Provides` method applies the `DayScoped` scoping only to the
`@DayScoped Allowance`. Instead, the default constructor of `Allowance` is used
to create a new instance every time a `Spender` is created.

If `@DayScope` wasn't a `Qualifier`, the provider method would do the
right thing: the un-annotated `Announce` binding would be scoped to DayScope,
implemented by a single `DailyAllowance` instance per day.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("OverlappingQualifierAndScopeAnnotation")` annotation to the enclosing element.

----------

### Positive examples
__OverlappingQualifierAndScopeAnnotationPositiveCases.java__

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

package com.google.errorprone.bugpatterns.inject.testdata;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class OverlappingQualifierAndScopeAnnotationPositiveCases {

  @javax.inject.Scope
  @javax.inject.Qualifier
  // BUG: Diagnostic contains: OverlappingQualifierAndScopeAnnotation
  @interface JavaxScopeAndJavaxQualifier {}

  @com.google.inject.ScopeAnnotation
  @javax.inject.Qualifier
  // BUG: Diagnostic contains: OverlappingQualifierAndScopeAnnotation
  @interface GuiceScopeAndJavaxQualifier {}

  @com.google.inject.ScopeAnnotation
  @com.google.inject.BindingAnnotation
  // BUG: Diagnostic contains: OverlappingQualifierAndScopeAnnotation
  @interface GuiceScopeAndGuiceBindingAnnotation {}

  @javax.inject.Scope
  @com.google.inject.BindingAnnotation
  // BUG: Diagnostic contains: OverlappingQualifierAndScopeAnnotation
  @interface JavaxScopeAndGuiceBindingAnnotation {}
}
{% endhighlight %}

### Negative examples
__OverlappingQualifierAndScopeAnnotationNegativeCases.java__

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

package com.google.errorprone.bugpatterns.inject.testdata;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class OverlappingQualifierAndScopeAnnotationNegativeCases {

  @javax.inject.Scope
  @interface MyJavaxScope {}

  @com.google.inject.ScopeAnnotation
  @interface MyGuiceScope {}

  @javax.inject.Qualifier
  @interface MyJavaxQualifier {}

  @com.google.inject.BindingAnnotation
  @interface MyGuiceBindingAnnotation {}

  // supression tests
  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @javax.inject.Scope
  @javax.inject.Qualifier
  @interface JavaxScopeAndJavaxQualifier {}

  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @com.google.inject.ScopeAnnotation
  @javax.inject.Qualifier
  @interface GuiceScopeAndJavaxQualifier {}

  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @com.google.inject.ScopeAnnotation
  @com.google.inject.BindingAnnotation
  @interface GuiceScopeAndGuiceBindingAnnotation {}

  @SuppressWarnings("OverlappingQualifierAndScopeAnnotation")
  @javax.inject.Scope
  @com.google.inject.BindingAnnotation
  @interface JavaxScopeAndGuiceBindingAnnotation {}
}
{% endhighlight %}

