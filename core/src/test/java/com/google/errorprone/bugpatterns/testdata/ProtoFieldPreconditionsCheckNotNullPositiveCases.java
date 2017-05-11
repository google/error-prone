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

package com.google.errorprone.bugpatterns.testdata;

import com.google.common.base.Preconditions;
import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

/**
 * Positive examples for checking a proto message field using {@link
 * Preconditions#checkNotNull(Object)} and related methods.
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
