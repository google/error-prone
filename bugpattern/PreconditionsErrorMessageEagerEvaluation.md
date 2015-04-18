---
title: PreconditionsErrorMessageEagerEvaluation
layout: bugpattern
category: GUAVA
severity: WARNING
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>GUAVA</td></tr>
<tr><td>Severity</td><td>WARNING</td></tr>
<tr><td>Maturity</td><td>EXPERIMENTAL</td></tr>
</table></div>

# Bug pattern: PreconditionsErrorMessageEagerEvaluation
__Second argument to Preconditions.* is a call to String.format(), which can be unwrapped__

## The problem
Preconditions checks take an error message to display if the check fails. The error message is rarely needed, so it should either be cheap to construct or constructed only when needed. This check ensures that these error messages are not constructed using expensive methods that are evaluated eagerly.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("PreconditionsErrorMessageEagerEvaluation")` annotation to the enclosing element.

----------

# Examples
__PreconditionsExpensiveStringNegativeCase1.java__

{% highlight java %}
/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.common.base.Preconditions;

/**
 * Preconditions calls which shouldn't be picked up for expensive string operations
 * 
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class PreconditionsExpensiveStringNegativeCase1 {
  public void error() {
    int foo = 42;
    Preconditions.checkState(true, "The foo %s foo  is not a good foo", foo);

    // This call should not be converted because of the %d, which does some locale specific
    // behaviour. If it were an %s, it would be fair game.
    Preconditions.checkState(true, String.format("The foo %d foo is not a good foo", foo));
  }
}
{% endhighlight %}

__PreconditionsExpensiveStringNegativeCase2.java__

{% highlight java %}
/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.common.base.Preconditions;

/**
 * Test for methodIs call including string concatenation.
 * (Not yet supported, so this is a negative case)
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class PreconditionsExpensiveStringNegativeCase2 {
  public void error() {
    int foo = 42;
    Preconditions.checkState(true, "The foo" + foo + " is not a good foo");
  }
}
{% endhighlight %}

__PreconditionsExpensiveStringPositiveCase1.java__

{% highlight java %}
/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.common.base.Preconditions;

/**
 * Test for methodIs call involving String.format() and %s
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class PreconditionsExpensiveStringPositiveCase1 {
  public void error() {
    int foo = 42;
    int bar = 78;
    Preconditions.checkState(true, String.format("The foo %s (%s) is not a good foo", foo, bar));
  }
}
{% endhighlight %}

