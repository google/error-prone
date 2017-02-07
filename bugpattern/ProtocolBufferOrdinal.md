---
title: ProtocolBufferOrdinal
summary: ordinal() value of Protocol Buffer Enum can change if enumeration order is changed
layout: bugpattern
category: PROTOBUF
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Shuffling of values in a Protocol Buffer enum can change the ordinal value of the enum member. Since changing tag number isn't advisable in protos, use #getNumber() instead which gives the tag number.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ProtocolBufferOrdinal")` annotation to the enclosing element.

----------

### Positive examples
__ProtocolBufferOrdinalPositiveCases.java__

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

import com.google.errorprone.bugpatterns.proto.TestEnum;

/** Positive test cases for {@link ProtocolBufferOrdinal} check. */
public class ProtocolBufferOrdinalPositiveCases {

  public static void checkCallOnOrdinal() {
    // BUG: Diagnostic contains: TestEnum.TEST_ENUM_VAL.getNumber()
    TestEnum.TEST_ENUM_VAL.ordinal();
  }
}
{% endhighlight %}

### Negative examples
__ProtocolBufferOrdinalNegativeCases.java__

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

import com.google.errorprone.bugpatterns.proto.TestEnum;

/** Negative test cases for {@link ProtocolBufferOrdinal} check. */
public class ProtocolBufferOrdinalNegativeCases {

  public static void checkProtoEnum() {
    TestEnum.TEST_ENUM_VAL.getNumber();
  }
}
{% endhighlight %}

