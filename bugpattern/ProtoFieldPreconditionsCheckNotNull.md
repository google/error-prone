---
title: ProtoFieldPreconditionsCheckNotNull
layout: bugpattern
category: GUAVA
severity: WARNING
maturity: MATURE
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>GUAVA</td></tr>
<tr><td>Severity</td><td>WARNING</td></tr>
<tr><td>Maturity</td><td>MATURE</td></tr>
</table></div>

# Bug pattern: ProtoFieldPreconditionsCheckNotNull
__Protobuf fields cannot be null, so this check is redundant__

## The problem
This checker looks for comparisons of protocol buffer fields with null via the com.google.common.base.Preconditions.checkNotNull method. If a proto field is not specified, its field accessor will return a non-null default value. Thus, the result of calling one of these accessors can never be null, and comparisons like these often indicate a nearby error.

If you meant to check whether an optional field has been set, you should use the hasField() method instead.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ProtoFieldPreconditionsCheckNotNull")` annotation to the enclosing element.

----------

# Examples
__ProtoFieldPreconditionsCheckNotNullNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

/**
 * Negative examples for invalid null comparison of a proto message field.
 */
public class ProtoFieldPreconditionsCheckNotNullNegativeCases {
  public static void main(String[] args) {
    TestProtoMessage message = TestProtoMessage.newBuilder().build();

    // This is not {@link com.google.common.base.Preconditions#checkNotNull}, so it should
    // be acceptable.
    Preconditions.checkNotNull(message.getMessage());
    Preconditions.checkNotNull(message.getMultiFieldList());
    Preconditions.checkNotNull(message.getMessage(), "Message");
    Preconditions.checkNotNull(message.getMultiFieldList(), "Message");
    Preconditions.checkNotNull(message.getMessage(), "Message %s", new Object());
    Preconditions.checkNotNull(message.getMultiFieldList(), "Message %s", new Object());

    // Checking a non-proto object should be acceptable.
    com.google.common.base.Preconditions.checkNotNull(new Object());
    com.google.common.base.Preconditions.checkNotNull(new Object(), "Message");
    com.google.common.base.Preconditions.checkNotNull(new Object(), "Message %s", new Object());
  }

  private static final class Preconditions {
    private Preconditions() {}

    static void checkNotNull(Object reference) {}

    static void checkNotNull(Object reference, Object errorMessage) {}

    static void checkNotNull(
        Object reference,
        String errorMessageTemplate,
        Object... errorMessageArgs) {}
  }
}

{% endhighlight %}

__ProtoFieldPreconditionsCheckNotNullPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

/**
 * Positive examples for checking a proto message field using
 * {@link Preconditions#checkNotNull(Object)} and related methods.
 */
public class ProtoFieldPreconditionsCheckNotNullPositiveCases {
  public static void main(String[] args) {
    TestProtoMessage message = TestProtoMessage.newBuilder().build();

    // BUG: Diagnostic contains: check is redundant
    // remove this line
    Preconditions.checkNotNull(message.getMessage());
    // BUG: Diagnostic contains: check is redundant
    // remove this line
    Preconditions.checkNotNull(message.getMultiFieldList());

    // BUG: Diagnostic contains: check is redundant
    // remove this line
    Preconditions.checkNotNull(message.getMessage(), new Object());
    // BUG: Diagnostic contains: check is redundant
    // remove this line
    Preconditions.checkNotNull(message.getMultiFieldList(), new Object());

    // BUG: Diagnostic contains: check is redundant
    // remove this line
    Preconditions.checkNotNull(message.getMessage(), "%s", new Object());
    // BUG: Diagnostic contains: check is redundant
    // remove this line
    Preconditions.checkNotNull(message.getMultiFieldList(), "%s", new Object());

    // BUG: Diagnostic contains: fieldMessage = message.getMessage();
    TestFieldProtoMessage fieldMessage = Preconditions.checkNotNull(message.getMessage());

    // BUG: Diagnostic contains: fieldMessage2 = message.getMessage()
    TestFieldProtoMessage fieldMessage2 = Preconditions.checkNotNull(message.getMessage(), "Msg");

    // BUG: Diagnostic contains: message.getMessage().toString();
    Preconditions.checkNotNull(message.getMessage()).toString();
    // BUG: Diagnostic contains: message.getMessage().toString();
    Preconditions.checkNotNull(message.getMessage(), "Message").toString();
  }
}

{% endhighlight %}

