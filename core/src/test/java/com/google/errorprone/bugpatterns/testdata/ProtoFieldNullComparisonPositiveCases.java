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

import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

/** Positive examples for invalid null comparison of a proto message field. */
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
