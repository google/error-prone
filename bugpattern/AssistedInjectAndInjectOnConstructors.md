---
title: AssistedInjectAndInjectOnConstructors
summary: '@AssistedInject and @Inject should not be used on different constructors
  in the same class.'
layout: bugpattern
category: INJECT
severity: WARNING
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Mixing @Inject and @AssistedInject leads to confusing code and the documentation specifies not to do it. See http://google-guice.googlecode.com/git/javadoc/com/google/inject/assistedinject/AssistedInject.html

## Suppression
Suppress false positives by adding an `@SuppressWarnings("AssistedInjectAndInjectOnConstructors")` annotation to the enclosing element.

----------

### Positive examples
__AssistedInjectAndInjectOnConstructorsPositiveCases.java__

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

import com.google.inject.assistedinject.AssistedInject;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class AssistedInjectAndInjectOnConstructorsPositiveCases {
  /**
   * Class has a constructor annotated with @javax.inject.Inject and another constructor annotated
   * with @AssistedInject.
   */
  public class TestClass1 {
    // BUG: Diagnostic contains: remove
    @javax.inject.Inject
    public TestClass1() {}

    // BUG: Diagnostic contains: remove
    @AssistedInject
    public TestClass1(int n) {}
  }
  
  /**
   * Class has a constructor annotated with @com.google.inject.Inject and another constructor
   * annotated with @AssistedInject.
   */
  public class TestClass2 {
    // BUG: Diagnostic contains: remove
    @com.google.inject.Inject
    public TestClass2() {}

    // BUG: Diagnostic contains: remove
    @AssistedInject
    public TestClass2(int n) {}
  }
  
  /**
   * Class has a constructor annotated with @com.google.inject.Inject, another constructor
   * annotated with @AssistedInject, and a third constructor with no annotation.
   */
  public class TestClass3 {
    // BUG: Diagnostic contains: remove
    @com.google.inject.Inject
    public TestClass3() {}

    // BUG: Diagnostic contains: remove
    @AssistedInject
    public TestClass3(int n) {}

    public TestClass3(String s) {}
  }
}
{% endhighlight %}

### Negative examples
__AssistedInjectAndInjectOnConstructorsNegativeCases.java__

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

import com.google.inject.assistedinject.AssistedInject;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class AssistedInjectAndInjectOnConstructorsNegativeCases {
  /**
   * Class has a single constructor with no annotation.
   */
  public class TestClass1 {
    TestClass1() {}
  }

  /**
   * Class has a constructor with a @javax.inject.Inject annotation.
   */
  public class TestClass2 {
    @javax.inject.Inject
    public TestClass2() {}
  }
  
  /**
   * Class has a constructor with a @com.google.inject.Inject annotation.
   */
  public class TestClass3 {
    @com.google.inject.Inject
    public TestClass3() {}
  }
  
  /**
   * Class has a constructor annotated with @AssistedInject
   */
  public class TestClass4 {
    @AssistedInject
    public TestClass4() {}
  }
  
  /**
   * Class has a constructor with a @AssistedInject annotation as well as an injectable field
   */
  public class TestClass5 {
    @javax.inject.Inject
    private int n;

    @AssistedInject
    public TestClass5() {}
  }
  
  /**
   * Class has a constructor annotated with @javax.inject.Inject and another constructor annotated
   * with @AssistedInject. The warning is suppressed.
   */
  @SuppressWarnings("AssistedInjectAndInjectOnConstructors")
  public class TestClass6 {
    @javax.inject.Inject
    public TestClass6() {}

    @AssistedInject
    public TestClass6(int n) {}
  }
}
{% endhighlight %}

