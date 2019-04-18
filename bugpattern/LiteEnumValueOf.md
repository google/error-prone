---
title: LiteEnumValueOf
summary: Instead of converting enums to string and back, its numeric value should be used instead as it is the stable part of the protocol defined by the enum.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Byte code optimizers can change the implementation of `toString()` in lite
runtime and thus using `valueOf(String)` is discouraged. Instead of converting
enums to string and back, its numeric value should be used instead as it is the
stable part of the protocol defined by the enum.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LiteEnumValueOf")` to the enclosing element.

----------

### Positive examples
__LiteEnumValueOfPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2019 The Error Prone Authors.
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

/** Positive test cases for {@link LiteEnumValueOf} check. */
public class LiteEnumValueOfPositiveCases {

  @SuppressWarnings("StaticQualifiedUsingExpression")
  public static void checkCallOnValueOf() {
    // BUG: Diagnostic contains: LiteEnumValueOf
    ProtoLiteEnum.valueOf("FOO");

    // BUG: Diagnostic contains: LiteEnumValueOf
    ProtoLiteEnum.FOO.valueOf("FOO");
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
__LiteEnumValueOfNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2019 The Error Prone Authors.
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

import com.google.errorprone.bugpatterns.proto.ProtoTest.TestEnum;

/** Negative test cases for {@link LiteEnumValueOf} check. */
public class LiteEnumValueOfNegativeCases {

  @SuppressWarnings("StaticQualifiedUsingExpression")
  public static void checkCallOnValueOf() {
    TestEnum.valueOf("TEST_ENUM_VAL");

    TestEnum.TEST_ENUM_VAL.valueOf("TEST_ENUM_VAL");
  }
}
{% endhighlight %}

