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

/**
 * Extracted minimal implementation from generated code of a proto enum.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
package com.google.errorprone.bugpatterns.proto;

/** Protobuf enum {@code com.google.errorprone.bugpatterns.TestEnum} */
public enum TestEnum implements com.google.protobuf.ProtocolMessageEnum {
  /** <code>TEST_ENUM_UNKNOWN = 0;</code> */
  TEST_ENUM_UNKNOWN(0),
  /** <code>TEST_ENUM_VAL = 1;</code> */
  TEST_ENUM_VAL(1),
  ;

  /** <code>TEST_ENUM_UNKNOWN = 0;</code> */
  public static final int TEST_ENUM_UNKNOWN_VALUE = 0;
  /** <code>TEST_ENUM_VAL = 1;</code> */
  public static final int TEST_ENUM_VAL_VALUE = 1;

  public final int getNumber() {
    return value;
  }

  public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
    return null;
  }

  public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
    return null;
  }

  private final int value;

  private TestEnum(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:com.google.errorprone.bugpatterns.TestEnum)
}
