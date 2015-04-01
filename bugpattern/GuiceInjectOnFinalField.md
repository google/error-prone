---
title: GuiceInjectOnFinalField
layout: bugpattern
category: GUICE
severity: WARNING
maturity: EXPERIMENTAL
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>GUICE</td></tr>
<tr><td>Severity</td><td>WARNING</td></tr>
<tr><td>Maturity</td><td>EXPERIMENTAL</td></tr>
</table></div>

# Bug pattern: GuiceInjectOnFinalField
__Although Guice allows injecting final fields, doing so is not recommended because the injected value may not be visible to other threads.__

## The problem
See https://code.google.com/p/google-guice/wiki/InjectionPoints

## Suppression
Suppress false positives by adding an `@SuppressWarnings("GuiceInjectOnFinalField")` annotation to the enclosing element.

----------

# Examples
__GuiceInjectOnFinalFieldNegativeCases.java__

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

import com.google.inject.Inject;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class GuiceInjectOnFinalFieldNegativeCases {
  
  /**
   * Class has no final fields or @Inject annotations.
   */
  public class TestClass1 {}

  /**
   * Class has a final field that is not injectable.
   */
  public class TestClass2 {
    public final int n = 0;
  }
  
  /**
   * Class has an injectable(com.google.inject.Inject) field that is not final.
   */
  public class TestClass3 {
    @Inject public int n;
  }
  
  /**
   * Class has an injectable(com.google.inject.Inject), final method.
   */
  public class TestClass4 {
    @Inject
    final void method() {}
  }
}
{% endhighlight %}

__GuiceInjectOnFinalFieldPositiveCases.java__

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

import com.google.inject.Inject;

import java.util.Set;

import javax.annotation.Nullable;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class GuiceInjectOnFinalFieldPositiveCases {
  /**
   * Class has a final injectable(com.google.inject.Inject) field.
   */
  public class TestClass1 {
    // BUG: Diagnostic contains: @Inject int a
    @Inject final int a = 0;

    
    // BUG: Diagnostic contains: @Inject public int b
    @Inject
    public final int b = 0;
  
    // BUG: Diagnostic contains: @Inject @Nullable Object c
    @Inject @Nullable
    final Object c = null;
  }
}
{% endhighlight %}

