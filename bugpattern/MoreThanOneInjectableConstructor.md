---
title: MoreThanOneInjectableConstructor
summary: A class may not have more than one injectable constructor.
layout: bugpattern
category: INJECT
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Having more than one injectable constructor will throw a runtime error in compliant JSR-330 frameworks such as Guice or Dagger

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MoreThanOneInjectableConstructor")` annotation to the enclosing element.

----------

### Positive examples
__MoreThanOneInjectableConstructorPositiveCases.java__

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

import com.google.inject.Inject;
import java.beans.ConstructorProperties;

/**
 * @author sgoldfeder@google.com(Steven Goldfeder)
 */
public class MoreThanOneInjectableConstructorPositiveCases {

  /**
   * Class has 2 constructors, both are injectable
   */
  public class TestClass1 {
    // BUG: Diagnostic contains: remove
    @Inject
    public TestClass1() {}

    // BUG: Diagnostic contains: remove
    @Inject
    public TestClass1(int n) {}
  }

  /**
   * Class has 3 constructors, two of which are injectable.
   */
  public class TestClass2 {
    // BUG: Diagnostic contains: remove
    @Inject
    public TestClass2() {}

    // BUG: Diagnostic contains: remove
    @Inject
    public TestClass2(int n) {}

    public TestClass2(String s) {}
  }

  /**
   * testing that the error appears on the @Inject annotation even in the presence of other
   * annotations
   */
  public class TestClass3 {
    // BUG: Diagnostic contains: remove
    @Inject
    public TestClass3() {}

    // BUG: Diagnostic contains: remove
    @Inject
    @ConstructorProperties({"m", "n"})
    public TestClass3(int m, int n) {}
  }

  /**
   * This class tests that the error appears on the @Inject annotation even in the presence of other
   * unrelated annotations.
   */
  public class TestClass4 {

    // BUG: Diagnostic contains: remove
    @Inject
    public TestClass4() {}

    @ConstructorProperties({"m", "n"}) 
    // BUG: Diagnostic contains: remove
    @Inject
    public TestClass4(int m, int n) {}
  }
}
{% endhighlight %}

### Negative examples
__MoreThanOneInjectableConstructorNegativeCases.java__

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

import com.google.inject.Inject;

/**
 * @author sgoldfeder@google.com(Steven Goldfeder)
 */

public class MoreThanOneInjectableConstructorNegativeCases {

  /**
   * Class has a single non-injectable constructor.
   */
  public class TestClass1 {
    public TestClass1() {}
  }

  /**
   * Class has a single injectable constructor.
   */
  public class TestClass2 {
    @Inject
    public TestClass2() {}
  }

  /**
   * Class has two constructors, one of which is injectable.
   */
  public class TestClass3 {
    @Inject
    public TestClass3() {}

    public TestClass3(int n) {}
  }

  /**
   * Class has two constructors, one of which is injectable. Class also has an injectable field.
   */
  public class TestClass4 {

    @Inject
    String x;

    @Inject
    public TestClass4() {}

    public TestClass4(int n) {}
  }
  
  /**
   * Class has 2 constructors, both are injectable. Error is suppressed.
   */
  @SuppressWarnings("MoreThanOneInjectableConstructor")
  public class TestClass5 {
    @Inject
    public TestClass5() {}

    @Inject
    public TestClass5(int n) {}
  }

}
{% endhighlight %}

