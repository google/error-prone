---
title: NonRuntimeAnnotation
summary: Calling getAnnotation on an annotation that is not retained at runtime.
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
Calling getAnnotation on an annotation that does not have its Retention set to RetentionPolicy.RUNTIME will always return null.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("NonRuntimeAnnotation")` annotation to the enclosing element.

----------

## Examples
__NonRuntimeAnnotationNegativeCases.java__

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

package com.google.errorprone.bugpatterns;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author scottjohnson@google.com (Scott Johnsson)
 */
@NonRuntimeAnnotationNegativeCases.Runtime
public class NonRuntimeAnnotationNegativeCases {

  public Runtime testAnnotation() {
    return this.getClass().getAnnotation(NonRuntimeAnnotationNegativeCases.Runtime.class);
  }

  /**
   * Annotation that is retained at runtime
   */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Runtime {
  }
}
{% endhighlight %}

__NonRuntimeAnnotationPositiveCases.java__

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

package com.google.errorprone.bugpatterns;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * @author scottjohnson@google.com (Scott Johnsson)
 */
@NonRuntimeAnnotationPositiveCases.NotSpecified
@NonRuntimeAnnotationPositiveCases.NonRuntime
public class NonRuntimeAnnotationPositiveCases {

  public NonRuntime testAnnotation() {
    // BUG: Diagnostic contains: null
    NonRuntimeAnnotationPositiveCases.class.getAnnotation(
        NonRuntimeAnnotationPositiveCases.NonRuntime.class);
    // BUG: Diagnostic contains: null
    NonRuntimeAnnotationPositiveCases.class.getAnnotation(
        NonRuntimeAnnotationPositiveCases.NotSpecified.class);
    // BUG: Diagnostic contains: null
    return this.getClass().getAnnotation(NonRuntimeAnnotationPositiveCases.NonRuntime.class);
  }

  /**
   * Annotation that is explicitly NOT retained at runtime
   */
  @Retention(RetentionPolicy.SOURCE)
  public @interface NonRuntime {
  }
  
  /**
   * Annotation that is implicitly NOT retained at runtime
   */
  public @interface NotSpecified {
  }
}
{% endhighlight %}

