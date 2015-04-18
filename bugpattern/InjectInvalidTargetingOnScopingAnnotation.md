---
title: InjectInvalidTargetingOnScopingAnnotation
layout: bugpattern
category: INJECT
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>INJECT</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>EXPERIMENTAL</td></tr>
</table></div>

# Bug pattern: InjectInvalidTargetingOnScopingAnnotation
__The target of a scoping annotation must be set to METHOD and/or TYPE.__

## The problem
Scoping annotations are only appropriate for provision and therefore are only appropriate on @Provides methods and classes that will be provided just-in-time.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("InjectInvalidTargetingOnScopingAnnotation")` annotation to the enclosing element.

----------

# Examples
__InjectInvalidTargetingOnScopingAnnotationNegativeCases.java__

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

package com.google.errorprone.bugpatterns;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

import javax.inject.Scope;

/**
 * @author sgoldfeder@google.com(Steven Goldfeder)
 */
public class InjectInvalidTargetingOnScopingAnnotationNegativeCases {

  /**
   * A scoping annotation with legal targeting.
   */
  @Target(TYPE)
  @Scope
  public @interface TestAnnotation1 {
  }

  /**
   * A scoping annotation with legal targeting.
   */
  @Target(METHOD)
  @Scope
  public @interface TestAnnotation2 {
  }

  /**
   * A scoping annotation with legal targeting.
   */
  @Target({TYPE, METHOD})
  @Scope
  public @interface TestAnnotation3 {
  }

  /**
   * A non-scoping annotation with targeting that would be illegal if it were a scoping annotation.
   */
  @Target(PARAMETER)
  public @interface TestAnnotation4 {
  }
}
{% endhighlight %}

__InjectInvalidTargetingOnScopingAnnotationPositiveCases.java__

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

package com.google.errorprone.bugpatterns;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

import javax.inject.Scope;

/**
 * @author sgoldfeder@google.com(Steven Goldfeder)
 */
public class InjectInvalidTargetingOnScopingAnnotationPositiveCases {

  /**
   * A scoping annotation with no specified target.
   */
  @Scope 
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  public @interface TestAnnotation1 {
  }

  /**
   * @Target is given an empty array
   */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  @Target({})
  @Scope 
  public @interface TestAnnotation2 {
  }

  /**
   * A scoping annotation with taeget TYPE, METHOD, and (illegal) PARAMETER.
   */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  @Target({TYPE, METHOD, PARAMETER})
  @Scope 
  public @interface TestAnnotation3 {
  }

  /**
   * A scoping annotation target set to PARAMETER.
   */
  // BUG: Diagnostic contains: @Target({TYPE, METHOD})
  @Target(PARAMETER)
  @Scope 
  public @interface TestAnnotation4 {
  }
}
{% endhighlight %}

