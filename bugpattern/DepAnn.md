---
title: DepAnn
summary: Deprecated item is not annotated with @Deprecated
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: dep-ann_

## The problem
A declaration has the `@deprecated` Javadoc tag but no `@Deprecated` annotation. Please add an `@Deprecated` annotation to this declaration in addition to the `@deprecated` tag in the Javadoc.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("DepAnn")` annotation to the enclosing element.

----------

### Positive examples
__DepAnnPositiveCases.java__

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

package com.google.errorprone.bugpatterns.testdata;

/** @deprecated */
// BUG: Diagnostic contains: @Deprecated
public class DepAnnPositiveCases {

  /** @deprecated */
  // BUG: Diagnostic contains: @Deprecated
  public DepAnnPositiveCases() {}

  /** @deprecated */
  // BUG: Diagnostic contains: @Deprecated
  int myField;

  /** @deprecated */
  // BUG: Diagnostic contains: @Deprecated
  enum Enum {
    VALUE,
  }

  /** @deprecated */
  // BUG: Diagnostic contains: @Deprecated
  interface Interface {}

  /** @deprecated */
  // BUG: Diagnostic contains: @Deprecated
  public void deprecatedMethood() {}
}
{% endhighlight %}

### Negative examples
__DepAnnNegativeCase1.java__

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

package com.google.errorprone.bugpatterns.testdata;

/** @deprecated */
@Deprecated
public class DepAnnNegativeCase1 {

  /** @deprecated */
  @Deprecated
  public DepAnnNegativeCase1() {}

  /** @deprecated */
  @Deprecated int myField;

  /** @deprecated */
  @Deprecated
  enum Enum {
    VALUE,
  }

  /** @deprecated */
  @Deprecated
  interface Interface {}

  /** @deprecated */
  @Deprecated
  public void deprecatedMethood() {}

  @Deprecated
  public void deprecatedMethoodWithoutComment() {}

  /** deprecated */
  public void deprecatedMethodWithMalformedComment() {}

  /** @deprecated */
  @SuppressWarnings("dep-ann")
  public void suppressed() {}

  public void newMethod() {}
}
{% endhighlight %}

__DepAnnNegativeCase2.java__

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

package com.google.errorprone.bugpatterns.testdata;

/** @deprecated */
@Deprecated
public class DepAnnNegativeCase2 {

  abstract class Builder2<P> {
    class SummaryRowKey<P> {}

    @Deprecated
    /** @deprecated use {@link Selector.Builder#withSummary()} */
    public abstract void withSummaryRowKeys(int summaryRowKeys);

    /** @deprecated use {@link Selector.Builder#withSummary()} */
    @Deprecated
    public abstract void m1();

    public abstract void m2();
  }
}
{% endhighlight %}

