---
title: InjectOnMemberAndConstructor
summary: Members shouldn't be annotated with @Inject if constructor is already annotated @Inject
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InjectOnMemberAndConstructor")` to the enclosing element.

----------

### Negative examples
__InjectOnMemberAndConstructorNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2018 The Error Prone Authors.
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

import javax.inject.Inject;

/**
 * Negative test cases for {@link InjectOnMemberAndConstructor} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class InjectOnMemberAndConstructorNegativeCases {

  public class InjectOnConstructorOnly {
    private final String stringFieldWithoutInject;

    @Inject
    public InjectOnConstructorOnly(String stringFieldWithoutInject) {
      this.stringFieldWithoutInject = stringFieldWithoutInject;
    }
  }

  public class InjectOnFieldOnly {
    @Inject private String stringFieldWithInject;
  }

  public class MixedInject {
    @Inject private String stringFieldWithInject;

    @Inject
    public MixedInject() {}
  }
}
{% endhighlight %}

