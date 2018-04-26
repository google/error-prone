---
title: BadNestedImport
summary: Importing nested classes with commonly-used names can make code harder to read, because it may not be clear from the context exactly which type is being referred to. Qualifying the name with that of the containing class can make the code clearer.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BadNestedImport")` to the enclosing element.

----------

### Positive examples
__BadNestedImportPositiveCases.java__

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
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Tests for {@link BadNestedImport}.
 *
 * @author awturner@google.com (Andy Turner)
 */
class BadNestedImportPositiveCases {
  public void variableDeclarations() {
    // BUG: Diagnostic contains: ImmutableList.Builder
    Builder<String> qualified;
    // BUG: Diagnostic contains: ImmutableList.Builder
    Builder raw;
  }

  public void variableDeclarationsNestedGenerics() {
    // BUG: Diagnostic contains: ImmutableList.Builder
    Builder<Builder<String>> builder1;
    // BUG: Diagnostic contains: ImmutableList.Builder
    Builder<Builder> builder1Raw;
    // BUG: Diagnostic contains: ImmutableList.Builder
    ImmutableList.Builder<Builder<String>> builder2;
    // BUG: Diagnostic contains: ImmutableList.Builder
    ImmutableList.Builder<Builder> builder2Raw;
  }

  public void newClass() {
    // BUG: Diagnostic contains: ImmutableList.Builder
    new Builder<String>();
    // BUG: Diagnostic contains: ImmutableList.Builder
    new Builder<Builder<String>>();
  }

  // BUG: Diagnostic contains: ImmutableList.Builder
  Builder<String> returnGenericExplicit() {
    // BUG: Diagnostic contains: ImmutableList.Builder
    return new Builder<String>();
  }

  // BUG: Diagnostic contains: ImmutableList.Builder
  Builder<String> returnGenericDiamond() {
    // BUG: Diagnostic contains: ImmutableList.Builder
    return new Builder<>();
  }

  // BUG: Diagnostic contains: ImmutableList.Builder
  Builder returnRaw() {
    // BUG: Diagnostic contains: ImmutableList.Builder
    return new Builder();
  }

  void classLiteral() {
    // BUG: Diagnostic contains: ImmutableList.Builder
    System.out.println(Builder.class);
  }
}
{% endhighlight %}

__BadNestedImportPositiveCases_expected.java__

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
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Tests for {@link BadNestedImport}.
 *
 * @author awturner@google.com (Andy Turner)
 */
class BadNestedImportPositiveCases {
  public void variableDeclarations() {
    ImmutableList.Builder<String> qualified;
    ImmutableList.Builder raw;
  }

  public void variableDeclarationsNestedGenerics() {
    ImmutableList.Builder<ImmutableList.Builder<String>> builder1;
    ImmutableList.Builder<ImmutableList.Builder> builder1Raw;
    ImmutableList.Builder<ImmutableList.Builder<String>> builder2;
    ImmutableList.Builder<ImmutableList.Builder> builder2Raw;
  }

  public void newClass() {
    new ImmutableList.Builder<String>();
    new ImmutableList.Builder<ImmutableList.Builder<String>>();
  }

  ImmutableList.Builder<String> returnGenericExplicit() {
    return new ImmutableList.Builder<String>();
  }

  ImmutableList.Builder<String> returnGenericDiamond() {
    return new ImmutableList.Builder<>();
  }

  ImmutableList.Builder returnRaw() {
    return new ImmutableList.Builder();
  }

  void classLiteral() {
    System.out.println(ImmutableList.Builder.class);
  }
}
{% endhighlight %}

### Negative examples
__BadNestedImportNegativeCases.java__

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
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Tests for {@link BadNestedImport}.
 *
 * @author awturner@google.com (Andy Turner)
 */
public class BadNestedImportNegativeCases {
  public void qualified() {
    ImmutableList.Builder<String> qualified;
    com.google.common.collect.ImmutableList.Builder<String> fullyQualified;
    ImmutableList.Builder raw;

    new ImmutableList.Builder<String>();
  }

  static class Nested {
    static class Builder {}

    void useNestedBuilder() {
      new Builder();
    }
  }
}
{% endhighlight %}

