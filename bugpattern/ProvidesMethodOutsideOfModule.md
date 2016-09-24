---
title: ProvidesMethodOutsideOfModule
summary: '@Provides methods need to be declared in a Module to have any effect.'
layout: bugpattern
category: GUICE
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Guice `@Provides` methods annotate methods that are used as a means of declaring bindings. However, this is only helpful inside of a module. Methods outside of these modules are not used for binding declaration.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ProvidesMethodOutsideOfModule")` annotation to the enclosing element.

----------

### Positive examples
__ProvidesMethodOutsideOfModulePositiveCases.java__

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

package com.google.errorprone.bugpatterns.inject.guice.testdata;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/** Tests for {@code ProvidesMethodOutsideOfModule} */
public class ProvidesMethodOutsideOfModulePositiveCases {

  /** Random class contains a provides method. */
  public class TestClass1 {
    // BUG: Diagnostic contains: remove
    @Provides
    void providesBlah() {}
  }

  /** Module contains an anonymous inner with a Provides method. */
  public class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      Object x =
          new Object() {
            // BUG: Diagnostic contains: remove
            @Provides
            void providesBlah() {}
          };
    }
  }

  /** Class has inner module class */
  public class TestClass2 {
    class NestedModule extends AbstractModule {
      @Override
      protected void configure() {}

      @Provides
      int thisIsOk() {
        return 42;
      }
    }

    // BUG: Diagnostic contains: remove
    @Provides
    int thisIsNotOk() {
      return 42;
    }
  }
}
{% endhighlight %}

### Negative examples
__ProvidesMethodOutsideOfModuleNegativeCases.java__

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

package com.google.errorprone.bugpatterns.inject.guice.testdata;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.GinModule;
import com.google.gwt.inject.client.binder.GinBinder;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;

/** Tests for {@code ProvidesMethodOutsideOfModule} */
public class ProvidesMethodOutsideOfModuleNegativeCases {

  /** Regular module */
  class Module1 extends AbstractModule {
    @Override
    protected void configure() {}

    @Provides
    int providesFoo() {
      return 42;
    }
  }

  /** implements the Module interface directly */
  class Module2 implements Module {
    @Override
    public void configure(Binder binder) {}

    @Provides
    int providesFoo() {
      return 42;
    }
  }

  /** Regular GinModule */
  class GinModule1 extends AbstractGinModule {

    @Override
    protected void configure() {}

    @Provides
    int providesFoo() {
      return 42;
    }
  }

  /** Implements the GinModule interface directly */
  class GinModule2 implements GinModule {
    @Override
    public void configure(GinBinder binder) {}

    @Provides
    int providesFoo() {
      return 42;
    }
  }
}
{% endhighlight %}

