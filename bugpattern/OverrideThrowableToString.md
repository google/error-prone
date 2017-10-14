---
title: OverrideThrowableToString
summary: To return a custom message with a Throwable class, one should override getMessage() instead of toString() for Throwable.
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
Many logging tools build a string representation out of getMessage() and ignores toString() completely.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("OverrideThrowableToString")` annotation to the enclosing element.

----------

### Positive examples
__OverrideThrowableToStringPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** @author mariasam@google.com (Maria Sam) */
class OverrideThrowableToStringPositiveCases {

  // BUG: Diagnostic contains: override
  class BasicTest extends Throwable {

    @Override
    public String toString() {
      return "";
    }
  }

  // BUG: Diagnostic contains: override
  class MultipleMethods extends Throwable {

    public MultipleMethods() {
      ;
    }

    @Override
    public String toString() {
      return "";
    }
  }

  // BUG: Diagnostic contains: override
  class NoOverride extends Throwable {

    public String toString() {
      return "";
    }
  }
}
{% endhighlight %}

__OverrideThrowableToStringPositiveCases_expected.java__

{% highlight java %}
/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** @author mariasam@google.com (Maria Sam) */
class OverrideThrowableToStringPositiveCases {

  // BUG: Diagnostic contains: override
  class BasicTest extends Throwable {

    @Override
    public String getMessage() {
      return "";
    }
  }

  class MultipleMethods extends Throwable {

    public MultipleMethods() {
      ;
    }

    @Override
    public String getMessage() {
      return "";
    }
  }

  class NoOverride extends Throwable {

    public String getMessage() {
      return "";
    }
  }
}
{% endhighlight %}

### Negative examples
__OverrideThrowableToStringNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** @author mariasam@google.com (Maria Sam) */
public class OverrideThrowableToStringNegativeCases {

  class BasicTest extends Throwable {}

  class OtherToString {
    public String toString() {
      return "";
    }
  }

  class NoToString extends Throwable {
    public void test() {
      System.out.println("test");
    }
  }

  class GetMessage extends Throwable {
    public String getMessage() {
      return "";
    }
  }
}
{% endhighlight %}

