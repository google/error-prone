---
title: ProtoFieldNullComparison
summary: Protobuf fields cannot be null
layout: bugpattern
category: ONE_OFF
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
This checker looks for comparisons of protocol buffer fields with null. If a proto field is not specified, its field accessor will return a non-null default value. Thus, the result of calling one of these accessors can never be null, and comparisons like these often indicate a nearby error.

If you meant to check whether an optional field has been set, you should use the hasField() method instead.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ProtoFieldNullComparison")` annotation to the enclosing element.

----------

### Positive examples
__ProtoFieldNullComparisonPositiveCases.java__

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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

/**
 * Positive examples for invalid null comparison of a proto message field.
 */
public class ProtoFieldNullComparisonPositiveCases {
  public static void main(String[] args) {
    TestProtoMessage message = TestProtoMessage.newBuilder().build();
    // BUG: Diagnostic contains: message.hasMessage()
    if (message.getMessage() != null) {
      System.out.println("always true");
    // BUG: Diagnostic contains: !message.hasMessage()
    } else if (message.getMessage() == null) {
      System.out.println("impossible");
    // BUG: Diagnostic contains: message.hasMessage()
    } else if (null != message.getMessage()) {
      System.out.println("always true");
    // BUG: Diagnostic contains: message.getMessage().hasField()
    } else if (message.getMessage().getField() != null) {
      System.out.println("always true");
    // BUG: Diagnostic contains: !message.getMultiFieldList().isEmpty()
    } else if (message.getMultiFieldList() != null) {
      System.out.println("always true");
    // BUG: Diagnostic contains: message.getMultiFieldList().isEmpty()
    } else if (null == message.getMultiFieldList()) {
      System.out.println("impossible");
    }
  }
}
{% endhighlight %}

### Negative examples
__ProtoFieldNullComparisonNegativeCases.java__

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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

/**
 * Negative examples for invalid null comparison of a proto message field.
 */
public class ProtoFieldNullComparisonNegativeCases {
  public static void main(String[] args) {
    TestProtoMessage message = TestProtoMessage.newBuilder().build();
    Object object = new Object();
    if (message.getMessage() != object) {
    } else if (object != message.getMessage()) {
    } else if (message.getMessage().getField() != object) {
    } else if (message.getMultiFieldList() != object) {
    } else if (object == message.getMultiFieldList()) {
    }
  }
}
{% endhighlight %}

