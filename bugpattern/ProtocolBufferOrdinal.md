---
title: ProtocolBufferOrdinal
summary: To get the tag number of a protocol buffer enum, use getNumber() instead.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The generated Java source files for Protocol Buffer enums have `getNumber()` as
accessors for the tag number in the protobuf file.

In addition, since it's a java enum, it also has the `ordinal()` method,
returning its positional index within the generated java enum.

The `ordinal()` order of the generated Java enums isn't guaranteed, and can
change when a new enum value is inserted into a proto enum. The `getNumber()`
value won't change for an enum value (since making that change is a
backwards-incompatible change for the protocol buffer).

You should very likely use `getNumber()` in preference to `ordinal()` in all
circumstances since it's a more stable value.

Note: If you're changing code that was already using ordinal(), it's likely that
getNumber() will return a different real value. Tread carefully to avoid
mismatches if the ordinal was persisted elsewhere.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ProtocolBufferOrdinal")` to the enclosing element.

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
    // BUG: Diagnostic contains: ProtocolBufferOrdinal
    TestEnum.TEST_ENUM_VAL.ordinal();

    // BUG: Diagnostic contains: ProtocolBufferOrdinal
    ProtoLiteEnum.FOO.ordinal();
  }

  enum ProtoLiteEnum implements com.google.protobuf.Internal.EnumLite {
    FOO(1),
    BAR(2);
    private final int number;

    private ProtoLiteEnum(int number) {
      this.number = number;
    }

    @Override
    public int getNumber() {
      return number;
    }
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

