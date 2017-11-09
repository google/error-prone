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

import com.google.common.html.types.SafeHtmlProto;
import com.google.protobuf.ByteString;

public class PrivateSecurityContractProtoAccessPositiveCases {
  static SafeHtmlProto safeHtmlProto;

  static {
    safeHtmlProto =
        SafeHtmlProto.newBuilder()
            // BUG: Diagnostic contains: Forbidden access to a private proto field
            .clearPrivateDoNotAccessOrElseSafeHtmlWrappedValue()
            // BUG: Diagnostic contains: Forbidden access to a private proto field
            .setPrivateDoNotAccessOrElseSafeHtmlWrappedValue("foo")
            .build();
  }

  static SafeHtmlProto safeHtmlProtoFromBytes;

  static {
    safeHtmlProtoFromBytes =
        SafeHtmlProto.newBuilder()
            // BUG: Diagnostic contains: Forbidden access to a private proto field
            .setPrivateDoNotAccessOrElseSafeHtmlWrappedValueBytes(ByteString.copyFromUtf8("foo"))
            .build();
  }

  static String readSafeHtmlProto(SafeHtmlProto safeHtmlProto) {
    // BUG: Diagnostic contains: Forbidden access to a private proto field
    if (safeHtmlProto.hasPrivateDoNotAccessOrElseSafeHtmlWrappedValue()) {
      // BUG: Diagnostic contains: Forbidden access to a private proto field
      return safeHtmlProto.getPrivateDoNotAccessOrElseSafeHtmlWrappedValue();
    }
    return "";
  }

  static ByteString readSafeHtmlProtoBytes(SafeHtmlProto safeHtmlProto) {
    // BUG: Diagnostic contains: Forbidden access to a private proto field
    return safeHtmlProto.getPrivateDoNotAccessOrElseSafeHtmlWrappedValueBytes();
  }

  static String readSafeHtmlProtoBuilder(SafeHtmlProto.Builder safeHtmlProto) {
    // BUG: Diagnostic contains: Forbidden access to a private proto field
    if (safeHtmlProto.hasPrivateDoNotAccessOrElseSafeHtmlWrappedValue()) {
      // BUG: Diagnostic contains: Forbidden access to a private proto field
      return safeHtmlProto.getPrivateDoNotAccessOrElseSafeHtmlWrappedValue();
    }
    return "";
  }

  static ByteString readSafeHtmlProtoBuilderBytes(SafeHtmlProto.Builder safeHtmlProto) {
    // BUG: Diagnostic contains: Forbidden access to a private proto field
    return safeHtmlProto.getPrivateDoNotAccessOrElseSafeHtmlWrappedValueBytes();
  }
}
