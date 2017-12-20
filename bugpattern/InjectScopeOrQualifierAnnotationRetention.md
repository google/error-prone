---
title: InjectScopeOrQualifierAnnotationRetention
summary: Scoping and qualifier annotations must have runtime retention.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Qualifier and Scope annotations are used by dependency injection frameworks to adjust their
behavior. Not having runtime retention on scoping or qualifier annotations will cause unexpected
behavior in frameworks that use reflection:

```java
class CreditCardProcessor { @Inject CreditCardProcessor(...) }

@Qualifier
@interface ForTests

@Provides
@ForTests
CreditCardProcessor providesTestProcessor() { return new TestCreditCardProcessor(...) }
...

@Inject
MyApp(CreditCardProcessor processor) {
  processor.issueCharge(...); // Issues a charge against a fake!
}
```
Since the Qualifier doesn't have runtime retention, the Guice provider method doesn't see the
annotation, and will use the TestCreditCardProcessor for the normal CreditCardProcessor injection
point.

NOTE: Even for dependency injection frameworks traditionally considered to be
compile-time dependent, the JSR-330 specification still requires runtime
retention for both [`Qualifier`] and [`Scope`].

[`Qualifier`]: http://docs.oracle.com/javaee/6/api/javax/inject/Qualifier.html
[`Scope`]: http://docs.oracle.com/javaee/6/api/javax/inject/Scope.html

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InjectScopeOrQualifierAnnotationRetention")` to the enclosing element.

----------

### Positive examples
__ScopeOrQualifierAnnotationRetentionPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2013 The Error Prone Authors.
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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import com.google.inject.BindingAnnotation;
import com.google.inject.ScopeAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Qualifier;
import javax.inject.Scope;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class ScopeOrQualifierAnnotationRetentionPositiveCases {
  /** A scoping (@Scope) annotation with SOURCE retention */
  @Scope
  @Target({TYPE, METHOD})
  // BUG: Diagnostic contains: @Retention(RUNTIME)
  @Retention(SOURCE)
  public @interface TestAnnotation1 {}

  /** A scoping (@ScopingAnnotation) annotation with SOURCE retention. */
  @ScopeAnnotation
  @Target({TYPE, METHOD})
  // BUG: Diagnostic contains: @Retention(RUNTIME)
  @Retention(SOURCE)
  public @interface TestAnnotation2 {}

  /** A qualifer(@Qualifier) annotation with SOURCE retention. */
  @Qualifier
  @Target({TYPE, METHOD})
  // BUG: Diagnostic contains: @Retention(RUNTIME)
  @Retention(SOURCE)
  public @interface TestAnnotation3 {}

  /** A qualifer(@BindingAnnotation) annotation with SOURCE retention. */
  @BindingAnnotation
  @Target({TYPE, METHOD})
  // BUG: Diagnostic contains: @Retention(RUNTIME)
  @Retention(SOURCE)
  public @interface TestAnnotation4 {}

  /** A qualifer annotation with default retention. */
  @BindingAnnotation
  @Target({TYPE, METHOD})
  // BUG: Diagnostic contains: @Retention(RUNTIME)
  public @interface TestAnnotation5 {}
}
{% endhighlight %}

### Negative examples
__ScopeOrQualifierAnnotationRetentionNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2013 The Error Prone Authors.
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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import com.google.inject.BindingAnnotation;
import com.google.inject.ScopeAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Qualifier;
import javax.inject.Scope;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class ScopeOrQualifierAnnotationRetentionNegativeCases {
  /** A scoping (@Scope) annotation with runtime retention */
  @Scope
  @Target({TYPE, METHOD})
  @Retention(RUNTIME)
  public @interface TestAnnotation1 {}

  /** A scoping (@ScopingAnnotation) annotation with runtime retention. */
  @ScopeAnnotation
  @Target({TYPE, METHOD})
  @Retention(RUNTIME)
  public @interface TestAnnotation2 {}

  /** A qualifer(@Qualifier) annotation with runtime retention. */
  @Qualifier
  @Target({TYPE, METHOD})
  @Retention(RUNTIME)
  public @interface TestAnnotation3 {}

  /** A qualifer(@BindingAnnotation) annotation with runtime retention. */
  @BindingAnnotation
  @Target({TYPE, METHOD})
  @Retention(RUNTIME)
  public @interface TestAnnotation4 {}

  /** A non-qualifer, non-scoping annotation without runtime retention. */
  @Retention(SOURCE)
  public @interface TestAnnotation5 {}
}
{% endhighlight %}

