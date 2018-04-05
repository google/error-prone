---
title: AutoFactoryAtInject
summary: '@AutoFactory and @Inject should not be used in the same type.'
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
@AutoFactory classes should not be @Inject-ed, inject the generated factory
instead. Classes that are annotated with @AutoFactory are intended to be
constructed by invoking the factory method on the generated factory. Typically
this is because some of the necessary constructor arguments are not part of the
binding graph. Generated @AutoFactory classes are automatically marked @Inject -
prefer to inject that instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AutoFactoryAtInject")` to the enclosing element.

----------

### Positive examples
__AutoFactoryAtInjectPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.auto.factory.AutoFactory;
import javax.inject.Inject;

class AssistedInjectAndInjectOnSameConstructorPositiveCases {

  @AutoFactory
  static class HasAutoFactoryOnClass {
    // BUG: Diagnostic contains: remove
    @Inject
    HasAutoFactoryOnClass() {}
  }

  @AutoFactory
  static class UsesGuiceInject {
    // BUG: Diagnostic contains: remove
    @com.google.inject.Inject
    UsesGuiceInject() {}
  }

  static class HasAutoFactoryOnConstructor {
    // BUG: Diagnostic contains: remove
    @Inject
    @AutoFactory
    HasAutoFactoryOnConstructor() {}
  }
}
{% endhighlight %}

### Negative examples
__AutoFactoryAtInjectNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.auto.factory.AutoFactory;
import javax.inject.Inject;

class AutoFactoryAtInjectNegativeCases {

  @AutoFactory
  static class AtInjectOnInnerType {
    static class InnerType {
      @Inject
      InnerType() {}
    }
  }

  static class AutoFactoryOnInnerType {
    @Inject
    AutoFactoryOnInnerType() {}

    @AutoFactory
    static class InnerType {}
  }

  static class OnDifferentConstructors {
    @Inject
    OnDifferentConstructors(String string) {}

    @AutoFactory
    OnDifferentConstructors(Object object) {}
  }
}
{% endhighlight %}

