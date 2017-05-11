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

import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

/** Negative examples for invalid null comparison of a proto message field. */
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
        Object reference, String errorMessageTemplate, Object... errorMessageArgs) {}
  }
}
